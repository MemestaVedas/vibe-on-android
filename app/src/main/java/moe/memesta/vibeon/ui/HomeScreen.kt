package moe.memesta.vibeon.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import moe.memesta.vibeon.data.AlbumInfo
import moe.memesta.vibeon.data.TrackInfo
import moe.memesta.vibeon.ui.components.*
import moe.memesta.vibeon.ui.theme.Dimens
import moe.memesta.vibeon.ui.theme.VibeAnimations
import moe.memesta.vibeon.ui.utils.LocalDisplayLanguage
import moe.memesta.vibeon.ui.utils.getDisplayArtist
import moe.memesta.vibeon.ui.utils.getDisplayName

// Accent color — now uses MaterialTheme.colorScheme.primary for dynamic theming

@Composable
fun HomeScreen(
    viewModel: LibraryViewModel,
    onTrackSelected: (TrackInfo) -> Unit,
    onAlbumSelected: (String) -> Unit,
    onArtistSelected: (String) -> Unit,
    onSearchClick: () -> Unit,
    onViewAllSongs: () -> Unit,
    contentPadding: PaddingValues,
    connectionViewModel: ConnectionViewModel
) {
    val tracks: List<TrackInfo> by viewModel.tracks.collectAsState()
    val albums by viewModel.homeAlbums.collectAsState()
    val artists by viewModel.homeArtists.collectAsState()
    val featuredAlbums by viewModel.featuredAlbums.collectAsState()
    val connectionState by connectionViewModel.connectionState.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val displayLanguage = LocalDisplayLanguage.current

    val isLoading = tracks.isEmpty() && connectionState == ConnectionState.CONNECTED

    val listState = rememberLazyListState()
    val isDragged by listState.interactionSource.collectIsDraggedAsState()

    // Pre-calculate chunks for Recently Added grid
    val recentChunks = remember(tracks) {
        tracks.take(20).chunked(4)
    }

    // Page-load entrance animation
    var pageVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(50)
        pageVisible = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                top = 72.dp,
                bottom = contentPadding.calculateBottomPadding() + Dimens.SectionSpacing
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Section: Recently Added
            item(key = "section_recent") {
                AnimatedVisibility(
                    visible = pageVisible && !isLoading,
                    enter = fadeIn(tween(300, delayMillis = 80)) + slideInHorizontally(
                        initialOffsetX = { -80 },
                        animationSpec = tween(350, delayMillis = 80)
                    )
                ) {
                    Column(modifier = Modifier.padding(top = Dimens.SectionSpacing)) {
                        SectionHeader(title = "Recently Added", onSeeAllClick = onViewAllSongs)
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

                if (isLoading) {
                    Column(modifier = Modifier.padding(top = Dimens.SectionSpacing)) {
                        SectionHeader("Recently Added")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
                        ) {
                            items(5, key = { "skeleton_recent_$it" }) { SkeletonSquareCard() }
                        }
                    }
                }
            }

            // Section: Featured Carousel (16:9 Hero - Unblurred)
            item(key = "section_featured_carousel") {
                if (featuredAlbums.isNotEmpty() && !isLoading) {
                    HeroHeader(
                        albums = featuredAlbums,
                        onPlayClick = { album -> onAlbumSelected(album.name) },
                        displayLanguage = displayLanguage
                    )
                }
            }

            // Section: Albums (5 items + See All)
            item(key = "section_albums") {
                AnimatedVisibility(
                    visible = pageVisible && !isLoading,
                    enter = fadeIn(tween(300, delayMillis = 160)) + slideInHorizontally(
                        initialOffsetX = { -80 },
                        animationSpec = tween(350, delayMillis = 160)
                    )
                ) {
                    Column(modifier = Modifier.padding(top = Dimens.SectionSpacing)) {
                        SectionHeader("Albums", onSeeAllClick = { /* Navigate to albums page */ })
                        FadeEdgeLazyRow(
                            contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
                        ) {
                            items(
                                items = albums.take(5),  // Limit to 5 albums
                                key = { "album_${it.name}" }
                            ) { album ->
                                val onAlbumClick = remember(album.name) {
                                    { onAlbumSelected(album.name) }
                                }
                                AlbumCard(
                                    albumName = album.getDisplayName(displayLanguage),
                                    coverUrl = album.coverUrl,
                                    onClick = onAlbumClick
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

            // Section: Artists (5 items + See All)
            item(key = "section_artists") {
                AnimatedVisibility(
                    visible = pageVisible && !isLoading,
                    enter = fadeIn(tween(300, delayMillis = 240)) + slideInHorizontally(
                        initialOffsetX = { -80 },
                        animationSpec = tween(350, delayMillis = 240)
                    )
                ) {
                    Column(modifier = Modifier.padding(top = Dimens.SectionSpacing)) {
                        SectionHeader("Artists", onSeeAllClick = { /* Navigate to artists page */ })
                        FadeEdgeLazyRow(
                            contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
                        ) {
                            items(
                                items = artists.take(5),  // Limit to 5 artists
                                key = { "artist_${it.name}" }
                            ) { artist ->
                                val onArtistClick = remember(artist.name) {
                                    { onArtistSelected(artist.name) }
                                }
                                ArtistPill(
                                    artistName = artist.getDisplayName(displayLanguage),
                                    photoUrl = artist.photoUrl,
                                    onClick = onArtistClick
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

            // Section: Statistics
            item(key = "section_stats") {
                AnimatedVisibility(
                    visible = pageVisible && !isLoading,
                    enter = fadeIn(tween(300, delayMillis = 320)) + slideInHorizontally(
                        initialOffsetX = { -80 },
                        animationSpec = tween(350, delayMillis = 320)
                    )
                ) {
                    Column(modifier = Modifier.padding(top = Dimens.SectionSpacing)) {
                        StatisticsSection(stats = stats)
                    }
                }
            }
        }

        // Top-right: Connection Status + Search Icon (glassmorphic pill)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = Dimens.ScreenPadding, end = Dimens.ScreenPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ConnectionStatusIndicator(
                connectionState = connectionState,
                modifier = Modifier
            )

            // Search Icon — glassmorphic button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onSearchClick
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(20.dp)
                    )
                }
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenPadding)
            .padding(top = 64.dp) // Space for the search button overlay
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val album = albums.getOrNull(page) ?: return@HorizontalPager

            // 16:9 card, full-width with rounded corners
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                val context = LocalContext.current
                val request = remember(album.coverUrl) {
                    ImageRequest.Builder(context)
                        .data(album.coverUrl)
                        .crossfade(true)
                        .build()
                }

                // Album art background - NO BLUR for featured section
                AsyncImage(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.85f),
                    contentScale = ContentScale.Crop,
                    model = request,
                    contentDescription = null
                )

                // Dark overlay for legibility - Solid Matte
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0A0A0C).copy(alpha = 0.65f))
                )

                /* Inner glow accent removed for matte look */

                // Content
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    // "New for you" badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "✦  New for you",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = album.getDisplayName(displayLanguage),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White
                    )

                    Text(
                        text = album.getDisplayArtist(displayLanguage),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Play button
                    Button(
                        onClick = { onPlayClick(album) },
                        modifier = Modifier.height(44.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Play Now",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Pager dots
        Row(
            Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pagerState.pageCount) { iteration ->
                val isActive = pagerState.currentPage == iteration
                val dotWidth by animateDpAsState(
                    targetValue = if (isActive) 20.dp else 6.dp,
                    animationSpec = tween(250),
                    label = "dotWidth"
                )
                val dotColor by animateColorAsState(
                    targetValue = if (isActive) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.25f),
                    animationSpec = tween(250),
                    label = "dotColor"
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(dotColor)
                        .height(6.dp)
                        .width(dotWidth)
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