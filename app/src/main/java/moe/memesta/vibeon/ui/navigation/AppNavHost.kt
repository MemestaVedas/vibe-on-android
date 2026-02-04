package moe.memesta.vibeon.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import moe.memesta.vibeon.data.DiscoveredDevice
import moe.memesta.vibeon.ui.*

@Composable
fun AppNavHost(
    connectionViewModel: ConnectionViewModel,
    playbackViewModel: PlaybackViewModel
) {
    val navController = rememberNavController()
    var currentDevice by remember { mutableStateOf<DiscoveredDevice?>(null) }
    
    // Determine if bottom bar should be transparent
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in listOf("library", "albums", "search", "artists", "settings")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                moe.memesta.vibeon.ui.components.BottomPlayerBar(
                    navController = navController,
                    connectionViewModel = connectionViewModel,
                    playbackViewModel = playbackViewModel,
                    onNavigateToPlayer = { navController.navigate("now_playing") }
                )
            }
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        NavHost(
            navController = navController, 
            startDestination = "discovery",
            modifier = Modifier.fillMaxSize().padding(bottom = if(showBottomBar) 0.dp else 0.dp) // Let content go behind nav bar
        ) {
            composable("discovery") {
                DiscoveryScreen(
                    viewModel = connectionViewModel,
                    onDeviceSelected = { device ->
                        currentDevice = device
                        connectionViewModel.connectToDevice(device)
                        navController.navigate("library")
                    }
                )
            }
            
            composable("library") {
                val device = currentDevice
                // Re-using exiting logic for creating VM
                val libraryViewModel = remember(device) {
                    if (device != null) {
                         LibraryViewModel(host = device.host, port = device.port, wsClient = connectionViewModel.wsClient)
                    } else null
                }
                
                if (libraryViewModel != null) {
                    // Replaced LibraryScreen with HomeScreen for the main "Home" tab
                     HomeScreen(
                        viewModel = libraryViewModel,
                        onTrackSelected = { navController.navigate("now_playing") },
                        onAlbumSelected = { albumName -> navController.navigate("album/$albumName") },
                        onArtistSelected = { artistName -> navController.navigate("artist/$artistName") },
                        contentPadding = innerPadding
                    )
                } else {
                     // Fallback
                     LaunchedEffect(Unit) { navController.navigate("discovery") }
                }
            }

            // Albums Tab - Full Grid View
            composable("albums") {
                val device = currentDevice
                val libraryViewModel = remember(device) {
                    if (device != null) LibraryViewModel(host = device.host, port = device.port, wsClient = connectionViewModel.wsClient) else null
                }
                
                if (libraryViewModel != null) {
                    AlbumsGridScreen(
                        viewModel = libraryViewModel,
                        onBackClick = { navController.popBackStack() },
                        onAlbumClick = { albumName -> navController.navigate("album/$albumName") },
                        onPlayAlbum = { albumName -> 
                            libraryViewModel.playAlbum(albumName)
                            navController.navigate("now_playing")
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                } else {
                    LaunchedEffect(Unit) { navController.navigate("discovery") }
                }
            }
            
            // Search Tab - Full Search Screen
            composable("search") {
                val device = currentDevice
                val libraryViewModel = remember(device) {
                    if (device != null) LibraryViewModel(host = device.host, port = device.port, wsClient = connectionViewModel.wsClient) else null
                }
                
                if (libraryViewModel != null) {
                    SearchScreen(viewModel = libraryViewModel)
                } else {
                    LaunchedEffect(Unit) { navController.navigate("discovery") }
                }
            }
            
            // Artists Tab - Full List View
            composable("artists") {
                val device = currentDevice
                val libraryViewModel = remember(device) {
                    if (device != null) LibraryViewModel(host = device.host, port = device.port, wsClient = connectionViewModel.wsClient) else null
                }
                
                if (libraryViewModel != null) {
                    ArtistsListScreen(
                        viewModel = libraryViewModel,
                        onBackClick = { navController.popBackStack() },
                        onArtistClick = { artistName -> navController.navigate("artist/$artistName") },
                        onPlayArtist = { artistName -> 
                            libraryViewModel.playArtist(artistName)
                            navController.navigate("now_playing")
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                } else {
                    LaunchedEffect(Unit) { navController.navigate("discovery") }
                }
            }
            
            // Detail Screens
            composable(
                route = "album/{albumName}",
                arguments = listOf(androidx.navigation.navArgument("albumName") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val albumName = backStackEntry.arguments?.getString("albumName") ?: return@composable
                val device = currentDevice
                val libraryViewModel = remember(device) {
                    if (device != null) LibraryViewModel(host = device.host, port = device.port, wsClient = connectionViewModel.wsClient) else null
                }

                if (libraryViewModel != null) {
                    AlbumDetailScreen(
                        albumName = albumName,
                        viewModel = libraryViewModel,
                        onBackClick = { navController.popBackStack() },
                        onTrackSelected = { navController.navigate("now_playing") },
                        contentPadding = innerPadding
                    )
                }
            }

            composable(
                route = "artist/{artistName}",
                arguments = listOf(androidx.navigation.navArgument("artistName") { type = androidx.navigation.NavType.StringType })
            ) { backStackEntry ->
                val artistName = backStackEntry.arguments?.getString("artistName") ?: return@composable
                 val device = currentDevice
                val libraryViewModel = remember(device) {
                    if (device != null) LibraryViewModel(host = device.host, port = device.port, wsClient = connectionViewModel.wsClient) else null
                }

                if (libraryViewModel != null) {
                    ArtistDetailScreen(
                        artistName = artistName,
                        viewModel = libraryViewModel,
                        onBackClick = { navController.popBackStack() },
                        onTrackSelected = { navController.navigate("now_playing") },
                        contentPadding = innerPadding
                    )
                }
            }
            
            composable("now_playing") {
                NowPlayingScreen(
                    playbackViewModel = playbackViewModel,
                    connectionViewModel = connectionViewModel,
                    onBackPressed = { navController.popBackStack() }
                )
            }
            
            composable("settings") {
                SettingsScreen()
            }
        }
    }
}
