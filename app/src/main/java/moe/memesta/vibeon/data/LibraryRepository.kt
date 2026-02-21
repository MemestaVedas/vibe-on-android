package moe.memesta.vibeon.data

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    private val streamClient = MusicStreamClient(host, port)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _syncStatus = MutableStateFlow(SyncStatus())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    // Expose tracks from DB as the single source of truth
    // Dynamically inject the current baseUrl for relative coverUrls
    val tracks: Flow<List<TrackInfo>> = trackDao.getAllTracks().map { entities ->
        entities.map { entity ->
            val finalCoverUrl = entity.albumArtUrl?.let { url ->
                if (url.startsWith("/")) "$baseUrl$url" else url
            }
            TrackInfo(
                path = entity.id,
                title = entity.title,
                artist = entity.artist,
                album = entity.album,
                duration = entity.duration,
                coverUrl = finalCoverUrl,
                titleRomaji = entity.titleRomaji,
                titleEn = entity.titleEn,
                artistRomaji = entity.artistRomaji,
                artistEn = entity.artistEn,
                albumRomaji = entity.albumRomaji,
                albumEn = entity.albumEn
            )
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

            // Always request via WS if connected (async — response handled by init collector)
            if (wsClient.isConnected.value) {
                wsClient.sendGetLibrary()
            }

            // Always also fetch via HTTP as the reliable synchronous path
            val response = streamClient.browseLibrary(0, 10000)
            response?.let {
                saveTracksToDb(it.tracks)
            }
        } catch (e: Exception) {
            Log.e("LibraryRepository", "❌ Error refreshing library: ${e.message}")
        }
    }
    
    suspend fun saveTracksToDb(tracks: List<TrackInfo>) {
        if (tracks.isEmpty()) return
        
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
            val entities = chunk.map { track ->
                // Ensure we store relative paths in DB to handle base URL changes
                val relativeCoverUrl = track.coverUrl?.let { url ->
                    if (url.startsWith("http")) {
                        // Strip origin, keep path (e.g. "/cover/...")
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
                    year = null,
                    genre = null,
                    trackNumber = null,
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
        // Briefly show 100% then clear
        _syncStatus.value = _syncStatus.value.copy(progress = 1f, statusText = "Sync complete!")
        kotlinx.coroutines.delay(1500)
        _syncStatus.value = SyncStatus()
    }

    suspend fun searchTracks(): List<TrackInfo> {
        return emptyList()
    }
    
    // Pass-through for stats
    suspend fun getStats() = streamClient.getStats()
}
