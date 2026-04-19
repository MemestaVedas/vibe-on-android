package moe.memesta.vibeon.data

import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.system.measureTimeMillis
import moe.memesta.vibeon.data.local.TrackDao
import moe.memesta.vibeon.data.local.TrackEntity

data class SyncStatus(
    val isSyncing: Boolean = false,
    val progress: Float = 0f, // 0.0 to 1.0
    val totalTracks: Int = 0,
    val syncedTracks: Int = 0,
    val statusText: String = ""
)

class LibraryRepository(
    private val trackDao: TrackDao,
    private val wsClient: WebSocketClient,
    private val host: String,
    private val port: Int
) {
    companion object {
        private const val HTTP_PAGE_SIZE = 500
    }

    private val streamClient = MusicStreamClient(host, port)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _syncStatus = MutableStateFlow(SyncStatus())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    // Expose tracks from DB as the single source of truth (deduped by canonicalId)
    // Dynamically inject the current baseUrl for relative coverUrls
    val tracks: Flow<List<TrackInfo>> = trackDao.getTracksDeduped().map { entities ->
        entities.map(::toTrackInfo)
    }

    fun getPagedTracks(query: String, sortOption: SortOption): Flow<PagingData<TrackInfo>> {
        val normalizedQuery = query.trim()
        return Pager(
            config = PagingConfig(
                pageSize = 60,
                prefetchDistance = 30,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                trackDao.getTracksDedupedPaging(
                    query = normalizedQuery,
                    sortKey = sortOption.storageKey
                )
            }
        ).flow.map { pagingData ->
            pagingData.map(::toTrackInfo)
        }
    }

    val baseUrl: String = streamClient.getBaseUrl()

    init {
        // Watch WS library updates and persist them to DB automatically
        scope.launch {
            wsClient.library.collect { tracks ->
                if (tracks.isNotEmpty()) {
                    Log.i("LibraryRepository", "📡 WS library update: ${tracks.size} tracks → saving to DB")
                    saveTracksToDb(tracks)
                }
            }
        }
    }

    suspend fun refreshLibrary() {
        try {
            Log.i("LibraryRepository", "🔄 Refreshing library from $host:$port...")
            val elapsedMs = measureTimeMillis {
                // Prefer WebSocket sync when connected to avoid duplicate full HTTP fetch.
                if (wsClient.isConnected.value) {
                    wsClient.sendGetLibrary()
                    return
                }

                refreshLibraryViaHttpPaging()
            }
            Log.i("LibraryRepository", "✅ Refresh flow completed in ${elapsedMs}ms")
        } catch (e: Exception) {
            Log.e("LibraryRepository", "❌ Error refreshing library: ${e.message}")
        }
    }

    private suspend fun refreshLibraryViaHttpPaging() {
        var offset = 0
        var total = 0
        var synced = 0

        _syncStatus.value = SyncStatus(
            isSyncing = true,
            progress = 0f,
            totalTracks = 0,
            syncedTracks = 0,
            statusText = "Syncing library..."
        )

        while (true) {
            val response = streamClient.browseLibrary(offset = offset, limit = HTTP_PAGE_SIZE) ?: break
            val page = response.tracks
            if (page.isEmpty()) break

            if (total <= 0) {
                total = response.total.coerceAtLeast(page.size)
            }

            persistTrackChunk(page)
            synced += page.size
            offset += page.size

            val boundedSynced = min(synced, total)
            _syncStatus.value = _syncStatus.value.copy(
                totalTracks = total,
                syncedTracks = boundedSynced,
                progress = if (total > 0) boundedSynced.toFloat() / total else 0f,
                statusText = "Synced $boundedSynced of $total tracks"
            )

            if (page.size < HTTP_PAGE_SIZE || boundedSynced >= total) {
                break
            }
        }

        _syncStatus.value = SyncStatus()
    }
    
    suspend fun saveTracksToDb(tracks: List<TrackInfo>) = withContext(Dispatchers.IO) {
        if (tracks.isEmpty()) return@withContext
        
        Log.i("LibraryRepository", "💾 Saving ${tracks.size} tracks to database...")
        _syncStatus.value = SyncStatus(
            isSyncing = true,
            progress = 0f,
            totalTracks = tracks.size,
            syncedTracks = 0,
            statusText = "Syncing library..."
        )

        val total = tracks.size
        // We don't call clearAll() here because insertTracks uses REPLACE strategy, 
        // and clearing would cause the UI to blink empty while inserting.
        tracks.chunked(100).forEachIndexed { index, chunk ->
            persistTrackChunk(chunk)
            
            val syncedCount = (index + 1) * 100
            val currentSynced = if (syncedCount > total) total else syncedCount
            val progress = currentSynced.toFloat() / total
            
            _syncStatus.value = _syncStatus.value.copy(
                progress = progress,
                syncedTracks = currentSynced,
                statusText = "Synced $currentSynced of $total tracks"
            )
        }

        Log.i("LibraryRepository", "✅ Saved $total tracks to DB")
        _syncStatus.value = SyncStatus()
    }

    private suspend fun persistTrackChunk(tracks: List<TrackInfo>) {
        val entities = tracks.map { track ->
            // Store relative cover paths to survive host/ip changes.
            val relativeCoverUrl = track.coverUrl?.let { url ->
                if (url.startsWith("http")) {
                    try {
                        val uri = java.net.URI(url)
                        uri.path
                    } catch (e: Exception) {
                        url
                    }
                } else {
                    url
                }
            }

            TrackEntity(
                id = track.path,
                title = track.title,
                artist = track.artist,
                album = track.album,
                duration = track.duration,
                albumArtUrl = relativeCoverUrl,
                albumMainColor = track.albumMainColor,
                year = track.year ?: inferYearFromPath(track.path, track.album),
                source = "pc",  // Mark as coming from PC
                canonicalId = TrackEntity.generateCanonicalId(track.title, track.artist, track.album),
                genre = null,
                trackNumber = track.trackNumber,
                discNumber = track.discNumber,
                titleRomaji = track.titleRomaji,
                titleEn = track.titleEn,
                artistRomaji = track.artistRomaji,
                artistEn = track.artistEn,
                albumRomaji = track.albumRomaji,
                albumEn = track.albumEn,
                lastUpdated = System.currentTimeMillis()
            )
        }
        trackDao.insertTracks(entities)
    }

    private fun inferYearFromPath(path: String, album: String): Int? {
        val regex = Regex("(19|20)\\d{2}")
        val pathYear = regex.find(path)?.value?.toIntOrNull()
        if (pathYear in 1900..2099) return pathYear
        val albumYear = regex.find(album)?.value?.toIntOrNull()
        return albumYear?.takeIf { it in 1900..2099 }
    }

    suspend fun searchTracks(): List<TrackInfo> {
        return emptyList()
    }
    
    // Pass-through for stats
    suspend fun getStats() = streamClient.getStats()

    suspend fun getPlaybackEvents(startMs: Long? = null, endMs: Long? = null) =
        streamClient.getPlaybackEvents(startMs, endMs)

    private fun toTrackInfo(entity: TrackEntity): TrackInfo {
        val finalCoverUrl = entity.albumArtUrl?.let { url ->
            if (url.startsWith("/")) "$baseUrl$url" else url
        }
        return TrackInfo(
            path = entity.id,
            title = entity.title,
            artist = entity.artist,
            album = entity.album,
            duration = entity.duration,
            coverUrl = finalCoverUrl,
            albumMainColor = entity.albumMainColor,
            year = entity.year,
            titleRomaji = entity.titleRomaji,
            titleEn = entity.titleEn,
            artistRomaji = entity.artistRomaji,
            artistEn = entity.artistEn,
            albumRomaji = entity.albumRomaji,
            albumEn = entity.albumEn,
            discNumber = entity.discNumber,
            trackNumber = entity.trackNumber
        )
    }
}
