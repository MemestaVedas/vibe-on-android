package moe.memesta.vibeon.widget

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.palette.graphics.Palette
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
import java.io.File

/**
 * Singleton that pushes playback state into the Glance DataStore
 * and triggers widget recomposition. Call the update methods from
 * [moe.memesta.vibeon.MediaNotificationManager] whenever track or
 * play-state changes.
 */
object WidgetUpdater {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastCoverUrl: String? = null
    private var cachedArtPath: String? = null
    private var cachedColors: IntArray = defaultPaletteColors()

    fun onTrackChanged(track: MediaSessionData, isPlaying: Boolean) {
        val ctx = VibeonApp.instance
        scope.launch {
            // Fetch cover art (and extract palette) only when the URL changes
            val (artPath, colors) = when {
                track.coverUrl != null && track.coverUrl != lastCoverUrl -> {
                    lastCoverUrl = track.coverUrl
                    loadCoverData(ctx, track.coverUrl)
                }
                track.coverUrl == lastCoverUrl -> Pair(cachedArtPath, cachedColors)
                else -> {
                    lastCoverUrl = null
                    Pair(null, defaultPaletteColors())
                }
            }
            cachedArtPath  = artPath
            cachedColors   = colors

            pushState(ctx, WidgetPlaybackState(
                title            = track.title.ifEmpty { "No Track" },
                artist           = track.artist.ifEmpty { "Unknown Artist" },
                isPlaying        = isPlaying,
                isLiked          = MediaNotificationManager.isCurrentFavorite,
                isShuffled       = MediaNotificationManager.isShuffled,
                isMobilePlayback = MediaNotificationManager.isMobilePlayback,
                albumArtPath     = artPath,
                colorPrimary               = colors[0],
                colorOnPrimary             = colors[1],
                colorSecondary             = colors[2],
                colorOnSecondary           = colors[3],
                colorError                 = colors[4],
                colorPrimaryContainer      = colors[5],
                colorOnPrimaryContainer    = colors[6],
                colorSecondaryContainer    = colors[7],
                colorOnSecondaryContainer  = colors[8],
                colorErrorContainer        = colors[9],
                colorOnErrorContainer      = colors[10],
                repeatMode       = MediaNotificationManager.repeatMode,
                volumeLevel      = volumeDoubleToLevel(MediaNotificationManager.volume),
                widgetPlaylists  = MediaNotificationManager.widgetPlaylists
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

    fun onRepeatChanged(repeatMode: String) {
        val ctx = VibeonApp.instance
        scope.launch {
            pushStateUpdate(ctx) { it.copy(repeatMode = repeatMode) }
        }
    }

    fun onVolumeChanged(volume: Double) {
        val ctx = VibeonApp.instance
        scope.launch {
            pushStateUpdate(ctx) { it.copy(volumeLevel = volumeDoubleToLevel(volume)) }
        }
    }

    fun onPlaylistsChanged(playlists: List<PlaylistWidgetInfo>) {
        val ctx = VibeonApp.instance
        scope.launch {
            pushStateUpdate(ctx) { it.copy(widgetPlaylists = playlists) }
        }
    }

    /** Toggles the more-options overlay on all widget instances. */
    fun onMoreOptionsChanged(showing: Boolean) {
        val ctx = VibeonApp.instance
        scope.launch {
            pushStateUpdate(ctx) { it.copy(showingMoreOptions = showing) }
        }
    }

    // ------------------------------------------------------------------

    private suspend fun pushState(context: Context, state: WidgetPlaybackState) {
        try {
            val manager = GlanceAppWidgetManager(context)
            for (widgetClass in listOf(AlbumArtWidget::class.java, AlbumArtWidgetSolid::class.java)) {
                val ids = manager.getGlanceIds(widgetClass)
                ids.forEach { id ->
                    updateAppWidgetState(context, WidgetStateDefinition, id) { state }
                }
            }
            // Trigger recomposition after all state is written
            manager.getGlanceIds(AlbumArtWidget::class.java).forEach { AlbumArtWidget().update(context, it) }
            manager.getGlanceIds(AlbumArtWidgetSolid::class.java).forEach { AlbumArtWidgetSolid().update(context, it) }
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
            for (widgetClass in listOf(AlbumArtWidget::class.java, AlbumArtWidgetSolid::class.java)) {
                val ids = manager.getGlanceIds(widgetClass)
                ids.forEach { id ->
                    updateAppWidgetState(context, WidgetStateDefinition, id, transform)
                }
            }
            manager.getGlanceIds(AlbumArtWidget::class.java).forEach { AlbumArtWidget().update(context, it) }
            manager.getGlanceIds(AlbumArtWidgetSolid::class.java).forEach { AlbumArtWidgetSolid().update(context, it) }
        } catch (e: Exception) {
            Log.w("WidgetUpdater", "pushStateUpdate failed", e)
        }
    }

    private suspend fun loadCoverData(context: Context, url: String): Pair<String?, IntArray> {
        return try {
            val loader = ImageLoader(context)
            // Keep album art small — Glance RemoteViews has a tight bundle size limit
            // 192 px keeps the decompressed ARGB_8888 bitmap under ~150 KB.
            // That comfortably fits inside the ~500 KB practical limit for all
            // bitmaps in a single RemoteViews Binder parcel.
            val req = ImageRequest.Builder(context).data(url).size(192).build()
            val result = loader.execute(req)
            val bitmap = (result as? SuccessResult)?.drawable?.let { d ->
                if (d is android.graphics.drawable.BitmapDrawable) d.bitmap else null
            } ?: return Pair(null, defaultPaletteColors())
            // Extract palette on the full-quality bitmap before compression
            val colors = extractPaletteColors(bitmap)
            // Save to internal storage — passing a file path through RemoteViews IPC
            // avoids the 1 MB bundle size limit that would otherwise kill the bitmap.
            val file = File(context.filesDir, "widget_art.jpg")
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
            Pair(file.absolutePath, colors)
        } catch (e: Exception) {
            Log.w("WidgetUpdater", "Cover art download failed: ${e.message}")
            Pair(null, defaultPaletteColors())
        }
    }

    private fun extractPaletteColors(bitmap: Bitmap): IntArray {
        val palette = Palette.from(bitmap).maximumColorCount(16).generate()
        val primary = palette.vibrantSwatch
            ?: palette.dominantSwatch
            ?: palette.darkVibrantSwatch
            ?: palette.darkMutedSwatch
        val secondary = palette.mutedSwatch
            ?: palette.lightVibrantSwatch
            ?: palette.lightMutedSwatch

        val primaryRgb   = primary?.rgb           ?: 0xFF1C1B1F.toInt()
        val secondaryRgb = secondary?.rgb          ?: 0xFF49454F.toInt()

        // Derive Material-3-style dark-theme container colours from the palette swatches.
        // Container = same hue, heavily darkened value (dark bg) with near-black saturation.
        // OnContainer = same hue, dramatically lightened (mostly-white tinted with hue).
        val primaryContainer    = deriveContainerColor(primaryRgb)
        val onPrimaryContainer  = deriveOnContainerColor(primaryRgb)
        val secondContainer     = deriveContainerColor(secondaryRgb)
        val onSecondContainer   = deriveOnContainerColor(secondaryRgb)

        return intArrayOf(
            primaryRgb,                             // [0] colorPrimary
            primary?.bodyTextColor ?: 0xFFFFFFFF.toInt(), // [1] colorOnPrimary
            secondaryRgb,                           // [2] colorSecondary
            secondary?.bodyTextColor ?: 0xFFFFFFFF.toInt(), // [3] colorOnSecondary
            0xFFE53935.toInt(),                     // [4] colorError (constant)
            primaryContainer,                       // [5] colorPrimaryContainer
            onPrimaryContainer,                     // [6] colorOnPrimaryContainer
            secondContainer,                        // [7] colorSecondaryContainer
            onSecondContainer,                      // [8] colorOnSecondaryContainer
            0xFF8C1D18.toInt(),                     // [9] colorErrorContainer
            0xFFF9DEDC.toInt()                      // [10] colorOnErrorContainer
        )
    }

    /** Darkens a colour's HSV Value to produce a dark M3-style container. */
    private fun deriveContainerColor(rgb: Int): Int {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(rgb, hsv)
        // Compress saturation and clamp value to a dark range (0.10 – 0.30)
        hsv[1] = (hsv[1] * 0.6f).coerceIn(0f, 1f)
        hsv[2] = (hsv[2] * 0.28f + 0.05f).coerceIn(0.05f, 0.35f)
        return android.graphics.Color.HSVToColor(hsv)
    }

    /** Produces a light on-container colour from the same hue (near-white tinted). */
    private fun deriveOnContainerColor(rgb: Int): Int {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(rgb, hsv)
        // Same hue, very low saturation, very high value → near-white with hue tint
        hsv[1] = (hsv[1] * 0.15f).coerceIn(0f, 1f)
        hsv[2] = 0.93f
        return android.graphics.Color.HSVToColor(hsv)
    }

    private fun defaultPaletteColors() = intArrayOf(
        0xFF1C1B1F.toInt(), // colorPrimary
        0xFFFFFFFF.toInt(), // colorOnPrimary
        0xFF49454F.toInt(), // colorSecondary
        0xFFFFFFFF.toInt(), // colorOnSecondary
        0xFFE53935.toInt(), // colorError
        0xFF1F3140.toInt(), // colorPrimaryContainer
        0xFFD3E4F7.toInt(), // colorOnPrimaryContainer
        0xFF2B2535.toInt(), // colorSecondaryContainer
        0xFFEADDFF.toInt(), // colorOnSecondaryContainer
        0xFF8C1D18.toInt(), // colorErrorContainer
        0xFFF9DEDC.toInt()  // colorOnErrorContainer
    )

    private fun volumeDoubleToLevel(volume: Double): Int = when {
        volume > 0.75 -> 2
        volume > 0.1  -> 1
        else          -> 0
    }
}
