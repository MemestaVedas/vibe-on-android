package moe.memesta.vibeon.ui

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material3.*
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs
import kotlin.math.PI
import kotlin.math.sin
import moe.memesta.vibeon.R
import moe.memesta.vibeon.data.AlbumInfo
import moe.memesta.vibeon.data.ArtistItemData
import moe.memesta.vibeon.data.TrackInfo
import moe.memesta.vibeon.ui.components.*
import moe.memesta.vibeon.ui.shapes.*
import moe.memesta.vibeon.ui.theme.Dimens
import moe.memesta.vibeon.ui.theme.VibeAnimations
import moe.memesta.vibeon.ui.theme.bouncyClickable
import moe.memesta.vibeon.ui.image.AppImageLoader
import moe.memesta.vibeon.ui.utils.LocalDisplayLanguage
import moe.memesta.vibeon.ui.utils.getDisplayArtist
import moe.memesta.vibeon.ui.utils.getDisplayName
import moe.memesta.vibeon.ui.utils.parseAlbum

// Accent color — now uses MaterialTheme.colorScheme.primary for dynamic theming

private val MPlusRoundedBold = moe.memesta.vibeon.ui.theme.GoogleSansFlexFamily

@OptIn(ExperimentalMaterial3Api::class)
@androidx.compose.animation.ExperimentalSharedTransitionApi
@Composable
fun HomeScreen(
    viewModel: LibraryViewModel,
    statsViewModel: moe.memesta.vibeon.ui.stats.StatsViewModel?,
    onTrackSelected: (TrackInfo) -> Unit,
    onAlbumSelected: (String) -> Unit,
    onArtistSelected: (String) -> Unit,
    onSearchClick: () -> Unit,
    onViewAllSongs: () -> Unit,
    onViewFavorites: () -> Unit,
    onViewPlaylists: () -> Unit,
    onViewStats: () -> Unit,
    onViewOfflineSongs: () -> Unit,
        onViewTorrents: () -> Unit,
        onViewServerDetails: () -> Unit,
    onViewAllAlbums: () -> Unit,
    onViewAllArtists: () -> Unit,
    contentPadding: PaddingValues,
    connectionViewModel: ConnectionViewModel,
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null
) {
    val tracks by viewModel.tracks.collectAsState()
    val albums by viewModel.homeAlbums.collectAsState()
    val artists by viewModel.homeArtists.collectAsState()
    val featuredAlbums by viewModel.featuredAlbums.collectAsState()
    val connectionState by connectionViewModel.connectionState.collectAsState()
        val connectedDevice by connectionViewModel.connectedDevice.collectAsState()
    val statsSummary by statsViewModel?.weeklyOverview?.collectAsState()
        ?: remember { mutableStateOf(null) }
    val isManualRefreshing by viewModel.isManualRefreshRunning.collectAsState()
    val displayLanguage = LocalDisplayLanguage.current

    // isLoading moved below so it can consider preserved data during refresh

    // Define distinct section colors for sidebar layering — tinted by current song
    val themeArgb = MaterialTheme.colorScheme.primary.toArgb()
    val bgColors = remember(themeArgb) {
        val hct = com.google.android.material.color.utilities.Hct.fromInt(themeArgb)
        val scheme = com.google.android.material.color.utilities.SchemeTonalSpot(hct, true, 0.0)
        // neutralPalette is near-achromatic — same hue undertone but chroma ~4,
        // so tone 5 and tone 20 are very dark with only a whisper of the song color.
        Pair(
            Color(scheme.neutralPalette.tone(5)),
            Color(scheme.neutralPalette.tone(20))
        )
    }
    val appBackground = bgColors.first
    val sectionSurface = Color.White.copy(alpha = 0.08f).compositeOver(bgColors.second)
    // Sidebar section colors - distinct for each section to blend wavy separators
    val sidebarTopSection = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val sidebarMiddleSection = MaterialTheme.colorScheme.surface
    val sidebarBottomSection = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

    val listState = rememberLazyListState()
    val heroScrollOffsetPx by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0) {
                listState.firstVisibleItemScrollOffset.toFloat()
            } else {
                520f
            }
        }
    }

    // Pull-to-refresh state (declared early so it can be referenced by preserved-data logic below)
    var isRefreshing by remember { mutableStateOf(false) }
    var rawPullOffset by remember { mutableStateOf(0f) }
    val pullThreshold = 150f
    val pullOffset by animateFloatAsState(
        targetValue = rawPullOffset,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "home_pull_offset"
    )

    // Preserve last non-empty snapshots so UI doesn't go blank during refresh
    var preservedTracks by remember { mutableStateOf(tracks) }
    var preservedAlbums by remember { mutableStateOf(albums) }
    var preservedArtists by remember { mutableStateOf(artists) }
    var preservedFeatured by remember { mutableStateOf(featuredAlbums) }

    // Update preserved snapshots when non-empty data arrives
    LaunchedEffect(tracks, albums, artists, featuredAlbums) {
        if (tracks.isNotEmpty()) preservedTracks = tracks
        if (albums.isNotEmpty()) preservedAlbums = albums
        if (artists.isNotEmpty()) preservedArtists = artists
        if (featuredAlbums.isNotEmpty()) preservedFeatured = featuredAlbums
    }

    // Use preserved data during refresh to avoid blank UI
    val effectiveTracks = if (isRefreshing) preservedTracks else tracks
    val effectiveAlbums = if (isRefreshing) preservedAlbums else albums
    val effectiveArtists = if (isRefreshing) preservedArtists else artists
    val effectiveFeatured = if (isRefreshing) preservedFeatured else featuredAlbums
    val heroPullProgress = if (isRefreshing) 0f else (pullOffset / pullThreshold).coerceIn(0f, 1f)
    val contentPullOffset = if (isRefreshing) {
        maxOf(pullOffset, pullThreshold * 0.72f)
    } else {
        pullOffset
    }

    // Pre-calculate chunks for Recently Added grid
    val recentChunks = remember(effectiveTracks) {
        effectiveTracks.take(20).chunked(4)
    }

    // Follow desktop home logic: prioritize listening history-derived ordering first.
    val prioritizedAlbums = remember(effectiveAlbums, statsSummary) {
        val normalizedAlbums = effectiveAlbums.associateBy { parseAlbum(it.name, null).baseName.lowercase() }
        val summaryAlbumOrder = statsSummary?.topAlbums.orEmpty()
            .mapNotNull { summaryAlbum ->
                normalizedAlbums[parseAlbum(summaryAlbum.album, null).baseName.lowercase()]
            }

        (summaryAlbumOrder + effectiveAlbums)
            .distinctBy { parseAlbum(it.name, null).baseName.lowercase() }
            .take(10)
    }

    val prioritizedArtists = remember(effectiveArtists, statsSummary) {
        val artistsByName = effectiveArtists.associateBy { it.name.lowercase() }
        val summaryArtistOrder = statsSummary?.topArtists.orEmpty()
            .mapNotNull { summaryArtist -> artistsByName[summaryArtist.artist.lowercase()] }

        (summaryArtistOrder + effectiveArtists)
            .distinctBy { it.name.lowercase() }
            .take(10)
    }

    val isLoading = effectiveTracks.isEmpty() && connectionState == ConnectionState.CONNECTED && !isManualRefreshing

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Nested scroll connection for pull-to-refresh
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (isRefreshing) return Offset.Zero
                
                // Allow pull down at top
                if (available.y < 0 && rawPullOffset > 0f) {
                    val consumed = rawPullOffset.coerceAtMost(-available.y)
                    rawPullOffset -= consumed
                    return Offset(0f, -consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (isRefreshing) return Offset.Zero
                
                // Only consume vertical scrolls at top, and only if gesture is primarily vertical
                // This allows horizontal pager swipes to work properly
                if (available.y > 0f && listState.firstVisibleItemIndex == 0) {
                    // Check if the gesture is more vertical than horizontal
                    val isVerticalGesture = abs(available.y) > abs(available.x) * 1.5f
                    
                    if (isVerticalGesture) {
                        rawPullOffset = (rawPullOffset + available.y * 0.55f).coerceAtMost(pullThreshold * 1.55f)
                        return Offset(0f, available.y)
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (rawPullOffset > pullThreshold && !isRefreshing) {
                    isRefreshing = true
                    rawPullOffset = pullThreshold * 0.72f
                    viewModel.refreshLibraryManually()
                } else {
                    rawPullOffset = 0f
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (!isRefreshing && rawPullOffset > 0f) {
                    rawPullOffset = 0f
                }
                return Velocity.Zero
            }
        }
    }

    LaunchedEffect(isManualRefreshing) {
        if (isRefreshing && !isManualRefreshing) {
            isRefreshing = false
            rawPullOffset = 0f
        }
    }

    LaunchedEffect(listState.isScrollInProgress, isRefreshing) {
        if (!isRefreshing && !listState.isScrollInProgress && rawPullOffset > 0f) {
            rawPullOffset = 0f
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = sidebarMiddleSection,
                drawerContentColor = MaterialTheme.colorScheme.onSurface,
                windowInsets = WindowInsets.statusBars
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(sidebarMiddleSection)
                ) {
                    // --- TOP SECTION: FIXED ---
                    // Logo + Top Section Background
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(sidebarTopSection)
                            .statusBarsPadding()
                            .padding(Dimens.ScreenPadding)
                    ) {
                        Column {
                            Image(
                                painter = painterResource(id = R.drawable.ic_vibe_logo),
                                contentDescription = null,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Vibe-On",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Your Music, Your Way",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Top wavy separator - from top section to middle section
                    WavySeparator(
                        colorTop = sidebarTopSection,
                        colorBottom = sidebarMiddleSection,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp),
                        waveHeight = 20.dp
                    )

                    // --- MIDDLE SECTION: SCROLLABLE ---
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(sidebarMiddleSection)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Top
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        NavigationDrawerItem(
                            icon = { 
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                )
                            },
                            label = { Text("All Songs") },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onViewAllSongs()
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )

                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.Favorite,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            label = { Text("Favorites") },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onViewFavorites()
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )

                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.PlaylistPlay,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            label = { Text("Playlists") },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onViewPlaylists()
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )

                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.PlaylistPlay,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            },
                            label = { Text("Offline Songs") },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onViewOfflineSongs()
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )

                        NavigationDrawerItem(
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            },
                            label = { Text("Torrent") },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onViewTorrents()
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // --- BOTTOM SECTION: FIXED ---
                    // Bottom wavy section - from middle to bottom section
                    WavySeparator(
                        colorTop = sidebarMiddleSection,
                        colorBottom = sidebarBottomSection,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp),
                        waveHeight = 20.dp,
                        showAtTop = false
                    )

                    // Server connection section
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(sidebarBottomSection)
                            .clickable {
                                scope.launch { drawerState.close() }
                                onViewServerDetails()
                            },
                        color = Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Connected to",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = connectedDevice?.name ?: "Unknown Server",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize().background(appBackground)) {
        
        // Refresh indicator
        HomeLoadingRefreshIndicator(
            pullOffset = pullOffset,
            contentPullOffset = contentPullOffset,
            pullThreshold = pullThreshold,
            isRefreshing = isRefreshing,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationY = contentPullOffset }
                .nestedScroll(nestedScrollConnection),
            contentPadding = PaddingValues(
                top = 0.dp,
                bottom = contentPadding.calculateBottomPadding() + Dimens.SectionSpacing + 80.dp 
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Keep top spacing only when hero is unavailable.
            if (effectiveFeatured.isEmpty()) {
                item(key = "top_spacer") {
                    Spacer(modifier = Modifier.statusBarsPadding().height(72.dp))
                }
            }

            // Restore hero carousel at the top (blend with collapsed sections below).
            item(key = "section_featured_carousel") {
                if (effectiveFeatured.isNotEmpty() && !isLoading) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        HeroHeader(
                            albums = effectiveFeatured,
                            onPlayClick = { album -> onAlbumSelected(album.name) },
                            displayLanguage = displayLanguage,
                            pullProgress = heroPullProgress,
                            scrollOffsetPx = heroScrollOffsetPx
                        )
                    }
                }
            }

            // Section: Albums
            item(key = "section_albums") {
                if (!isLoading) {
                    Column(
                        modifier = Modifier
                            .background(sectionSurface)
                            .padding(bottom = 24.dp)
                    ) {
                        SectionHeader(
                            "Albums", 
                            onSeeAllClick = onViewAllAlbums,
                            modifier = Modifier.padding(top = Dimens.SectionPadding, start = Dimens.ScreenPadding, end = Dimens.ScreenPadding)
                        )
                        HomeSquishyAlbumCarousel(
                            albums = prioritizedAlbums,
                            displayLanguage = displayLanguage,
                            onAlbumSelected = onAlbumSelected,
                            sharedTransitionScope = null,
                            animatedVisibilityScope = null
                        )
                    }
                }
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Dimens.SectionSpacing, bottom = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        VibeLoadingIndicator(label = "Loading albums...")
                    }
                }
            }

            item(key = "sep_albums_artists") {
                WavySeparator(
                    colorTop = sectionSurface,
                    colorBottom = appBackground
                )
            }

            // Section: Artists
            item(key = "section_artists") {
                if (!isLoading) {
                    Column(
                        modifier = Modifier
                            .background(appBackground)
                            .padding(bottom = 24.dp)
                    ) {
                        SectionHeader(
                            "Your Top Artists", 
                            onSeeAllClick = onViewAllArtists,
                            modifier = Modifier.padding(top = Dimens.SectionPadding, start = Dimens.ScreenPadding, end = Dimens.ScreenPadding)
                        )
                        HomeSquishyArtistCarousel(
                            artists = prioritizedArtists,
                            displayLanguage = displayLanguage,
                            onArtistSelected = onArtistSelected,
                            sharedTransitionScope = null,
                            animatedVisibilityScope = null
                        )
                    }
                }
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Dimens.SectionSpacing, bottom = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        VibeLoadingIndicator(label = "Loading artists...")
                    }
                }
            }

            item(key = "sep_artists_recent") {
                WavySeparator(
                    colorTop = appBackground,
                    colorBottom = sectionSurface
                )
            }

            // Section: Your Songs (Recently Added) - Now after Artists
            item(key = "section_recent") {
                if (!isLoading) {
                    Column(
                        modifier = Modifier
                            .background(sectionSurface)
                            .padding(bottom = 24.dp)
                    ) {
                        SectionHeader(
                            title = "Your Songs", 
                            onSeeAllClick = onViewAllSongs,
                            modifier = Modifier.padding(top = Dimens.SectionPadding, start = Dimens.ScreenPadding, end = Dimens.ScreenPadding)
                        )
                        AnimatedSquigglyLine(
                            modifier = Modifier
                                .padding(horizontal = Dimens.ScreenPadding)
                                .fillMaxWidth()
                                .height(22.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
                        )
                        FadeEdgeLazyRow(
                            contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(
                                items = recentChunks,
                                key = { chunk -> chunk.joinToString("-") { it.path } }
                            ) { columnTracks ->
                                Column(
                                    modifier = Modifier
                                        .width(286.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.45f))
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    columnTracks.forEach { track ->
                                        GridTrackCard(
                                            track = track,
                                            onClick = {
                                                viewModel.playTrack(track)
                                                onTrackSelected(track)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item(key = "sep_recent_stats") {
                WavySeparator(
                    colorTop = sectionSurface,
                    colorBottom = appBackground
                )
            }

            // Section: Statistics
            item(key = "section_stats") {
                if (!isLoading) {
                    Column(
                        modifier = Modifier
                            .background(appBackground)
                            .fillMaxWidth()
                            .padding(bottom = 40.dp)
                    ) {
                        Box(modifier = Modifier.padding(top = Dimens.SectionPadding)) {
                            StatisticsSection(
                                summary = statsSummary,
                                onViewStats = onViewStats
                            )
                        }
                    }
                }
            }
        }

        // Logo Overlay (Static - no rotation)
        Image(
            painter = painterResource(id = R.drawable.ic_vibe_logo),
            contentDescription = "Vibe-On Logo",
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(Dimens.ScreenPadding)
                .size(40.dp)
                .clickable {
                    scope.launch { drawerState.open() }
                }
        )

        // Connection Status Indicator Only (Search icon removed)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(Dimens.ScreenPadding)
        ) {
            ConnectionStatusIndicator(
                connectionState = connectionState,
                modifier = Modifier
            )
        }
        }
    }
}

// Custom easing
private val EaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)

@Composable
fun FadeEdgeLazyRow(
    contentPadding: PaddingValues,
    horizontalArrangement: Arrangement.Horizontal,
    content: LazyListScope.() -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        LazyRow(
            contentPadding = contentPadding,
            horizontalArrangement = horizontalArrangement,
            content = content,
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}

@Composable
private fun AnimatedSquigglyLine(
    modifier: Modifier = Modifier,
    color: Color,
    alpha: Float = 0.92f,
    strokeWidth: androidx.compose.ui.unit.Dp = 3.dp,
    amplitude: androidx.compose.ui.unit.Dp = 3.dp,
    waves: Float = 7.6f,
    animationDurationMillis: Int = 2000,
    samples: Int = 360
) {
    val density = LocalDensity.current
    val infiniteTransition = rememberInfiniteTransition(label = "HomeSongsWave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = animationDurationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "homeSongsWavePhase"
    )

    Canvas(modifier = modifier) {
        if (samples < 2 || size.width <= 0f) return@Canvas
        val centerY = size.height / 2f
        val strokePx = with(density) { strokeWidth.toPx() }
        val amplitudePx = with(density) { amplitude.toPx() }

        val path = Path().apply {
            val step = size.width / (samples - 1)
            moveTo(0f, centerY + (amplitudePx * sin(phase)))
            for (i in 1 until samples) {
                val x = i * step
                val theta = (x / size.width) * ((2f * PI).toFloat() * waves) + phase
                val y = centerY + amplitudePx * sin(theta)
                lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round),
            alpha = alpha
        )
    }
}

@Composable
fun HeroHeader(
    albums: List<AlbumInfo>,
    onPlayClick: (AlbumInfo) -> Unit,
    displayLanguage: moe.memesta.vibeon.data.local.DisplayLanguage = moe.memesta.vibeon.data.local.DisplayLanguage.ORIGINAL,
    pullProgress: Float = 0f,
    scrollOffsetPx: Float = 0f
) {
    if (albums.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { albums.size })
    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
    val heroThemeArgb = MaterialTheme.colorScheme.primary.toArgb()
    val sectionSurface = remember(heroThemeArgb) {
        val hct = com.google.android.material.color.utilities.Hct.fromInt(heroThemeArgb)
        val scheme = com.google.android.material.color.utilities.SchemeTonalSpot(hct, true, 0.0)
        // Use neutral palette tone 20 so this surface matches the neutral background tone
        Color.White.copy(alpha = 0.08f).compositeOver(Color(scheme.neutralPalette.tone(20)))
    }
    val topCorner = (36f * pullProgress.coerceIn(0f, 1f)).dp

    // Auto-scroll
    LaunchedEffect(pagerState, isDragged) {
        if (!isDragged && albums.isNotEmpty()) {
            while (kotlinx.coroutines.currentCoroutineContext().isActive) {
                delay(7000)
                try {
                    if (albums.isNotEmpty()) {
                        val nextPage = (pagerState.currentPage + 1) % albums.size
                        pagerState.animateScrollToPage(
                            nextPage,
                            animationSpec = tween(durationMillis = VibeAnimations.HeroDuration, easing = FastOutSlowInEasing)
                        )
                    }
                } catch (_: CancellationException) {
                    break
                } catch (e: Exception) {
                    Log.w("HomeScreen", "Hero auto-scroll failed", e)
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .clip(
                    if (pullProgress > 0.01f) {
                        RoundedCornerShape(topStart = topCorner, topEnd = topCorner)
                    } else {
                        RectangleShape
                    }
                )
                .aspectRatio(14f/9f),
            userScrollEnabled = true,
            flingBehavior = PagerDefaults.flingBehavior(state = pagerState)
        ) { page ->
            val album = albums.getOrNull(page) ?: return@HorizontalPager

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                val context = LocalContext.current
                val request = remember(album.coverUrl) {
                    ImageRequest.Builder(context)
                        .data(album.coverUrl)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .crossfade(true)
                        .build()
                }

                // Use persisted album main color for overlay
                var dynamicBaseColor by remember(album.albumMainColor) { mutableStateOf(album.albumMainColor?.let(::Color)) }
                // Animated title and artist colors derived from album art palette
                var titleColor by remember { mutableStateOf(Color.White) }
                var artistColor by remember { mutableStateOf(Color.White.copy(alpha = 0.85f)) }
                
                LaunchedEffect(album.albumMainColor) {
                    val seed = album.albumMainColor ?: 0xFF121113.toInt()
                    val hct = com.google.android.material.color.utilities.Hct.fromInt(seed)
                    val scheme = com.google.android.material.color.utilities.SchemeTonalSpot(hct, true, 0.0)
                    val primaryMid = scheme.primaryPalette.tone(50)
                    titleColor = if (primaryMid < 50.0) {
                        Color(scheme.primaryPalette.tone(95))
                    } else {
                        Color(scheme.primaryPalette.tone(10))
                    }
                    artistColor = Color(scheme.secondaryPalette.tone(90))
                    dynamicBaseColor = Color(scheme.primaryPalette.tone(50))
                }

                val finalOverlayColor = dynamicBaseColor ?: MaterialTheme.colorScheme.surface

                // Album art background
                AsyncImage(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Draw-phase-only parallax avoids relayout during scroll.
                            translationY = (scrollOffsetPx * 0.22f).coerceIn(0f, 120f)
                        },
                    contentScale = ContentScale.Crop,
                    model = request,
                    contentDescription = null
                )

                // Tinted overlay for better coloring as requested - now dynamic!
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    finalOverlayColor.copy(alpha = 0f),
                                    finalOverlayColor.copy(alpha = 0.5f),
                                    finalOverlayColor.copy(alpha = 1.0f)
                                )
                            )
                        )
                )

                // Content Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = Dimens.ScreenPadding, vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(end = 88.dp)
                    ) {
                        val animTitleColor by animateColorAsState(targetValue = titleColor, animationSpec = tween(600))
                        val animArtistColor by animateColorAsState(targetValue = artistColor, animationSpec = tween(600))

                        Text(
                            text = album.getDisplayName(displayLanguage),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = MPlusRoundedBold,
                                lineHeight = 32.sp
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = animTitleColor
                        )

                        Text(
                            text = album.getDisplayArtist(displayLanguage).uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = animArtistColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(28.dp))
                    }

                    // Petal Play Button
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 36.dp, end = 16.dp)
                            .size(60.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { onPlayClick(album) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        // STATIC Wavy transition overlay (outside the pager so it doesn't swipe)
        WavySeparator(
            colorTop = sectionSurface,
            colorBottom = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .align(Alignment.BottomCenter),
            waveHeight = 12.dp, // Match other sections
            waveFrequency = 6.5f, // Match other sections
            showAtTop = false // Keep rising direction
        )

        // Pager Indicators with dome shapes - filled for active, outline for inactive
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(albums.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 14.dp else 10.dp)
                        .clip(if (isSelected) ArrowBlobShape else CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.secondary.copy(alpha = 0.65f)
                        )
                )
            }
        }
    }
}

