package moe.memesta.vibeon.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.glance.appwidget.updateAll
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.SchemeTonalSpot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import moe.memesta.vibeon.VibeonApp
import moe.memesta.vibeon.MediaNotificationManager
import moe.memesta.vibeon.data.MediaSessionData
import moe.memesta.vibeon.data.local.DisplayLanguage
import moe.memesta.vibeon.ui.utils.getDisplayArtist
import moe.memesta.vibeon.ui.utils.getDisplayAlbum
import moe.memesta.vibeon.ui.utils.getDisplayName
import java.io.ByteArrayOutputStream
import androidx.core.graphics.drawable.toBitmap

/**
 * Singleton that updates widget state when playback changes.
 * Call methods from MediaNotificationManager on track/state changes.
 */
object WidgetUpdater {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastCoverUrl: String? = null
    private var cachedAlbumArtData: ByteArray? = null
    private var cachedColors: PaletteColors? = null

    private data class PaletteColors(
        val primary: Int,
        val onPrimary: Int,
        val secondary: Int,
        val onSecondary: Int,
        val error: Int,
        val primaryContainer: Int,
        val onPrimaryContainer: Int,
        val secondaryContainer: Int,
        val onSecondaryContainer: Int,
        val errorContainer: Int,
        val onErrorContainer: Int
    )

    /**
     * Updates widget with new track information.
     */
    fun onTrackChanged(track: MediaSessionData, isPlaying: Boolean, displayLanguage: DisplayLanguage) {
        val context = VibeonApp.instance
        scope.launch {
            // Download and cache album art only when URL changes
            val albumArtData = when {
                track.coverUrl != null && track.coverUrl != lastCoverUrl -> {
                    lastCoverUrl = track.coverUrl
                    downloadAlbumArt(context, track.coverUrl)
                }
                track.coverUrl == lastCoverUrl -> cachedAlbumArtData
                else -> {
                    lastCoverUrl = null
                    null
                }
            }
            cachedAlbumArtData = albumArtData

            val colors = track.albumMainColor?.let { colorsFromMainColor(it) }
            cachedColors = colors

            val displayTitle = track.getDisplayName(displayLanguage).ifEmpty { "No Track" }
            val displayArtist = track.getDisplayArtist(displayLanguage).ifEmpty { "Unknown Artist" }
            val displayAlbum = track.getDisplayAlbum(displayLanguage)

            updateWidget(context, WidgetPlaybackState(
                title = displayTitle,
                artist = displayArtist,
                album = displayAlbum,
                isPlaying = isPlaying,
                isLiked = MediaNotificationManager.isCurrentFavorite,
                isShuffled = MediaNotificationManager.isShuffled,
                isMobilePlayback = MediaNotificationManager.isMobilePlayback,
                repeatMode = MediaNotificationManager.wsClient?.repeatMode?.value ?: "off",
                volumeLevel = when {
                    (MediaNotificationManager.wsClient?.volume?.value ?: 1.0) <= 0.0 -> 0
                    (MediaNotificationManager.wsClient?.volume?.value ?: 1.0) < 0.66 -> 1
                    else -> 2
                },
                albumArtBitmapData = albumArtData,
                colorPrimary = colors?.primary ?: 0xFF1C1B1F.toInt(),
                colorOnPrimary = colors?.onPrimary ?: 0xFFFFFFFF.toInt(),
                colorSecondary = colors?.secondary ?: 0xFF49454F.toInt(),
                colorOnSecondary = colors?.onSecondary ?: 0xFFFFFFFF.toInt(),
                colorError = colors?.error ?: 0xFFE53935.toInt(),
                colorPrimaryContainer = colors?.primaryContainer ?: 0xFF1F3140.toInt(),
                colorOnPrimaryContainer = colors?.onPrimaryContainer ?: 0xFFD3E4F7.toInt(),
                colorSecondaryContainer = colors?.secondaryContainer ?: 0xFF2B2535.toInt(),
                colorOnSecondaryContainer = colors?.onSecondaryContainer ?: 0xFFEADDFF.toInt(),
                colorErrorContainer = colors?.errorContainer ?: 0xFF8C1D18.toInt(),
                colorOnErrorContainer = colors?.onErrorContainer ?: 0xFFF9DEDC.toInt()
            ))
        }
    }

    /**
     * Updates only play/pause state.
     */
    fun onPlayStateChanged(isPlaying: Boolean) {
        val context = VibeonApp.instance
        scope.launch {
            updateWidgetField(context) { it.copy(isPlaying = isPlaying) }
        }
    }

