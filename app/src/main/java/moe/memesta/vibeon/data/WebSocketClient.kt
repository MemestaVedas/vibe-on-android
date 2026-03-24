package moe.memesta.vibeon.data

import android.util.Log
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import org.json.JSONObject
import java.net.URI

// ═════════════════════════════════════════════════════════════════════════════
// Data classes
// ═════════════════════════════════════════════════════════════════════════════

@Immutable
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

@Immutable
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

@Immutable
data class LyricsData(
    val trackPath: String = "",
    val hasSynced: Boolean = false,
    val syncedLyrics: String? = null,
    val syncedLyricsRomaji: String? = null,
    val plainLyrics: String? = null,
    val instrumental: Boolean = false
)

@Immutable
data class PlaylistInfo(
    val id: String,
    val name: String,
    val trackCount: Int,
    val createdAt: String = "",
    val updatedAt: String = ""
)

// ═════════════════════════════════════════════════════════════════════════════
// WebSocket Client
// ═════════════════════════════════════════════════════════════════════════════

class WebSocketClient {

    companion object {
        private const val TAG = "WebSocket"
        private const val MAX_RECONNECT_ATTEMPTS = 10
        private const val BASE_RECONNECT_DELAY_MS = 1000L
        private const val SKIP_DEBOUNCE_MS = 700L
    }

    // ── OkHttp ───────────────────────────────────────────────────────────
    private val okHttpClient = OkHttpClient.Builder()
        .pingInterval(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    private var webSocket: WebSocket? = null

    // ── Connection info ──────────────────────────────────────────────────
    var host: String = "localhost"; private set
    var port: Int = 5000; private set
    private var baseUrl: String = ""
    private var clientName: String = "Android"
    private var controlToken: String? = null

    // ── Reconnection ─────────────────────────────────────────────────────
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0
    private val reconnectScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ── Skip-command debounce ────────────────────────────────────────────
    private var lastSkipCommandAtMs: Long = 0L
    private var lastSkipDirection: String = ""

    // ── Observable state ─────────────────────────────────────────────────
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _messages = MutableStateFlow("")
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

    private val _isShuffled = MutableStateFlow(false)
    val isShuffled: StateFlow<Boolean> = _isShuffled.asStateFlow()

    private val _repeatMode = MutableStateFlow("off")
    val repeatMode: StateFlow<String> = _repeatMode.asStateFlow()

    private val _volume = MutableStateFlow(0.5)
    val volume: StateFlow<Double> = _volume.asStateFlow()

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _playlists = MutableStateFlow<List<PlaylistInfo>>(emptyList())
    val playlists: StateFlow<List<PlaylistInfo>> = _playlists.asStateFlow()

    private val _currentPlaylistTracks = MutableStateFlow<List<TrackInfo>>(emptyList())
    val currentPlaylistTracks: StateFlow<List<TrackInfo>> = _currentPlaylistTracks.asStateFlow()

    private val _statsUpdated = MutableStateFlow(0L)
    val statsUpdated: StateFlow<Long> = _statsUpdated.asStateFlow()

    private var lastTrackId: String? = null
    private var lastHandoffAtMs: Long = 0L

    // ═════════════════════════════════════════════════════════════════════
    // Connection lifecycle
    // ═════════════════════════════════════════════════════════════════════

    fun connect(host: String, port: Int, clientName: String = "Android", controlToken: String? = null) {
        this.host = host
        this.port = port
        this.clientName = clientName
        this.controlToken = controlToken?.trim()?.takeIf { it.isNotEmpty() }
        baseUrl = "http://$host:$port"
        reconnectAttempts = 0

        val tokenParam = this.controlToken?.let { "?token=${java.net.URLEncoder.encode(it, Charsets.UTF_8.name())}" } ?: ""
        val wsUrl = "ws://$host:$port/control$tokenParam"
        Log.i(TAG, "Connecting to $wsUrl")
        val request = Request.Builder().url(wsUrl).build()
        webSocket = okHttpClient.newWebSocket(request, Listener())
    }

    fun setControlToken(token: String?) {
        controlToken = token?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun disconnect() {
        reconnectJob?.cancel()
        reconnectAttempts = MAX_RECONNECT_ATTEMPTS // Prevent auto-reconnect
        webSocket?.close(1000, "Client disconnecting")
        webSocket = null
        _isConnected.value = false
    }

    private fun scheduleReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Log.w(TAG, "Max reconnect attempts reached, giving up")
            return
        }
        val delay = BASE_RECONNECT_DELAY_MS * (1L shl reconnectAttempts.coerceAtMost(5))
        reconnectAttempts++
        Log.i(TAG, "Reconnecting in ${delay}ms (attempt $reconnectAttempts/$MAX_RECONNECT_ATTEMPTS)")
        reconnectJob?.cancel()
        reconnectJob = reconnectScope.launch {
            delay(delay)
            try { connect(host, port, clientName, controlToken) }
            catch (e: Exception) { Log.e(TAG, "Reconnect failed: ${e.message}") }
        }
    }

