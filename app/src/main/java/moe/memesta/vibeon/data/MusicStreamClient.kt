package moe.memesta.vibeon.data

import androidx.compose.runtime.Immutable
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import moe.memesta.vibeon.data.stats.PlaybackEvent

/**
 * Data classes for library browsing
 */
@Immutable
data class TrackInfo(
    val path: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Double,
    val coverUrl: String? = null,
    val discNumber: Int? = null,
    val trackNumber: Int? = null,
    val titleRomaji: String? = null,
    val titleEn: String? = null,
    val artistRomaji: String? = null,
    val artistEn: String? = null,
    val albumRomaji: String? = null,
    val albumEn: String? = null,
    val playlistTrackId: Long? = null
)

@Immutable
data class LibraryResponse(
    val tracks: List<TrackInfo>,
    val total: Int
)

@Immutable
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
    suspend fun getServerInfo(): ServerInfo? = withContext(Dispatchers.IO) {
        try {
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
    }
    
    /**
     * Browse library with pagination
     */
    suspend fun browseLibrary(offset: Int = 0, limit: Int = 50): LibraryResponse? = withContext(Dispatchers.IO) {
        try {
            _isLoading.value = true
            _error.value = null
            
            val url = "$baseUrl/api/library?offset=$offset&limit=$limit"
            Log.i("MusicStreamClient", "URL: $url")
            
            val request = Request.Builder()
                .url(url)
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            
            if (response.isSuccessful) {
                val bodyString = response.body?.string() ?: "{}"
                val json = JSONObject(bodyString)
                val tracksArray = json.getJSONArray("tracks")
                val tracks = mutableListOf<TrackInfo>()
                
                for (i in 0 until tracksArray.length()) {
                    val track = tracksArray.getJSONObject(i)
                    var coverUrl = track.optString("coverUrl", null)?.takeIf { it != "null" && it.isNotEmpty() }
                        ?: track.optString("cover_url", null)?.takeIf { it != "null" && it.isNotEmpty() }
                    
                    if (coverUrl != null && !coverUrl.startsWith("http")) {
                        coverUrl = if (coverUrl.startsWith("/")) {
                            "$baseUrl$coverUrl"
                        } else {
                            "$baseUrl/$coverUrl"
                        }
                    }
                    tracks.add(
                        TrackInfo(
                            path = track.getString("path"),
                            title = track.getString("title"),
                            artist = track.getString("artist"),
                            album = track.getString("album"),
                            duration = track.optDouble("durationSecs", 0.0),
                            coverUrl = coverUrl,
                            discNumber = track.optInt("discNumber", -1).takeIf { it != -1 },
                            trackNumber = track.optInt("trackNumber", -1).takeIf { it != -1 },
                            titleRomaji = track.optString("titleRomaji", null).takeIf { it != "null" },
                            titleEn = track.optString("titleEn", null).takeIf { it != "null" },
                            artistRomaji = track.optString("artistRomaji", null).takeIf { it != "null" },
                            artistEn = track.optString("artistEn", null).takeIf { it != "null" },
                            albumRomaji = track.optString("albumRomaji", null).takeIf { it != "null" },
                            albumEn = track.optString("albumEn", null).takeIf { it != "null" },
                            playlistTrackId = track.optLong("playlistTrackId", -1L).takeIf { it != -1L }
                        )
                    )
                }
                
                val total = json.optInt("total", tracks.size)
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
    }
    
    /**
     * Search library for tracks
     */
    suspend fun searchLibrary(query: String, offset: Int = 0, limit: Int = 50): List<TrackInfo>? = withContext(Dispatchers.IO) {
        try {
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
                    val coverUrl = track.optString("coverUrl", null)
                    tracks.add(
                        TrackInfo(
                            path = track.getString("path"),
                            title = track.getString("title"),
                            artist = track.getString("artist"),
                            album = track.getString("album"),
                            duration = track.optDouble("durationSecs", 0.0),
                            coverUrl = coverUrl,
                            discNumber = track.optInt("discNumber", -1).takeIf { it != -1 },
                            trackNumber = track.optInt("trackNumber", -1).takeIf { it != -1 },
                            titleRomaji = track.optString("titleRomaji", null).takeIf { it != "null" },
                            titleEn = track.optString("titleEn", null).takeIf { it != "null" },
                            artistRomaji = track.optString("artistRomaji", null).takeIf { it != "null" },
                            artistEn = track.optString("artistEn", null).takeIf { it != "null" },
                            albumRomaji = track.optString("albumRomaji", null).takeIf { it != "null" },
                            albumEn = track.optString("albumEn", null).takeIf { it != "null" },
                            playlistTrackId = track.optLong("playlistTrackId", -1L).takeIf { it != -1L }
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
    suspend fun getStats(): LibraryStats? = withContext(Dispatchers.IO) {
        try {
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
    }

    suspend fun getPlaybackEvents(startMs: Long? = null, endMs: Long? = null): List<PlaybackEvent>? = withContext(Dispatchers.IO) {
        try {
            val queryParams = buildList {
                startMs?.let { add("startMs=$it") }
                endMs?.let { add("endMs=$it") }
            }.joinToString("&")
            val url = if (queryParams.isNotBlank()) {
                "$baseUrl/api/stats/events?$queryParams"
            } else {
                "$baseUrl/api/stats/events"
            }
            val request = Request.Builder()
                .url(url)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonText = response.body?.string().orEmpty()
                val jsonArray = JSONArray(jsonText)
                val events = mutableListOf<PlaybackEvent>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    events.add(
                        PlaybackEvent(
                            songId = obj.optString("songId"),
                            timestamp = obj.optLong("timestamp"),
                            durationMs = obj.optLong("durationMs"),
                            startTimestamp = obj.optLong("startTimestamp").takeIf { obj.has("startTimestamp") },
                            endTimestamp = obj.optLong("endTimestamp").takeIf { obj.has("endTimestamp") },
                            output = obj.optString("output", "desktop")
                        )
                    )
                }
                events
            } else {
                Log.e("MusicStreamClient", "❌ Failed to get playback events: ${response.code}")
                null
            }
        } catch (e: Exception) {
            Log.e("MusicStreamClient", "Error getting playback events: ${e.message}", e)
            null
        }
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
    suspend fun getTrackMetadata(trackPath: String): TrackInfo? = withContext(Dispatchers.IO) {
        try {
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
                            duration = track.optDouble("durationSecs", 0.0),
                            coverUrl = track.optString("coverUrl", null),
                            discNumber = track.optInt("discNumber", -1).takeIf { it != -1 },
                            trackNumber = track.optInt("trackNumber", -1).takeIf { it != -1 },
                            titleRomaji = track.optString("titleRomaji", null).takeIf { it != "null" },
                            titleEn = track.optString("titleEn", null).takeIf { it != "null" },
                            artistRomaji = track.optString("artistRomaji", null).takeIf { it != "null" },
                            artistEn = track.optString("artistEn", null).takeIf { it != "null" },
                            albumRomaji = track.optString("albumRomaji", null).takeIf { it != "null" },
                            albumEn = track.optString("albumEn", null).takeIf { it != "null" },
                            playlistTrackId = track.optLong("playlistTrackId", -1L).takeIf { it != -1L }
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
