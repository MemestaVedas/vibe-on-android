package moe.memesta.vibeon.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import moe.memesta.vibeon.data.AlbumInfo
import moe.memesta.vibeon.data.ArtistItemData
import moe.memesta.vibeon.data.TrackInfo
import moe.memesta.vibeon.ui.LibraryViewModel
import moe.memesta.vibeon.ui.components.SectionHeader
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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
    
    // Skeleton / Loading State
    val isLoading = tracks.isEmpty() && connectionState == moe.memesta.vibeon.ui.ConnectionState.CONNECTED
    
    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = contentPadding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Hero Header
            item(key = "hero_header") {
                HeroHeader(
                    albums = featuredAlbums,
                    onPlayClick = { album ->
                        // TODO: Play album
                        onAlbumSelected(album.name) 
                    },
                    scrollState = listState
                )
            }

            // Section: Recently Added
            item(key = "section_recent") {
                AnimatedSection(visible = !isLoading, delayMillis = 100) {
                    Column {
                        SectionHeader(
                            title = "Recently Added",
                            onSeeAllClick = onViewAllSongs
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp) // Spacing between columns
                        ) {
                            val recentTracks = tracks.take(20) // Limit to 20 items
                            val chunks = recentTracks.chunked(4) // 4 items per column
                            
                            items(items = chunks, key = { it.firstOrNull()?.path ?: "" }) { columnTracks ->
                                Column(
                                    modifier = Modifier.width(300.dp), // Fixed width for columns to ensure cards have space
                                    verticalArrangement = Arrangement.spacedBy(8.dp) // Spacing between rows
                                ) {
                                    columnTracks.forEach { track ->
                                        moe.memesta.vibeon.ui.components.GridTrackCard(
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
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(5) { moe.memesta.vibeon.ui.components.SkeletonSquareCard() }
                        }
                    }
                }
            }

            // Section: Albums
            item(key = "section_albums") {
                AnimatedSection(visible = !isLoading, delayMillis = 200) {
                    Column {
                        SectionHeader("Albums")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = albums,
                                key = { it.name }
                            ) { album ->
                                moe.memesta.vibeon.ui.components.AlbumCard(
                                    albumName = album.name,
                                    coverUrl = album.coverUrl,
                                    onClick = { onAlbumSelected(album.name) }
                                )
                            }
                        }
                    }
                }
                 if (isLoading) {
                    Column {
                        SectionHeader("Albums")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(5, key = { it }) { moe.memesta.vibeon.ui.components.SkeletonAlbumCard() }
                        }
                    }
                }
            }

            // Section: Artists
            item(key = "section_artists") {
                 AnimatedSection(visible = !isLoading, delayMillis = 300) {
                    Column {
                        SectionHeader("Artists")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = artists,
                                key = { it.name }
                            ) { artist ->
                                moe.memesta.vibeon.ui.components.ArtistPill(
                                    artistName = artist.name,
                                    photoUrl = artist.photoUrl,
                                    onClick = { onArtistSelected(artist.name) }
                                )
                            }
                        }
                    }
                }
                if (isLoading) {
                    Column {
                        SectionHeader("Artists")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(5, key = { it }) { moe.memesta.vibeon.ui.components.SkeletonArtistPill() }
                        }
                    }
                }
            }
            
            // Section: Statistics
            item(key = "section_stats") {
                val stats by viewModel.stats.collectAsState()
                AnimatedSection(visible = !isLoading, delayMillis = 400) {
                    moe.memesta.vibeon.ui.components.StatisticsSection(
                        stats = stats
                    )
                }
            }
        }
        
        // Connection Status Indicator (Top-Right)
        moe.memesta.vibeon.ui.components.ConnectionStatusIndicator(
            connectionState = connectionState,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 24.dp) // Adjusted top padding for Hero
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

    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { albums.size })
    
    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
    
    // Auto-scroll (Pauses when user creates interaction)
    LaunchedEffect(pagerState, isDragged) {
        if (!isDragged && albums.isNotEmpty()) {
            while(true) {
                kotlinx.coroutines.delay(7000)
                val nextPage = (pagerState.currentPage + 1) % albums.size
                pagerState.animateScrollToPage(
                    nextPage, 
                    animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .graphicsLayer {
                val scrollOffset = if (scrollState.firstVisibleItemIndex == 0) scrollState.firstVisibleItemScrollOffset else 0
                translationY = scrollOffset * 0.5f
                alpha = 1f - (scrollOffset / 600f).coerceIn(0f, 1f)
            }
    ) {
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val album = albums.getOrNull(page) ?: return@HorizontalPager
            
            Box(modifier = Modifier.fillMaxSize()) {
                // blurred background
                AsyncImage(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.6f),
                    contentScale = ContentScale.Crop,
                    model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data(album.coverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null
                )

                // Gradient Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                     MaterialTheme.colorScheme.background.copy(alpha = 0.2f),
                                     MaterialTheme.colorScheme.background
                                )
                            )
                        )
                )

                // Content
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Featured Album",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = album.name,
                        style = MaterialTheme.typography.headlineLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = album.artist,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { onPlayClick(album) }) {
                        Text("Play Now")
                    }
                }
            }
        }
        
        // Pager Indicator (Simple dots)
        Row(
            Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pagerState.pageCount) { iteration ->
                val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(8.dp)
                )
            }
        }
    }
}

@Composable
fun AnimatedSection(
    visible: Boolean,
    delayMillis: Int, // Keep parameter for compatibility but ignore it
    content: @Composable () -> Unit
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.fadeIn(
            animationSpec = androidx.compose.animation.core.tween(durationMillis = 200)
        )
    ) {
        content()
    }
}
