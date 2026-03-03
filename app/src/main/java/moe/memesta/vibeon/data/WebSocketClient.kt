package moe.memesta.vibeon.data

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import org.json.JSONObject

data class MediaSessionData(
    val title: String = "No Track",
    val artist: String = "Unknown Artist",
    val album: String = "",
    val duration: Double = 0.0,
    val coverUrl: String? = null,
    val lyrics: String = "",
    val titleRomaji: String? = null,
    val titleEn: String? = null,
    val artistRomaji: String? = null,
    val artistEn: String? = null,
    val albumRomaji: String? = null,
    val albumEn: String? = null,
    val path: String = ""
)

data class QueueItem(
    val path: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Double,
    val coverUrl: String? = null,
    val titleRomaji: String? = null,
    val titleEn: String? = null,
    val artistRomaji: String? = null,
    val artistEn: String? = null,
    val albumRomaji: String? = null,
    val albumEn: String? = null
)

data class LyricsData(
    val trackPath: String = "",
    val hasSynced: Boolean = false,
    val syncedLyrics: String? = null,
    val syncedLyricsRomaji: String? = null,
    val plainLyrics: String? = null,
    val instrumental: Boolean = false
)

data class PlaylistInfo(
    val id: String,
    val name: String,
    val trackCount: Int,
    val createdAt: String = "",
    val updatedAt: String = ""
)