    /**
     * Updates favorite state.
     */
    fun onFavoriteChanged(isFavorite: Boolean) {
        val context = VibeonApp.instance
        scope.launch {
            updateWidgetField(context) { it.copy(isLiked = isFavorite) }
        }
    }

    /**
     * Updates shuffle state.
     */
    fun onShuffleChanged(isShuffled: Boolean) {
        val context = VibeonApp.instance
        scope.launch {
            updateWidgetField(context) { it.copy(isShuffled = isShuffled) }
        }
    }

    /**
     * Updates playback source (mobile/PC).
     */
    fun onOutputChanged(isMobilePlayback: Boolean) {
        val context = VibeonApp.instance
        scope.launch {
            updateWidgetField(context) { it.copy(isMobilePlayback = isMobilePlayback) }
        }
    }

    /**
     * Updates repeat mode.
     */
    fun onRepeatChanged(mode: String) {
        val context = VibeonApp.instance
        scope.launch {
            updateWidgetField(context) { it.copy(repeatMode = mode) }
        }
    }

    /**
     * Updates volume.
     */
    fun onVolumeChanged(volume: Double) {
        val context = VibeonApp.instance
        scope.launch {
            val volumeLevel = when {
                volume <= 0.0 -> 0
                volume < 0.66 -> 1
                else -> 2
            }
            updateWidgetField(context) { it.copy(volumeLevel = volumeLevel) }
        }
    }

    /**
     * Updates playlists.
     */
    fun onPlaylistsChanged(playlists: List<moe.memesta.vibeon.widget.PlaylistWidgetInfo>) {
        val context = VibeonApp.instance
        scope.launch {
            updateWidgetField(context) { it.copy(widgetPlaylists = playlists.take(4)) }
        }
    }

    // --- Private helpers ---

    private suspend fun updateWidget(context: Context, playbackState: WidgetPlaybackState) {
        try {
            val dataStore = WidgetStateDefinition.getDataStore(context, "")
            dataStore.updateData { playbackState }
            // Update all widget types
            VibeonWidget().updateAll(context)
            LikedWidget().updateAll(context)
            MoreDetailWidget().updateAll(context)
            Log.d("WidgetUpdater", "Widgets updated: ${playbackState.title}")
        } catch (e: Exception) {
            Log.e("WidgetUpdater", "Failed to update widgets", e)
        }
    }

    private suspend fun updateWidgetField(
        context: Context,
        transform: (WidgetPlaybackState) -> WidgetPlaybackState
    ) {
        try {
            val dataStore = WidgetStateDefinition.getDataStore(context, "")
            dataStore.updateData { transform(it) }
            // Update all widget types
            VibeonWidget().updateAll(context)
            LikedWidget().updateAll(context)
            MoreDetailWidget().updateAll(context)
        } catch (e: Exception) {
            Log.e("WidgetUpdater", "Failed to update widget field", e)
        }
    }

    private suspend fun downloadAlbumArt(context: Context, url: String): ByteArray? {
        return try {
            val loader = ImageLoader(context)
            // Download at 512px for better quality on large widgets
            val request = ImageRequest.Builder(context)
                .data(url)
                .size(512)
                .allowHardware(false)
                .build()
            
            val result = loader.execute(request)
            val bitmap = (result as? SuccessResult)?.drawable?.toBitmap() ?: return null

            // Compress to JPEG ByteArray
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val byteArray = outputStream.toByteArray()
            
            Log.d("WidgetUpdater", "Album art downloaded: ${byteArray.size} bytes")
            byteArray
        } catch (e: Exception) {
            Log.e("WidgetUpdater", "Failed to download album art", e)
            null
        }
    }

    private fun colorsFromMainColor(mainColor: Int): PaletteColors {
        val hct = Hct.fromInt(mainColor)
        val scheme = SchemeTonalSpot(hct, true, 0.0)
        return PaletteColors(
            primary = scheme.primaryPalette.tone(80),
            onPrimary = scheme.primaryPalette.tone(20),
            secondary = scheme.secondaryPalette.tone(80),
            onSecondary = scheme.secondaryPalette.tone(20),
            error = 0xFFE53935.toInt(),
            primaryContainer = scheme.primaryPalette.tone(30),
            onPrimaryContainer = scheme.primaryPalette.tone(90),
            secondaryContainer = scheme.secondaryPalette.tone(30),
            onSecondaryContainer = scheme.secondaryPalette.tone(90),
            errorContainer = 0xFF8C1D18.toInt(),
            onErrorContainer = 0xFFF9DEDC.toInt()
        )
    }
}