private class PullInwardTopShape(
    private val progress: Float
) : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): Outline {
        val p = progress.coerceIn(0f, 1f)
        val dip = size.height * (0.12f * p)
        val path = Path().apply {
            moveTo(0f, 0f)
            quadraticTo(size.width * 0.25f, 0f, size.width * 0.5f, dip)
            quadraticTo(size.width * 0.75f, 0f, size.width, 0f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}
/**
 * "See All" button to show more items in a horizontal scrollable list
 */
@Composable
fun SeeAllButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Color.White.copy(alpha = 0.06f),
                RoundedCornerShape(16.dp)
            )
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "→",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = "See\nAll",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}



/**
 * Custom pull-to-refresh indicator with morphing shapes animation
 * Positioned to push content down like Google Photos
 */
@Composable
fun HomeLoadingRefreshIndicator(
    pullOffset: Float,
    contentPullOffset: Float,
    pullThreshold: Float,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    // Calculate pull progress (0f to 1f)
    val pullProgress = (pullOffset / pullThreshold).coerceIn(0f, 1f)
    val density = LocalDensity.current
    val statusBarInsetPx = WindowInsets.statusBars.getTop(density).toFloat()
    val indicatorHeightPx = with(density) { 54.dp.toPx() }
    val topPaddingPx = with(density) { 12.dp.toPx() }
    val minClearancePx = with(density) { 6.dp.toPx() }
    val indicatorLiftPx = indicatorHeightPx * 0.5f
    val centeredGapOffset = ((contentPullOffset - indicatorHeightPx) * 0.5f).coerceAtLeast(0f)
    val indicatorY = (statusBarInsetPx + topPaddingPx + centeredGapOffset - indicatorLiftPx)
        .coerceAtLeast(statusBarInsetPx + minClearancePx)
    val displayProgress = if (isRefreshing) 1f else pullProgress
    val indicatorScale = 0.86f + (1f - 0.86f) * displayProgress
    val showIndicator = isRefreshing || pullProgress > 0.08f
    val showContainedLoader = isRefreshing || pullProgress >= 0.82f
    
    AnimatedVisibility(
        visible = showIndicator,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f),
        modifier = modifier
            .offset {
                androidx.compose.ui.unit.IntOffset(
                    0,
                    indicatorY.toInt()
                )
            }
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (showContainedLoader) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.93f),
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .height(52.dp)
                        .widthIn(min = 176.dp)
                        .graphicsLayer {
                            alpha = displayProgress
                            scaleX = indicatorScale
                            scaleY = indicatorScale
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        moe.memesta.vibeon.ui.components.VibeLoadingIndicator(
                            modifier = Modifier.size(28.dp),
                            showLabel = false
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = displayProgress
                            scaleX = indicatorScale
                            scaleY = indicatorScale
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Pull down to sync",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.92f),
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Box(
                            modifier = Modifier
                                .size(width = 26.dp, height = 22.dp)
                                .clip(ArrowBlobShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
private fun HomeSquishyAlbumCarousel(
    albums: List<AlbumInfo>,
    displayLanguage: moe.memesta.vibeon.data.local.DisplayLanguage,
    onAlbumSelected: (String) -> Unit,
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope?,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope?
) {
    if (albums.isEmpty()) return

    val carouselState = rememberCarouselState { albums.size }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cardWidth = (maxWidth * 0.58f).coerceIn(168.dp, 220.dp)

        HorizontalMultiBrowseCarousel(
            state = carouselState,
            preferredItemWidth = cardWidth,
            modifier = Modifier
                .fillMaxWidth()
                .height(cardWidth + 34.dp),
            itemSpacing = Dimens.ItemSpacing,
            contentPadding = PaddingValues(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding)
        ) { index ->
            val album = albums[index]
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                AlbumCard(
                    albumName = album.getDisplayName(displayLanguage),
                    coverUrl = album.coverUrl,
                    onClick = { onAlbumSelected(album.name) },
                    cardSize = cardWidth,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
private fun HomeSquishyArtistCarousel(
    artists: List<ArtistItemData>,
    displayLanguage: moe.memesta.vibeon.data.local.DisplayLanguage,
    onArtistSelected: (String) -> Unit,
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope?,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope?
) {
    if (artists.isEmpty()) return

    val carouselState = rememberCarouselState { artists.size }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cardWidth = (maxWidth * 0.5f).coerceIn(150.dp, 190.dp)

        HorizontalMultiBrowseCarousel(
            state = carouselState,
            preferredItemWidth = cardWidth,
            modifier = Modifier
                .fillMaxWidth()
                .height(cardWidth + 44.dp),
            itemSpacing = Dimens.ItemSpacing,
            contentPadding = PaddingValues(start = Dimens.ScreenPadding, end = Dimens.ScreenPadding)
        ) { index ->
            val artist = artists[index]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .bouncyClickable(scaleDown = 0.96f, indication = null) { onArtistSelected(artist.name) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(cardWidth)
                        .then(
                            if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                                with(sharedTransitionScope) {
                                    Modifier.sharedElement(
                                        sharedContentState = rememberSharedContentState(key = "artist-${artist.name}-grid"),
                                        animatedVisibilityScope = animatedVisibilityScope
                                    )
                                }
                            } else Modifier
                        )
                        .clip(ArtistsShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    if (artist.photoUrl != null) {
                        val context = LocalContext.current
                        val request = remember(artist.photoUrl) {
                            ImageRequest.Builder(context)
                                .data(artist.photoUrl)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .crossfade(true)
                                .build()
                        }
                        AsyncImage(
                            model = request,
                            contentDescription = artist.getDisplayName(displayLanguage),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = artist.getDisplayName(displayLanguage).take(1).uppercase(),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = artist.getDisplayName(displayLanguage),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}