    /**
     * Consume the pending stream URL so it won't re-trigger the PlaybackViewModel
     * on subsequent recomposition.
     */
    fun consumeStreamUrl() {
        _streamUrl.value = null
    }

    // ═════════════════════════════════════════════════════════════════════
    // Send helpers — each builds a JSON message and sends it
    // ═════════════════════════════════════════════════════════════════════

    private fun send(msg: JSONObject) {
        webSocket?.send(msg.toString())
        Log.d(TAG, "→ ${msg.optString("type")}")
    }

    private fun sendSimple(type: String) = send(JSONObject().apply { put("type", type) })

    fun sendHello(name: String = "Android") = send(JSONObject().apply {
        put("type", "hello")
        put("clientName", name)
    })

    fun sendPlay()    = sendSimple("play")
    fun sendPause()   = sendSimple("pause")
    fun sendGetStatus() = sendSimple("getStatus")
    fun sendGetLyrics() = sendSimple("getLyrics")
    fun sendGetLibrary() = sendSimple("getLibrary")
    fun sendGetPlaylists() = sendSimple("getPlaylists")
    fun sendStartMobilePlayback() = sendSimple("startMobilePlayback")
    fun sendStopMobilePlayback()  = sendSimple("stopMobilePlayback")
    fun sendToggleShuffle() = sendSimple("toggleShuffle")
    fun sendToggleRepeat()  = sendSimple("cycleRepeat")

    fun sendNext() {
        if (!allowSkip("next")) return
        sendSimple("next")
    }

    fun sendPrevious() {
        if (!allowSkip("previous")) return
        sendSimple("previous")
    }

    fun sendSeek(positionSecs: Double) = send(JSONObject().apply {
        put("type", "seek")
        put("positionSecs", positionSecs)
    })

    fun sendSetVolume(volume: Double) = send(JSONObject().apply {
        put("type", "setVolume")
        put("volume", volume)
    })

    fun sendPlayTrack(path: String) = send(JSONObject().apply {
        put("type", "playTrack")
        put("path", path)
    })

    fun sendSetQueue(paths: List<String>) = send(JSONObject().apply {
        put("type", "setQueue")
        put("paths", org.json.JSONArray().apply { paths.forEach { put(it) } })
    })

    fun sendPlayAlbum(albumName: String, artist: String) = send(JSONObject().apply {
        put("type", "playAlbum")
        put("album", albumName)
        put("artist", artist)
    })

    fun sendPlayArtist(artist: String) = send(JSONObject().apply {
        put("type", "playArtist")
        put("artist", artist)
    })

    fun sendMobilePositionUpdate(positionSecs: Double) = send(JSONObject().apply {
        put("type", "mobilePositionUpdate")
        put("positionSecs", positionSecs)
    })

    fun sendReportPlaybackEvent(
        songId: String,
        timestampMs: Long,
        durationMs: Long,
        startMs: Long?,
        endMs: Long?,
        output: String = "mobile"
    ) = send(JSONObject().apply {
        put("type", "reportPlaybackEvent")
        put("songId", songId)
        put("timestamp", timestampMs)
        put("durationMs", durationMs)
        startMs?.let { put("startTimestamp", it) }
        endMs?.let { put("endTimestamp", it) }
        put("output", output)
    })

    /**
     * Bulk-send locally stored playback events to the PC for sync.
     * Used on reconnect to push events that were recorded while offline.
     */
    fun sendSyncPlaybackEvents(events: List<moe.memesta.vibeon.data.stats.PlaybackEvent>) {
        events.forEach { ev ->
            sendReportPlaybackEvent(
                songId = ev.songId,
                timestampMs = ev.timestamp,
                durationMs = ev.durationMs,
                startMs = ev.startTimestamp,
                endMs = ev.endTimestamp,
                output = ev.output
            )
        }
    }

