package moe.memesta.vibeon.ui

import android.util.Log
import android.os.SystemClock
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
import moe.memesta.vibeon.data.local.PlayerSettingsRepository
import moe.memesta.vibeon.data.local.ScrubberMode

data class HeartBurstEvent(
    val x: Float,
    val y: Float,
    val timestampMs: Long = System.currentTimeMillis()
)

class PlaybackViewModel(
    private val webSocketClient: WebSocketClient,
    private val playerSettingsRepository: PlayerSettingsRepository,
    private var player: Player? = null
) : ViewModel() {

    companion object {
        private const val TAG = "PlaybackVM"
        private const val AUTO_NEXT_COOLDOWN_MS = 1500L
        private const val HANDOFF_RECOVERY_DELAY_MS = 1200L
        private const val HANDOFF_RETRY_MIN_GAP_MS = 2500L
    }

    // ── State ────────────────────────────────────────────────────────────
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _isMobilePlayback = MutableStateFlow(false)
    val isMobilePlayback: StateFlow<Boolean> = _isMobilePlayback.asStateFlow()

    private val _offlineSong = MutableStateFlow<OfflineSong?>(null)
    val offlineSong: StateFlow<OfflineSong?> = _offlineSong.asStateFlow()

    val scrubberMode: StateFlow<ScrubberMode> = playerSettingsRepository.scrubberMode
    val sheetHintShown: StateFlow<Boolean> = playerSettingsRepository.sheetHintShown
    val artGestureHintShown: StateFlow<Boolean> = playerSettingsRepository.artGestureHintShown

    private val _heartBurstEvent = MutableStateFlow<HeartBurstEvent?>(null)
    val heartBurstEvent: StateFlow<HeartBurstEvent?> = _heartBurstEvent.asStateFlow()

    // ── Internal ─────────────────────────────────────────────────────────
    private var pendingStreamUrl: String? = null
    private var currentStreamUrl: String? = null
    private var streamingListener: Player.Listener? = null
    private var progressJob: kotlinx.coroutines.Job? = null
    private var desktopProgressJob: kotlinx.coroutines.Job? = null
    private var autoNextArmed = true
    private var lastAutoNextAtMs = 0L
    private var remoteAnchorPositionMs = 0L
    private var remoteAnchorRealtimeMs = 0L
    private var handoffRecoveryJob: kotlinx.coroutines.Job? = null
    private var lastHandoffRetryAtMs = 0L
    private var pendingLocalVolume = 0.5f

    // ═════════════════════════════════════════════════════════════════════
    // Player attachment
    // ═════════════════════════════════════════════════════════════════════

    fun setPlayer(player: Player) {
        this.player = player
        player.volume = pendingLocalVolume.coerceIn(0f, 1f)
        Log.i(TAG, "Player attached")

        // Apply any stream URL that arrived before the player was ready
        pendingStreamUrl?.let { url ->
            Log.i(TAG, "Applying deferred handoff URL")
            if (startMobileStreaming(url)) {
                webSocketClient.consumeStreamUrl()
                pendingStreamUrl = null
            }
        }

        if (_isMobilePlayback.value) {
            scheduleHandoffRecovery("player-attached")
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
                if (!isMobile) {
                    handoffRecoveryJob?.cancel()
                    handoffRecoveryJob = null
                    stopMobileStreaming()
                    maybeStartDesktopProgressJob()
                } else {
                    stopDesktopProgressJob()
                    scheduleHandoffRecovery("mobile-output-true")
                }
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
                    if (isPlaying) {
                        maybeStartDesktopProgressJob()
                    } else {
                        stopDesktopProgressJob()
                    }
                }
            }
        }

        // Remote position (only relevant when NOT playing locally)
        viewModelScope.launch {
            webSocketClient.progress.collect { position ->
                if (!_isMobilePlayback.value) {
                    val positionMs = (position * 1000).toLong()
                    updateRemoteAnchor(positionMs)
                    _playbackState.value = _playbackState.value.copy(
                        currentPosition = positionMs
                    )
                    maybeStartDesktopProgressJob()
                }
            }
        }

        // Keep local player volume in sync while mobile/offline playback is active.
        viewModelScope.launch {
            webSocketClient.volume.collect { volume ->
                val clamped = volume.toFloat().coerceIn(0f, 1f)
                pendingLocalVolume = clamped
                if (_isMobilePlayback.value || _offlineSong.value != null) {
                    player?.volume = clamped
                }
            }
        }
    }

    private fun updateRemoteAnchor(positionMs: Long) {
        remoteAnchorPositionMs = positionMs.coerceAtLeast(0L)
        remoteAnchorRealtimeMs = SystemClock.elapsedRealtime()
    }

    private fun predictedDesktopPositionMs(): Long {
        val now = SystemClock.elapsedRealtime()
        val elapsed = (now - remoteAnchorRealtimeMs).coerceAtLeast(0L)
        val base = remoteAnchorPositionMs
        val predicted = if (_playbackState.value.isPlaying) base + elapsed else base
        val duration = _playbackState.value.duration.coerceAtLeast(1L)
        return predicted.coerceIn(0L, duration)
    }

    private fun maybeStartDesktopProgressJob() {
        if (_isMobilePlayback.value || !_playbackState.value.isPlaying) return
        if (desktopProgressJob?.isActive == true) return

        desktopProgressJob = viewModelScope.launch {
            while (!_isMobilePlayback.value && _playbackState.value.isPlaying) {
                val predicted = predictedDesktopPositionMs()
                val current = _playbackState.value.currentPosition
                if (kotlin.math.abs(predicted - current) >= 30L) {
                    _playbackState.value = _playbackState.value.copy(currentPosition = predicted)
                }
                kotlinx.coroutines.delay(75)
            }
        }
    }

    private fun hasActiveLocalStream(): Boolean {
        val p = player ?: return false
        return currentStreamUrl != null && (
            p.isPlaying ||
            p.playbackState == Player.STATE_BUFFERING ||
            p.playbackState == Player.STATE_READY
        )
    }

    private fun scheduleHandoffRecovery(reason: String) {
        if (handoffRecoveryJob?.isActive == true) return

        handoffRecoveryJob = viewModelScope.launch {
            kotlinx.coroutines.delay(HANDOFF_RECOVERY_DELAY_MS)

            if (!_isMobilePlayback.value || hasActiveLocalStream()) {
                return@launch
            }

            val now = System.currentTimeMillis()
            if (now - lastHandoffRetryAtMs < HANDOFF_RETRY_MIN_GAP_MS) {
                return@launch
            }

            lastHandoffRetryAtMs = now
            Log.w(TAG, "Recovering mobile handoff ($reason): requesting fresh stream handoff")
            webSocketClient.sendStartMobilePlayback()
        }
    }

    private fun stopDesktopProgressJob() {
        desktopProgressJob?.cancel()
        desktopProgressJob = null
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
            handoffRecoveryJob?.cancel()
            handoffRecoveryJob = null
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
        p.volume = pendingLocalVolume.coerceIn(0f, 1f)
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
        handoffRecoveryJob?.cancel()
        handoffRecoveryJob = null
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
        scheduleHandoffRecovery("manual-request")
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

    fun setVolume(volume: Double) {
        val clamped = volume.toFloat().coerceIn(0f, 1f)
        pendingLocalVolume = clamped

        if (_isMobilePlayback.value || _offlineSong.value != null) {
            player?.volume = clamped
        }

        // Keep server/client volume state in sync so UI reflects latest value consistently.
        webSocketClient.sendSetVolume(clamped.toDouble())
    }

    fun setScrubberMode(mode: ScrubberMode) {
        playerSettingsRepository.setScrubberMode(mode)
    }

    fun toggleScrubberMode() {
        playerSettingsRepository.toggleScrubberMode()
    }

    fun markSheetHintShown() {
        playerSettingsRepository.setSheetHintShown(true)
    }

    fun markArtGestureHintShown() {
        playerSettingsRepository.setArtGestureHintShown(true)
    }

    fun triggerHeartBurst(x: Float, y: Float) {
        _heartBurstEvent.value = HeartBurstEvent(x = x, y = y)
    }

    fun clearHeartBurst() {
        _heartBurstEvent.value = null
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
            p.volume = pendingLocalVolume.coerceIn(0f, 1f)
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
        stopDesktopProgressJob()
        handoffRecoveryJob?.cancel()
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
