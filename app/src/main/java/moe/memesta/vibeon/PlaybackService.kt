package moe.memesta.vibeon

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.ForwardingPlayer
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import moe.memesta.vibeon.data.P2PDataSource
import moe.memesta.vibeon.data.StreamRepository
// WidgetActions removed; use local action constants instead
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream

class PlaybackService : MediaSessionService() {

    companion object {
        var streamRepository: StreamRepository? = null
        var isOfflineMode = false

        const val ACTION_SHUFFLE = "moe.memesta.vibeon.ACTION_SHUFFLE"
        const val ACTION_REPEAT = "moe.memesta.vibeon.ACTION_REPEAT"
        const val ACTION_TOGGLE_OUTPUT = "moe.memesta.vibeon.ACTION_TOGGLE_OUTPUT"
        const val ACTION_PLAY_PAUSE = "moe.memesta.vibeon.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "moe.memesta.vibeon.ACTION_NEXT"
        const val ACTION_PREVIOUS = "moe.memesta.vibeon.ACTION_PREVIOUS"
    }

    private var mediaSession: MediaSession? = null
    lateinit var player: ExoPlayer
        private set
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Are we in "notification-only" mode (playing silence) vs real mobile streaming?
    private var isSilentMode = false
    private var isForwardingEnabled = true
    
    // Prevent rapid-fire 'next' commands to stop infinite skipping loops
    private var lastNextSentTime = 0L
    private val SKIP_DEBOUNCE_MS = 2000L

    // ---------------------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()

        val resolvedStreamRepository = runCatching {
            streamRepository ?: VibeonApp.instance.container.streamRepository.also {
                streamRepository = it
            }
        }.getOrNull()

        val dataSourceFactory: DataSource.Factory = if (resolvedStreamRepository != null) {
            DataSource.Factory { P2PDataSource(resolvedStreamRepository, CoroutineScope(SupervisorJob())) }
        } else {
            DefaultDataSource.Factory(this)
        }

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setWakeMode(androidx.media3.common.C.WAKE_MODE_NETWORK)
            .build()

        // Wrap in a ForwardingPlayer that sends all controls to PC via WebSocket
        val forwardingPlayer = VibeonForwardingPlayer(player)

