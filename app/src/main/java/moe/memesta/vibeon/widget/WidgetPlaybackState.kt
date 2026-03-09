package moe.memesta.vibeon.widget

import kotlinx.serialization.Serializable

/** Slim playlist metadata carried in the widget state (up to 4 entries). */
@Serializable
data class PlaylistWidgetInfo(
    val id: String = "",
    val name: String = ""
)

/**
 * Serializable snapshot of the current playback state, stored in the Glance DataStore
 * so the widget can render without a live connection to the app process.
 */
@Serializable
data class WidgetPlaybackState(
    val title: String = "No Track",
    val artist: String = "Unknown Artist",
    val isPlaying: Boolean = false,
    val isLiked: Boolean = false,
    val isShuffled: Boolean = false,
    val isMobilePlayback: Boolean = false,
    /** Absolute path to the cached album art JPEG file, or null when there's no art. */
    val albumArtPath: String? = null,
    // Palette colours (ARGB Int) extracted from the album art
    val colorPrimary:     Int = 0xFF1C1B1F.toInt(),  // scrim + card bg
    val colorOnPrimary:   Int = 0xFFFFFFFF.toInt(),  // title, artist, app logo
    val colorSecondary:   Int = 0xFF49454F.toInt(),  // source-toggle circle + unliked heart bg
    val colorOnSecondary: Int = 0xFFFFFFFF.toInt(),  // source-toggle icon, unliked heart icon
    val colorError:       Int = 0xFFE53935.toInt(),  // liked heart
    // Extended M3-style container colours derived at palette-extraction time
    val colorPrimaryContainer:     Int = 0xFF1F3140.toInt(),
    val colorOnPrimaryContainer:   Int = 0xFFD3E4F7.toInt(),
    val colorSecondaryContainer:   Int = 0xFF2B2535.toInt(),
    val colorOnSecondaryContainer: Int = 0xFFEADDFF.toInt(),
    val colorErrorContainer:       Int = 0xFF8C1D18.toInt(),
    val colorOnErrorContainer:     Int = 0xFFF9DEDC.toInt(),
    // More-options panel state
    val showingMoreOptions: Boolean = false,
    /** "off" | "one" | "all" — mirrors WebSocketClient.repeatMode */
    val repeatMode: String = "off",
    /** 0 = muted, 1 = 50 %, 2 = 100 % */
    val volumeLevel: Int = 2,
    /** Up to 4 playlists shown as save-to targets in the more-options panel. */
    val widgetPlaylists: List<PlaylistWidgetInfo> = emptyList()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WidgetPlaybackState) return false
        if (title != other.title) return false
        if (artist != other.artist) return false
        if (isPlaying != other.isPlaying) return false
        if (isLiked != other.isLiked) return false
        if (isShuffled != other.isShuffled) return false
        if (isMobilePlayback != other.isMobilePlayback) return false
        if (albumArtPath != other.albumArtPath) return false
        if (colorPrimary     != other.colorPrimary)     return false
        if (colorOnPrimary   != other.colorOnPrimary)   return false
        if (colorSecondary   != other.colorSecondary)   return false
        if (colorOnSecondary != other.colorOnSecondary) return false
        if (colorError       != other.colorError)       return false
        if (colorPrimaryContainer     != other.colorPrimaryContainer)     return false
        if (colorOnPrimaryContainer   != other.colorOnPrimaryContainer)   return false
        if (colorSecondaryContainer   != other.colorSecondaryContainer)   return false
        if (colorOnSecondaryContainer != other.colorOnSecondaryContainer) return false
        if (colorErrorContainer       != other.colorErrorContainer)       return false
        if (colorOnErrorContainer     != other.colorOnErrorContainer)     return false
        if (showingMoreOptions != other.showingMoreOptions) return false
        if (repeatMode != other.repeatMode) return false
        if (volumeLevel != other.volumeLevel) return false
        if (widgetPlaylists != other.widgetPlaylists) return false
        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + artist.hashCode()
        result = 31 * result + isPlaying.hashCode()
        result = 31 * result + isLiked.hashCode()
        result = 31 * result + isShuffled.hashCode()
        result = 31 * result + isMobilePlayback.hashCode()
        result = 31 * result + (albumArtPath?.hashCode() ?: 0)
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
        return result
    }
}
