package moe.memesta.vibeon.widget

import kotlinx.serialization.Serializable

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
    /** JPEG-compressed album art bytes, or null when there's no art. */
    val albumArtBytes: ByteArray? = null
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
        if (albumArtBytes != null) {
            if (other.albumArtBytes == null) return false
            if (!albumArtBytes.contentEquals(other.albumArtBytes)) return false
        } else if (other.albumArtBytes != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + artist.hashCode()
        result = 31 * result + isPlaying.hashCode()
        result = 31 * result + isLiked.hashCode()
        result = 31 * result + isShuffled.hashCode()
        result = 31 * result + isMobilePlayback.hashCode()
        result = 31 * result + (albumArtBytes?.contentHashCode() ?: 0)
        return result
    }
}