        val activityIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, forwardingPlayer)
            .setSessionActivity(activityIntent)
            .setCallback(VibeonSessionCallback())
            .build()

        MediaNotificationManager.registerService(this)
        Log.i("PlaybackService", "✅ Service created")
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                if (MediaNotificationManager.isMobilePlayback || isOfflineMode) {
                    // In mobile/offline mode, control local player directly.
                    if (player.isPlaying) player.pause() else player.play()
                } else {
                    // In PC-controlled mode, forward to desktop.
                    val ws = MediaNotificationManager.wsClient
                    if (ws?.isConnected?.value == true) {
                        if (player.isPlaying) ws.sendPause() else ws.sendPlay()
                    } else {
                        if (player.isPlaying) player.pause() else player.play()
                    }
                }
                Log.i("PlaybackService", "Widget: play/pause")
            }
            ACTION_NEXT -> {
                MediaNotificationManager.wsClient?.sendNext()
                Log.i("PlaybackService", "Widget: next")
            }
            ACTION_PREVIOUS -> {
                MediaNotificationManager.wsClient?.sendPrevious()
                Log.i("PlaybackService", "Widget: previous")
            }
            ACTION_SHUFFLE -> {
                MediaNotificationManager.wsClient?.sendToggleShuffle()
                Log.i("PlaybackService", "Widget: shuffle")
            }
            ACTION_REPEAT -> {
                MediaNotificationManager.wsClient?.sendToggleRepeat()
                Log.i("PlaybackService", "Widget: repeat")
            }
            ACTION_TOGGLE_OUTPUT -> {
                if (MediaNotificationManager.isMobilePlayback) {
                    MediaNotificationManager.wsClient?.sendStopMobilePlayback()
                } else {
                    MediaNotificationManager.wsClient?.sendStartMobilePlayback()
                }
                Log.i("PlaybackService", "Widget: toggle output")
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        MediaNotificationManager.unregisterService()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (MediaNotificationManager.wsClient?.isConnected?.value == true) {
            Log.i("PlaybackService", "App killed but PC connected — service stays alive")
        } else {
            stopSelf()
        }
    }

    // ---------------------------------------------------------------------------
    // Silent playback — tricks Android into showing media notification
    // ---------------------------------------------------------------------------

    fun startSilentPlayback() {
        if (isSilentMode) return
        
            val silenceMediaSource = androidx.media3.exoplayer.source.SilenceMediaSource(Long.MAX_VALUE)

        val metadata = MediaMetadata.Builder()
            .setTitle("Vibe-On")
            .setArtist("Connected to PC")
            .build()
            
        val item = MediaItem.Builder()
            .setMediaId("silence")
            .setUri("silence://infinite")
            .setMediaMetadata(metadata)
            .build()
            
        isForwardingEnabled = false
        // SilenceMediaSource doesn't need MediaItem set via setMediaSource this way, it has its own empty item. Wait, SilenceMediaSource takes duration.
        // Wait, Media3 SilenceMediaSource constructor is `SilenceMediaSource(Long)`
        player.setMediaSource(silenceMediaSource)
        player.playWhenReady = true
        // No need for REPEAT_MODE_ONE since SilenceDataSource is practically infinite
        player.repeatMode = Player.REPEAT_MODE_OFF
        player.volume = 0f
        player.prepare()
        player.play()
        isForwardingEnabled = true
        
        isSilentMode = true
        refreshCustomLayout()
        Log.i("PlaybackService", "🔇 Silent playback started using infinite silence source")
    }

    fun stopSilentPlayback() {
        player.stop()
        player.clearMediaItems()
        isSilentMode = false
        Log.i("PlaybackService", "⏹️ Silent playback stopped")
    }

    /**
     * Update track metadata shown in the notification.
     * If in silent mode, replaces the silence item with updated metadata.
     */
    fun updateMetadata(metadata: MediaMetadata) {
        if (isSilentMode) {
            val metadataWithDuration = metadata.buildUpon().build()
            Log.d("PlaybackService", "📝 Updating Silent Loop Metadata: ${metadata.title}")

            // To update metadata for a MediaSource already playing, we replace the media item
            val item = MediaItem.Builder()
                .setMediaId("silence")
                .setUri("silence://infinite")
                .setMediaMetadata(metadataWithDuration)
                .build()

            isForwardingEnabled = false
            player.replaceMediaItem(0, item)
            isForwardingEnabled = true
        } else {
            // In real streaming mode, we just update the player current item's metadata
            // Media3 will automatically refresh the notification.
            Log.d("PlaybackService", "📝 Updating Real Stream Metadata: ${metadata.title}")
            // Note: replaceMediaItem(current, ...) might restart playback. 
            // In mobile mode, we rely on the PlaybackViewModel setting metadata on MediaItem.
        }
    }

    /**
     * Sync the play/pause visual state in the notification.
     * In silent mode, we pause/resume the silent loop to toggle the icon.
     */
    fun syncPlayPauseState(isPlaying: Boolean) {
        if (!isSilentMode) return
        isForwardingEnabled = false
        if (isPlaying && !player.isPlaying) {
            player.play()
        } else if (!isPlaying && player.isPlaying) {
            player.pause()
        }
        isForwardingEnabled = true
    }

    /**
     * Called by PlaybackViewModel when mobile streaming stops.
     * Re-enters silent mode to keep the notification up.
     */
    fun reenterSilentMode() {
        isSilentMode = false // reset so startSilentPlayback runs
        startSilentPlayback()
    }

    /**
     * Called right before mobile streaming starts.
     * Exits silent mode so the real stream URL can take over.
     */
    fun exitSilentMode() {
        Log.i("PlaybackService", "🔓 Exiting silent mode for mobile streaming")
        isSilentMode = false
        isForwardingEnabled = false
        player.stop()
        player.clearMediaItems()
        isForwardingEnabled = true
    }

    fun isSilent(): Boolean = isSilentMode

    // ---------------------------------------------------------------------------
    // Custom notification buttons
    // ---------------------------------------------------------------------------

    fun refreshCustomLayout() {
        val session = mediaSession ?: return
        val isShuffled = MediaNotificationManager.isShuffled
        val repeatMode = MediaNotificationManager.repeatMode

        val shuffleButton = CommandButton.Builder()
            .setDisplayName(if (isShuffled) "Shuffle On" else "Shuffle Off")
            .setSessionCommand(SessionCommand(ACTION_SHUFFLE, Bundle.EMPTY))
            .setIconResId(
                R.drawable.ic_widget_shuffle
            )
            .setEnabled(true)
            .build()

        val repeatButton = CommandButton.Builder()
            .setDisplayName(
                when (repeatMode) {
                    "one" -> "Repeat One"
                    "all" -> "Repeat All"
                    else -> "Repeat Off"
                }
            )
            .setSessionCommand(SessionCommand(ACTION_REPEAT, Bundle.EMPTY))
            .setIconResId(
                when (repeatMode) {
                    "one" -> R.drawable.ic_widget_repeat_one
                    else -> R.drawable.ic_widget_repeat
                }
            )
            .setEnabled(true)
            .build()

        session.setCustomLayout(ImmutableList.of(shuffleButton, repeatButton))
        Log.i("PlaybackService", "✨ Custom layout refreshed: Shuffle=$isShuffled, Repeat=$repeatMode")
    }

    internal fun getSession(): MediaSession? = mediaSession

    // ---------------------------------------------------------------------------
    // ForwardingPlayer — intercepts media controls → sends to PC via WebSocket
    // ---------------------------------------------------------------------------

    private inner class VibeonForwardingPlayer(player: ExoPlayer) : ForwardingPlayer(player) {

        // Always advertise next/prev commands so the notification shows skip buttons
        // even though the underlying ExoPlayer only has one silent item on repeat.
        override fun getAvailableCommands(): Player.Commands {
            return super.getAvailableCommands().buildUpon()
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                .build()
        }

        override fun isCommandAvailable(command: Int): Boolean {
            return when (command) {
                Player.COMMAND_SEEK_TO_NEXT,
                Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                Player.COMMAND_SEEK_TO_PREVIOUS,
                Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM -> true
                else -> super.isCommandAvailable(command)
            }
        }

        override fun getDuration(): Long {
            if (isSilentMode && MediaNotificationManager.wsClient != null) {
                return (MediaNotificationManager.wsClient!!.duration.value * 1000).toLong()
            }
            return super.getDuration()
        }

        override fun getCurrentPosition(): Long {
            if (isSilentMode && MediaNotificationManager.wsClient != null) {
                return (MediaNotificationManager.wsClient!!.progress.value * 1000).toLong()
            }
            return super.getCurrentPosition()
        }

        override fun getBufferedPosition(): Long {
            if (isSilentMode && MediaNotificationManager.wsClient != null) {
                return (MediaNotificationManager.wsClient!!.progress.value * 1000).toLong()
            }
            return super.getBufferedPosition()
        }

        override fun getTotalBufferedDuration(): Long {
            if (isSilentMode) return 0
            return super.getTotalBufferedDuration()
        }

        override fun seekTo(positionMs: Long) {
            if (isSilentMode && !isOfflineMode) {
                // In silent mode, everything is on PC
                if (isForwardingEnabled) {
                    MediaNotificationManager.wsClient?.sendSeek(positionMs / 1000.0)
                    Log.i("PlaybackService", "📍 Seek to ${positionMs}ms forwarded to PC (Silent Mode)")
                }
            } else {
                // In real mobile streaming, seek locally
                // PlaybackViewModel listener will notify PC of the position change
                super.seekTo(positionMs)
                Log.i("PlaybackService", "📍 Local playback seeking to ${positionMs}ms")
            }
        }

        override fun play() {
            super.play()
            if (isForwardingEnabled && !isOfflineMode && isSilentMode) {
                // Only forward to PC when in silent mode (PC is playing, not mobile)
                MediaNotificationManager.wsClient?.sendPlay()
                Log.i("PlaybackService", "▶️ Play forwarded to PC")
            }
        }

        override fun pause() {
            super.pause()
            if (isForwardingEnabled && !isOfflineMode && isSilentMode) {
                // Only forward to PC when in silent mode (PC is playing, not mobile)
                MediaNotificationManager.wsClient?.sendPause()
                Log.i("PlaybackService", "⏸️ Pause forwarded to PC")
            }
        }

        override fun seekToNext() {
            if (isForwardingEnabled && !isOfflineMode) {
                val now = System.currentTimeMillis()
                if (now - lastNextSentTime > SKIP_DEBOUNCE_MS) {
                    lastNextSentTime = now
                    MediaNotificationManager.wsClient?.sendNext()
                    Log.i("PlaybackService", "⏭️ Next forwarded to PC")
                }
            } else if (!isSilentMode || isOfflineMode) {
                super.seekToNext()
            }
        }

        override fun seekToPrevious() {
            if (isForwardingEnabled && !isOfflineMode) {
                MediaNotificationManager.wsClient?.sendPrevious()
                Log.i("PlaybackService", "⏮️ Previous forwarded to PC")
            } else if (!isSilentMode || isOfflineMode) {
                super.seekToPrevious()
            }
        }

        override fun seekToNextMediaItem() {
            if (isForwardingEnabled && !isOfflineMode) {
                MediaNotificationManager.wsClient?.sendNext()
            } else if (!isSilentMode || isOfflineMode) {
                super.seekToNextMediaItem()
            }
        }

        override fun seekToPreviousMediaItem() {
            if (isForwardingEnabled && !isOfflineMode) {
                MediaNotificationManager.wsClient?.sendPrevious()
            } else if (!isSilentMode || isOfflineMode) {
                super.seekToPreviousMediaItem()
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Session Callback — handles custom command button taps
    // ---------------------------------------------------------------------------

    private inner class VibeonSessionCallback : MediaSession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                .buildUpon()
                .add(SessionCommand(ACTION_SHUFFLE, Bundle.EMPTY))
                .add(SessionCommand(ACTION_REPEAT, Bundle.EMPTY))
                .add(SessionCommand(ACTION_TOGGLE_OUTPUT, Bundle.EMPTY))
                .build()

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            val wsClient = MediaNotificationManager.wsClient
            Log.i("PlaybackService", "🔔 Custom command: ${customCommand.customAction}")
            when (customCommand.customAction) {
                ACTION_SHUFFLE -> wsClient?.sendToggleShuffle()
                ACTION_REPEAT -> wsClient?.sendToggleRepeat()
                ACTION_TOGGLE_OUTPUT -> {
                    if (MediaNotificationManager.isMobilePlayback) {
                        wsClient?.sendStopMobilePlayback()
                    } else {
                        wsClient?.sendStartMobilePlayback()
                    }
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }
}
