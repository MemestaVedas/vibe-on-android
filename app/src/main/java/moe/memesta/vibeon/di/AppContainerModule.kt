package moe.memesta.vibeon.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import moe.memesta.vibeon.AppContainer
import moe.memesta.vibeon.VibeonApp
import moe.memesta.vibeon.data.DiscoveryRepository
import moe.memesta.vibeon.data.WebSocketClient
import moe.memesta.vibeon.data.local.FavoritesManager
import moe.memesta.vibeon.data.local.OnboardingManager
import moe.memesta.vibeon.data.local.PlayerSettingsRepository
import moe.memesta.vibeon.data.stats.LocalPlaybackStatsRepository

@Module
@InstallIn(SingletonComponent::class)
object AppContainerModule {

    @Provides
    @Singleton
    fun provideAppContainer(@ApplicationContext context: Context): AppContainer {
        // Reuse the app-owned container so Hilt and non-Hilt call sites share singletons.
        return (context.applicationContext as VibeonApp).container
    }

    @Provides
    @Singleton
    fun provideWebSocketClient(container: AppContainer): WebSocketClient = container.webSocketClient

    @Provides
    @Singleton
    fun provideDiscoveryRepository(container: AppContainer): DiscoveryRepository = container.discoveryRepository

    @Provides
    @Singleton
    fun provideFavoritesManager(container: AppContainer): FavoritesManager = container.favoritesManager

    @Provides
    @Singleton
    fun providePlayerSettingsRepository(container: AppContainer): PlayerSettingsRepository =
        container.playerSettingsRepository

    @Provides
    @Singleton
    fun provideLocalPlaybackStatsRepository(container: AppContainer): LocalPlaybackStatsRepository =
        container.localStatsRepository

    @Provides
    @Singleton
    fun provideOnboardingManager(container: AppContainer): OnboardingManager = container.onboardingManager
}
