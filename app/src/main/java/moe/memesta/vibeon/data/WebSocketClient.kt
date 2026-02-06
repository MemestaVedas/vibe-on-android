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
    val coverUrl: String? = null
)

data class QueueItem(
    val path: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Double
)

data class LyricsData(
    val trackPath: String = "",
    val hasSynced: Boolean = false,
    val syncedLyrics: String? = null,
    val plainLyrics: String? = null,
    val instrumental: Boolean = false
)

class WebSocketClient {
    private val okHttpClient = OkHttpClient()
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
    
    private val _isMobilePlayback = MutableStateFlow(false)
    val isMobilePlayback: StateFlow<Boolean> = _isMobilePlayback.asStateFlow()

    private val _library = MutableStateFlow<List<TrackInfo>>(emptyList())
    val library: StateFlow<List<TrackInfo>> = _library.asStateFlow()
    
    private val _lyrics = MutableStateFlow<LyricsData?>(null)
    val lyrics: StateFlow<LyricsData?> = _lyrics.asStateFlow()
    
    private val _isLoadingLyrics = MutableStateFlow(false)
    val isLoadingLyrics: StateFlow<Boolean> = _isLoadingLyrics.asStateFlow()
    
    private var lastTrackId: String? = null
    
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
        sendMessage(message)
    }
    
    fun sendPrevious() {
        val message = JSONObject().apply {
            put("type", "previous")
        }
        sendMessage(message)
    }
    
    fun sendSeek(positionSecs: Double) {
        val message = JSONObject().apply {
            put("type", "seek")
            put("positionSecs", positionSecs)
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
    
    fun sendPlayAlbum(albumName: String, artist: String) {
        val message = JSONObject().apply {
            put("type", "playAlbum")
            put("album", albumName)
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
            put("positionSecs", positionSecs)
        }
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
                        } else if (client._lyrics.value == null && trackId.isNotEmpty()) {
                            // Retry fetching if we have no lyrics but have a track
                             client.getLyrics()
                        }
                        var coverUrl = json.optString("cover_url", null) ?: json.optString("coverUrl", null)

                        if (coverUrl != null && !coverUrl.startsWith("http")) {
                            coverUrl = "${client.baseUrl}$coverUrl"
                        }
                        
                        client._currentTrack.value = MediaSessionData(
                            title = title,
                            artist = artist,
                            album = album,
                            duration = duration,
                            coverUrl = coverUrl
                        )
                        client._isPlaying.value = isPlaying
                        client._duration.value = duration
                        client._progress.value = position
                        Log.i("WebSocket", "📀 Now playing: $title by $artist (Playing: $isPlaying) - ${position}s / ${duration}s")
                    }
                    "status" -> {
                        // Update status (doesn't include isPlaying state)
                        val volume = json.optDouble("volume")
                        val shuffle = json.optBoolean("shuffle")
                        Log.i("WebSocket", "📊 Volume: $volume, Shuffle: $shuffle, Playing: ${client._isPlaying.value}")
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
                            val item = queueArray.getJSONObject(i)
                            queueItems.add(
                                QueueItem(
                                    path = item.getString("path"),
                                    title = item.getString("title"),
                                    artist = item.getString("artist"),
                                    album = item.getString("album"),
                                    duration = item.getDouble("durationSecs")
                                )
                            )
                        }
                        client._queue.value = queueItems
                        client._currentIndex.value = json.optInt("currentIndex", 0)
                        Log.i("WebSocket", "📋 Queue updated with ${queueItems.size} tracks")
                    }
                    "error" -> {
                        val message = json.optString("message")
                        Log.e("WebSocket", "Error from server: $message")
                    }
                    "handoffPrepare" -> {
                        // Server is ready to stream to mobile
                        val url = json.optString("url")
                        val sample = json.optLong("sample", 0)
                        val positionSecs = sample / 44100.0
                        client._streamUrl.value = url
                        client._isMobilePlayback.value = true
                        Log.i("WebSocket", "🎵 Received stream URL: $url at position ${String.format("%.2f", positionSecs)}s")
                    }
                    "lyrics" -> {
                        // Handle lyrics
                        client._isLoadingLyrics.value = false
                        client._lyrics.value = LyricsData(
                            trackPath = json.optString("trackPath"),
                            hasSynced = json.optBoolean("hasSynced"),
                            syncedLyrics = json.optString("syncedLyrics", null).takeIf { it != "null" },
                            plainLyrics = json.optString("plainLyrics", null).takeIf { it != "null" },
                            instrumental = json.optBoolean("instrumental")
                        )
                        Log.i("WebSocket", "📜 Received lyrics: Synced=${json.optBoolean("hasSynced")}")
                    }
                    "error" -> {
                         // Check if error is related to lyrics (simple heuristic)
                         val msg = json.optString("message", "")
                         if (msg == "Lyrics not found" || msg == "No track playing") {
                             client._isLoadingLyrics.value = false
                         }
                    }
                    "streamStopped" -> {
                        // Mobile streaming stopped
                        client._isMobilePlayback.value = false
                        client._streamUrl.value = null
                        Log.i("WebSocket", "🛑 Mobile streaming stopped")
                    }
                    "library" -> {
                        val tracksArray = json.optJSONArray("tracks") ?: return
                        val tracks = mutableListOf<TrackInfo>()
                        val base = client.baseUrl
                        for (i in 0 until tracksArray.length()) {
                            val t = tracksArray.getJSONObject(i)
                            val path = t.getString("path")
                            var cover = t.optString("coverUrl", null)
                            if (cover != null && !cover.startsWith("http")) {
                                cover = "$base$cover"
                            }
                            tracks.add(TrackInfo(
                                path = path,
                                title = t.getString("title"),
                                artist = t.getString("artist"),
                                album = t.getString("album"),
                                duration = t.getDouble("durationSecs"),
                                coverUrl = cover
                            ))
                        }
                        client._library.value = tracks
                        Log.i("WebSocket", "📚 Received library with ${tracks.size} tracks")
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

