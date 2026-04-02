package moe.memesta.vibeon

import android.content.Context
import moe.memesta.vibeon.data.cast.CastStateHolder
import moe.memesta.vibeon.data.DiscoveryRepository
import moe.memesta.vibeon.data.StreamRepository
import moe.memesta.vibeon.data.WebSocketClient
import moe.memesta.vibeon.data.local.FavoritesManager
import moe.memesta.vibeon.data.local.OnboardingManager
import moe.memesta.vibeon.data.local.PlayerSettingsRepository
import moe.memesta.vibeon.data.stats.LocalPlaybackStatsRepository
import moe.memesta.vibeon.torrent.TorrentDownloadManager
import moe.memesta.vibeon.torrent.TorrentSessionSettingsRepository
import moe.memesta.vibeon.torrent.TorrentStoragePreferences

class AppContainer(private val context: Context) {
    val webSocketClient: WebSocketClient by lazy {
        WebSocketClient()
    }

    val discoveryRepository: DiscoveryRepository by lazy {
        DiscoveryRepository(context)
    }

    val favoritesManager: FavoritesManager by lazy {
        FavoritesManager(context)
    }

    val playerSettingsRepository: PlayerSettingsRepository by lazy {
        PlayerSettingsRepository(context)
    }

    val localStatsRepository: LocalPlaybackStatsRepository by lazy {
        LocalPlaybackStatsRepository(context.applicationContext)
    }

    val onboardingManager: OnboardingManager by lazy {
        OnboardingManager(context)
    }

    val torrentDownloadManager: TorrentDownloadManager by lazy {
        TorrentDownloadManager(
            context = context.applicationContext,
            settingsRepository = torrentSessionSettingsRepository
        )
    }

    val streamRepository: StreamRepository by lazy {
        StreamRepository()
    }

    val torrentSessionSettingsRepository: TorrentSessionSettingsRepository by lazy {
        TorrentSessionSettingsRepository(context.applicationContext)
    }

    val torrentStoragePreferences: TorrentStoragePreferences by lazy {
        TorrentStoragePreferences(context.applicationContext)
    }

    val castStateHolder: CastStateHolder by lazy {
        CastStateHolder(context.applicationContext)
    }
}