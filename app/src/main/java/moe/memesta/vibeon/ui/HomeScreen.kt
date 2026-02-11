package moe.memesta.vibeon.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import moe.memesta.vibeon.data.AlbumInfo
import moe.memesta.vibeon.data.TrackInfo
import moe.memesta.vibeon.ui.components.*
import moe.memesta.vibeon.ui.theme.Dimens
import moe.memesta.vibeon.ui.theme.VibeAnimations
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    viewModel: LibraryViewModel,
    onTrackSelected: (TrackInfo) -> Unit,
    onAlbumSelected: (String) -> Unit,
    onArtistSelected: (String) -> Unit,
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
    
    val isLoading = tracks.isEmpty() && connectionState == ConnectionState.CONNECTED
    
    val listState = rememberLazyListState()
    
    // listState tracking
    val isDragged by listState.interactionSource.collectIsDraggedAsState()
    
    // Pre-calculate chunks
    val recentChunks = remember(tracks) { 
        tracks.take(20).chunked(4) 
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                bottom = contentPadding.calculateBottomPadding() + Dimens.SectionSpacing
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing)
        ) {
            // Hero Header
            item(key = "hero_header") {
                HeroHeader(
                    albums = featuredAlbums,
                    onPlayClick = { album ->
                         // Play first track of album (logic handled in VM usually, for now opening album)
                         onAlbumSelected(album.name)
                    },
                    scrollState = listState
                )
            }

            // Section: Recently Added
            item(key = "section_recent") {
                AnimatedVisibility(
                    visible = !isLoading,
                    enter = fadeIn(animationSpec = tween(300))
                ) {
                    Column {
                        SectionHeader(
                            title = "Recently Added",
                            onSeeAllClick = onViewAllSongs
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(
                                items = recentChunks, 
                                key = { chunk -> 
                                    // Combine paths of all tracks in chunk for a truly unique key
                                    chunk.joinToString("-") { it.path }
                                }
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
                    Column {
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

            // Section: Albums
            item(key = "section_albums") {
                AnimatedVisibility(
                    visible = !isLoading,
                    enter = fadeIn(animationSpec = tween(300, delayMillis = 100))
                ) {
                    Column {
                        SectionHeader("Albums")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
                        ) {
                            items(
                                items = albums,
                                key = { "album_${it.name}" }
                            ) { album ->
                                val onAlbumClick = remember(album.name) {
                                    { onAlbumSelected(album.name) }
                                }
                                AlbumCard(
                                    albumName = album.name,
                                    coverUrl = album.coverUrl,
                                    onClick = onAlbumClick
                                )
                            }
                        }
                    }
                }
                if (isLoading) {
                    Column {
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

            // Section: Artists
            item(key = "section_artists") {
                AnimatedVisibility(
                    visible = !isLoading,
                    enter = fadeIn(animationSpec = tween(300, delayMillis = 200))
                ) {
                    Column {
                        SectionHeader("Artists")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
                        ) {
                            items(
                                items = artists,
                                key = { "artist_${it.name}" }
                            ) { artist ->
                                val onArtistClick = remember(artist.name) {
                                    { onArtistSelected(artist.name) }
                                }
                                ArtistPill(
                                    artistName = artist.name,
                                    photoUrl = artist.photoUrl,
                                    onClick = onArtistClick
                                )
                            }
                        }
                    }
                }
                if (isLoading) {
                    Column {
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
                    visible = !isLoading,
                    enter = fadeIn(animationSpec = tween(300, delayMillis = 300))
                ) {
                    StatisticsSection(stats = stats)
                }
            }
        }
        
        // Connection Status Indicator (Top-Right)
        ConnectionStatusIndicator(
            connectionState = connectionState,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = Dimens.ScreenPadding, end = Dimens.ScreenPadding + 8.dp)
        )
    }
}

@Composable
fun HeroHeader(
    albums: List<AlbumInfo>,
    onPlayClick: (AlbumInfo) -> Unit,
    scrollState: androidx.compose.foundation.lazy.LazyListState
) {
    if (albums.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { albums.size })
    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
    
    // Auto-scroll logic optimized
    LaunchedEffect(pagerState, isDragged) {
        if (!isDragged && albums.isNotEmpty()) {
            while(true) {
                delay(7000)
                try {
                    if (albums.isNotEmpty()) {
                        val nextPage = (pagerState.currentPage + 1) % albums.size
                        pagerState.animateScrollToPage(
                            nextPage, 
                            animationSpec = tween(durationMillis = VibeAnimations.HeroDuration, easing = FastOutSlowInEasing)
                        )
                    }
                } catch (e: Exception) {
                    // Handle potential cancellation or index issues safely
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(450.dp) // Slightly taller for more impact
            .graphicsLayer {
                val scrollOffset = if (scrollState.firstVisibleItemIndex == 0) scrollState.firstVisibleItemScrollOffset else 0
                translationY = scrollOffset * 0.5f // Parallax
                alpha = 1f - (scrollOffset / 800f).coerceIn(0f, 1f)
            }
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val album = albums.getOrNull(page) ?: return@HorizontalPager
            
            Box(modifier = Modifier.fillMaxSize()) {
                // Blurred Background Image (High Res)
                val context = LocalContext.current
                val request = remember(album.coverUrl) {
                    ImageRequest.Builder(context)
                        .data(album.coverUrl)
                        .crossfade(true)
                        .build()
                }
                AsyncImage(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.7f),
                    contentScale = ContentScale.Crop,
                    model = request,
                    contentDescription = null
                )

                // Gradient Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                     MaterialTheme.colorScheme.background.copy(alpha = 0.1f),
                                     MaterialTheme.colorScheme.background.copy(alpha = 0.4f),
                                     MaterialTheme.colorScheme.background
                                )
                            )
                        )
                )

                // Text Content
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(Dimens.ScreenPadding)
                        .padding(bottom = 24.dp)
                ) {
                    Text(
                        text = "Featured Album",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = album.name,
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 40.sp), // Override for hero
                        maxLines = 2,
                        lineHeight = 48.sp,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = album.artist,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { onPlayClick(album) },
                        modifier = Modifier.height(Dimens.ButtonHeight),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("Play Now")
                    }
                }
            }
        }
        
        // Pager Indicator
        Row(
            Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pagerState.pageCount) { iteration ->
                val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(if (pagerState.currentPage == iteration) 10.dp else 8.dp)
                )
            }
        }
    }
}
