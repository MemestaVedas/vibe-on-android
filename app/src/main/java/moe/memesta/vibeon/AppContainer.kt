package moe.memesta.vibeon

import android.content.Context
import moe.memesta.vibeon.data.DiscoveryRepository
import moe.memesta.vibeon.data.StreamRepository
import moe.memesta.vibeon.data.WebSocketClient
import moe.memesta.vibeon.data.local.FavoritesManager
import moe.memesta.vibeon.data.local.OnboardingManager
import moe.memesta.vibeon.data.local.PlayerSettingsRepository
import moe.memesta.vibeon.data.stats.LocalPlaybackStatsRepository

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
}