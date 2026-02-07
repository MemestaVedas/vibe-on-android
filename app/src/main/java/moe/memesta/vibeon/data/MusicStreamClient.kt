package moe.memesta.vibeon.data

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Data classes for library browsing
 */
data class TrackInfo(
    val path: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Double,
    val coverUrl: String? = null
)

data class LibraryResponse(
    val tracks: List<TrackInfo>,
    val total: Int
)

data class ServerInfo(
    val name: String,
    val version: String,
    val librarySize: Int,
    val localIp: String? = null
)

/**
 * HTTP client for browsing library and streaming audio from PC
 */
class MusicStreamClient(
    private val host: String,
    private val port: Int = 5000
) {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    private val baseUrl = "http://$host:$port"
    
    // Expose baseUrl for album art loading
    fun getBaseUrl(): String = baseUrl
    
    private val _libraryTracks = MutableStateFlow<List<TrackInfo>>(emptyList())
    val libraryTracks: StateFlow<List<TrackInfo>> = _libraryTracks.asStateFlow()
    
    private val _totalTracks = MutableStateFlow(0)
    val totalTracks: StateFlow<Int> = _totalTracks.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    /**
     * Get server information
     */
    suspend fun getServerInfo(): ServerInfo? = try {
        val request = Request.Builder()
            .url("$baseUrl/api/info")
            .build()
        
        val response = okHttpClient.newCall(request).execute()
        if (response.isSuccessful) {
            val json = JSONObject(response.body?.string() ?: "{}")
            ServerInfo(
                name = json.getString("name"),
                version = json.getString("version"),
                librarySize = json.getInt("librarySize"),
                localIp = json.optString("localIp", null)
            ).also {
                Log.d("MusicStreamClient", "✅ Server info: ${it.name} v${it.version} with ${it.librarySize} tracks")
            }
        } else {
            Log.e("MusicStreamClient", "❌ Failed to get server info: ${response.code}")
            null
        }
    } catch (e: Exception) {
        Log.e("MusicStreamClient", "Error getting server info: ${e.message}", e)
        null
    }
    
    /**
     * Browse library with pagination
     */
    suspend fun browseLibrary(offset: Int = 0, limit: Int = 50): LibraryResponse? = try {
        _isLoading.value = true
        _error.value = null
        
        val url = "$baseUrl/api/library?offset=$offset&limit=$limit"
        Log.i("MusicStreamClient", "")
        Log.i("MusicStreamClient", "═══════════════════════════════")
        Log.i("MusicStreamClient", "📡 Attempting connection...")
        Log.i("MusicStreamClient", "   Host: $host")
        Log.i("MusicStreamClient", "   Port: $port")
        Log.i("MusicStreamClient", "   URL: $url")
        Log.i("MusicStreamClient", "═══════════════════════════════")
        
        val request = Request.Builder()
            .url(url)
            .build()
        
        Log.i("MusicStreamClient", "⏳ Sending HTTP request...")
        val response = okHttpClient.newCall(request).execute()
        
        Log.i("MusicStreamClient", "📥 Response code: ${response.code}")
        
        if (response.isSuccessful) {
            val bodyString = response.body?.string() ?: "{}"
            Log.d("MusicStreamClient", "Response body size: ${bodyString.length} bytes")
            
            val json = JSONObject(bodyString)
            val tracksArray = json.getJSONArray("tracks")
            val tracks = mutableListOf<TrackInfo>()
            
            for (i in 0 until tracksArray.length()) {
                val track = tracksArray.getJSONObject(i)
                var coverUrl = track.optString("coverUrl", null)
                if (coverUrl != null && !coverUrl.startsWith("http")) {
                    coverUrl = "$baseUrl$coverUrl"
                }
                tracks.add(
                    TrackInfo(
                        path = track.getString("path"),
                        title = track.getString("title"),
                        artist = track.getString("artist"),
                        album = track.getString("album"),
                        duration = track.getDouble("durationSecs"),
                        coverUrl = coverUrl
                    )
                )
            }
            
            val total = json.getInt("total")
            _libraryTracks.value = tracks
            _totalTracks.value = total
            
            Log.i("MusicStreamClient", "✅ Successfully loaded ${tracks.size} tracks (total: $total)")
            
            LibraryResponse(tracks, total)
        } else {
            val errorMsg = "HTTP Error ${response.code}: ${response.message}"
            _error.value = errorMsg
            Log.e("MusicStreamClient", "❌ $errorMsg\nURL: $url")
            null
        }
    } catch (e: java.net.ConnectException) {
        val errorMsg = "Cannot connect to $host:$port\n\n• Is the desktop app running?\n• Are you on the same WiFi?\n• Is the IP address correct?"
        _error.value = errorMsg
        Log.e("MusicStreamClient", "❌ Connection refused: ${e.message}")
        null
    } catch (e: java.net.UnknownHostException) {
        val errorMsg = "Cannot resolve host: $host\n\nCheck if the IP address is correct."
        _error.value = errorMsg
        Log.e("MusicStreamClient", "❌ Unknown host: ${e.message}")
        null
    } catch (e: java.net.SocketTimeoutException) {
        val errorMsg = "Connection timeout\n\nThe server is not responding. Check your network connection."
        _error.value = errorMsg
        Log.e("MusicStreamClient", "❌ Timeout: ${e.message}")
        null
    } catch (e: Exception) {
        val errorMsg = "Network error: ${e.javaClass.simpleName}\n${e.message}"
        _error.value = errorMsg
        Log.e("MusicStreamClient", "❌ Error: ${e.message}", e)
        null
    } finally {
        _isLoading.value = false
    }
    
    /**
     * Search library for tracks
     */
    suspend fun searchLibrary(query: String, offset: Int = 0, limit: Int = 50): List<TrackInfo>? = try {
        _isLoading.value = true
        _error.value = null
        
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "$baseUrl/api/library/search?q=$encodedQuery&offset=$offset&limit=$limit"
        val request = Request.Builder()
            .url(url)
            .build()
        
        val response = okHttpClient.newCall(request).execute()
        if (response.isSuccessful) {
            val json = JSONObject(response.body?.string() ?: "{}")
            val tracksArray = json.getJSONArray("tracks")
            val tracks = mutableListOf<TrackInfo>()
            
            for (i in 0 until tracksArray.length()) {
                val track = tracksArray.getJSONObject(i)
                var coverUrl = track.optString("coverUrl", null)
                if (coverUrl != null && !coverUrl.startsWith("http")) {
                    coverUrl = "$baseUrl$coverUrl"
                }
                tracks.add(
                    TrackInfo(
                        path = track.getString("path"),
                        title = track.getString("title"),
                        artist = track.getString("artist"),
                        album = track.getString("album"),
                        duration = track.getDouble("durationSecs"),
                        coverUrl = coverUrl
                    )
                )
            }
            
            Log.d("MusicStreamClient", "✅ Found ${tracks.size} tracks matching '$query'")
            tracks
        } else {
            val errorMsg = "Failed to search library: ${response.code}"
            _error.value = errorMsg
            Log.e("MusicStreamClient", "❌ $errorMsg")
            null
        }
    } catch (e: Exception) {
        val errorMsg = "Error searching library: ${e.message}"
        _error.value = errorMsg
        Log.e("MusicStreamClient", errorMsg, e)
        null
    } finally {
        _isLoading.value = false
    }
    
    /**
     * Get stream URL for a track
     * The URL can be used directly with Android's MediaPlayer or ExoPlayer
     */
    fun getStreamUrl(trackPath: String): String {
        val encodedPath = java.net.URLEncoder.encode(trackPath, "UTF-8")
        return "$baseUrl/stream/$encodedPath"
    }
    
    /**
     * Get library statistics
     */
    suspend fun getStats(): LibraryStats? = try {
        val url = "$baseUrl/api/stats"
        val request = Request.Builder()
            .url(url)
            .build()
        
        val response = okHttpClient.newCall(request).execute()
        if (response.isSuccessful) {
            val json = JSONObject(response.body?.string() ?: "{}")
            LibraryStats(
                totalSongs = json.getInt("totalSongs"),
                totalAlbums = json.getInt("totalAlbums"),
                totalArtists = json.getInt("totalArtists"),
                totalDurationHours = json.getDouble("totalDurationHours")
            )
        } else {
            Log.e("MusicStreamClient", "❌ Failed to get stats: ${response.code}")
            null
        }
    } catch (e: Exception) {
        Log.e("MusicStreamClient", "Error getting stats: ${e.message}", e)
        null
    }
    
    /**
     * Get cover URL for a track
     */
    fun getCoverUrl(trackPath: String): String {
        val encodedPath = java.net.URLEncoder.encode(trackPath, "UTF-8")
        return "$baseUrl/cover/$encodedPath"
    }
    
    /**
     * Download metadata for a specific track
     */
    suspend fun getTrackMetadata(trackPath: String): TrackInfo? {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/api/library")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "{}")
                val tracksArray = json.getJSONArray("tracks")
                
                var foundTrack: TrackInfo? = null
                for (i in 0 until tracksArray.length()) {
                    val track = tracksArray.getJSONObject(i)
                    if (track.getString("path") == trackPath) {
                        foundTrack = TrackInfo(
                            path = track.getString("path"),
                            title = track.getString("title"),
                            artist = track.getString("artist"),
                            album = track.getString("album"),
                            duration = track.getDouble("durationSecs"),
                            coverUrl = track.optString("coverUrl", null)
                        )
                        break
                    }
                }
                foundTrack
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("MusicStreamClient", "Error getting track metadata: ${e.message}", e)
            null
        }
    }
}
