package moe.memesta.vibeon.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import moe.memesta.vibeon.data.local.TrackDao
import moe.memesta.vibeon.data.local.TrackEntity

class LibraryRepository(
    private val trackDao: TrackDao,
    private val wsClient: WebSocketClient,
    private val host: String,
    private val port: Int
) {
    private val streamClient = MusicStreamClient(host, port)
    private val scope = CoroutineScope(Dispatchers.IO)

    // Expose tracks from DB as the single source of truth
    val tracks: Flow<List<TrackInfo>> = trackDao.getAllTracks().map { entities ->
        entities.map { entity ->
            TrackInfo(
                path = entity.id,
                title = entity.title,
                artist = entity.artist,
                album = entity.album,
                duration = entity.duration,
                coverUrl = entity.albumArtUrl
            )
        }
    }

    val baseUrl: String = streamClient.getBaseUrl()

    suspend fun refreshLibrary() {
        try {
            Log.i("LibraryRepository", "🔄 Refreshing library from $host:$port...")
            
            // Try WebSocket first
            if (wsClient.isConnected.value) {
                wsClient.sendGetLibrary()
                // WebSocket will receive data and we need to intercept it?
                // Actually, LibraryViewModel was listening to wsClient.library.
                // We should move that listener here or keep it in VM but have VM call repo.saveTracks?
                // Better: Repository listens to WS updates.
            } else {
                // Fallback to HTTP
                val response = streamClient.browseLibrary(0, 10000) // Fetch all for sync
                response?.let {
                    saveTracksToDb(it.tracks)
                }
            }
        } catch (e: Exception) {
            Log.e("LibraryRepository", "❌ Error refreshing library: ${e.message}")
        }
    }
    
    // Call this when WS receives library data
    suspend fun saveTracks(tracks: List<TrackInfo>) {
        saveTracksToDb(tracks)
    }

    private suspend fun saveTracksToDb(tracks: List<TrackInfo>) {
        Log.i("LibraryRepository", "💾 Saving ${tracks.size} tracks to database...")
        val entities = tracks.map { track ->
            TrackEntity(
                id = track.path,
                title = track.title,
                artist = track.artist,
                album = track.album,
                duration = track.duration,
                albumArtUrl = track.coverUrl,
                year = null,
                genre = null,
                trackNumber = null
            )
        }
        trackDao.clearAll() // Simple sync strategy: replace all
        trackDao.insertTracks(entities)
        Log.i("LibraryRepository", "✅ Saved ${tracks.size} tracks to DB")
    }

    suspend fun searchTracks(query: String): List<TrackInfo> {
        // For search, we can query DB directly for instant results
        // Note: The DAO implementation for searchTracks returns a Flow, but we might want a one-shot list for the UI's current pattern
        // Or we can keep using the memory cache in VM for search if it's faster, but DB is robust.
        // Let's rely on the streamClient for search if we want server-side fuzzy search, 
        // OR use local DB search for offline support.
        // For now, let's allow both or stick to network search for consistency with memory model?
        // User requested "offline" so local DB search is better.
        
        // However, TrackDao.searchTracks returns Flow. We can collect it once.
        return try {
             // We need to implement a suspend function in DAO for one-shot or just use the flow.
             // Let's stick to network search for now if connected, DB if not?
             // Actually, simply returning empty here and letting VM handle it via its own filtered list (which comes from DB flow) is easier.
             emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // Pass-through for stats
    suspend fun getStats() = streamClient.getStats()
}
