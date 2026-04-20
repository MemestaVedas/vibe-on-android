package moe.memesta.vibeon.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import moe.memesta.vibeon.ui.*
import kotlinx.coroutines.launch
import java.net.URLEncoder

@androidx.compose.animation.ExperimentalSharedTransitionApi
@Composable
fun MainContentPager(
    pagerState: PagerState,
    libraryViewModel: LibraryViewModel,
    statsViewModel: moe.memesta.vibeon.ui.stats.StatsViewModel?,
    connectionViewModel: ConnectionViewModel,
    playbackViewModel: PlaybackViewModel,
    favoritesManager: moe.memesta.vibeon.data.local.FavoritesManager,
    playerSettingsRepository: moe.memesta.vibeon.data.local.PlayerSettingsRepository,
    navController: NavController,
    contentPadding: PaddingValues,
    onNavigateToPlayer: () -> Unit,
    onSearchClick: () -> Unit,
    onReplayOnboarding: () -> Unit,
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 1, // Optimization to keep adjacent pages ready
        key = { it } // Use page index as key
    ) { page ->
        when (page) {
            0 -> HomeScreen(
                viewModel = libraryViewModel,
                statsViewModel = statsViewModel,
                onTrackSelected = { /* Update pill only, no navigation */ },
                onAlbumSelected = { albumName -> navController.navigate("album/${URLEncoder.encode(albumName, "UTF-8")}") },
                onArtistSelected = { artistName -> navController.navigate("artist/${URLEncoder.encode(artistName, "UTF-8")}") },
                onSearchClick = onSearchClick,
                onViewAllSongs = { navController.navigate("all_songs") },
                onViewFavorites = { scope.launch { pagerState.animateScrollToPage(4) } },
                onViewPlaylists = { navController.navigate("playlists") },
                onViewStats = { scope.launch { pagerState.animateScrollToPage(2) } },
                onViewOfflineSongs = { navController.navigate("offline_songs") },
                    onViewTorrents = { navController.navigate("torrents") },
                    onViewServerDetails = { navController.navigate(ServerDetailsRoute.path) },
                onViewAllAlbums = { 
                    scope.launch {
                        pagerState.animateScrollToPage(1)
                    }
                },
                onViewAllArtists = {
                    scope.launch {
                        pagerState.animateScrollToPage(3)
                    }
                },
                contentPadding = contentPadding,
                connectionViewModel = connectionViewModel,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
            1 -> AlbumsGridScreen(
                viewModel = libraryViewModel,
                onBackClick = { /* Pager handles back in Activity if needed, but here we stay */ },
                onSidebarClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                onAlbumClick = { albumName -> navController.navigate("album/${URLEncoder.encode(albumName, "UTF-8")}") },
                onPlayAlbum = { albumName -> 
                    libraryViewModel.playAlbum(albumName)
                    navController.navigate("now_playing")
                },
                contentPadding = contentPadding,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
            2 -> statsViewModel?.let { vm ->
                moe.memesta.vibeon.ui.stats.StatsScreen(
                    statsViewModel = vm,
                    onBackPressed = { scope.launch { pagerState.animateScrollToPage(0) } }
                )
            }
            3 -> ArtistsListScreen(
                viewModel = libraryViewModel,
                onBackClick = { },
                onSidebarClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                onArtistClick = { artistName -> navController.navigate("artist/${URLEncoder.encode(artistName, "UTF-8")}") },
                onPlayArtist = { artistName -> 
                    libraryViewModel.playArtist(artistName)
                    navController.navigate("now_playing")
                },
                contentPadding = contentPadding,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
            4 -> SettingsScreen(
                connectionViewModel = connectionViewModel,
                libraryViewModel = libraryViewModel,
                favoritesManager = favoritesManager,
                playerSettingsRepository = playerSettingsRepository,
                onReplayOnboarding = onReplayOnboarding,
                contentPadding = contentPadding
            )
        }
    }
}
