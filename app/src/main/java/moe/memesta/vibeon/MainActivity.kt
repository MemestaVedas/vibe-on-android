package moe.memesta.vibeon

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
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
import moe.memesta.vibeon.ui.*
import moe.memesta.vibeon.ui.theme.VibeonTheme
import moe.memesta.vibeon.ui.navigation.AppNavHost
import android.content.ComponentName
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.common.Player
import com.google.common.util.concurrent.ListenableFuture
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val connectionViewModel: ConnectionViewModel by viewModels()
    private val playbackViewModel: PlaybackViewModel by viewModels()

    @Inject
    lateinit var favoritesManager: moe.memesta.vibeon.data.local.FavoritesManager

    @Inject
    lateinit var playerSettingsRepository: moe.memesta.vibeon.data.local.PlayerSettingsRepository

    @Inject
    lateinit var onboardingManager: moe.memesta.vibeon.data.local.OnboardingManager

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var playerListener: Player.Listener? = null
    private var controllerRetryJob: Job? = null
    private var controllerRetryCount = 0

    companion object {
        private const val CONTROLLER_MAX_RETRIES = 8
        private val POCO_F1_IDENTIFIERS = listOf("beryllium", "poco f1", "pocophone f1")
    }

    private fun isAllowedDebugDevice(): Boolean {
        val fields = listOf(
            Build.DEVICE,
            Build.PRODUCT,
            Build.MODEL,
            Build.MANUFACTURER,
            Build.BRAND
        )
        return fields.any { field ->
            val normalized = field?.lowercase().orEmpty()
            POCO_F1_IDENTIFIERS.any { normalized.contains(it) }
        }
    }

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

    private fun ensureMediaControllerConnected(forceReconnect: Boolean = false) {
        mediaController?.let {
            attachControllerListener(it)
            return
        }

        if (!forceReconnect && controllerFuture != null) {
            return
        }

        if (forceReconnect) {
            try {
                controllerFuture?.let { MediaController.releaseFuture(it) }
            } catch (e: IllegalArgumentException) {
                Log.w("MainActivity", "Ignoring controller future release error: ${e.message}")
            }
            controllerFuture = null
        }

        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                val controller = controllerFuture?.get() ?: return@addListener
                mediaController = controller
                controllerRetryCount = 0
                controllerRetryJob?.cancel()
                controllerRetryJob = null
                attachControllerListener(controller)
                Log.i("MainActivity", "✅ MediaController connected")
            } catch (e: Exception) {
                Log.w("MainActivity", "MediaController connect attempt failed: ${e.message}")
                scheduleControllerRetry()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun scheduleControllerRetry() {
        if (controllerRetryCount >= CONTROLLER_MAX_RETRIES) {
            Log.e("MainActivity", "❌ MediaController retries exhausted")
            return
        }
        controllerRetryCount += 1
        val delayMs = (500L * (1L shl (controllerRetryCount - 1).coerceAtMost(4))).coerceAtMost(5000L)
        controllerRetryJob?.cancel()
        controllerRetryJob = lifecycleScope.launch {
            delay(delayMs)
            ensureMediaControllerConnected(forceReconnect = true)
        }
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

        if (BuildConfig.DEBUG && !isAllowedDebugDevice()) {
            setContent {
                VibeonTheme {
                    DebugDeviceRestrictionScreen()
                }
            }
            return
        }

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                connectionViewModel.wsIsConnected.collect { connected ->
                    if (connected && mediaController == null) {
                        ensureMediaControllerConnected(forceReconnect = false)
                    }
                }
            }
        }

        setContent {
            // Observe current track cover for dynamic theming
            val currentTrack by connectionViewModel.currentTrack.collectAsState()
            val displayLanguage by playerSettingsRepository.displayLanguage.collectAsState()
            val albumViewStyle by playerSettingsRepository.albumViewStyle.collectAsState()
            val artistViewStyle by playerSettingsRepository.artistViewStyle.collectAsState()
            val coverBitmap = moe.memesta.vibeon.ui.theme.rememberBitmapFromUrl(currentTrack.coverUrl)

            CompositionLocalProvider(
                moe.memesta.vibeon.ui.utils.LocalDisplayLanguage provides displayLanguage,
                moe.memesta.vibeon.ui.utils.LocalAlbumViewStyle provides albumViewStyle,
                moe.memesta.vibeon.ui.utils.LocalArtistViewStyle provides artistViewStyle
            ) {
                moe.memesta.vibeon.ui.theme.DynamicTheme(seedBitmap = coverBitmap) {
                    // Initialize ViewModels or generic state if needed for global context
                    // But for now, AppNavHost handles navigation

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavHost(
                            connectionViewModel,
                            playbackViewModel,
                            favoritesManager,
                            playerSettingsRepository,
                            trackDaoProvider = { moe.memesta.vibeon.data.local.LibraryDatabase.getInstance(applicationContext).trackDao() },
                            onboardingManager
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        ensureMediaControllerConnected(forceReconnect = false)
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
        controllerRetryJob?.cancel()
        controllerRetryJob = null
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

@Composable
private fun DebugDeviceRestrictionScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Debug build locked to Poco F1",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Connect and run on Poco F1 (beryllium) for this debug cycle.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
