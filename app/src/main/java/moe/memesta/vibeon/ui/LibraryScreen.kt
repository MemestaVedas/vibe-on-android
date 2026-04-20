package moe.memesta.vibeon.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import moe.memesta.vibeon.ui.shapes.*
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
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import coil.request.ImageRequest
import moe.memesta.vibeon.data.SortOption
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
import moe.memesta.vibeon.ui.components.ExpressiveScrollBar

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onBackClick: () -> Unit,
    onTrackSelected: (TrackInfo) -> Unit,
    onNavigateToPlayer: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    BackHandler(onBack = onBackClick)

    val isLoading by viewModel.isLoading.collectAsState()
    val pagedTracks = viewModel.pagedTracks.collectAsLazyPagingItems()
    
    val listState = rememberLazyListState()
    
    val isDragged by listState.interactionSource.collectIsDraggedAsState()
    
    val trackSortOption by viewModel.currentTrackSortOption.collectAsState()
    
    var showSortSheet by remember { mutableStateOf(false) }
    
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            onClick = onBackClick,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", modifier = Modifier.padding(8.dp))
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Text(
                            text = "Library",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    IconButton(
                        onClick = { showSortSheet = true },
                        modifier = Modifier.background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            CircleShape
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                // Content
                if (isLoading && pagedTracks.itemCount == 0) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        VibeContainedLoadingIndicator(label = "Loading your library...")
                    }
                } else if (pagedTracks.loadState.refresh is LoadState.Error && pagedTracks.itemCount == 0) {
                    val error = (pagedTracks.loadState.refresh as LoadState.Error).error
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Unable to load library",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                error.localizedMessage ?: "Unknown paging error",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { pagedTracks.retry() }) {
                                Text("Retry")
                            }
                        }
                    }
                } else if (pagedTracks.itemCount == 0 && pagedTracks.loadState.refresh is LoadState.NotLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No tracks found", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = Dimens.ScreenPadding,
                                end = Dimens.ScreenPadding + 28.dp,
                                top = 8.dp,
                                bottom = contentPadding.calculateBottomPadding() + Dimens.SectionSpacing
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(
                                count = pagedTracks.itemCount,
                                key = { index -> pagedTracks[index]?.path ?: "track_$index" }
                            ) { index ->
                                val track = pagedTracks[index] ?: return@items
                                val prevTrack = if (index > 0) pagedTracks.peek(index - 1) else null
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
                                        viewModel.playTrack(track)
                                        onTrackSelected(track)
                                    }
                                }
                                TrackListItem(
                                    track = track,
                                    onTrackClick = onTrackClick
                                )
                            }

                            if (pagedTracks.loadState.append is LoadState.Loading) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(strokeWidth = 2.dp)
                                    }
                                }
                            }
                        }

                        ExpressiveScrollBar(
                            listState = listState,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 4.dp, top = 12.dp, bottom = contentPadding.calculateBottomPadding() + 12.dp)
                        )
                    }
                }
            }
        }

        if (showSortSheet) {
            moe.memesta.vibeon.ui.components.SortBottomSheet(
                title = "Sort tracks by",
                options = SortOption.TRACKS,
                selectedOption = trackSortOption,
                onDismiss = { showSortSheet = false },
                onOptionSelected = {
                    viewModel.setTrackSortOption(it)
                    showSortSheet = false
                }
            )
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
        shape = RoundedCornerShape(16.dp), // Softened corner radius
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp), // Increased padding
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
                        .clip(RoundedCornerShape(10.dp)), // Softer cover shape
                    contentScale = ContentScale.Crop
                )
            } else if (track.coverUrl != null && !allowImageLoad) {
                // Placeholder
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp))
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
