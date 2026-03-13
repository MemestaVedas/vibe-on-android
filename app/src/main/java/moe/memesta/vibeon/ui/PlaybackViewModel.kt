package moe.memesta.vibeon.ui

import android.util.Log
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import moe.memesta.vibeon.data.WebSocketClient
import moe.memesta.vibeon.MediaNotificationManager
import moe.memesta.vibeon.PlaybackService

class PlaybackViewModel(
    private val webSocketClient: WebSocketClient,
    private var player: Player? = null
) : ViewModel() {

    companion object {
        private const val TAG = "PlaybackVM"
        private const val AUTO_NEXT_COOLDOWN_MS = 1500L
    }

    // ── State ────────────────────────────────────────────────────────────
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _isMobilePlayback = MutableStateFlow(false)
    val isMobilePlayback: StateFlow<Boolean> = _isMobilePlayback.asStateFlow()

    private val _offlineSong = MutableStateFlow<OfflineSong?>(null)
    val offlineSong: StateFlow<OfflineSong?> = _offlineSong.asStateFlow()

    // ── Internal ─────────────────────────────────────────────────────────
    private var pendingStreamUrl: String? = null
    private var currentStreamUrl: String? = null
    private var streamingListener: Player.Listener? = null
    private var progressJob: kotlinx.coroutines.Job? = null
    private var autoNextArmed = true
    private var lastAutoNextAtMs = 0L

    // ═════════════════════════════════════════════════════════════════════
    // Player attachment
    // ═════════════════════════════════════════════════════════════════════

    fun setPlayer(player: Player) {
        this.player = player
        Log.i(TAG, "Player attached")

        // Apply any stream URL that arrived before the player was ready
        pendingStreamUrl?.let { url ->
            Log.i(TAG, "Applying deferred handoff URL")
            if (startMobileStreaming(url)) {
                webSocketClient.consumeStreamUrl()
                pendingStreamUrl = null
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // WebSocket observers
    // ═════════════════════════════════════════════════════════════════════

    init {
        // Stream URL → start mobile streaming
        viewModelScope.launch {
            webSocketClient.streamUrl.collect { url ->
                if (url != null) {
                    if (startMobileStreaming(url)) {
                        webSocketClient.consumeStreamUrl()
                        pendingStreamUrl = null
                    } else {
                        pendingStreamUrl = url
                        Log.w(TAG, "Deferring stream start — player not ready")
                    }
                }
            }
        }

        // Mobile-playback flag → stop streaming when false
        viewModelScope.launch {
            webSocketClient.isMobilePlayback.collect { isMobile ->
                _isMobilePlayback.value = isMobile
                if (!isMobile) stopMobileStreaming()
            }
        }

        // Track metadata
        viewModelScope.launch {
            webSocketClient.currentTrack.collect { track ->
                _playbackState.value = _playbackState.value.copy(
                    title = track.title,
                    artist = track.artist,
                    duration = track.duration.toLong() * 1000
                )
            }
        }

        // Remote isPlaying (only applies when NOT in mobile-playback mode,
        // because during mobile playback ExoPlayer is the source of truth
        // and the desktop reports isPlaying=false after handoff)
        viewModelScope.launch {
            webSocketClient.isPlaying.collect { isPlaying ->
                if (!_isMobilePlayback.value) {
                    _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)
                }
            }
        }

        // Remote position (only relevant when NOT playing locally)
        viewModelScope.launch {
            webSocketClient.progress.collect { position ->
                if (!_isMobilePlayback.value) {
                    _playbackState.value = _playbackState.value.copy(
                        currentPosition = (position * 1000).toLong()
                    )
                }
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // Mobile streaming
    // ═════════════════════════════════════════════════════════════════════

    private fun startMobileStreaming(url: String): Boolean {
        val p = player ?: run {
            Log.w(TAG, "Cannot start streaming: player is null")
            return false
        }

        // Skip if already playing this exact URL
        if (url == currentStreamUrl && p.playbackState == Player.STATE_READY) {
            if (!p.isPlaying) {
                Log.i(TAG, "Resuming already-prepared stream URL")
                p.playWhenReady = true
                p.play()
            }
            _playbackState.value = _playbackState.value.copy(isPlaying = p.isPlaying)
            Log.d(TAG, "Already streaming $url — keeping current player")
            return true
        }
        currentStreamUrl = url

        MediaNotificationManager.exitSilentMode()

        val state = _playbackState.value
        val metadata = MediaMetadata.Builder()
            .setTitle(state.title)
            .setArtist(state.artist)
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .setMediaMetadata(metadata)
            .build()

        p.setMediaItem(mediaItem)
        p.prepare()

        // Remove the previous listener before installing a new one
        streamingListener?.let { p.removeListener(it) }
        streamingListener = null

        val handoffSecs = webSocketClient.handoffPosition.value
        val isFlac = url.substringBefore('?').lowercase().endsWith(".flac")
        var handoffApplied = false

        streamingListener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> if (!handoffApplied) {
                        handoffApplied = true
                        autoNextArmed = true
                        // Seek to handoff position (skip for FLAC to avoid decode issues)
                        if (handoffSecs > 0 && !isFlac) {
                            p.seekTo((handoffSecs * 1000).toLong())
                        }
                        p.play()
                        _playbackState.value = _playbackState.value.copy(isPlaying = true)
                        startProgressPolling()
                    }
                    Player.STATE_ENDED -> if (_isMobilePlayback.value) {
                        val now = System.currentTimeMillis()
                        if (autoNextArmed && now - lastAutoNextAtMs > AUTO_NEXT_COOLDOWN_MS) {
                            autoNextArmed = false
                            lastAutoNextAtMs = now
                            Log.i(TAG, "Track ended — auto-next")
                            webSocketClient.sendNext()
                        }
                    }
                    else -> {}
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                val secs = p.currentPosition / 1000.0
                webSocketClient.sendMobilePositionUpdate(secs)
                updateProgress(p.currentPosition)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)
                if (isPlaying) startProgressPolling()
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e(TAG, "ExoPlayer error: ${error.errorCodeName} — ${error.message}", error)
            }
        }.also { p.addListener(it) }

        p.playWhenReady = true
        return true
    }

    private fun stopMobileStreaming() {
        progressJob?.cancel()
        currentStreamUrl = null
        _playbackState.value = _playbackState.value.copy(isPlaying = false)
        player?.let { p ->
            Log.i(TAG, "Stopping mobile streaming")
            streamingListener?.let { p.removeListener(it) }
            streamingListener = null
            p.stop()
            p.clearMediaItems()
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // Progress polling
    // ═════════════════════════════════════════════════════════════════════

    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (_isMobilePlayback.value && player?.isPlaying == true) {
                player?.let { updateProgress(it.currentPosition) }
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // Public API
    // ═════════════════════════════════════════════════════════════════════

    fun requestMobilePlayback() {
        webSocketClient.sendStartMobilePlayback()
    }

    fun stopMobilePlayback() {
        webSocketClient.sendStopMobilePlayback()
    }

    fun updateHeader(header: uniffi.vibe_on_core.StreamHeader) {
        _playbackState.value = _playbackState.value.copy(
            title = header.title,
            artist = header.artist,
            duration = header.durationSecs.toLong() * 1000
        )
    }

    fun updateProgress(position: Long) {
        _playbackState.value = _playbackState.value.copy(currentPosition = position)
        if (_isMobilePlayback.value) {
            webSocketClient.sendMobilePositionUpdate(position / 1000.0)
        }
    }

    fun updateIsPlaying(isPlaying: Boolean) {
        _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)
    }

    fun setPlayerPlayWhenReady(playWhenReady: Boolean) {
        player?.playWhenReady = playWhenReady
    }

    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs)
    }

    fun playOfflineSong(song: OfflineSong) {
        PlaybackService.isOfflineMode = true
        _offlineSong.value = song
        _playbackState.value = _playbackState.value.copy(
            title = song.title,
            artist = song.artist,
            duration = song.duration
        )

        MediaNotificationManager.exitSilentMode()

        player?.let { p ->
            val metadata = MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist)
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(song.uri)
                .setMediaId(song.uri)
                .setMediaMetadata(metadata)
                .build()

            p.setMediaItem(mediaItem)
            p.prepare()
            p.play()

            _isMobilePlayback.value = true
            startProgressPolling()
        }
    }

    fun stopOfflinePlayback() {
        if (_offlineSong.value != null) {
            PlaybackService.isOfflineMode = false
            _offlineSong.value = null
            _isMobilePlayback.value = false
            stopMobileStreaming()
            // Reset to current PC track
            webSocketClient.currentTrack.value.let { track ->
                _playbackState.value = _playbackState.value.copy(
                    title = track.title,
                    artist = track.artist,
                    duration = track.duration.toLong() * 1000
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
        player?.let { p -> streamingListener?.let { p.removeListener(it) } }
    }
}

// ═════════════════════════════════════════════════════════════════════════════

@Stable
data class PlaybackState(
    val title: String = "No Track",
    val artist: String = "Unknown Artist",
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0,
    val duration: Long = 1, // Avoid division by zero
) {
    val progress: Float
        get() = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
}
