package moe.memesta.vibeon

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import moe.memesta.vibeon.PlaybackService
import moe.memesta.vibeon.data.DiscoveryRepository
import moe.memesta.vibeon.data.StreamRepository
import moe.memesta.vibeon.data.DiscoveredDevice
import moe.memesta.vibeon.data.stats.LocalPlaybackStatsRepository
import moe.memesta.vibeon.ui.*
import moe.memesta.vibeon.ui.theme.VibeonTheme
import moe.memesta.vibeon.ui.navigation.AppNavHost
import android.content.ComponentName
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.common.Player
import com.google.common.util.concurrent.ListenableFuture
import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {
    private lateinit var discoveryRepository: DiscoveryRepository
    private lateinit var connectionViewModel: ConnectionViewModel
    private lateinit var localStatsRepository: LocalPlaybackStatsRepository
    private lateinit var playbackViewModel: PlaybackViewModel
    private lateinit var favoritesManager: moe.memesta.vibeon.data.local.FavoritesManager
    private lateinit var playerSettingsRepository: moe.memesta.vibeon.data.local.PlayerSettingsRepository
    private lateinit var onboardingManager: moe.memesta.vibeon.data.local.OnboardingManager
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var streamRepository: StreamRepository? = null
    private var playerListener: Player.Listener? = null

    private fun attachControllerListener(controller: MediaController) {
        playerListener?.let { controller.removeListener(it) }

        val listener = object : Player.Listener {
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
        }

        playerListener = listener
        controller.addListener(listener)
        playbackViewModel.setPlayer(controller)
    }

    // Runtime notification permission launcher (Android 13+)
    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Log.i("MainActivity", if (granted) "✅ Notification permission granted" else "❌ Notification permission denied")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
val appContainer = VibeonApp.instance.container

        discoveryRepository = appContainer.discoveryRepository
        favoritesManager = appContainer.favoritesManager
        playerSettingsRepository = appContainer.playerSettingsRepository
        localStatsRepository = appContainer.localStatsRepository
        onboardingManager = appContainer.onboardingManager

        // Use ViewModelProvider so ViewModels survive configuration changes
        connectionViewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return ConnectionViewModel(discoveryRepository, localStatsRepository, appContainer.webSocketClient) as T
            }
        })[ConnectionViewModel::class.java]

        playbackViewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return PlaybackViewModel(webSocketClient = appContainer.webSocketClient) as T
            }
        })[PlaybackViewModel::class.java]
        
        // Auto-start discovery for favorite device detection
        connectionViewModel.startScanning()
        
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
            // Observe current track cover for dynamic theming
            val currentTrack by connectionViewModel.currentTrack.collectAsState()
            val displayLanguage by playerSettingsRepository.displayLanguage.collectAsState()
            val coverBitmap = moe.memesta.vibeon.ui.theme.rememberBitmapFromUrl(currentTrack.coverUrl)
            
            CompositionLocalProvider(
                moe.memesta.vibeon.ui.utils.LocalDisplayLanguage provides displayLanguage
            ) {
                moe.memesta.vibeon.ui.theme.DynamicTheme(seedBitmap = coverBitmap) {
                // Initialize ViewModels or generic state if needed for global context
                // But for now, AppNavHost handles navigation
                
                val libraryDatabase = moe.memesta.vibeon.data.local.LibraryDatabase.getInstance(applicationContext)
                val trackDao = libraryDatabase.trackDao()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost(
                        connectionViewModel, 
                        playbackViewModel, 
                        favoritesManager, 
                        playerSettingsRepository,
                        trackDao,
                        onboardingManager
                    )
                }
            }
        }
    }
}
    override fun onStart() {
        super.onStart()
        mediaController?.let {
            attachControllerListener(it)
            return
        }

        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                val controller = controllerFuture?.get() ?: return@addListener
                mediaController = controller
                attachControllerListener(controller)
            } catch (e: Exception) {
                Log.e("MainActivity", "❌ Failed to connect to MediaController: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onStop() {
        // Remove listener to prevent accumulation
        playerListener?.let { mediaController?.removeListener(it) }
        playerListener = null
        // Do NOT release MediaController here — ExoPlayer must keep running
        // when the app is backgrounded (e.g. screen off, app switch).
        // The service's onDestroy handles cleanup.
        super.onStop()
    }

    override fun onDestroy() {
        playerListener?.let { mediaController?.removeListener(it) }
        playerListener = null
        try {
            controllerFuture?.let { MediaController.releaseFuture(it) }
        } catch (e: IllegalArgumentException) {
            Log.w("MainActivity", "Ignoring controller release error: ${e.message}")
        }
        mediaController = null
        controllerFuture = null
        super.onDestroy()
    }
}
