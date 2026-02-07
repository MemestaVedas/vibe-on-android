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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
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
    contentPadding: PaddingValues,
    connectionViewModel: ConnectionViewModel
) {
    val tracks: List<TrackInfo> by viewModel.tracks.collectAsState()
    val connectionState by connectionViewModel.connectionState.collectAsState()
    
    // Skeleton / Loading State
    val isLoading = tracks.isEmpty() && connectionState == moe.memesta.vibeon.ui.ConnectionState.CONNECTED

    // Derived Data
    val albums: List<AlbumInfo> = remember(tracks) {
        tracks.groupBy { it.album }
            .map { (album, albumTracks) ->
                AlbumInfo(
                    name = album,
                    artist = albumTracks.firstOrNull()?.artist ?: "",
                    coverUrl = albumTracks.firstOrNull()?.coverUrl
                )
            }
            .sortedBy { it.name }
    }
    
    val artists: List<ArtistItemData> = remember(tracks) {
        tracks.groupBy { it.artist }
            .map { (artist, artistTracks) ->
                ArtistItemData(
                    name = artist,
                    followerCount = "${artistTracks.size} Tracks",
                    photoUrl = artistTracks.firstOrNull()?.coverUrl
                )
            }
            .sortedBy { it.name }
    }
    
    // Hero Header Data (Random Album)
    val featuredAlbum = remember(albums) { albums.shuffled().firstOrNull() }
    
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
            item {
                HeroHeader(
                    album = featuredAlbum,
                    onPlayClick = { 
                        featuredAlbum?.let { 
                            // TODO: Play album
                            onAlbumSelected(it.name) 
                        } 
                    },
                    scrollState = listState
                )
            }

            // Section: Recently Added
            item {
                AnimatedSection(visible = !isLoading, delayMillis = 100) {
                    Column {
                        SectionHeader("Recently Added")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(tracks.take(10)) { track ->
                                moe.memesta.vibeon.ui.components.SquareTrackCard(
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
            item {
                AnimatedSection(visible = !isLoading, delayMillis = 200) {
                    Column {
                        SectionHeader("Albums")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(albums) { album ->
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
                            items(5) { moe.memesta.vibeon.ui.components.SkeletonAlbumCard() }
                        }
                    }
                }
            }

            // Section: Artists
            item {
                 AnimatedSection(visible = !isLoading, delayMillis = 300) {
                    Column {
                        SectionHeader("Artists")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(artists) { artist ->
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
                            items(5) { moe.memesta.vibeon.ui.components.SkeletonArtistPill() }
                        }
                    }
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
    album: AlbumInfo?,
    onPlayClick: () -> Unit,
    scrollState: androidx.compose.foundation.lazy.LazyListState
) {
    if (album == null) return

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
        // blurred background
        AsyncImage(
            model = album.coverUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.6f),
            contentScale = ContentScale.Crop
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
            Button(onClick = onPlayClick) {
                Text("Play Now")
            }
        }
    }
}

@Composable
fun AnimatedSection(
    visible: Boolean,
    delayMillis: Int,
    content: @Composable () -> Unit
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.fadeIn(
            animationSpec = androidx.compose.animation.core.tween(durationMillis = 500, delayMillis = delayMillis)
        ) + androidx.compose.animation.slideInVertically(
            initialOffsetY = { 50 },
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessLow
            )
        )
    ) {
        content()
    }
}
