package moe.memesta.vibeon.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import moe.memesta.vibeon.ui.theme.Dimens
import moe.memesta.vibeon.ui.theme.VibeAnimations
import moe.memesta.vibeon.data.SyncStatus
import moe.memesta.vibeon.data.DiscoveredDevice
import moe.memesta.vibeon.ui.*
import moe.memesta.vibeon.ui.pairing.PairingScreen
import moe.memesta.vibeon.ui.onboarding.WelcomeScreen
import moe.memesta.vibeon.ui.onboarding.OnboardingOverlay
import moe.memesta.vibeon.data.local.OnboardingManager
import moe.memesta.vibeon.ui.theme.rememberBitmapFromUrl

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavHost(
    connectionViewModel: ConnectionViewModel,
    playbackViewModel: PlaybackViewModel,
    favoritesManager: moe.memesta.vibeon.data.local.FavoritesManager,
    playerSettingsRepository: moe.memesta.vibeon.data.local.PlayerSettingsRepository,
    trackDao: moe.memesta.vibeon.data.local.TrackDao,
    onboardingManager: OnboardingManager
) {
    val navController = rememberNavController()
    var currentDevice by remember { mutableStateOf<DiscoveredDevice?>(null) }
    
    val pagerState = rememberPagerState(pageCount = { 5 })
    val scope = rememberCoroutineScope()
    
    // Start at main. Pairing is an overlay.
    val startDestination = "main"
    val favorites = remember { favoritesManager.getFavorites() }
    var userDismissedPairing by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(favorites.isNotEmpty()) }
    
    // --- Onboarding state ---
    var showWelcome by androidx.compose.runtime.saveable.rememberSaveable {
        mutableStateOf(!onboardingManager.isWelcomeCompleted)
    }
    var showWalkthrough by remember { mutableStateOf(false) }
    
    // Handle optimistic navigation when auto-connecting to favorites
    val connectionState by connectionViewModel.connectionState.collectAsState()
    val connectedDevice by connectionViewModel.connectedDevice.collectAsState()
    
    // Album art URL + bitmap for PairingScreen morph + dynamic palette
    val currentTrack by connectionViewModel.currentTrack.collectAsState()
    val albumArtUrl = currentTrack.coverUrl
    val albumArtBitmap = rememberBitmapFromUrl(albumArtUrl)

    val libraryRepository = remember(connectedDevice) {
        connectedDevice?.let { device ->
            moe.memesta.vibeon.data.LibraryRepository(trackDao, connectionViewModel.wsClient, device.host, device.port)
        }
    }

    // Shared LibraryViewModel instance - preserved across navigation
    val libraryViewModel = remember(libraryRepository) {
        libraryRepository?.let { repo ->
            LibraryViewModel(repository = repo, wsClient = connectionViewModel.wsClient)
        }
    }

    val statsViewModel = remember(libraryRepository) {
        libraryRepository?.let { repo ->
            moe.memesta.vibeon.ui.stats.StatsViewModel(
                repository = repo,
                trackDao = trackDao,
                wsClient = connectionViewModel.wsClient,
                localStatsRepository = connectionViewModel.localStatsRepository
            )
        }
    }
    
    LaunchedEffect(connectionState, connectedDevice) {
        // If connection fails, show the pairing screen again
        if (connectionState == ConnectionState.FAILED) {
            userDismissedPairing = false
        }
    }
    
    // Determine if bottom bar should be transparent
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
        val showBottomBar = currentRoute in listOf("main", "all_songs", "library", "albums", "search", "artists", "settings", "stats", "torrents", "server_details")


    SharedTransitionLayout {
        // Enclose everything in a Box so the PairingScreen can overlay the entire Scaffold
        Box(modifier = Modifier.fillMaxSize()) {
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
                        onNavigateToSearch = { navController.navigate("search") },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this,
                        pagerState = pagerState
                    )
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            val syncStatus by libraryViewModel?.syncStatus?.collectAsState() ?: remember { mutableStateOf(SyncStatus()) }
            
            Box(modifier = Modifier.fillMaxSize()) {
                NavHost(
                navController = navController, 
                startDestination = startDestination,
                modifier = Modifier.fillMaxSize(),
                // Standard enter (forward push)
                enterTransition = {
                    if (initialState.destination.route == "now_playing" || targetState.destination.route == "now_playing") {
                         fadeIn(animationSpec = tween(VibeAnimations.ScreenEnterDuration))
                    } else {
                        enterTransition()
                    }
                },
                // Standard exit (forward push)
                exitTransition = {
                    if (targetState.destination.route == "now_playing" || initialState.destination.route == "now_playing") {
                        fadeOut(animationSpec = tween(VibeAnimations.ScreenExitDuration))
                    } else {
                        exitTransition()
                    }
                },
                // Pop enter (going back) - previous page slides in from left with parallax & zoom
                popEnterTransition = {
                    if (initialState.destination.route == "now_playing") {
                        fadeIn(animationSpec = tween(VibeAnimations.ScreenEnterDuration))
                    } else {
                        popEnterTransition()
                    }
                },
                // Pop exit (going back) - current page slides out to right with scale down
                popExitTransition = {
                    if (initialState.destination.route == "now_playing" || targetState.destination.route == "now_playing") {
                         fadeOut(animationSpec = tween(VibeAnimations.ScreenExitDuration))
                    } else {
                        popExitTransition()
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
                        onNavigateToAlbum = { albumName ->
                            navController.popBackStack()
                            navController.navigate("album/${java.net.URLEncoder.encode(albumName, "UTF-8")}")
                        },
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
                    
                    // Start scanning when this screen is active
                    DisposableEffect(Unit) {
                        connectionViewModel.startScanning()
                        onDispose {
                            connectionViewModel.stopScanning()
                        }
                    }
                    
                    val devices by connectionViewModel.discoveredDevices.collectAsState()

                    moe.memesta.vibeon.ui.pairing.PairingScreen(
                        devices = devices,
                        connectionState = connectionState,
                        connectedDevice = connectedDevice,
                        albumArtUrl = albumArtUrl,
                        albumArtBitmap = albumArtBitmap,
                        onConnect = { ip, port ->
                            val device = DiscoveredDevice(
                                name = "Manual: $ip",
                                host = ip,
                                port = port
                            )
                            currentDevice = device
                            connectionViewModel.connectToDevice(device)
                        },
                        onDeviceSelected = { device ->
                            currentDevice = device
                            connectionViewModel.connectToDevice(device)
                        },
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToScan = {
                            // TODO: Implement QR Scanning
                        },
                        onNavigateToOffline = {
                            navController.navigate("all_songs") { launchSingleTop = true }
                        }
                    )
                }

                composable(
                    route = "pairing",
                    enterTransition = {
                        fadeIn(animationSpec = tween(VibeAnimations.HeroDuration)) +
                            slideInVertically(
                                initialOffsetY = { fullHeight -> fullHeight / 8 },
                                animationSpec = tween(VibeAnimations.HeroDuration)
                            ) +
                            scaleIn(initialScale = 0.98f)
                    },
                    exitTransition = {
                        fadeOut(animationSpec = tween(VibeAnimations.ScreenExitDuration)) +
                            scaleOut(targetScale = 0.98f)
                    },
                    popEnterTransition = {
                        fadeIn(animationSpec = tween(VibeAnimations.ScreenEnterDuration)) +
                            scaleIn(initialScale = 0.98f)
                    },
                    popExitTransition = {
                        fadeOut(animationSpec = tween(VibeAnimations.ScreenExitDuration)) +
                            slideOutVertically(
                                targetOffsetY = { fullHeight -> fullHeight / 10 },
                                animationSpec = tween(VibeAnimations.ScreenExitDuration)
                            )
                    }
                ) {
                    DisposableEffect(Unit) {
                        connectionViewModel.startScanning()
                        onDispose { connectionViewModel.stopScanning() }
                    }

                    val devices by connectionViewModel.discoveredDevices.collectAsState()
                    val connectionState by connectionViewModel.connectionState.collectAsState()
                    val connectedDevice by connectionViewModel.connectedDevice.collectAsState()

                    LaunchedEffect(connectionState, connectedDevice) {
                        if (connectionState == ConnectionState.CONNECTING && connectedDevice != null) {
                            navController.navigate("library") {
                                popUpTo("pairing") { inclusive = true }
                            }
                        }
                    }

                    PairingScreen(
                        devices = devices,
                        connectionState = connectionState,
                        connectedDevice = connectedDevice,
                        albumArtUrl = albumArtUrl,
                        albumArtBitmap = albumArtBitmap,
                        onConnect = { ip, port ->
                            connectionViewModel.connectToDevice(
                                DiscoveredDevice(
                                    name = "Manual: $ip",
                                    host = ip,
                                    port = port
                                )
                            )
                        },
                        onDeviceSelected = { device ->
                            currentDevice = device
                            connectionViewModel.connectToDevice(device)
                        },
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToScan = { /* TODO: QR pairing */ },
                        onNavigateToOffline = {
                            navController.navigate("all_songs") { launchSingleTop = true }
                        }
                    )
                }
                
                composable("main") {
                    BackHandler(enabled = pagerState.currentPage != 0) {
                        scope.launch { pagerState.animateScrollToPage(0) }
                    }

                    if (connectedDevice != null) {
                        currentDevice = connectedDevice
                    }
                    
                    if (libraryViewModel != null) {
                        MainContentPager(
                            pagerState = pagerState,
                            libraryViewModel = libraryViewModel,
                            statsViewModel = statsViewModel,
                            connectionViewModel = connectionViewModel,
                            playbackViewModel = playbackViewModel,
                            favoritesManager = favoritesManager,
                            playerSettingsRepository = playerSettingsRepository,
                            navController = navController,
                            contentPadding = innerPadding,
                            onNavigateToPlayer = { navController.navigate("now_playing") { launchSingleTop = true } },
                            onSearchClick = { navController.navigate("search") },
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this
                        )
                    } else {
                        // Keep a placeholder behind the overlay while pairing
                        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF141414)))
                    }
                }

                composable("library") {
                    LaunchedEffect(Unit) {
                        navController.navigate("main") {
                            popUpTo("library") { inclusive = true }
                        }
                    }
                }

                // All Songs - Reusing LibraryScreen
                composable("all_songs") {
                    // Use shared LibraryViewModel
                    if (connectedDevice != null) {
                        currentDevice = connectedDevice
                    }
                    
                    if (libraryViewModel != null) {
                        moe.memesta.vibeon.ui.LibraryScreen(
                            viewModel = libraryViewModel,
                            onBackClick = { navController.popBackStack() },
                            onTrackSelected = { /* Update pill only, no navigation */ },
                            onNavigateToPlayer = { navController.navigate("now_playing") },
                            contentPadding = innerPadding
                        )
                    } else {
                        LaunchedEffect(Unit) { navController.navigate("discovery") }
                    }
                }

                // Albums Tab - Redirecting to main pager
                composable("albums") {
                    LaunchedEffect(Unit) {
                        navController.navigate("main") {
                            popUpTo("albums") { inclusive = true }
                        }
                        pagerState.scrollToPage(1)
                    }
                }
                // Search Screen - Global Overlay Dialog
                dialog(
                    route = "search",
                    dialogProperties = androidx.compose.ui.window.DialogProperties(
                        usePlatformDefaultWidth = false,
                        dismissOnBackPress = true,
                        dismissOnClickOutside = true,
                        decorFitsSystemWindows = false
                    )
                ) {
                    if (libraryViewModel != null) {
                        SearchScreen(
                            viewModel = libraryViewModel,
                            onTrackSelected = {
                                navController.popBackStack()
                                /* Update pill only, no navigation */
                            },
                            onAlbumSelected = { albumName ->
                                navController.popBackStack()
                                navController.navigate("album/${java.net.URLEncoder.encode(albumName, "UTF-8")}")
                            },
                            onArtistSelected = { artistName ->
                                navController.popBackStack()
                                navController.navigate("artist/${java.net.URLEncoder.encode(artistName, "UTF-8")}")
                            },
                            onClose = { navController.popBackStack() },
                            contentPadding = innerPadding
                        )
                    } else {
                        LaunchedEffect(Unit) { navController.navigate("discovery") }
                    }
                }
                
                // Artists Tab - Redirecting to main pager
                composable("artists") {
                    LaunchedEffect(Unit) {
                        navController.navigate("main") {
                            popUpTo("artists") { inclusive = true }
                        }
                        pagerState.scrollToPage(3)
                    }
                }
                
                // Detail Screens
                composable(
                    route = "album/{albumName}",
                    arguments = listOf(androidx.navigation.navArgument("albumName") { type = androidx.navigation.NavType.StringType })
                ) { backStackEntry ->
                    val albumName = backStackEntry.arguments?.getString("albumName") ?: return@composable
                    // Use shared LibraryViewModel

                    if (libraryViewModel != null) {
                        AlbumDetailScreen(
                            albumName = albumName,
                            viewModel = libraryViewModel,
                            navController = navController,
                            onBackClick = { navController.popBackStack() },
                            onTrackSelected = { /* Update pill only, no navigation */ },
                            contentPadding = innerPadding
                        )
                    }
                }

                composable(
                    route = "artist/{artistName}",
                    arguments = listOf(androidx.navigation.navArgument("artistName") { type = androidx.navigation.NavType.StringType })
                ) { backStackEntry ->
                    val artistName = backStackEntry.arguments?.getString("artistName") ?: return@composable
                        // Use shared LibraryViewModel

                    if (libraryViewModel != null) {
                        ArtistDetailScreen(
                            artistName = artistName,
                            viewModel = libraryViewModel,
                            navController = navController,
                            onBackClick = { navController.popBackStack() },
                            onTrackSelected = { /* Update pill only, no navigation */ },
                            contentPadding = innerPadding
                        )
                    }
                }
                
                composable(
                    route = "playlist/{playlistId}",
                    arguments = listOf(androidx.navigation.navArgument("playlistId") { type = androidx.navigation.NavType.StringType })
                ) { backStackEntry ->
                    val playlistId = backStackEntry.arguments?.getString("playlistId") ?: return@composable

                    if (libraryViewModel != null) {
                        PlaylistDetailScreen(
                            playlistId = playlistId,
                            viewModel = libraryViewModel,
                            connectionViewModel = connectionViewModel,
                            onBackClick = { navController.popBackStack() },
                            onTrackSelected = {
                                navController.popBackStack()
                                /* Update pill only, no navigation */
                            },
                            contentPadding = innerPadding
                        )
                    }
                }
                // (Discovery, library, albums, search, artists, detail screens are above)
                
                composable("settings") {
                    LaunchedEffect(Unit) {
                        navController.navigate("main") {
                            popUpTo("settings") { inclusive = true }
                        }
                        pagerState.scrollToPage(4)
                    }
                }
                composable("stats") {
                    statsViewModel?.let { vm ->
                        moe.memesta.vibeon.ui.stats.StatsScreen(
                            statsViewModel = vm,
                            onBackPressed = { navController.popBackStack() }
                        )
                    }
                }
                
                    composable("torrents") {
                        TorrentsScreen(
                            onBackPressed = { navController.popBackStack() }
                        )
                    }
                
                    composable("server_details") {
                        ServerDetailsScreen(
                            connectionViewModel = connectionViewModel,
                            onBackPressed = { navController.popBackStack() },
                            onDisconnect = {
                                connectionViewModel.disconnect()
                            }
                        )
                    }
                }
            
                // Global Sync Banner
                AnimatedVisibility(
                    visible = syncStatus.isSyncing,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(Dimens.ScreenPadding)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Dimens.CornerRadiusLarge))
                            .clickable {
                                navController.navigate("settings")
                            },
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 8.dp,
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Syncing library: ${(syncStatus.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Home screen walkthrough — one-time overlay after first connection
                if (showWalkthrough) {
                    OnboardingOverlay(
                        onDismiss = {
                            onboardingManager.isWalkthroughCompleted = true
                            showWalkthrough = false
                        }
                    )
                }
            }
        } // End of Scaffold
            
        // Pairing Overlay - Covers the app until dismissed, placed OUTSIDE Scaffold
        if (!userDismissedPairing && !showWelcome) {
            // We need to manage scanning
            DisposableEffect(Unit) {
                connectionViewModel.startScanning()
                onDispose {
                    connectionViewModel.stopScanning()
                }
            }
            
            val devices by connectionViewModel.discoveredDevices.collectAsState()
            
            moe.memesta.vibeon.ui.pairing.PairingScreen(
                devices = devices,
                connectionState = connectionState,
                connectedDevice = connectedDevice,
                albumArtUrl = albumArtUrl,
                albumArtBitmap = albumArtBitmap,
                onConnect = { ip, port ->
                    val device = DiscoveredDevice(name = "Manual: $ip", host = ip, port = port)
                    connectionViewModel.connectToDevice(device)
                },
                onDeviceSelected = { device ->
                    connectionViewModel.connectToDevice(device)
                },
                onNavigateBack = { 
                    // Dismiss if desired, or let back handle it
                },
                onNavigateToScan = { },
                onNavigateToOffline = {
                    navController.navigate("all_songs") { launchSingleTop = true }
                },
                onDismiss = {
                    userDismissedPairing = true
                    // Trigger walkthrough for new users after first connection
                    if (!onboardingManager.isWalkthroughCompleted) {
                        showWalkthrough = true
                    }
                },
                onTroubleshoot = {
                    // Restart scanning
                    connectionViewModel.stopScanning()
                    connectionViewModel.startScanning()
                }
            )
        }
        
        // Welcome Screen overlay — shown only on first launch, before pairing
        if (showWelcome) {
            WelcomeScreen(
                onComplete = {
                    onboardingManager.isWelcomeCompleted = true
                    showWelcome = false
                }
            )
        }
            
    } // End of parent Box
} // End of SharedTransitionLayout
}
