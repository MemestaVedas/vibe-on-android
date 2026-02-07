@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package moe.memesta.vibeon.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import moe.memesta.vibeon.data.TrackInfo
import moe.memesta.vibeon.ui.theme.VibeAnimations
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    albumName: String,
    viewModel: LibraryViewModel,
    onBackClick: () -> Unit,
    onTrackSelected: (TrackInfo) -> Unit,
    contentPadding: PaddingValues
) {
    val tracks by viewModel.tracks.collectAsState()
    
    // Decode URL-encoded album name
    val decodedAlbumName = remember(albumName) {
        try {
            URLDecoder.decode(albumName, StandardCharsets.UTF_8.toString())
        } catch (e: Exception) {
            albumName
        }
    }
    
    val albumTracks = remember(tracks, decodedAlbumName) {
        tracks.filter { it.album == decodedAlbumName }
    }
    
    val scrollState = rememberLazyListState()
    val coverUrl = albumTracks.firstOrNull()?.coverUrl
    val artistName = albumTracks.firstOrNull()?.artist ?: "Unknown Artist"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- 1. Immersive Background (Blurred) ---
        if (coverUrl != null) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .alpha(0.4f)
                    .blur(50.dp),
                contentScale = ContentScale.Crop
            )
            
            // Gradient Overlay to fade into background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background.copy(alpha = 0.2f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )
        }

        // --- 2. Content ---
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
                top = 0.dp // Start at top for transparent status bar effect
            )
        ) {
            // Header Space
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 100.dp, bottom = 24.dp), // Space for status bar + top content
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Album Art
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .shadow(24.dp, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (coverUrl != null) {
                            AsyncImage(
                                model = coverUrl,
                                contentDescription = decodedAlbumName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow, // Fallback icon
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Text Info
                    Text(
                        text = decodedAlbumName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 24.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = artistName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { 
                            // TODO: Navigate to artist
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Album • ${albumTracks.size} Songs",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Action Buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Shuffle Button
                        OutlinedButton(
                            onClick = { 
                                // TODO: Shuffle play 
                                if (albumTracks.isNotEmpty()) {
                                    viewModel.playTrack(albumTracks.random(), albumTracks)
                                    onTrackSelected(albumTracks.first())
                                }
                            },
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Shuffle")
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        // Play Button
                        Button(
                            onClick = {
                                if (albumTracks.isNotEmpty()) {
                                    viewModel.playAlbum(decodedAlbumName)
                                    onTrackSelected(albumTracks.first())
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Play")
                        }
                    }
                }
            }
            
            // Track List
            itemsIndexed(albumTracks) { index, track ->
                AlbumTrackRow(
                    index = index + 1,
                    track = track,
                    onClick = {
                        viewModel.playTrack(track, albumTracks)
                        onTrackSelected(track)
                    }
                )
            }
        }

        // --- 3. Custom Top Bar (Collapsing style logic visual only) ---
        // This stays fixed at top
        val showTitle by remember {
            derivedStateOf { scrollState.firstVisibleItemIndex > 0 || scrollState.firstVisibleItemScrollOffset > 600 }
        }
        
        SmallTopAppBar(
            title = {
                AnimatedVisibility(
                    visible = showTitle,
                    enter = fadeIn() + slideInVertically { 20 },
                    exit = fadeOut()
                ) {
                    Text(decodedAlbumName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.Default.ArrowBack, 
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            colors = TopAppBarDefaults.smallTopAppBarColors(
                containerColor = if (showTitle) MaterialTheme.colorScheme.background.copy(alpha = 0.95f) else Color.Transparent,
                scrolledContainerColor = MaterialTheme.colorScheme.background
            ),
             // Removed statusBarsPadding to allow background to fill the status bar area.
             // TopAppBar handles content insets automatically or we rely on the transparent container.
             modifier = Modifier
        )
    }
}

@Composable
fun AlbumTrackRow(
    index: Int,
    track: TrackInfo,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Index
        Text(
            text = index.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(32.dp)
        )
        
        // Title & Artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Artist name is redundant if it's the same as album artist, but sometimes it differs (compilations)
            // Keeping it subtle
            Text(
                text = track.artist,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Simple More Option (Visual for now)
        Icon(
            Icons.Rounded.MoreVert,
            contentDescription = "More",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp).alpha(0.6f)
        )
    }
}
