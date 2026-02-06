package moe.memesta.vibeon.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import moe.memesta.vibeon.ui.theme.VibeAnimations

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavHost(
    connectionViewModel: ConnectionViewModel,
    playbackViewModel: PlaybackViewModel,
    favoritesManager: moe.memesta.vibeon.data.local.FavoritesManager
) {
    val navController = rememberNavController()
    var currentDevice by remember { mutableStateOf<DiscoveredDevice?>(null) }
    
    // Determine smart start destination based on favorites
    val favorites = remember { favoritesManager.getFavorites() }
    val startDestination = if (favorites.isNotEmpty()) "library" else "discovery"
    
    // Handle optimistic navigation when auto-connecting to favorites
    val connectionState by connectionViewModel.connectionState.collectAsState()
    val connectedDevice by connectionViewModel.connectedDevice.collectAsState()
    
    LaunchedEffect(connectionState, connectedDevice) {
        // When we successfully connect via auto-connect, ensure we're on library screen
        when (connectionState) {
            ConnectionState.CONNECTED -> {
                if (connectedDevice != null && navController.currentDestination?.route == "discovery") {
                    navController.navigate("library") {
                        popUpTo("discovery") { inclusive = true }
                    }
                }
            }
            ConnectionState.FAILED -> {
                // If auto-connect failed and we're on library, go back to discovery
                if (navController.currentDestination?.route == "library" && connectedDevice == null) {
                    navController.navigate("discovery") {
                        popUpTo("library") { inclusive = true }
                    }
                }
            }
            else -> { /* No action needed for IDLE or CONNECTING */ }
        }
    }
    
    // Determine if bottom bar should be transparent
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in listOf("library", "albums", "search", "artists", "settings")


    SharedTransitionLayout {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                // Use AnimatedVisibility with a stable fade to avoid layout jumps for shared elements
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = fadeIn(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(300))
                ) {
                    moe.memesta.vibeon.ui.components.BottomPlayerBar(
                        navController = navController,
                        connectionViewModel = connectionViewModel,
                        playbackViewModel = playbackViewModel,
                        onNavigateToPlayer = { 
                            navController.navigate("now_playing") {
                                launchSingleTop = true
                            }
                        },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this
                    )
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            NavHost(
                navController = navController, 
                startDestination = startDestination,
                modifier = Modifier.fillMaxSize(),
                // Standard enter
                enterTransition = {
                    if (initialState.destination.route == "now_playing" || targetState.destination.route == "now_playing") {
                         fadeIn(animationSpec = tween(VibeAnimations.ScreenEnterDuration))
                    } else {
                        fadeIn(animationSpec = tween(VibeAnimations.ScreenEnterDuration)) +
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth / 4 },
                            animationSpec = tween(VibeAnimations.ScreenEnterDuration)
                        )
                    }
                },
                // Standard exit
                exitTransition = {
                    if (targetState.destination.route == "now_playing" || initialState.destination.route == "now_playing") {
                        fadeOut(animationSpec = tween(VibeAnimations.ScreenExitDuration))
                    } else {
                        fadeOut(animationSpec = tween(VibeAnimations.ScreenExitDuration)) +
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> -fullWidth / 4 },
                            animationSpec = tween(VibeAnimations.ScreenExitDuration)
                        )
                    }
                },
                // Pop enter (going back)
                popEnterTransition = {
                    if (initialState.destination.route == "now_playing") {
                        fadeIn(animationSpec = tween(VibeAnimations.ScreenEnterDuration))
                    } else {
                        fadeIn(animationSpec = tween(VibeAnimations.ScreenEnterDuration)) +
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> -fullWidth / 4 },
                            animationSpec = tween(VibeAnimations.ScreenEnterDuration)
                        )
                    }
                },
                // Pop exit (going back)
                popExitTransition = {
                    if (initialState.destination.route == "now_playing" || targetState.destination.route == "now_playing") {
                         fadeOut(animationSpec = tween(VibeAnimations.ScreenExitDuration))
                    } else {
                        fadeOut(animationSpec = tween(VibeAnimations.ScreenExitDuration)) +
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth / 4 },
                            animationSpec = tween(VibeAnimations.ScreenExitDuration)
                        )
                    }
                }
            ) {
                // ... discovery etc same
                
                // --- Now Playing ---
                composable(
                    route = "now_playing",
                    enterTransition = {
                        // Use ONLY fade to avoidjitter with Shared Element Transition
                        fadeIn(animationSpec = tween(VibeAnimations.ScreenEnterDuration))
                    },
                    popExitTransition = {
                        fadeOut(animationSpec = tween(VibeAnimations.ScreenExitDuration))
                    }
                ) {
                    NowPlayingScreen(
                        playbackViewModel = playbackViewModel,
                        connectionViewModel = connectionViewModel,
                        onBackPressed = { navController.popBackStack() },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this
                    )
                }

                composable("discovery") {
                    // Observe connection state for optimistic navigation
                    val connectionState by connectionViewModel.connectionState.collectAsState()
                    val connectedDevice by connectionViewModel.connectedDevice.collectAsState()
                    
                    // Optimistically navigate to library when connecting
                    LaunchedEffect(connectionState) {
                        if (connectionState == ConnectionState.CONNECTING && connectedDevice != null) {
                            navController.navigate("library") {
                                popUpTo("discovery") { inclusive = true }
                            }
                        }
                    }
                    
                    DiscoveryScreen(
                        viewModel = connectionViewModel,
                        onDeviceSelected = { device ->
                            currentDevice = device
                            connectionViewModel.connectToDevice(device)
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                
                composable("library") {
                    // Sync currentDevice with connectedDevice from ViewModel
                    val device = connectionViewModel.connectedDevice.collectAsState().value
                    currentDevice = device
                    
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
                            contentPadding = innerPadding,
                            connectionViewModel = connectionViewModel
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
                        SearchScreen(
                            viewModel = libraryViewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
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
                // (Discovery, library, albums, search, artists, detail screens are above)
                
                composable("settings") {
                    SettingsScreen(
                        connectionViewModel = connectionViewModel,
                        favoritesManager = favoritesManager,
                        contentPadding = innerPadding
                    )
                }
            }
        }
    }
}
