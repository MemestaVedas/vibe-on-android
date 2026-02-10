package moe.memesta.vibeon.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import moe.memesta.vibeon.ui.*

@Composable
fun MainContentPager(
    pagerState: PagerState,
    libraryViewModel: LibraryViewModel,
    connectionViewModel: ConnectionViewModel,
    favoritesManager: moe.memesta.vibeon.data.local.FavoritesManager,
    navController: NavController,
    contentPadding: PaddingValues
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 1, // Optimization to keep adjacent pages ready
        key = { it } // Use page index as key
    ) { page ->
        when (page) {
            0 -> HomeScreen(
                viewModel = libraryViewModel,
                onTrackSelected = { navController.navigate("now_playing") },
                onAlbumSelected = { albumName -> navController.navigate("album/$albumName") },
                onArtistSelected = { artistName -> navController.navigate("artist/$artistName") },
                onViewAllSongs = { navController.navigate("all_songs") },
                contentPadding = contentPadding,
                connectionViewModel = connectionViewModel
            )
            1 -> AlbumsGridScreen(
                viewModel = libraryViewModel,
                onBackClick = { /* Pager handles back in Activity if needed, but here we stay */ },
                onAlbumClick = { albumName -> navController.navigate("album/$albumName") },
                onPlayAlbum = { albumName -> 
                    libraryViewModel.playAlbum(albumName)
                    navController.navigate("now_playing")
                },
                contentPadding = contentPadding
            )
            2 -> SearchScreen(
                viewModel = libraryViewModel,
                onTrackSelected = { navController.navigate("now_playing") },
                onAlbumSelected = { albumName -> navController.navigate("album/$albumName") },
                onArtistSelected = { artistName -> navController.navigate("artist/$artistName") },
                contentPadding = contentPadding
            )
            3 -> ArtistsListScreen(
                viewModel = libraryViewModel,
                onBackClick = { },
                onArtistClick = { artistName -> navController.navigate("artist/$artistName") },
                onPlayArtist = { artistName -> 
                    libraryViewModel.playArtist(artistName)
                    navController.navigate("now_playing")
                },
                contentPadding = contentPadding
            )
            4 -> SettingsScreen(
                connectionViewModel = connectionViewModel,
                favoritesManager = favoritesManager,
                contentPadding = contentPadding
            )
        }
    }
}
