package moe.memesta.vibeon.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import moe.memesta.vibeon.data.WebSocketClient
import uniffi.vibe_on_core.StreamHeader

class PlaybackViewModel(
    private val webSocketClient: WebSocketClient,
    private var player: Player? = null
) : ViewModel() {
    
    fun setPlayer(player: Player) {
        this.player = player
        Log.i("PlaybackViewModel", "🎧 Player attached")
    }
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()
    
    private val _isMobilePlayback = MutableStateFlow(false)
    val isMobilePlayback: StateFlow<Boolean> = _isMobilePlayback.asStateFlow()

    init {
        // Listen for WebSocket stream URL updates
        viewModelScope.launch {
            webSocketClient.streamUrl.collect { url ->
                url?.let { startMobileStreaming(it) }
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
                
                // Sync local player if streaming
                if (_isMobilePlayback.value) {
                    if (isPlaying) {
                        if (player?.isPlaying == false) player?.play()
                    } else {
                        if (player?.isPlaying == true) player?.pause()
                    }
                }
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
    
    private fun startMobileStreaming(url: String) {
        player?.let {
            Log.i("PlaybackViewModel", "🎵 Starting mobile streaming: $url")
            val mediaItem = MediaItem.fromUri(url)
            it.setMediaItem(mediaItem)
            it.prepare()
            it.play()
            
            // Start polling for progress
            startProgressPolling()
            
            // Add listener to sync position
            it.addListener(object : Player.Listener {
                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int
                ) {
                    val positionSecs = it.currentPosition / 1000.0
                    webSocketClient.sendMobilePositionUpdate(positionSecs)
                    updateProgress(it.currentPosition)
                }
                
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        startProgressPolling()
                    }
                }
                
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        Log.i("PlaybackViewModel", "🎵 Track ended, auto-skipping to next")
                        webSocketClient.sendNext()
                    }
                }
                
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    Log.e("PlaybackViewModel", "❌ ExoPlayer Error: ${error.message}", error)
                    Log.e("PlaybackViewModel", "❌ Error Code: ${error.errorCodeName}")
                    if (error.cause != null) {
                        Log.e("PlaybackViewModel", "❌ Cause: ${error.cause?.message}", error.cause)
                    }
                }
            })
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
        player?.let {
            Log.i("PlaybackViewModel", "⏹️ Stopping mobile streaming")
            it.stop()
            it.clearMediaItems()
        }
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
}

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