    fun sendToggleFavorite(trackPath: String) = send(JSONObject().apply {
        put("type", "toggleFavorite")
        put("path", trackPath)
    })

    fun sendGetPlaylistTracks(playlistId: String) = send(JSONObject().apply {
        put("type", "getPlaylistTracks")
        put("playlist_id", playlistId)
    })

    fun sendAddToPlaylist(playlistId: String, trackPath: String) = send(JSONObject().apply {
        put("type", "addToPlaylist")
        put("playlist_id", playlistId)
        put("path", trackPath)
    })

    fun sendRemoveFromPlaylist(playlistId: String, playlistTrackId: Long) = send(JSONObject().apply {
        put("type", "removeFromPlaylist")
        put("playlist_id", playlistId)
        put("playlist_track_id", playlistTrackId)
    })

    fun sendReorderPlaylistTracks(playlistId: String, trackIds: List<Long>) = send(JSONObject().apply {
        put("type", "reorderPlaylistTracks")
        put("playlist_id", playlistId)
        put("track_ids", org.json.JSONArray().apply { trackIds.forEach { put(it) } })
    })

    fun sendCreatePlaylist(
        name: String,
        songPaths: List<String>,
        customization: moe.memesta.vibeon.ui.PlaylistCustomization
    ) = send(JSONObject().apply {
        put("type", "createPlaylist")
        put("name", name)
        put("songs", org.json.JSONArray().apply { songPaths.forEach { put(it) } })
        put("customizationType", customization.type.name)
        when (customization.type) {
            moe.memesta.vibeon.ui.PlaylistCustomizationType.Image ->
                put("imageUri", customization.imageUri?.toString())
            moe.memesta.vibeon.ui.PlaylistCustomizationType.Icon -> {
                put("color", customization.color)
                put("iconName", customization.iconName)
            }
            moe.memesta.vibeon.ui.PlaylistCustomizationType.Default ->
                put("color", customization.color)
        }
    })

    /** Request lyrics for the current track (with loading indicator). */
    fun sendGetLyricsForCurrentTrack() {
        _isLoadingLyrics.value = true
        sendGetLyrics()
    }

    // ═════════════════════════════════════════════════════════════════════
    // Internal helpers
    // ═════════════════════════════════════════════════════════════════════

