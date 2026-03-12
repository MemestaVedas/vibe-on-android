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
import uniffi.vibe_on_core.StreamHeader
import moe.memesta.vibeon.PlaybackService

class PlaybackViewModel(
    private val webSocketClient: WebSocketClient,
    private var player: Player? = null
) : ViewModel() {
    
    private val _offlineSong = MutableStateFlow<OfflineSong?>(null)
    val offlineSong: StateFlow<OfflineSong?> = _offlineSong.asStateFlow()

    private var pendingStreamUrl: String? = null
    
    fun setPlayer(player: Player) {
        this.player = player
        Log.i("PlaybackViewModel", "🎧 Player attached")

        pendingStreamUrl?.let { url ->
            Log.i("PlaybackViewModel", "🔁 Applying pending handoff URL after player attach")
            if (startMobileStreaming(url)) {
                webSocketClient.consumeStreamUrl()
                pendingStreamUrl = null
            }
        }
    }
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()
    
    private val _isMobilePlayback = MutableStateFlow(false)
    val isMobilePlayback: StateFlow<Boolean> = _isMobilePlayback.asStateFlow()

    init {
        // Listen for WebSocket stream URL updates — consume-and-clear to prevent stale re-triggers
        viewModelScope.launch {
            webSocketClient.streamUrl.collect { url ->
                if (url != null) {
                    val started = startMobileStreaming(url)
                    if (started) {
                        // Consume only after successful stream start setup.
                        webSocketClient.consumeStreamUrl()
                        pendingStreamUrl = null
                    } else {
                        pendingStreamUrl = url
                        Log.w("PlaybackViewModel", "⏳ Deferring stream start until player is attached")
                    }
                }
            }
        }
        
        // Listen for mobile playback state
        viewModelScope.launch {
            webSocketClient.isMobilePlayback.collect { isMobile ->
                _isMobilePlayback.value = isMobile
                if (!isMobile) {
                    stopMobileStreaming()
                }
            }
        }
        
        // Listen for track updates from WebSocket
        viewModelScope.launch {
            webSocketClient.currentTrack.collect { track ->
                _playbackState.value = _playbackState.value.copy(
                    title = track.title,
                    artist = track.artist,
                    duration = track.duration.toLong() * 1000 // Convert seconds to ms
                )
            }
        }
        
        // Listen for playback state from WebSocket
         viewModelScope.launch {
            webSocketClient.isPlaying.collect { isPlaying ->
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = isPlaying
                )

                // During mobile playback, local ExoPlayer is the source of truth.
                // Desktop reports isPlaying=false right after handoff (PC is paused),
                // so mirroring that state here can incorrectly pause mobile audio.
            }
        }
        
        // Listen for progress from WebSocket (when not playing locally)
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
    
    private var streamingListener: Player.Listener? = null
    private var currentStreamUrl: String? = null

    private fun startMobileStreaming(url: String): Boolean {
        val p = player
        if (p == null) {
            Log.w("PlaybackViewModel", "⚠️ Cannot start mobile streaming yet: player is null")
            return false
        }

        // Guard: skip if already playing this exact URL
        if (url == currentStreamUrl && p.playbackState == Player.STATE_READY) {
            Log.i("PlaybackViewModel", "⏭️ Already streaming $url — skipping re-trigger")
            return true
        }
        currentStreamUrl = url

        // Exit silent notification mode before loading real audio
        MediaNotificationManager.exitSilentMode()
        Log.i("PlaybackViewModel", "🔓 Exited silent mode for mobile streaming")
            
        run {
            Log.i("PlaybackViewModel", "🎵 Starting mobile streaming from URL: $url")
            Log.d("PlaybackViewModel", "📊 Player state before loading: isPlaying=${p.isPlaying}, playbackState=${p.playbackState}")
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

            // Remove any previous streaming listener before adding a new one
            streamingListener?.let { p.removeListener(it) }
            streamingListener = null

            // Capture handoff position now — seek will be applied once STATE_READY fires
            val handoffSecs = webSocketClient.handoffPosition.value
            val isFlacStream = url.substringBefore('?').lowercase().endsWith(".flac")
            var handoffApplied = false

            // Add listener to sync position and apply the deferred seek
            streamingListener = object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY && !handoffApplied) {
                        handoffApplied = true
                        if (handoffSecs > 0 && !isFlacStream) {
                            Log.i("PlaybackViewModel", "⏭️ STATE_READY — seeking to handoff ${handoffSecs}s")
                            p.seekTo((handoffSecs * 1000).toLong())
                        } else if (handoffSecs > 0 && isFlacStream) {
                            Log.w("PlaybackViewModel", "⚠️ Skipping immediate handoff seek for FLAC stream to avoid decode errors")
                        }
                        p.play()
                        startProgressPolling()
                    }
                    if (playbackState == Player.STATE_ENDED && _isMobilePlayback.value) {
                        Log.i("PlaybackViewModel", "🎵 Track ended, auto-skipping to next")
                        webSocketClient.sendNext()
                    }
                }

                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int
                ) {
                    val positionSecs = p.currentPosition / 1000.0
                    webSocketClient.sendMobilePositionUpdate(positionSecs)
                    updateProgress(p.currentPosition)
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) startProgressPolling()
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    Log.e("PlaybackViewModel", "❌ ExoPlayer Error: ${error.errorCodeName}", error)
                    Log.e("PlaybackViewModel", "❌ Error details: ${error.cause?.message}", error.cause)
                    val errorMsg = when (error.errorCodeName) {
                        "ERROR_CODE_NETWORK_TIMEOUT" -> "Network timeout - check connection to PC"
                        "ERROR_CODE_NETWORK_PERMISSION" -> "Network permission error"
                        "ERROR_CODE_IO_FILE_NOT_FOUND" -> "Stream file not found on PC"
                        "ERROR_CODE_IO_NETWORK_CONNECTION_FAILED" -> "Cannot connect to PC - check network/IP address"
                        "ERROR_CODE_CLEARTEXT_NOT_PERMITTED" -> "HTTP not allowed (security config issue)"
                        "ERROR_CODE_UNSPECIFIED" -> "Unspecified error: ${error.message}"
                        else -> "ExoPlayer error: ${error.errorCodeName} - ${error.message}"
                    }
                    Log.e("PlaybackViewModel", "📱 Mobile Streaming Error: $errorMsg")
                    if (_isMobilePlayback.value) {
                        Log.i("PlaybackViewModel", "🔄 Mobile playback active, waiting for recovery...")
                    }
                }
            }.also { p.addListener(it) }

            // playWhenReady so ExoPlayer starts as soon as STATE_READY fires
            p.playWhenReady = true
        }

        return true
    }
    
    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
        player?.let { p ->
            streamingListener?.let { p.removeListener(it) }
        }
    }
    
    private var progressJob: kotlinx.coroutines.Job? = null
    
    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (_isMobilePlayback.value && player?.isPlaying == true) {
                player?.let {
                    val currentPos = it.currentPosition
                    updateProgress(currentPos)
                }
                kotlinx.coroutines.delay(1000)
            }
        }
    }
    
    private fun stopMobileStreaming() {
        progressJob?.cancel()
        currentStreamUrl = null
        player?.let { p ->
            Log.i("PlaybackViewModel", "⏹️ Stopping mobile streaming")
            streamingListener?.let { p.removeListener(it) }
            streamingListener = null
            p.stop()
            p.clearMediaItems()
        }
        // DO NOT clear media items from the service player — MediaNotificationManager
        // will handle re-entering silent mode via the isMobilePlayback flow observer.
    }
    
    fun requestMobilePlayback() {
        Log.i("PlaybackViewModel", "📱 User requested mobile playback")
        webSocketClient.sendStartMobilePlayback()
    }
    
    fun stopMobilePlayback() {
        Log.i("PlaybackViewModel", "🖥️ User requested to stop mobile playback")
        webSocketClient.sendStopMobilePlayback()
    }

    fun updateHeader(header: StreamHeader) {
        _playbackState.value = _playbackState.value.copy(
            title = header.title,
            artist = header.artist,
            duration = header.durationSecs.toLong() * 1000
        )
    }

    fun updateProgress(position: Long) {
        _playbackState.value = _playbackState.value.copy(currentPosition = position)
        
        // Sync position to PC if playing locally
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
                // Use a generic placeholder, media session handles real art if needed via MNM
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(song.uri)
                .setMediaId(song.uri)
                .setMediaMetadata(metadata)
                .build()

            p.setMediaItem(mediaItem)
            p.prepare()
            p.play()
            
            // Re-use progress polling
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
            // Reset to current track from PC
            webSocketClient.currentTrack.value.let { track ->
                _playbackState.value = _playbackState.value.copy(
                    title = track.title,
                    artist = track.artist,
                    duration = track.duration.toLong() * 1000
                )
            }
        }
    }
}

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
