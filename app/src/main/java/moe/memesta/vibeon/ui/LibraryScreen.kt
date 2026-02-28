package moe.memesta.vibeon.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import moe.memesta.vibeon.ui.theme.SongCoverShape
import androidx.compose.ui.Alignment
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import moe.memesta.vibeon.data.TrackInfo
import moe.memesta.vibeon.ui.components.VibeContainedLoadingIndicator
import moe.memesta.vibeon.ui.theme.Dimens
import moe.memesta.vibeon.ui.theme.bouncyClickable
import moe.memesta.vibeon.ui.utils.getDisplayName
import moe.memesta.vibeon.ui.utils.getDisplayArtist
import moe.memesta.vibeon.ui.utils.getDisplayAlbum
import moe.memesta.vibeon.ui.utils.parseAlbum
import moe.memesta.vibeon.ui.components.WavySeparator
import moe.memesta.vibeon.ui.components.SectionHeader

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onBackClick: () -> Unit,
    onTrackSelected: (TrackInfo) -> Unit,
    onNavigateToPlayer: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    BackHandler(onBack = onBackClick)

    val tracks by viewModel.tracks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    val listState = rememberLazyListState()
    
    val isDragged by listState.interactionSource.collectIsDraggedAsState()
    
    // Derived Data
    val displayedTracks = remember(tracks, searchQuery) {
        val filtered = if (searchQuery.isNotEmpty()) {
            tracks.filter { 
                it.title.contains(searchQuery, ignoreCase = true) || 
                it.artist.contains(searchQuery, ignoreCase = true) 
            }
        } else {
            tracks
        }
        
        // Sort: Album (Base) -> Disc -> Track
        filtered.sortedWith(
            compareBy<TrackInfo> { parseAlbum(it.album, it.discNumber).baseName }
                .thenBy { it.discNumber ?: 0 }
                .thenBy { it.trackNumber ?: 0 }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.ScreenPadding, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                            .bouncyClickable(onClick = onBackClick)
                            .padding(8.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Text(
                        text = "Library",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                // Content
                if (isLoading && tracks.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        VibeContainedLoadingIndicator(label = "Loading your library...")
                    }
                } else if (tracks.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No tracks found", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = PaddingValues(
                            start = Dimens.ScreenPadding, 
                            end = Dimens.ScreenPadding, 
                            top = 8.dp, 
                            bottom = contentPadding.calculateBottomPadding() + Dimens.SectionSpacing
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(displayedTracks) { index, track ->
                            val prevTrack = if (index > 0) displayedTracks[index - 1] else null
                            val currentAlbum = remember(track.album, track.discNumber) { 
                                parseAlbum(track.album, track.discNumber) 
                            }
                            val prevAlbum = remember(prevTrack?.album, prevTrack?.discNumber) { 
                                prevTrack?.let { parseAlbum(it.album, it.discNumber) } 
                            }

                            val showAlbumSeparator = prevAlbum == null || currentAlbum.baseName != prevAlbum.baseName
                            val showDiscSeparator = !showAlbumSeparator && currentAlbum.discNumber != prevAlbum?.discNumber

                            if (showAlbumSeparator) {
                                WavySeparator(
                                    colorTop = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    colorBottom = Color.Transparent,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                                SectionHeader(
                                    title = currentAlbum.baseName,
                                    modifier = Modifier.padding(bottom = 8.dp, start = 0.dp)
                                )
                            }
                            
                            if (showDiscSeparator) {
                                Text(
                                    text = "Disc ${currentAlbum.discNumber}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
                                )
                            }

                            val onTrackClick = remember(track) {
                                {
                                    viewModel.playTrack(track, displayedTracks)
                                    onTrackSelected(track)
                                }
                            }
                            TrackListItem(
                                track = track,
                                onTrackClick = onTrackClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistListItem(artist: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = artist,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TrackListItem(
    track: TrackInfo,
    onTrackClick: () -> Unit,
    allowImageLoad: Boolean = true
) {
    val displayLanguage = moe.memesta.vibeon.ui.utils.LocalDisplayLanguage.current
    val title = track.getDisplayName(displayLanguage)
    val artist = track.getDisplayArtist(displayLanguage)
    val album = track.getDisplayAlbum(displayLanguage)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClickable(onClick = onTrackClick, scaleDown = 0.98f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(Dimens.CornerRadiusLarge),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art
            if (track.coverUrl != null && allowImageLoad) {
                val context = LocalContext.current
                val request = remember(track.coverUrl) {
                    ImageRequest.Builder(context)
                        .data(track.coverUrl)
                        .crossfade(true)
                        .build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = title,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(SongCoverShape),
                    contentScale = ContentScale.Crop
                )
            } else if (track.coverUrl != null && !allowImageLoad) {
                // Placeholder
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(SongCoverShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(SongCoverShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Track Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "$artist • $album",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Play Button (Visual only, distinct touch target removed)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = null, // decorative
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