    private fun allowSkip(direction: String): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastSkipCommandAtMs < SKIP_DEBOUNCE_MS) {
            Log.w(TAG, "Dropping rapid '$direction' (${now - lastSkipCommandAtMs}ms since '$lastSkipDirection')")
            return false
        }
        lastSkipCommandAtMs = now
        lastSkipDirection = direction
        return true
    }

    /**
     * Rewrite the stream URL's host/port to match the WebSocket connection
     * endpoint, so the phone always hits the same NIC the WS is running on.
     */
    private fun normalizeStreamUrl(raw: String): String {
        return try {
            val uri = URI(raw)
            val uriPort = if (uri.port > 0) uri.port else port
            if (uri.host.equals(host, ignoreCase = true) && uriPort == port) raw
            else URI(
                uri.scheme ?: "http", null, host, port,
                uri.path, uri.query, uri.fragment
            ).toString().also {
                Log.w(TAG, "Stream URL mismatch fixed: $raw -> $it")
            }
        } catch (_: Exception) { raw }
    }

    // ═════════════════════════════════════════════════════════════════════
    // WebSocket listener — parses incoming server messages
    // ═════════════════════════════════════════════════════════════════════

    private inner class Listener : WebSocketListener() {

        override fun onOpen(ws: WebSocket, response: Response) {
            Log.i(TAG, "Connected")
            reconnectJob?.cancel()
            _isConnected.value = true
            reconnectAttempts = 0
            sendHello(clientName)
        }

        override fun onMessage(ws: WebSocket, text: String) {
            val startedAt = System.currentTimeMillis()
            _messages.value = text
            try {
                val json = JSONObject(text)
                when (json.optString("type")) {
                    "connected", "welcome" -> onConnected(json)
                    "mediaSession"         -> onMediaSession(json)
                    "status"               -> onStatus(json)
                    "PlaybackState"        -> onPlaybackState(json)
                    "queueUpdate"          -> onQueueUpdate(json)
                    "handoffPrepare"       -> onHandoffPrepare(json)
                    "streamStopped"        -> onStreamStopped()
                    "lyrics"               -> onLyrics(json)
                    "library"              -> onLibrary(json)
                    "playlists"            -> onPlaylists(json)
                    "playlistTracks"       -> onPlaylistTracks(json)
                    "statsUpdated"         -> onStatsUpdated(json)
                    "ack"                  -> onAck(json)
                    "error"                -> onError(json)
                    "pong"                 -> { /* keepalive, ignore */ }
                    else                   -> Log.d(TAG, "Unknown message type: ${json.optString("type")}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse message: ${e.message}")
            } finally {
                val elapsedMs = System.currentTimeMillis() - startedAt
                if (elapsedMs > 50) {
                    Log.w(TAG, "Slow message processing: ${elapsedMs}ms")
                }
            }
        }

        override fun onClosing(ws: WebSocket, code: Int, reason: String) {
            // Pass through original close code so onClosed can decide whether to reconnect
            ws.close(code, reason)
            _isConnected.value = false
        }

        override fun onClosed(ws: WebSocket, code: Int, reason: String) {
            _isConnected.value = false
            if (code != 1000 && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) scheduleReconnect()
        }

        override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "Connection error: ${t.message}")
            _isConnected.value = false
            scheduleReconnect()
        }

        // ── Individual message handlers ──────────────────────────────────

        private fun onConnected(json: JSONObject) {
            Log.i(TAG, "Identified as $clientName (id=${json.optString("clientId")})")
        }

        private fun onMediaSession(json: JSONObject) {
            val trackId  = json.optString("trackId")
            val title    = json.optString("title", "No Track")
            val artist   = json.optString("artist", "Unknown Artist")
            val album    = json.optString("album", "")
            val duration = json.optDouble("duration", 0.0)
            val playing  = json.optBoolean("isPlaying", false)
            val position = json.optDouble("position", 0.0)

            // Detect track change
            if (trackId.isNotEmpty() && trackId != lastTrackId) {
                lastTrackId = trackId
                _lyrics.value = null
                sendGetLyricsForCurrentTrack()
            }

            val rawCover = json.optString("coverUrl", null)?.takeIf { it != "null" && it.isNotEmpty() }
            val coverUrl = resolveCoverUrl(rawCover, baseUrl)

            // Preserve existing lyrics if this message doesn't carry new ones
            val existing = _currentTrack.value.lyrics
            val newLyrics = if (json.has("lyrics") && json.optString("lyrics").isNotEmpty()) {
                json.optString("lyrics", "")
            } else existing

            _currentTrack.value = MediaSessionData(
                title = title, artist = artist, album = album,
                duration = duration, coverUrl = coverUrl, lyrics = newLyrics,
                titleRomaji  = json.optStringOrNull("titleRomaji"),
                titleEn      = json.optStringOrNull("titleEn"),
                artistRomaji = json.optStringOrNull("artistRomaji"),
                artistEn     = json.optStringOrNull("artistEn"),
                albumRomaji  = json.optStringOrNull("albumRomaji"),
                albumEn      = json.optStringOrNull("albumEn"),
                path = trackId
            )
            _isPlaying.value = playing
            _duration.value  = duration
            _progress.value  = position
        }

        private fun onStatus(json: JSONObject) {
            _volume.value     = json.optDouble("volume", 0.5)
            _isShuffled.value = json.optBoolean("shuffle", false)
            _repeatMode.value = json.optString("repeatMode", "off").ifEmpty { "off" }

            if (json.has("output")) {
                val output = json.getString("output")
                val isRecentHandoff = (System.currentTimeMillis() - lastHandoffAtMs) in 0..1500

                // Ignore transient "desktop" status that raced with handoffPrepare.
                // The stream URL is consumed right after ExoPlayer starts preparing, so we
                // cannot rely on _streamUrl being non-null here — only check the time window.
                if (output == "desktop" && _isMobilePlayback.value && isRecentHandoff) {
                    Log.w(TAG, "Ignoring stale desktop status during handoff window (${System.currentTimeMillis() - lastHandoffAtMs}ms after handoff)")
                } else {
                    _isMobilePlayback.value = output == "mobile"
                }
            }
        }

        private fun onPlaybackState(json: JSONObject) {
            _isPlaying.value = json.optBoolean("is_playing", false)
            _progress.value  = json.optDouble("position", 0.0)
        }

        private fun onQueueUpdate(json: JSONObject) {
            val arr = json.optJSONArray("queue") ?: return
            _queue.value = (0 until arr.length()).map { arr.getJSONObject(it).toQueueItem(baseUrl) }
            _currentIndex.value = json.optInt("currentIndex", 0)
        }

        private fun onHandoffPrepare(json: JSONObject) {
            val rawUrl = json.optString("url")
            val url    = normalizeStreamUrl(rawUrl)
            val sample = json.optLong("sample", 0)

            lastHandoffAtMs = System.currentTimeMillis()
            _streamUrl.value       = url
            _handoffPosition.value = (sample / 44100.0).coerceAtLeast(0.0)
            _isMobilePlayback.value = true
            Log.i(TAG, "Stream ready: $url @ ${_handoffPosition.value}s")
        }

        private fun onStreamStopped() {
            _isMobilePlayback.value = false
            _streamUrl.value = null
        }

        private fun onLyrics(json: JSONObject) {
            _isLoadingLyrics.value = false

            val trackPath = json.optStringOrNull("trackPath", "track_path") ?: ""
            val hasSynced = json.optBooleanFlexible("hasSynced", "has_synced")
            val synced    = json.optStringOrNull("syncedLyrics", "synced_lyrics")
            val romaji    = json.optStringOrNull("syncedLyricsRomaji", "synced_lyrics_romaji")
            val plain     = json.optStringOrNull("plainLyrics", "plain_lyrics")
            val instrumental = json.optBoolean("instrumental")

            _lyrics.value = LyricsData(trackPath, hasSynced, synced, romaji, plain, instrumental)

            // Also update the current track's lyrics text
            val text = synced ?: plain ?: ""
            _currentTrack.value = _currentTrack.value.copy(lyrics = text)
            Log.i(TAG, "Lyrics received for $trackPath (Synced: $hasSynced, Romaji: ${!romaji.isNullOrBlank()}, Len: ${text.length})")
        }

        private fun onLibrary(json: JSONObject) {
            val arr = json.optJSONArray("tracks") ?: return
            _library.value = (0 until arr.length()).map { arr.getJSONObject(it).toTrackInfo(baseUrl) }
            Log.i(TAG, "Library: ${_library.value.size} tracks")
        }

        private fun onPlaylists(json: JSONObject) {
            val arr = json.optJSONArray("playlists") ?: return
            _playlists.value = (0 until arr.length()).map { i ->
                arr.getJSONObject(i).let { obj ->
                    PlaylistInfo(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        trackCount = obj.optInt("trackCount", 0),
                        createdAt  = obj.optString("createdAt", ""),
                        updatedAt  = obj.optString("updatedAt", "")
                    )
                }
            }
        }

        private fun onPlaylistTracks(json: JSONObject) {
            val arr = json.optJSONArray("tracks") ?: return
            _currentPlaylistTracks.value = (0 until arr.length()).map {
                arr.getJSONObject(it).toTrackInfo(baseUrl)
            }
        }

        private fun onStatsUpdated(json: JSONObject) {
            _statsUpdated.value = json.optLong("timestamp", System.currentTimeMillis())
        }

        private fun onAck(json: JSONObject) {
            Log.d(TAG, "Ack: ${json.optString("action")}")
        }

        private fun onError(json: JSONObject) {
            val msg = json.optString("message")
            Log.e(TAG, "Server error: $msg")

            // Clear loading state on lyrics-related errors
            if (msg.contains("lyrics", ignoreCase = true) || msg.contains("track", ignoreCase = true)) {
                _isLoadingLyrics.value = false
                if (lastTrackId != null && _lyrics.value == null) {
                    _lyrics.value = LyricsData(trackPath = lastTrackId!!)
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Extension helpers
// ═════════════════════════════════════════════════════════════════════════════

/** Returns `null` for absent keys, "null" strings, or empty strings. */
private fun JSONObject.optStringOrNull(vararg keys: String): String? {
    for (key in keys) {
        val value = optString(key, null)?.takeIf { it != "null" && it.isNotEmpty() }
        if (value != null) return value
    }
    return null
}

private fun JSONObject.optBooleanFlexible(primary: String, fallback: String): Boolean {
    return when {
        has(primary) -> optBoolean(primary)
        has(fallback) -> optBoolean(fallback)
        else -> false
    }
}
