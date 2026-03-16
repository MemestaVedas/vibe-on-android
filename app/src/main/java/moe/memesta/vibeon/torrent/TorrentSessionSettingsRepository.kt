package moe.memesta.vibeon.torrent

import android.content.Context
import android.content.SharedPreferences

class TorrentSessionSettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "vibe_on_torrent_session",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_CONNECTIONS_LIMIT = "connections_limit"
        private const val KEY_ACTIVE_DOWNLOADS = "active_downloads"
        private const val KEY_ACTIVE_SEEDS = "active_seeds"
        private const val KEY_ENABLE_DHT = "enable_dht"
        private const val KEY_ENABLE_LSD = "enable_lsd"
        private const val KEY_ENABLE_UTP = "enable_utp"
        private const val KEY_MAX_DOWNLOAD_RATE_BYTES = "max_download_rate_bytes"
        private const val KEY_MAX_UPLOAD_RATE_BYTES = "max_upload_rate_bytes"
    }

    fun load(): TorrentSessionSettings {
        val defaults = TorrentSessionSettings()
        return TorrentSessionSettings(
            connectionsLimit = prefs.getInt(KEY_CONNECTIONS_LIMIT, defaults.connectionsLimit),
            activeDownloads = prefs.getInt(KEY_ACTIVE_DOWNLOADS, defaults.activeDownloads),
            activeSeeds = prefs.getInt(KEY_ACTIVE_SEEDS, defaults.activeSeeds),
            enableDht = prefs.getBoolean(KEY_ENABLE_DHT, defaults.enableDht),
            enableLsd = prefs.getBoolean(KEY_ENABLE_LSD, defaults.enableLsd),
            enableUtp = prefs.getBoolean(KEY_ENABLE_UTP, defaults.enableUtp),
            maxDownloadRateBytes = prefs.getInt(KEY_MAX_DOWNLOAD_RATE_BYTES, defaults.maxDownloadRateBytes),
            maxUploadRateBytes = prefs.getInt(KEY_MAX_UPLOAD_RATE_BYTES, defaults.maxUploadRateBytes)
        ).normalized()
    }

    fun save(settings: TorrentSessionSettings) {
        val normalized = settings.normalized()
        prefs.edit()
            .putInt(KEY_CONNECTIONS_LIMIT, normalized.connectionsLimit)
            .putInt(KEY_ACTIVE_DOWNLOADS, normalized.activeDownloads)
            .putInt(KEY_ACTIVE_SEEDS, normalized.activeSeeds)
            .putBoolean(KEY_ENABLE_DHT, normalized.enableDht)
            .putBoolean(KEY_ENABLE_LSD, normalized.enableLsd)
            .putBoolean(KEY_ENABLE_UTP, normalized.enableUtp)
            .putInt(KEY_MAX_DOWNLOAD_RATE_BYTES, normalized.maxDownloadRateBytes)
            .putInt(KEY_MAX_UPLOAD_RATE_BYTES, normalized.maxUploadRateBytes)
            .apply()
    }
}
