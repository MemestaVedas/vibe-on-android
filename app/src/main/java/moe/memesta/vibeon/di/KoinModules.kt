package moe.memesta.vibeon.di

import moe.memesta.vibeon.AppContainer
import org.koin.dsl.module

fun appContainerModule(container: AppContainer) = module {
    single { container }
    single { container.webSocketClient }
    single { container.discoveryRepository }
    single { container.favoritesManager }
    single { container.playerSettingsRepository }
    single { container.localStatsRepository }
    single { container.onboardingManager }
    single { container.torrentDownloadManager }
    single { container.streamRepository }
    single { container.torrentSessionSettingsRepository }
    single { container.torrentStoragePreferences }
}
