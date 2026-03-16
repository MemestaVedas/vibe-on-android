package moe.memesta.vibeon.torrent

/**
 * Session-level controls inspired by LibreTorrent defaults.
 * Rate limits are bytes per second (0 means unlimited).
 */
data class TorrentSessionSettings(
    val connectionsLimit: Int = 200,
    val activeDownloads: Int = 5,
    val activeSeeds: Int = 5,
    val enableDht: Boolean = true,
    val enableLsd: Boolean = true,
    val enableUtp: Boolean = true,
    val maxDownloadRateBytes: Int = 0,
    val maxUploadRateBytes: Int = 0
) {
    fun normalized(): TorrentSessionSettings {
        return copy(
            connectionsLimit = connectionsLimit.coerceIn(20, 2000),
            activeDownloads = activeDownloads.coerceIn(1, 50),
            activeSeeds = activeSeeds.coerceIn(1, 50),
            maxDownloadRateBytes = maxDownloadRateBytes.coerceAtLeast(0),
            maxUploadRateBytes = maxUploadRateBytes.coerceAtLeast(0)
        )
    }
}
