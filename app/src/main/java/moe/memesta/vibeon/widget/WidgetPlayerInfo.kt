package moe.memesta.vibeon.widget

/**
 * Legacy file - use WidgetPlaybackState instead.
 * Kept for backwards compatibility during migration.
 */
@Deprecated("Use WidgetPlaybackState instead")
data class WidgetPlayerInfo(
    val songTitle: String = "",
    val artistName: String = "",
    val isPlaying: Boolean = false,
    val isFavorite: Boolean = false,
    val isShuffleEnabled: Boolean = false,
    val isMobilePlayback: Boolean = false,
    val albumArtBitmapData: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean = true
    override fun hashCode(): Int = 0
}
