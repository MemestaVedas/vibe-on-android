package moe.memesta.vibeon

import android.graphics.Bitmap
import android.util.Log
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaMetadata
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import moe.memesta.vibeon.data.MediaSessionData
import moe.memesta.vibeon.data.WebSocketClient
import moe.memesta.vibeon.widget.WidgetUpdater

/**
 * Singleton bridge between WebSocketClient and PlaybackService.
 * Starts/stops silent playback to keep the media notification alive
 * and keeps track metadata + action states synced.
 */
object MediaNotificationManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var wsClient: WebSocketClient? = null
        private set

    var isShuffled: Boolean = false
        private set
    var isCurrentFavorite: Boolean = false
        private set
    var isMobilePlayback: Boolean = false
        private set
    var repeatMode: String = "off"
        private set
    var volume: Double = 0.5
        private set
    var currentTrackPath: String? = null
        private set
    /** Up to 4 most-recent playlists pushed into widget state. */
    var widgetPlaylists: List<moe.memesta.vibeon.widget.PlaylistWidgetInfo> = emptyList()
        private set

    private var serviceRef: PlaybackService? = null
    private var lastArtBitmap: Bitmap? = null

    // ------------------------------------------------------------------
    // Initialisation
    // ------------------------------------------------------------------

    fun attach(client: WebSocketClient) {
        wsClient = client
        observeWebSocketState(client)
    }

    fun registerService(service: PlaybackService) { serviceRef = service }
    fun unregisterService() { serviceRef = null }

    fun exitSilentMode() { serviceRef?.exitSilentMode() }
    fun reenterSilentMode() { serviceRef?.reenterSilentMode() }

    // ------------------------------------------------------------------
    // Observe WebSocket → drive the service
    // ------------------------------------------------------------------

    private fun observeWebSocketState(client: WebSocketClient) {

        // Connection state → start/stop the foreground service
        client.isConnected.onEach { connected ->
            val ctx = VibeonApp.instance
            if (connected) {
                Log.i("MediaNotifManager", "🟢 PC connected — starting PlaybackService")
                ContextCompat.startForegroundService(ctx, Intent(ctx, PlaybackService::class.java))
                // Service will call registerService() in onCreate → then we start silent playback
            } else {
                Log.i("MediaNotifManager", "🔴 PC disconnected — stopping PlaybackService")
                serviceRef?.stopSilentPlayback()
                ctx.stopService(Intent(ctx, PlaybackService::class.java))
            }
        }.launchIn(scope)

        // Track metadata + play state → update notification
        combine(
            client.currentTrack,
            client.isPlaying
        ) { track, playing -> Pair(track, playing) }
            .onEach { (track, playing) ->
                val service = serviceRef ?: return@onEach
                currentTrackPath = track.path

                // Start silent playback if not already active and not mobile streaming
                if (!service.isSilent() && !isMobilePlayback) {
                    service.startSilentPlayback()
                }

                // Load album art offline then update
                scope.launch(Dispatchers.IO) {
                    val artBitmap: Bitmap? = track.coverUrl?.let { url ->
                        try {
                            val loader = ImageLoader(VibeonApp.instance)
                            val req = ImageRequest.Builder(VibeonApp.instance).data(url).build()
                            val result = loader.execute(req)
                            (result as? SuccessResult)?.drawable?.let { d ->
                                if (d is android.graphics.drawable.BitmapDrawable) d.bitmap else null
                            }
                        } catch (e: Exception) {
                            Log.w("MediaNotifManager", "Cover art load failed: ${e.message}")
                            null
                        }
                    }
                    lastArtBitmap = artBitmap

                    // Update the home-screen widget
                    WidgetUpdater.onTrackChanged(track, playing)

                    scope.launch(Dispatchers.Main) {
                        val metadata = MediaMetadata.Builder()
                            .setTitle(track.title.ifEmpty { "No Track" })
                            .setArtist(track.artist.ifEmpty { "Unknown Artist" })
                            .setAlbumTitle(track.album)
                            .apply {
                                artBitmap?.let {
                                    setArtworkData(
                                        bitmapToByteArray(it),
                                        MediaMetadata.PICTURE_TYPE_FRONT_COVER
                                    )
                                }
                            }
                            .build()
                        service.updateMetadata(metadata)
                        service.syncPlayPauseState(playing)
                    }
                }
            }.launchIn(scope)

        // Shuffle
        client.isShuffled.onEach { shuffled ->
            isShuffled = shuffled
            serviceRef?.refreshCustomLayout()
            WidgetUpdater.onShuffleChanged(shuffled)
        }.launchIn(scope)

        // Favorites
        client.favorites.onEach { favs ->
            isCurrentFavorite = currentTrackPath?.let { favs.contains(it) } ?: false
            serviceRef?.refreshCustomLayout()
            WidgetUpdater.onFavoriteChanged(isCurrentFavorite)
        }.launchIn(scope)

        // Mobile playback state
        client.isMobilePlayback.onEach { mobile ->
            val wasMobile = isMobilePlayback
            isMobilePlayback = mobile
            
            if (mobile) {
                Log.i("MediaNotifManager", "📱 Mobile playback started — exiting silent mode")
                serviceRef?.exitSilentMode()
            } else if (wasMobile) {
                Log.i("MediaNotifManager", "🖥️ Mobile playback stopped — re-entering silent mode")
                serviceRef?.reenterSilentMode()
            }
            serviceRef?.refreshCustomLayout()
            WidgetUpdater.onOutputChanged(mobile)
        }.launchIn(scope)

        // Repeat mode
        client.repeatMode.onEach { mode ->
            repeatMode = mode
            WidgetUpdater.onRepeatChanged(mode)
        }.launchIn(scope)

        // Volume
        client.volume.onEach { vol ->
            volume = vol
            WidgetUpdater.onVolumeChanged(vol)
        }.launchIn(scope)

        // Playlists — keep a condensed list for the widget
        client.playlists.onEach { playlists ->
            widgetPlaylists = playlists.take(4).map { p ->
                moe.memesta.vibeon.widget.PlaylistWidgetInfo(id = p.id, name = p.name)
            }
            WidgetUpdater.onPlaylistsChanged(widgetPlaylists)
        }.launchIn(scope)
    }

    // ------------------------------------------------------------------
    // Utilities
    // ------------------------------------------------------------------

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        return stream.toByteArray()
    }
}
