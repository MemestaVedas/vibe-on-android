package moe.memesta.vibeon.widget

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import moe.memesta.vibeon.VibeonApp
import moe.memesta.vibeon.MediaNotificationManager
import moe.memesta.vibeon.data.MediaSessionData
import java.io.ByteArrayOutputStream

/**
 * Singleton that pushes playback state into the Glance DataStore
 * and triggers widget recomposition. Call the update methods from
 * [moe.memesta.vibeon.MediaNotificationManager] whenever track or
 * play-state changes.
 */
object WidgetUpdater {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastCoverUrl: String? = null
    private var cachedArtBytes: ByteArray? = null

    fun onTrackChanged(track: MediaSessionData, isPlaying: Boolean) {
        val ctx = VibeonApp.instance
        scope.launch {
            // Fetch cover art if the URL changed
            val artBytes = if (track.coverUrl != null && track.coverUrl != lastCoverUrl) {
                lastCoverUrl = track.coverUrl
                loadCoverBytes(ctx, track.coverUrl)
            } else if (track.coverUrl == lastCoverUrl) {
                cachedArtBytes
            } else {
                lastCoverUrl = null
                null
            }
            cachedArtBytes = artBytes

            pushState(ctx, WidgetPlaybackState(
                title = track.title.ifEmpty { "No Track" },
                artist = track.artist.ifEmpty { "Unknown Artist" },
                isPlaying = isPlaying,
                isLiked = MediaNotificationManager.isCurrentFavorite,
                isShuffled = MediaNotificationManager.isShuffled,
                isMobilePlayback = MediaNotificationManager.isMobilePlayback,
                albumArtBytes = artBytes
            ))
        }
    }

    fun onPlayStateChanged(isPlaying: Boolean) {
        val ctx = VibeonApp.instance
        scope.launch {
            pushStateUpdate(ctx) { it.copy(isPlaying = isPlaying) }
        }
    }

    fun onOutputChanged(isMobilePlayback: Boolean) {
        val ctx = VibeonApp.instance
        scope.launch {
            pushStateUpdate(ctx) { it.copy(isMobilePlayback = isMobilePlayback) }
        }
    }

    fun onShuffleChanged(isShuffled: Boolean) {
        val ctx = VibeonApp.instance
        scope.launch {
            pushStateUpdate(ctx) { it.copy(isShuffled = isShuffled) }
        }
    }

    fun onFavoriteChanged(isLiked: Boolean) {
        val ctx = VibeonApp.instance
        scope.launch {
            pushStateUpdate(ctx) { it.copy(isLiked = isLiked) }
        }
    }

    // ------------------------------------------------------------------

    private suspend fun pushState(context: Context, state: WidgetPlaybackState) {
        try {
            val manager = GlanceAppWidgetManager(context)
            val ids = manager.getGlanceIds(AlbumArtWidget::class.java)
            ids.forEach { id ->
                updateAppWidgetState(context, WidgetStateDefinition, id) { state }
                AlbumArtWidget().update(context, id)
            }
        } catch (e: Exception) {
            Log.w("WidgetUpdater", "pushState failed", e)
        }
    }

    private suspend fun pushStateUpdate(
        context: Context,
        transform: (WidgetPlaybackState) -> WidgetPlaybackState
    ) {
        try {
            val manager = GlanceAppWidgetManager(context)
            val ids = manager.getGlanceIds(AlbumArtWidget::class.java)
            ids.forEach { id ->
                updateAppWidgetState(context, WidgetStateDefinition, id, transform)
                AlbumArtWidget().update(context, id)
            }
        } catch (e: Exception) {
            Log.w("WidgetUpdater", "pushStateUpdate failed", e)
        }
    }

    private suspend fun loadCoverBytes(context: Context, url: String): ByteArray? {
        return try {
            val loader = ImageLoader(context)
            val req = ImageRequest.Builder(context).data(url).size(1024).build()
            val result = loader.execute(req)
            val bitmap = (result as? SuccessResult)?.drawable?.let { d ->
                if (d is android.graphics.drawable.BitmapDrawable) d.bitmap else null
            } ?: return null
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            stream.toByteArray()
        } catch (e: Exception) {
            Log.w("WidgetUpdater", "Cover art download failed: ${e.message}")
            null
        }
    }
}
