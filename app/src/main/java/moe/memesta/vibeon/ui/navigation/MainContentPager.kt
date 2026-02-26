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

@Composable
fun MainContentPager(
    pagerState: PagerState,
    libraryViewModel: LibraryViewModel,
    statsViewModel: moe.memesta.vibeon.ui.stats.StatsViewModel?,
    connectionViewModel: ConnectionViewModel,
    favoritesManager: moe.memesta.vibeon.data.local.FavoritesManager,
    playerSettingsRepository: moe.memesta.vibeon.data.local.PlayerSettingsRepository,
    navController: NavController,
    contentPadding: PaddingValues
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
                onSearchClick = { navController.navigate("search") },
                onViewAllSongs = { navController.navigate("all_songs") },
                onViewStats = { navController.navigate("stats") },
                    onViewTorrents = { navController.navigate("torrents") },
                    onViewServerDetails = { navController.navigate("server_details") },
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
                connectionViewModel = connectionViewModel
            )
            1 -> AlbumsGridScreen(
                viewModel = libraryViewModel,
                onBackClick = { /* Pager handles back in Activity if needed, but here we stay */ },
                onAlbumClick = { albumName -> navController.navigate("album/${URLEncoder.encode(albumName, "UTF-8")}") },
                onPlayAlbum = { albumName -> 
                    libraryViewModel.playAlbum(albumName)
                    navController.navigate("now_playing")
                },
                contentPadding = contentPadding
            )
            2 -> PlaylistsScreen(
                viewModel = connectionViewModel,
                libraryViewModel = libraryViewModel,
                contentPadding = contentPadding,
                onPlaylistSelected = { playlistId -> navController.navigate("playlist/${URLEncoder.encode(playlistId, "UTF-8")}") }
            )
            3 -> ArtistsListScreen(
                viewModel = libraryViewModel,
                onBackClick = { },
                onArtistClick = { artistName -> navController.navigate("artist/${URLEncoder.encode(artistName, "UTF-8")}") },
                onPlayArtist = { artistName -> 
                    libraryViewModel.playArtist(artistName)
                    navController.navigate("now_playing")
                },
                contentPadding = contentPadding
            )
            4 -> SettingsScreen(
                connectionViewModel = connectionViewModel,
                libraryViewModel = libraryViewModel,
                favoritesManager = favoritesManager,
                playerSettingsRepository = playerSettingsRepository,
                contentPadding = contentPadding
            )
        }
    }
}
