package moe.memesta.vibeon.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.material3.*
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import moe.memesta.vibeon.R
import moe.memesta.vibeon.data.AlbumInfo
import moe.memesta.vibeon.data.TrackInfo
import moe.memesta.vibeon.ui.components.*
import moe.memesta.vibeon.ui.theme.Dimens
import moe.memesta.vibeon.ui.theme.DomeShape
import moe.memesta.vibeon.ui.theme.VibeAnimations
import moe.memesta.vibeon.ui.utils.LocalDisplayLanguage
import moe.memesta.vibeon.ui.utils.getDisplayArtist
import moe.memesta.vibeon.ui.utils.getDisplayName

// Accent color — now uses MaterialTheme.colorScheme.primary for dynamic theming

@Composable
fun HomeScreen(
    viewModel: LibraryViewModel,
    statsViewModel: moe.memesta.vibeon.ui.stats.StatsViewModel?,
    onTrackSelected: (TrackInfo) -> Unit,
    onAlbumSelected: (String) -> Unit,
    onArtistSelected: (String) -> Unit,
    onSearchClick: () -> Unit,
    onViewAllSongs: () -> Unit,
    onViewStats: () -> Unit,
        onViewTorrents: () -> Unit,
        onViewServerDetails: () -> Unit,
    onViewAllAlbums: () -> Unit,
    onViewAllArtists: () -> Unit,
    contentPadding: PaddingValues,
    connectionViewModel: ConnectionViewModel
) {
    val tracks by viewModel.tracks.collectAsState()
    val albums by viewModel.homeAlbums.collectAsState()
    val artists by viewModel.homeArtists.collectAsState()
    val featuredAlbums by viewModel.featuredAlbums.collectAsState()
    val connectionState by connectionViewModel.connectionState.collectAsState()
        val connectedDevice by connectionViewModel.connectedDevice.collectAsState()
    val statsSummary by statsViewModel?.weeklyOverview?.collectAsState()
        ?: remember { mutableStateOf(null) }
    val displayLanguage = LocalDisplayLanguage.current

    val isLoading = tracks.isEmpty() && connectionState == ConnectionState.CONNECTED

    // Define distinct section colors for sidebar layering
    val sectionSurface = Color.White.copy(alpha = 0.08f).compositeOver(MaterialTheme.colorScheme.surface)
    val appBackground = MaterialTheme.colorScheme.background
    // Sidebar section colors - distinct for each section to blend wavy separators
    val sidebarTopSection = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val sidebarMiddleSection = MaterialTheme.colorScheme.surface
    val sidebarBottomSection = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

    val listState = rememberLazyListState()
    // Pre-calculate chunks for Recently Added grid
    val recentChunks = remember(tracks) {
        tracks.take(20).chunked(4)
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Pull-to-refresh state
    var isRefreshing by remember { mutableStateOf(false) }
    var pullOffset by remember { mutableStateOf(0f) }
    val pullThreshold = 150f
    
    // Nested scroll connection for pull-to-refresh
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (isRefreshing) return Offset.Zero
                
                // Allow pull down at top
                if (available.y < 0 && pullOffset > 0f) {
                    val consumed = pullOffset.coerceAtMost(-available.y)
                    pullOffset -= consumed
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
                
                // Allow pull down when at top
                if (available.y > 0f && listState.firstVisibleItemIndex == 0) {
                    pullOffset = (pullOffset + available.y * 0.5f).coerceAtMost(pullThreshold * 1.5f)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pullOffset > pullThreshold && !isRefreshing) {
                    isRefreshing = true
                    scope.launch {
                        // Disconnect and reconnect to refresh the connection
                        val currentDevice = connectedDevice
                        if (currentDevice != null) {
                            connectionViewModel.disconnect()
                            delay(500)
                            connectionViewModel.connectToDevice(currentDevice)
                        }
                        delay(1500)
                        isRefreshing = false
                        pullOffset = 0f
                    }
                } else {
                    pullOffset = 0f
                }
                return Velocity.Zero
            }
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
                                    imageVector = Icons.Rounded.BarChart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            },
                            label = { Text("Statistics") },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onViewStats()
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
        MorphingShapesRefreshIndicator(
            pullOffset = pullOffset,
            pullThreshold = pullThreshold,
            isRefreshing = isRefreshing,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .nestedScroll(nestedScrollConnection)
                .offset { androidx.compose.ui.unit.IntOffset(0, pullOffset.toInt()) },
            contentPadding = PaddingValues(
                top = 0.dp,
                bottom = contentPadding.calculateBottomPadding() + Dimens.SectionSpacing + 80.dp 
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Spacer for logo/search if hero is missing or loading
            if (featuredAlbums.isEmpty() || isLoading) {
                item(key = "top_spacer") {
                    Spacer(modifier = Modifier.statusBarsPadding().height(72.dp))
                }
            }

            // Section: Featured Carousel (TOP)
            item(key = "section_featured_carousel") {
                if (featuredAlbums.isNotEmpty() && !isLoading) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        HeroHeader(
                            albums = featuredAlbums,
                            onPlayClick = { album -> onAlbumSelected(album.name) },
                            displayLanguage = displayLanguage
                        )
                    }
                }
            }

            // Transitions are now handled inside HeroHeader for smoother flow

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
                            modifier = Modifier.padding(top = Dimens.SectionPadding)
                        )
                        FadeEdgeLazyRow(
                            contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
                        ) {
                            items(
                                items = albums.take(5),
                                key = { "home_album_${it.name}" }
                            ) { album ->
                                AlbumCard(
                                    albumName = album.getDisplayName(displayLanguage),
                                    coverUrl = album.coverUrl,
                                    onClick = { onAlbumSelected(album.name) }
                                )
                            }
                        }
                    }
                }
                if (isLoading) {
                    Column(modifier = Modifier.padding(top = Dimens.SectionSpacing)) {
                        SectionHeader("Albums")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
                        ) {
                            items(5, key = { "skeleton_album_$it" }) { SkeletonAlbumCard() }
                        }
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
                            "Top Artists", 
                            onSeeAllClick = onViewAllArtists,
                            modifier = Modifier.padding(top = Dimens.SectionPadding)
                        )
                        FadeEdgeLazyRow(
                            contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
                        ) {
                            items(
                                items = artists.take(5),
                                key = { "artist_${it.name}" }
                            ) { artist ->
                                ArtistPill(
                                    artistName = artist.getDisplayName(displayLanguage),
                                    photoUrl = artist.photoUrl,
                                    onClick = { onArtistSelected(artist.name) }
                                )
                            }
                        }
                    }
                }
                if (isLoading) {
                    Column(modifier = Modifier.padding(top = Dimens.SectionSpacing)) {
                        SectionHeader("Artists")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
                        ) {
                            items(5, key = { "skeleton_artist_$it" }) { SkeletonArtistPill() }
                        }
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
                            modifier = Modifier.padding(top = Dimens.SectionPadding)
                        )
                        FadeEdgeLazyRow(
                            contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(
                                items = recentChunks,
                                key = { chunk -> chunk.joinToString("-") { it.path } }
                            ) { columnTracks ->
                                Column(
                                    modifier = Modifier.width(280.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
fun HeroHeader(
    albums: List<AlbumInfo>,
    onPlayClick: (AlbumInfo) -> Unit,
    displayLanguage: moe.memesta.vibeon.data.local.DisplayLanguage = moe.memesta.vibeon.data.local.DisplayLanguage.ORIGINAL
) {
    if (albums.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { albums.size })
    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
    val sectionSurface = Color.White.copy(alpha = 0.08f).compositeOver(MaterialTheme.colorScheme.surface)

    // Auto-scroll
    LaunchedEffect(pagerState, isDragged) {
        if (!isDragged && albums.isNotEmpty()) {
            while (true) {
                delay(7000)
                try {
                    if (albums.isNotEmpty()) {
                        val nextPage = (pagerState.currentPage + 1) % albums.size
                        pagerState.animateScrollToPage(
                            nextPage,
                            animationSpec = tween(durationMillis = VibeAnimations.HeroDuration, easing = FastOutSlowInEasing)
                        )
                    }
                } catch (_: Exception) { }
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
                        .crossfade(true)
                        .allowHardware(false) // Required for MCU palette extraction
                        .build()
                }

                // Extract Dynamic Color for Overlay
                var dynamicBaseColor by remember { mutableStateOf<Color?>(null) }
                
                LaunchedEffect(album.coverUrl) {
                    val loader = coil.ImageLoader(context)
                    val result = loader.execute(request)
                    if (result is coil.request.SuccessResult) {
                        val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                        if (bitmap != null) {
                            // Scale down for speed
                            val scaled = android.graphics.Bitmap.createScaledBitmap(bitmap, 64, 64, false)
                            val pixels = IntArray(scaled.width * scaled.height)
                            scaled.getPixels(pixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)
                            if (scaled != bitmap) scaled.recycle()

                            val quantized = com.google.android.material.color.utilities.QuantizerCelebi.quantize(pixels, 128)
                            val scored = com.google.android.material.color.utilities.Score.score(quantized)
                            val sourceColor = if (scored.isNotEmpty()) scored[0] else 0xFF121113.toInt()

                            val hct = com.google.android.material.color.utilities.Hct.fromInt(sourceColor)
                            val scheme = com.google.android.material.color.utilities.SchemeTonalSpot(hct, true, 0.0)
                            
                            // Use the primary palette at a darker tone (50) for vibrant dominant color
                            dynamicBaseColor = Color(scheme.primaryPalette.tone(50))
                        }
                    }
                }

                val finalOverlayColor = dynamicBaseColor ?: MaterialTheme.colorScheme.surface

                // Album art background
                AsyncImage(
                    modifier = Modifier.fillMaxSize(),
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
                        modifier = Modifier.align(Alignment.BottomStart)
                    ) {
                        // "New for you" badge - Redesigned
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(50))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Favorite,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "New for you",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = album.getDisplayName(displayLanguage),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Black,
                                lineHeight = 32.sp,
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.5f),
                                    blurRadius = 8f
                                )
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White
                        )

                        Text(
                            text = album.getDisplayArtist(displayLanguage).uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White.copy(alpha = 0.8f),
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
                            .clip(PetalShape(petals = 8, depth = 0.15f))
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
                        .size(width = 12.dp, height = 14.dp)
                        .clip(DomeShape)
                        .background(
                            if (isSelected) Color.White else Color.White.copy(alpha = 0.4f)
                        )
                )
            }
        }
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

class WavyBottomShape(private val waveHeight: androidx.compose.ui.unit.Dp, private val waveFrequency: Float) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(size: androidx.compose.ui.geometry.Size, layoutDirection: androidx.compose.ui.unit.LayoutDirection, density: androidx.compose.ui.unit.Density): androidx.compose.ui.graphics.Outline {
        val width = size.width
        val height = size.height
        val amplitude = with(density) { waveHeight.toPx() }
        val freq = waveFrequency * 2f * Math.PI.toFloat() / width

        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, 0f)
            lineTo(width, 0f)
            
            val waveBottomBase = height - amplitude
            for (x in width.toInt() downTo 0 step 5) {
                val angle: Double = ((x.toFloat() * freq)).toDouble()
                val sinValue: Float = kotlin.math.sin(angle).toFloat()
                val waveFactor: Float = (sinValue + 1f) * 0.5f 
                val y: Float = waveBottomBase + (waveFactor * amplitude)
                
                if (x == width.toInt()) {
                    lineTo(width, y)
                } else {
                    lineTo(x.toFloat(), y)
                }
            }
            lineTo(0f, waveBottomBase + ((kotlin.math.sin(0.0).toFloat() + 1f) * 0.5f * amplitude))
            close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

class PetalShape(private val petals: Int = 8, private val depth: Float = 0.15f) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(size: androidx.compose.ui.geometry.Size, layoutDirection: androidx.compose.ui.unit.LayoutDirection, density: androidx.compose.ui.unit.Density): androidx.compose.ui.graphics.Outline {
        val path = androidx.compose.ui.graphics.Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val baseRadius = size.width / 2f * (1f - depth)
        val variation = size.width / 2f * depth

        for (i in 0..360 step 5) {
            val angle = Math.toRadians(i.toDouble())
            val r = baseRadius + variation * kotlin.math.sin(angle * petals)
            val x = cx + r * kotlin.math.cos(angle).toFloat()
            val y = cy + r * kotlin.math.sin(angle).toFloat()
            if (i == 0) {
                path.moveTo(x.toFloat(), y.toFloat())
            } else {
                path.lineTo(x.toFloat(), y.toFloat())
            }
        }
        path.close()
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

/**
 * Custom pull-to-refresh indicator with morphing shapes animation
 * Positioned to push content down like Google Photos
 */
@Composable
fun MorphingShapesRefreshIndicator(
    pullOffset: Float,
    pullThreshold: Float,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    // Calculate pull progress (0f to 1f)
    val pullProgress = (pullOffset / pullThreshold).coerceIn(0f, 1f)
    
    AnimatedVisibility(
        visible = isRefreshing || pullProgress > 0f,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f),
        modifier = modifier
            .statusBarsPadding()
            .padding(vertical = 16.dp)
            .offset { androidx.compose.ui.unit.IntOffset(0, pullOffset.toInt()) }
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                        CircleShape
                    )
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    // Show a scaling indicator during pull - morphing circle
                    val animatedSize by animateDpAsState(
                        targetValue = (32.dp * pullProgress).coerceAtMost(32.dp),
                        label = "pullSize"
                    )
                    Box(
                        modifier = Modifier
                            .size(animatedSize)
                            .background(
                                MaterialTheme.colorScheme.onPrimaryContainer,
                                CircleShape
                            )
                    )
                }
            }
        }
    }
}