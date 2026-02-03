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
                val isPlaying by connectionViewModel.isPlaying.collectAsState()
                val progress by connectionViewModel.wsClient.progress.collectAsState()
                val duration by connectionViewModel.wsClient.duration.collectAsState()
                val isMobilePlayback by connectionViewModel.wsClient.isMobilePlayback.collectAsState()
                
                // Initialize playbackViewModel with WebSocket and player references
                LaunchedEffect(Unit) {
                    playbackViewModel = PlaybackViewModel(
                        webSocketClient = connectionViewModel.wsClient,
                        player = mediaController
                    )
                }

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
                                    }
                                )
                            }
                        }
                        "now_playing" -> NowPlayingScreen(
                            title = currentTrack.title,
                            artist = currentTrack.artist,
                            isPlaying = isPlaying,
                            progress = if (duration > 0) (progress / duration).toFloat() else 0f,
                            coverUrl = connectionViewModel.currentTrack.value.coverUrl,
                            baseUrl = libraryViewModel?.baseUrl ?: "http://${selectedDevice?.host ?: "192.168.1.34"}:${selectedDevice?.port ?: 5000}",
                            isMobilePlayback = isMobilePlayback,
                            onBackToLibrary = {
                                currentScreen = "library"
                            },
                            onPlayPauseToggle = { 
                                if (isPlaying) {
                                    connectionViewModel.pause()
                                } else {
                                    connectionViewModel.play()
                                }
                            },
                            onSkipNext = { connectionViewModel.next() },
                            onSkipPrevious = { connectionViewModel.previous() },
                            onSeek = { progressRatio ->
                                connectionViewModel.wsClient.sendSeek(progressRatio * duration)
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
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onStop() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
        super.onStop()
    }
}