class WebSocketClient {
    private val okHttpClient = OkHttpClient.Builder()
        .pingInterval(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    private var webSocket: WebSocket? = null
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    var host: String = "localhost"
        private set
    var port: Int = 5000
        private set
    private var baseUrl: String = ""

    
    private val _messages = MutableStateFlow<String>("")
    val messages: StateFlow<String> = _messages.asStateFlow()
    
    private val _currentTrack = MutableStateFlow(MediaSessionData())
    val currentTrack: StateFlow<MediaSessionData> = _currentTrack.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _queue = MutableStateFlow<List<QueueItem>>(emptyList())
    val queue: StateFlow<List<QueueItem>> = _queue.asStateFlow()
    
    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()
    
    private val _progress = MutableStateFlow(0.0)
    val progress: StateFlow<Double> = _progress.asStateFlow()
    
    private val _duration = MutableStateFlow(0.0)
    val duration: StateFlow<Double> = _duration.asStateFlow()
    
    private val _streamUrl = MutableStateFlow<String?>(null)
    val streamUrl: StateFlow<String?> = _streamUrl.asStateFlow()
    
    private val _handoffPosition = MutableStateFlow(0.0)
    val handoffPosition: StateFlow<Double> = _handoffPosition.asStateFlow()
    
    private val _isMobilePlayback = MutableStateFlow(false)
    val isMobilePlayback: StateFlow<Boolean> = _isMobilePlayback.asStateFlow()

    private val _library = MutableStateFlow<List<TrackInfo>>(emptyList())
    val library: StateFlow<List<TrackInfo>> = _library.asStateFlow()
    
    private val _lyrics = MutableStateFlow<LyricsData?>(null)
    val lyrics: StateFlow<LyricsData?> = _lyrics.asStateFlow()
    
    private val _isLoadingLyrics = MutableStateFlow(false)
    val isLoadingLyrics: StateFlow<Boolean> = _isLoadingLyrics.asStateFlow()
    
    private var lastTrackId: String? = null
    
    // Shuffle, Repeat, Volume, Favorites
    private val _isShuffled = MutableStateFlow(false)
    val isShuffled: StateFlow<Boolean> = _isShuffled.asStateFlow()
    
    private val _repeatMode = MutableStateFlow("off")
    val repeatMode: StateFlow<String> = _repeatMode.asStateFlow()
    
    private val _volume = MutableStateFlow(0.5)
    val volume: StateFlow<Double> = _volume.asStateFlow()
    
    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()
    
    // Playlists
    private val _playlists = MutableStateFlow<List<PlaylistInfo>>(emptyList())
    val playlists: StateFlow<List<PlaylistInfo>> = _playlists.asStateFlow()
    
    private val _currentPlaylistTracks = MutableStateFlow<List<TrackInfo>>(emptyList())
    val currentPlaylistTracks: StateFlow<List<TrackInfo>> = _currentPlaylistTracks.asStateFlow()

    private val _statsUpdated = MutableStateFlow(0L)
    val statsUpdated: StateFlow<Long> = _statsUpdated.asStateFlow()
    
    fun connect(host: String, port: Int, clientName: String = "Android") {
        this.host = host
        this.port = port
        baseUrl = "http://$host:$port"

        val wsUrl = "ws://$host:$port/control"
        Log.d("WebSocket", "Connecting to $wsUrl")
        
        val request = Request.Builder()
            .url(wsUrl)
            .build()
        
        webSocket = okHttpClient.newWebSocket(request, VibeonWebSocketListener(this, clientName))
    }
    
    fun getLyrics() {
        val message = JSONObject().apply {
            put("type", "getLyrics")
        }
        _isLoadingLyrics.value = true
        sendMessage(message)
    }
    
    fun disconnect() {
        webSocket?.close(1000, "Client disconnecting")
        webSocket = null
        _isConnected.value = false
    }
    
    fun sendMessage(message: JSONObject) {
        webSocket?.send(message.toString())
        Log.d("WebSocket", "Sent: ${message.toString()}")
    }
    
    fun sendHello(clientName: String = "Android") {
        val message = JSONObject().apply {
            put("type", "hello")
            put("client_name", clientName)  // Use snake_case to match server expectations
        }
        sendMessage(message)
    }
    
    fun sendPlay() {
        val message = JSONObject().apply {
            put("type", "play")
        }
        sendMessage(message)
    }
    
    fun sendPause() {
        val message = JSONObject().apply {
            put("type", "pause")
        }
        sendMessage(message)
    }
    
    fun sendNext() {
        val message = JSONObject().apply {
            put("type", "next")
        }
        Log.i("WebSocket", "➡️ Sending 'next' command")
        sendMessage(message)
    }
    
    fun sendPrevious() {
        val message = JSONObject().apply {
            put("type", "previous")
        }
        Log.i("WebSocket", "⬅️ Sending 'previous' command")
        sendMessage(message)
    }
    
    fun sendSeek(positionSecs: Double) {
        val message = JSONObject().apply {
            put("type", "seek")
            put("position_secs", positionSecs)
        }
        Log.i("WebSocket", "📍 Seeking to ${String.format("%.2f", positionSecs)}s")
        sendMessage(message)
    }
    
    fun sendSetVolume(volume: Double) {
        val message = JSONObject().apply {
            put("type", "setVolume")
            put("volume", volume)
        }
        sendMessage(message)
    }
    
    fun sendGetStatus() {
        val message = JSONObject().apply {
            put("type", "getStatus")
        }
        sendMessage(message)
    }

    fun sendGetLyrics() {
        val message = JSONObject().apply {
            put("type", "getLyrics")
        }
        sendMessage(message)
    }

    fun sendGetLibrary() {
        val message = JSONObject().apply {
            put("type", "getLibrary")
        }
        sendMessage(message)
    }

    
    fun sendPlayTrack(path: String) {
        val message = JSONObject().apply {
            put("type", "playTrack")
            put("path", path)
        }
        sendMessage(message)
    }

    fun sendSetQueue(paths: List<String>) {
        val pathsArray = org.json.JSONArray()
        paths.forEach { pathsArray.put(it) }
        
        val message = JSONObject().apply {
            put("type", "setQueue")
            put("paths", pathsArray)
        }
        sendMessage(message)
    }
    
    fun sendPlayAlbum(albumName: String, artist: String) {
        val message = JSONObject().apply {
            put("type", "playAlbum")
            put("album", albumName)
            put("artist", artist)
        }
        sendMessage(message)
    }
    
    fun sendPlayArtist(artist: String) {
        val message = JSONObject().apply {
            put("type", "playArtist")
            put("artist", artist)
        }
        sendMessage(message)
    }
    
    fun sendStartMobilePlayback() {
        val message = JSONObject().apply {
            put("type", "startMobilePlayback")
        }
        Log.i("WebSocket", "📱 Requesting mobile playback")
        sendMessage(message)
    }
    
    fun sendStopMobilePlayback() {
        val message = JSONObject().apply {
            put("type", "stopMobilePlayback")
        }
        Log.i("WebSocket", "🖥️ Stopping mobile playback, returning to desktop")
        sendMessage(message)
    }
    
    fun sendMobilePositionUpdate(positionSecs: Double) {
        val message = JSONObject().apply {
            put("type", "mobilePositionUpdate")
            put("position_secs", positionSecs) // Server expects snake_case for enum fields
        }
        sendMessage(message)
    }
    
    // Shuffle, Repeat, Favorites
    fun sendToggleShuffle() {
        val message = JSONObject().apply {
            put("type", "toggleShuffle")
        }
        Log.i("WebSocket", "🔀 Toggling shuffle")
        sendMessage(message)
    }
    
    fun sendToggleRepeat() {
        val message = JSONObject().apply {
            put("type", "cycleRepeat")
        }
        Log.i("WebSocket", "🔁 Toggling repeat")
        sendMessage(message)
    }
    
    fun sendToggleFavorite(trackPath: String) {
        val message = JSONObject().apply {
            put("type", "toggleFavorite")
            put("path", trackPath)
        }
        Log.i("WebSocket", "❤️ Toggling favorite for: $trackPath")
        sendMessage(message)
    }
    
    // Playlists
    fun sendGetPlaylists() {
        val message = JSONObject().apply {
            put("type", "getPlaylists")
        }
        Log.i("WebSocket", "📚 Requesting playlists")
        sendMessage(message)
    }
    
    fun sendGetPlaylistTracks(playlistId: String) {
        val message = JSONObject().apply {
            put("type", "getPlaylistTracks")
            put("playlist_id", playlistId)
        }
        Log.i("WebSocket", "📜 Requesting tracks for playlist: $playlistId")
        sendMessage(message)
    }
    
    fun sendAddToPlaylist(playlistId: String, trackPath: String) {
        val message = JSONObject().apply {
            put("type", "addToPlaylist")
            put("playlist_id", playlistId)
            put("path", trackPath)
        }
        Log.i("WebSocket", "➕ Adding track to playlist")
        sendMessage(message)
    }
    
    fun sendRemoveFromPlaylist(playlistId: String, playlistTrackId: Long) {
        val message = JSONObject().apply {
            put("type", "removeFromPlaylist")
            put("playlist_id", playlistId)
            put("playlist_track_id", playlistTrackId)
        }
        Log.i("WebSocket", "➖ Removing track from playlist")
        sendMessage(message)
    }
    
    fun sendReorderPlaylistTracks(playlistId: String, trackIds: List<Long>) {
        val trackIdsArray = org.json.JSONArray()
        trackIds.forEach { trackIdsArray.put(it) }
        
        val message = JSONObject().apply {
            put("type", "reorderPlaylistTracks")
            put("playlist_id", playlistId)
            put("track_ids", trackIdsArray)
        }
        Log.i("WebSocket", "↕️ Reordering playlist tracks")
        sendMessage(message)
    }
    
    fun sendCreatePlaylist(name: String, songPaths: List<String>, customization: moe.memesta.vibeon.ui.PlaylistCustomization) {
        val songsArray = org.json.JSONArray()
        songPaths.forEach { songsArray.put(it) }
        
        val message = JSONObject().apply {
            put("type", "createPlaylist")
            put("name", name)
            put("songs", songsArray)
            put("customizationType", customization.type.name)
            
            when (customization.type) {
                moe.memesta.vibeon.ui.PlaylistCustomizationType.Image -> {
                    put("imageUri", customization.imageUri?.toString())
                }
                moe.memesta.vibeon.ui.PlaylistCustomizationType.Icon -> {
                    put("color", customization.color)
                    put("iconName", customization.iconName)
                }
                moe.memesta.vibeon.ui.PlaylistCustomizationType.Default -> {
                    put("color", customization.color)
                }
            }
        }
        Log.i("WebSocket", "✨ Creating new playlist: $name")
        sendMessage(message)
    }
    
    private inner class VibeonWebSocketListener(
        private val client: WebSocketClient,
        private val clientName: String
    ) : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d("WebSocket", "Connected!")
            client._isConnected.value = true
            // Send hello message to identify ourselves
            client.sendHello(clientName)
        }
        
        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d("WebSocket", "Received: $text")
            client._messages.value = text
            try {
                val json = JSONObject(text)
                val type = json.optString("type")
                when (type) {
                    "connected", "welcome" -> {
                        Log.i("WebSocket", "Successfully connected as: $clientName")
                    }
                    "mediaSession" -> {
                        // Update playback state
                        val trackId = json.optString("trackId")
                        val title = json.optString("title", "No Track")
                        val artist = json.optString("artist", "Unknown Artist")
                        val album = json.optString("album", "")
                        val duration = json.optDouble("duration", 0.0)
                        val isPlaying = json.optBoolean("isPlaying", false)
                        val position = json.optDouble("position", 0.0)
                        
                        // Detect track change to fetch lyrics
                        if (trackId.isNotEmpty() && trackId != client.lastTrackId) {
                            client.lastTrackId = trackId
                            // Clear old lyrics and fetch new ones
                            client._lyrics.value = null
                            client.getLyrics()
                        }
                        var coverUrl = json.optString("cover_url", null)?.takeIf { it != "null" && it.isNotEmpty() }
                            ?: json.optString("coverUrl", null)?.takeIf { it != "null" && it.isNotEmpty() }

                        if (coverUrl != null && !coverUrl.startsWith("http")) {
                            coverUrl = if (coverUrl.startsWith("/")) {
                                "${client.baseUrl}$coverUrl"
                            } else {
                                "${client.baseUrl}/$coverUrl"
                            }
                        }
                        
                        Log.i("WebSocket", "🖼️ Resolved Cover URL: $coverUrl (Base: ${client.baseUrl}, Original: ${json.optString("cover_url") ?: json.optString("coverUrl")})")
                        
                        val hasLyrics = json.has("lyrics")
                        val lyricsLen = json.optString("lyrics", "").length
                        Log.i("WebSocket", "📀 MediaSession Update - Lyrics Present: $hasLyrics, Length: $lyricsLen")

                        // Preserve existing lyrics if this update doesn't include them
                        val newLyrics = if (hasLyrics && lyricsLen > 0) {
                            json.optString("lyrics", "")
                        } else {
                            client._currentTrack.value.lyrics
                        }

                        client._currentTrack.value = MediaSessionData(
                            title = title,
                            artist = artist,
                            album = album,
                            duration = duration,
                            coverUrl = coverUrl,
                            lyrics = newLyrics,
                            titleRomaji = json.optString("titleRomaji", null).takeIf { it != "null" },
                            titleEn = json.optString("titleEn", null).takeIf { it != "null" },
                            artistRomaji = json.optString("artistRomaji", null).takeIf { it != "null" },
                            artistEn = json.optString("artistEn", null).takeIf { it != "null" },
                            albumRomaji = json.optString("albumRomaji", null).takeIf { it != "null" },
                            albumEn = json.optString("albumEn", null).takeIf { it != "null" },
                            path = trackId
                        )
                        client._isPlaying.value = isPlaying
                        client._duration.value = duration
                        client._progress.value = position
                        Log.i("WebSocket", "📀 Now playing: $title by $artist (Playing: $isPlaying) - ${position}s / ${duration}s")
                    }
                    "status" -> {
                        // Update status (volume, shuffle, repeat, favorites)
                        val volume = json.optDouble("volume", 0.5)
                        val isShuffled = json.optBoolean("shuffle", false) || json.optBoolean("isShuffled", false)
                        val repeatMode = json.optString("repeat", "off").takeIf { it.isNotEmpty() } ?: "off"
                        
                        client._volume.value = volume
                        client._isShuffled.value = isShuffled
                        client._repeatMode.value = repeatMode
                        
                        // Parse output to sync mobile playback state
                        if (json.has("output")) {
                            val output = json.getString("output")
                            client._isMobilePlayback.value = (output == "mobile")
                            Log.i("WebSocket", "🔊 Active output: $output")
                        }
                        
                        // Parse favorites if included
                        if (json.has("favorites")) {
                            val favoritesArray = json.optJSONArray("favorites")
                            val favSet = mutableSetOf<String>()
                            if (favoritesArray != null) {
                                for (i in 0 until favoritesArray.length()) {
                                    favSet.add(favoritesArray.getString(i))
                                }
                            }
                            client._favorites.value = favSet
                        }
                        
                        Log.i("WebSocket", "📊 Status - Volume: ${(volume * 100).toInt()}%, Shuffle: $isShuffled, Repeat: $repeatMode, Playing: ${client._isPlaying.value}")
                    }
                    "PlaybackState" -> {
                        // Update playback state
                        val isPlaying = json.optBoolean("is_playing", false)
                        val position = json.optDouble("position", 0.0)
                        client._isPlaying.value = isPlaying
                        client._progress.value = position
                        Log.i("WebSocket", "▶️ Playback state: Playing=$isPlaying, Position=$position")
                    }
                    "progressUpdate" -> {
                        // Handle progress updates
                        val position = json.optDouble("positionSecs", 0.0)
                        client._progress.value = position
                        Log.d("WebSocket", "⏱️ Progress: $position")
                    }
                    "queueUpdate" -> {
                        // Handle queue updates
                        val queueArray = json.optJSONArray("queue") ?: return
                        val queueItems = mutableListOf<QueueItem>()

                        for (i in 0 until queueArray.length()) {
                            val trackObj = queueArray.getJSONObject(i)
                            var qCoverUrl = trackObj.optString("coverUrl", null)?.takeIf { it != "null" && it.isNotEmpty() }
                                ?: trackObj.optString("cover_url", null)?.takeIf { it != "null" && it.isNotEmpty() }
                            
                            if (qCoverUrl != null && !qCoverUrl.startsWith("http")) {
                                qCoverUrl = if (qCoverUrl.startsWith("/")) {
                                    "${client.baseUrl}$qCoverUrl"
                                } else {
                                    "${client.baseUrl}/$qCoverUrl"
                                }
                            }
                            queueItems.add(
                                QueueItem(
                                    path = trackObj.getString("path"),
                                    title = trackObj.getString("title"),
                                    artist = trackObj.getString("artist"),
                                    album = trackObj.getString("album"),
                                    duration = trackObj.optDouble("durationSecs", 0.0),
                                    coverUrl = qCoverUrl,
                                    titleRomaji = trackObj.optString("titleRomaji", null).takeIf { it != "null" },
                                    titleEn = trackObj.optString("titleEn", null).takeIf { it != "null" },
                                    artistRomaji = trackObj.optString("artistRomaji", null).takeIf { it != "null" },
                                    artistEn = trackObj.optString("artistEn", null).takeIf { it != "null" },
                                    albumRomaji = trackObj.optString("albumRomaji", null).takeIf { it != "null" },
                                    albumEn = trackObj.optString("albumEn", null).takeIf { it != "null" }
                                )
                            )
                        }
                        client._queue.value = queueItems
                        client._currentIndex.value = json.optInt("currentIndex", 0)
                        Log.i("WebSocket", "📋 Queue updated with ${queueItems.size} tracks")
                    }
                    "statsUpdated" -> {
                        val timestamp = json.optLong("timestamp", System.currentTimeMillis())
                        client._statsUpdated.value = timestamp
                        Log.i("WebSocket", "📊 Stats updated @ $timestamp")
                    }
                    "handoffPrepare" -> {
                        // Server is ready to stream to mobile
                        var url = json.optString("url")
                        val sample = json.optLong("sample", 0)
                        
                        // Robustness: If server returned localhost (127.0.0.1) but we are connected via a real IP,
                        // swap it out so the phone can actually reach the stream.
                        client.baseUrl?.let { baseUrl ->
                            if (url.startsWith("http://127.0.0.1") || url.startsWith("http://localhost")) {
                                val streamIndex = url.indexOf("/stream")
                                if (streamIndex != -1) {
                                    url = baseUrl + url.substring(streamIndex)
                                    Log.w("WebSocket", "⚠️ Replaced localhost with connected baseUrl: $url")
                                }
                            }
                        }
                        
                        val positionSecs = (sample / 44100.0).coerceAtLeast(0.0)
                        client._streamUrl.value = url
                        client._handoffPosition.value = positionSecs
                        client._isMobilePlayback.value = true
                        Log.i("WebSocket", "🎵 Received stream URL: $url at position ${String.format("%.2f", positionSecs)}s")
                    }
                    "lyrics" -> {
                        // Handle lyrics update
                        client._isLoadingLyrics.value = false
                        
                        val trackPath = json.optString("trackPath")
                        val hasSynced = json.optBoolean("hasSynced")
                        val plainLyrics = json.optString("plainLyrics", "").ifEmpty {
                            json.optString("plain_lyrics", "")
                        }.takeIf { it != "null" && it.isNotEmpty() }
                        val syncedLyrics = json.optString("syncedLyrics", "").ifEmpty {
                            json.optString("synced_lyrics", "")
                        }.takeIf { it != "null" && it.isNotEmpty() }
                        val syncedLyricsRomaji = json.optString("syncedLyricsRomaji", "").ifEmpty {
                            json.optString("synced_lyrics_romaji", "")
                        }.takeIf { it != "null" && it.isNotEmpty() }
                        val instrumental = json.optBoolean("instrumental")

                        // Update dedicated lyrics state
                        client._lyrics.value = LyricsData(
                            trackPath = trackPath,
                            hasSynced = hasSynced,
                            syncedLyrics = syncedLyrics,
                            syncedLyricsRomaji = syncedLyricsRomaji,
                            plainLyrics = plainLyrics,
                            instrumental = instrumental
                        )

                        // Update current track lyrics field for double-binding
                        val finalLyricsText = syncedLyrics ?: plainLyrics ?: ""
                        val current = client._currentTrack.value
                        client._currentTrack.value = current.copy(lyrics = finalLyricsText)
                        
                        Log.i("WebSocket", "📜 Lyrics received for $trackPath (Synced: $hasSynced, Romaji: ${!syncedLyricsRomaji.isNullOrEmpty()}, Length: ${finalLyricsText.length})")
                    }
                    "playlists" -> {
                        // Handle playlists list
                        val playlistsArray = json.optJSONArray("playlists")
                        val playlistsList = mutableListOf<PlaylistInfo>()
                        
                        if (playlistsArray != null) {
                            for (i in 0 until playlistsArray.length()) {
                                val playlistObj = playlistsArray.getJSONObject(i)
                                playlistsList.add(
                                    PlaylistInfo(
                                        id = playlistObj.getString("id"),
                                        name = playlistObj.getString("name"),
                                        trackCount = playlistObj.optInt("trackCount", 0),
                                        createdAt = playlistObj.optString("createdAt", ""),
                                        updatedAt = playlistObj.optString("updatedAt", "")
                                    )
                                )
                            }
                        }
                        
                        client._playlists.value = playlistsList
                        Log.i("WebSocket", "📚 Received ${playlistsList.size} playlists")
                    }
                    "library" -> {
                        // Handle library update
                        val tracksArray = json.optJSONArray("tracks")
                        val tracksList = mutableListOf<TrackInfo>()
                        
                        if (tracksArray != null) {
                            for (i in 0 until tracksArray.length()) {
                                val trackObj = tracksArray.getJSONObject(i)
                                var libCoverUrl = trackObj.optString("coverUrl", null)?.takeIf { it != "null" && it.isNotEmpty() }
                                    ?: trackObj.optString("cover_url", null)?.takeIf { it != "null" && it.isNotEmpty() }
                                
                                if (libCoverUrl != null && !libCoverUrl.startsWith("http")) {
                                    libCoverUrl = if (libCoverUrl.startsWith("/")) {
                                        "${client.baseUrl}$libCoverUrl"
                                    } else {
                                        "${client.baseUrl}/$libCoverUrl"
                                    }
                                }
                                
                                tracksList.add(
                                    TrackInfo(
                                        path = trackObj.getString("path"),
                                        title = trackObj.getString("title"),
                                        artist = trackObj.getString("artist"),
                                        album = trackObj.getString("album"),
                                        duration = trackObj.optDouble("durationSecs", 0.0),
                                        coverUrl = libCoverUrl,
                                        titleRomaji = trackObj.optString("titleRomaji", null).takeIf { it != "null" },
                                        titleEn = trackObj.optString("titleEn", null).takeIf { it != "null" },
                                        artistRomaji = trackObj.optString("artistRomaji", null).takeIf { it != "null" },
                                        artistEn = trackObj.optString("artistEn", null).takeIf { it != "null" },
                                        albumRomaji = trackObj.optString("albumRomaji", null).takeIf { it != "null" },
                                        albumEn = trackObj.optString("albumEn", null).takeIf { it != "null" },
                                        playlistTrackId = trackObj.optLong("playlistTrackId", -1L).takeIf { it != -1L }
                                    )
                                )
                            }
                        }
                        
                        client._library.value = tracksList
                        Log.i("WebSocket", "📚 Library updated with ${tracksList.size} tracks")
                    }
                    "error" -> {
                        val message = json.optString("message")
                        Log.e("WebSocket", "Error from server: $message")
                        
                        // Stop loading state on lyrics-related errors
                        if (message.contains("lyrics", ignoreCase = true) || message.contains("track", ignoreCase = true)) {
                            client._isLoadingLyrics.value = false
                            // Important: If we failed to get lyrics, set an empty state to prevent any potential UI retries
                            if (client.lastTrackId != null && client._lyrics.value == null) {
                                client._lyrics.value = LyricsData(trackPath = client.lastTrackId!!)
                            }
                        }
                    }
                    "streamStopped" -> {
                        client._isMobilePlayback.value = false
                        client._streamUrl.value = null
                    }
                    "playlistTracks" -> {
                        // Handle playlist tracks response
                        val tracksArray = json.optJSONArray("tracks")
                        val playlistId = json.optString("playlistId", "")
                        val tracksList = mutableListOf<TrackInfo>()
                        
                        if (tracksArray != null) {
                            for (i in 0 until tracksArray.length()) {
                                val trackObj = tracksArray.getJSONObject(i)
                                var coverUrl = trackObj.optString("coverUrl", null)
                                    ?: trackObj.optString("cover_url", null)
                                
                                if (coverUrl != null && !coverUrl.startsWith("http")) {
                                    coverUrl = "${client.baseUrl}$coverUrl"
                                }
                                
                                tracksList.add(
                                    TrackInfo(
                                        path = trackObj.getString("path"),
                                        title = trackObj.getString("title"),
                                        artist = trackObj.getString("artist"),
                                        album = trackObj.getString("album"),
                                        duration = trackObj.optDouble("durationSecs", 0.0),
                                        coverUrl = coverUrl,
                                        titleRomaji = trackObj.optString("titleRomaji", null).takeIf { it != "null" },
                                        titleEn = trackObj.optString("titleEn", null).takeIf { it != "null" },
                                        artistRomaji = trackObj.optString("artistRomaji", null).takeIf { it != "null" },
                                        artistEn = trackObj.optString("artistEn", null).takeIf { it != "null" },
                                        albumRomaji = trackObj.optString("albumRomaji", null).takeIf { it != "null" },
                                        albumEn = trackObj.optString("albumEn", null).takeIf { it != "null" },
                                        playlistTrackId = trackObj.optLong("playlistTrackId", -1L).takeIf { it != -1L }
                                    )
                                )
                            }
                        }
                        
                        client._currentPlaylistTracks.value = tracksList
                        Log.i("WebSocket", "📚 Playlist $playlistId received ${tracksList.size} tracks")
                    }
                }
            } catch (e: Exception) {
                Log.e("WebSocket", "Failed to parse message: ${e.message}")
            }
        }
        
        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d("WebSocket", "Closing: $code $reason")
            webSocket.close(1000, null)
            client._isConnected.value = false
        }
        
        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d("WebSocket", "Closed: $code $reason")
            client._isConnected.value = false
        }
        
        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e("WebSocket", "Error: ${t.message}")
            client._isConnected.value = false
        }
    }
}

