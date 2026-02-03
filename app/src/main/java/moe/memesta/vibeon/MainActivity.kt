package moe.memesta.vibeon

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import moe.memesta.vibeon.PlaybackService
import moe.memesta.vibeon.data.DiscoveryRepository
import moe.memesta.vibeon.data.StreamRepository
import moe.memesta.vibeon.data.DiscoveredDevice
import moe.memesta.vibeon.ui.*
import moe.memesta.vibeon.ui.theme.VibeonTheme
import android.content.ComponentName
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.common.Player
import com.google.common.util.concurrent.ListenableFuture

class MainActivity : ComponentActivity() {
    private lateinit var discoveryRepository: DiscoveryRepository
    private lateinit var connectionViewModel: ConnectionViewModel
    private lateinit var playbackViewModel: PlaybackViewModel
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var streamRepository: StreamRepository? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        discoveryRepository = DiscoveryRepository(this)
        connectionViewModel = ConnectionViewModel(discoveryRepository)
        // Initialize playbackViewModel immediately so it's ready for UI
        playbackViewModel = PlaybackViewModel(
            webSocketClient = connectionViewModel.wsClient
        )
        
        // Try to initialize StreamRepository but don't crash if it fails
        try {
            val streamRepo = StreamRepository()
            PlaybackService.streamRepository = streamRepo
            this.streamRepository = streamRepo
            Log.i("MainActivity", "✅ StreamRepository initialized successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.w("MainActivity", "⚠️ Native library not available, P2P streaming disabled: ${e.message}")
        } catch (e: NoClassDefFoundError) {
            Log.w("MainActivity", "⚠️ JNA library not found, P2P streaming disabled: ${e.message}")
        } catch (e: Exception) {
            Log.e("MainActivity", "⚠️ Failed to initialize StreamRepository: ${e.message}", e)
        }

        setContent {
            VibeonTheme {
                var currentScreen by remember { mutableStateOf("discovery") }
                var selectedDevice by remember { mutableStateOf<DiscoveredDevice?>(null) }
                var libraryViewModel by remember { mutableStateOf<LibraryViewModel?>(null) }
                val currentTrack by connectionViewModel.currentTrack.collectAsState()
                val wsIsPlaying by connectionViewModel.isPlaying.collectAsState()
                val playbackState by playbackViewModel.playbackState.collectAsState()
                val progress by connectionViewModel.wsClient.progress.collectAsState()
                val duration by connectionViewModel.wsClient.duration.collectAsState()
                val isMobilePlayback by connectionViewModel.wsClient.isMobilePlayback.collectAsState()
                
                // Determine effective playback state for UI
                val isPlaying = if (isMobilePlayback) playbackState.isPlaying else wsIsPlaying
                
                // Effective duration (in seconds)
                val activeDuration = if (isMobilePlayback) playbackState.duration else duration.toLong()
                
                // Calculate progress (0.0 - 1.0)
                val currentProgress = if (activeDuration > 0) {
                    if (isMobilePlayback) {
                        // playbackState.currentPosition is ms, duration is seconds (from Header/Track)
                        // Wait, check units below. Assuming duration is seconds.
                        (playbackState.currentPosition / 1000f) / activeDuration.toFloat()
                    } else {
                        // progress is seconds, duration is seconds
                        (progress / duration).toFloat()
                    }
                } else 0f
                
                
                // Initialize playbackViewModel with WebSocket
                /* Removed LaunchedEffect as it causes crash due to race condition */

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (currentScreen) {
                        "discovery" -> DiscoveryScreen(
                            viewModel = connectionViewModel,
                            onDeviceSelected = { device ->
                                selectedDevice = device
                                connectionViewModel.connectToDevice(device)
                                // Switch to library browsing
                                libraryViewModel = LibraryViewModel(device.host, device.port, connectionViewModel.wsClient)
                                currentScreen = "library"
                            }
                        )
                        "library" -> {
                            if (libraryViewModel != null && selectedDevice != null) {
                                LibraryScreen(
                                    viewModel = libraryViewModel!!,
                                    onBackClick = {
                                        currentScreen = "discovery"
                                        connectionViewModel.disconnect()
                                    },
                                    onTrackSelected = { track ->
                                        libraryViewModel!!.playTrack(track)
                                        currentScreen = "now_playing"
                                    },
                                    onNavigateToPlayer = {
                                        currentScreen = "now_playing"
                                    }
                                )
                            }
                        }
                        "now_playing" -> NowPlayingScreen(
                            title = currentTrack.title,
                            artist = currentTrack.artist,
                            isPlaying = isPlaying,
                            progress = currentProgress,
                            duration = activeDuration, 
                            coverUrl = connectionViewModel.currentTrack.value.coverUrl,
                            // baseUrl removed as coverUrl is now absolute
                            isMobilePlayback = isMobilePlayback,
                            onBackToLibrary = {
                                currentScreen = "library"
                            },
                            onPlayPauseToggle = { 
                                if (isMobilePlayback) {
                                    if (isPlaying) playbackViewModel.setPlayerPlayWhenReady(false)
                                    else playbackViewModel.setPlayerPlayWhenReady(true)
                                } else {
                                    if (isPlaying) connectionViewModel.pause() 
                                    else connectionViewModel.play()
                                }
                            },
                            onSkipNext = { connectionViewModel.next() },
                            onSkipPrevious = { connectionViewModel.previous() },
                            onSeek = { progressRatio ->
                                if (isMobilePlayback) {
                                    val newPosMs = (progressRatio * playbackState.duration * 1000).toLong()
                                    playbackViewModel.seekTo(newPosMs)
                                } else {
                                    connectionViewModel.wsClient.sendSeek(progressRatio * duration)
                                }
                            },
                            onTogglePlaybackLocation = {
                                if (isMobilePlayback) {
                                    playbackViewModel.stopMobilePlayback()
                                } else {
                                    playbackViewModel.requestMobilePlayback()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture?.addListener({
            mediaController = controllerFuture?.get()
            mediaController?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    playbackViewModel.updateIsPlaying(isPlaying)
                }
                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int
                ) {
                    playbackViewModel.updateProgress(newPosition.positionMs)
                }
            })
            // Attach player to ViewModel
            playbackViewModel.setPlayer(mediaController!!)
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onStop() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
        super.onStop()
    }
}
