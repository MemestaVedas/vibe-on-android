package moe.memesta.vibeon.widget

import kotlinx.serialization.Serializable

@Serializable
data class PlaylistWidgetInfo(
    val id: String,
    val name: String
)

@Serializable
data class WidgetPlaybackState(
    val title: String = "",
    val artist: String = "",
    val album: String? = null,
    val isPlaying: Boolean = false,
    val isLiked: Boolean = false,
    val isShuffled: Boolean = false,
    val isMobilePlayback: Boolean = false,
    val albumArtBitmapData: ByteArray? = null,
    val colorPrimary: Int = 0xFFD0BCFF.toInt(),
    val colorOnPrimary: Int = 0xFF381E72.toInt(),
    val colorSecondary: Int = 0xFFCCC2DC.toInt(),
    val colorOnSecondary: Int = 0xFF332D41.toInt(),
    val colorError: Int = 0xFFE53935.toInt(),
    val colorPrimaryContainer: Int = 0xFF4F378B.toInt(),
    val colorOnPrimaryContainer: Int = 0xFFEADDFF.toInt(),
    val colorSecondaryContainer: Int = 0xFF4A4458.toInt(),
    val colorOnSecondaryContainer: Int = 0xFFE8DEF8.toInt(),
    val colorErrorContainer: Int = 0xFF8C1D18.toInt(),
    val colorOnErrorContainer: Int = 0xFFF9DEDC.toInt(),
    val showingMoreOptions: Boolean = false,
    val repeatMode: String = "off",
    val volumeLevel: Int = 2,
    val widgetPlaylists: List<PlaylistWidgetInfo> = emptyList(),
    val widgetFontMode: String = "dynamic",
    val widgetManualWidth: Int = 100,
    val widgetManualWeight: Int = 640,
    val widgetManualRoundness: Int = 140
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WidgetPlaybackState

        if (title != other.title) return false
        if (artist != other.artist) return false
        if (album != other.album) return false
        if (isPlaying != other.isPlaying) return false
        if (isLiked != other.isLiked) return false
        if (isShuffled != other.isShuffled) return false
        if (isMobilePlayback != other.isMobilePlayback) return false
        if (albumArtBitmapData != null) {
            if (other.albumArtBitmapData == null) return false
            if (!albumArtBitmapData.contentEquals(other.albumArtBitmapData)) return false
        } else if (other.albumArtBitmapData != null) return false
        if (colorPrimary != other.colorPrimary) return false
        if (colorOnPrimary != other.colorOnPrimary) return false
        if (colorSecondary != other.colorSecondary) return false
        if (colorOnSecondary != other.colorOnSecondary) return false
        if (colorError != other.colorError) return false
        if (colorPrimaryContainer != other.colorPrimaryContainer) return false
        if (colorOnPrimaryContainer != other.colorOnPrimaryContainer) return false
        if (colorSecondaryContainer != other.colorSecondaryContainer) return false
        if (colorOnSecondaryContainer != other.colorOnSecondaryContainer) return false
        if (colorErrorContainer != other.colorErrorContainer) return false
        if (colorOnErrorContainer != other.colorOnErrorContainer) return false
        if (showingMoreOptions != other.showingMoreOptions) return false
        if (repeatMode != other.repeatMode) return false
        if (volumeLevel != other.volumeLevel) return false
        if (widgetPlaylists != other.widgetPlaylists) return false
        if (widgetFontMode != other.widgetFontMode) return false
        if (widgetManualWidth != other.widgetManualWidth) return false
        if (widgetManualWeight != other.widgetManualWeight) return false
        if (widgetManualRoundness != other.widgetManualRoundness) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + artist.hashCode()
        result = 31 * result + (album?.hashCode() ?: 0)
        result = 31 * result + isPlaying.hashCode()
        result = 31 * result + isLiked.hashCode()
        result = 31 * result + isShuffled.hashCode()
        result = 31 * result + isMobilePlayback.hashCode()
        result = 31 * result + (albumArtBitmapData?.contentHashCode() ?: 0)
        result = 31 * result + colorPrimary
        result = 31 * result + colorOnPrimary
        result = 31 * result + colorSecondary
        result = 31 * result + colorOnSecondary
        result = 31 * result + colorError
        result = 31 * result + colorPrimaryContainer
        result = 31 * result + colorOnPrimaryContainer
        result = 31 * result + colorSecondaryContainer
        result = 31 * result + colorOnSecondaryContainer
        result = 31 * result + colorErrorContainer
        result = 31 * result + colorOnErrorContainer
        result = 31 * result + showingMoreOptions.hashCode()
        result = 31 * result + repeatMode.hashCode()
        result = 31 * result + volumeLevel
        result = 31 * result + widgetPlaylists.hashCode()
        result = 31 * result + widgetFontMode.hashCode()
        result = 31 * result + widgetManualWidth
        result = 31 * result + widgetManualWeight
        result = 31 * result + widgetManualRoundness
        return result
    }
}
