package moe.memesta.vibeon.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Sort
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
import androidx.navigation.NavController
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import moe.memesta.vibeon.data.TrackInfo
import moe.memesta.vibeon.ui.theme.Dimens
import moe.memesta.vibeon.ui.theme.VibeAnimations
import moe.memesta.vibeon.ui.theme.bouncyClickable
import moe.memesta.vibeon.ui.utils.PaletteUtils
import moe.memesta.vibeon.ui.utils.ThemeColors
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import moe.memesta.vibeon.ui.utils.LocalDisplayLanguage
import moe.memesta.vibeon.ui.utils.getDisplayName
import moe.memesta.vibeon.ui.utils.getDisplayArtist
import moe.memesta.vibeon.ui.utils.getDisplayAlbum
import moe.memesta.vibeon.ui.utils.parseAlbum
import moe.memesta.vibeon.ui.components.SectionHeader
import moe.memesta.vibeon.data.SortOption
import moe.memesta.vibeon.ui.components.SortBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    albumName: String,
    viewModel: LibraryViewModel,
    navController: NavController,
    onBackClick: () -> Unit,
    onTrackSelected: (TrackInfo) -> Unit,
    contentPadding: PaddingValues
) {
    val displayLanguage = LocalDisplayLanguage.current
    val tracks by viewModel.tracks.collectAsState()
    val currentTrackSortOption by viewModel.currentTrackSortOption.collectAsState()
    var showSortSheet by remember { mutableStateOf(false) }
    
    // Decode URL-encoded album name
    val decodedAlbumName = remember(albumName) {
        try {
            URLDecoder.decode(albumName, StandardCharsets.UTF_8.toString())
        } catch (e: Exception) {
            albumName
        }
    }
    
    val albumTracks = remember(tracks, decodedAlbumName, currentTrackSortOption) {
        val filtered = tracks.filter { it.album == decodedAlbumName }
        
        // Apply sorting based on selected option
        when (currentTrackSortOption) {
            SortOption.TrackTitleAZ -> filtered.sortedBy { it.title.lowercase() }
            SortOption.TrackTitleZA -> filtered.sortedByDescending { it.title.lowercase() }
            SortOption.TrackDurationAsc -> filtered.sortedWith(compareBy { it.duration ?: 0L })
            SortOption.TrackDurationDesc -> filtered.sortedWith(compareByDescending { it.duration ?: 0L })
            else -> {
                // Default: sort by disc and track number
                filtered.sortedWith(
                    compareBy<TrackInfo> { it.discNumber ?: 1 }
                        .thenBy { it.trackNumber ?: 0 }
                )
            }
        }
    }
    
    val scrollState = rememberLazyListState()
    val firstTrack = albumTracks.firstOrNull()
    val displayAlbumName = firstTrack?.getDisplayAlbum(displayLanguage) ?: decodedAlbumName
    val coverUrl = firstTrack?.coverUrl
    val artistName = firstTrack?.artist ?: "Unknown Artist"
    val displayArtistName = firstTrack?.getDisplayArtist(displayLanguage) ?: artistName
    
    // Get context at composable level
    val context = LocalContext.current
    
    // Dynamic Theming
    var themeColors by remember { mutableStateOf(ThemeColors()) }
    val animatedVibrant by animateColorAsState(
        targetValue = if (themeColors.vibrant != Color.Transparent) themeColors.vibrant else MaterialTheme.colorScheme.primary,
        animationSpec = tween(1000),
        label = "albumVibrant"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- Content ---
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = contentPadding.calculateBottomPadding() + Dimens.SectionSpacing
            )
        ) {
            item {
                // Dynamic Header Box - Full immersive album art with overlay text
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(520.dp) // Large immersive header
                        .clip(RoundedCornerShape(bottomStart = 56.dp, bottomEnd = 56.dp))
                ) {
                    // Extract colors on the fly
                    LaunchedEffect(coverUrl) {
                        if (coverUrl != null) {
                            val loader = ImageLoader(context)
                            val request = ImageRequest.Builder(context)
                                .data(coverUrl)
                                .allowHardware(false)
                                .build()
                            val result = loader.execute(request)
                            if (result is SuccessResult) {
                                themeColors = PaletteUtils.extractColors(result.drawable)
                            }
                        }
                    }

                    // Background Album Art - Fullscreen
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(animatedVibrant.copy(alpha = 0.9f))
                    ) {
                        if (coverUrl != null) {
                            AsyncImage(
                                model = coverUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Rounded.PlayArrow, // Fallback
                                contentDescription = null,
                                modifier = Modifier
                                    .size(120.dp)
                                    .align(Alignment.Center),
                                tint = Color.White.copy(alpha = 0.2f)
                            )
                        }
                        
                        // Dark gradient overlay at bottom for text readability
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .align(Alignment.BottomCenter)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)
                                        ),
                                        startY = 100f
                                    )
                                )
                        )
                    }

                    // Back Button and Sort
                    Row(
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), CircleShape)
                        ) {
                            Icon(
                                Icons.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        IconButton(
                            onClick = { showSortSheet = true },
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                Icons.Rounded.Sort,
                                contentDescription = "Sort",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Overlay Content: Title, Artist and Play Button - at bottom
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayAlbumName,
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = displayArtistName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                modifier = Modifier.bouncyClickable(onClick = {
                                    navController.navigate("artist/${java.net.URLEncoder.encode(artistName, "UTF-8")}")
                                })
                            )
                        }

                        // Play and Shuffle Buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Shuffle Button - Expressive Shape
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .bouncyClickable {
                                        if (albumTracks.isNotEmpty()) {
                                            // Shuffle the album tracks and play
                                            val shuffledTracks = albumTracks.shuffled()
                                            viewModel.playTrack(shuffledTracks.first(), shuffledTracks)
                                            onTrackSelected(shuffledTracks.first())
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            
                            // Play Button - Expressive Shape
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .bouncyClickable {
                                        if (albumTracks.isNotEmpty()) {
                                            viewModel.playAlbum(decodedAlbumName)
                                            onTrackSelected(albumTracks.first())
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.PlayArrow,
                                    contentDescription = "Play All",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }

                // Metadata Divider Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Album • 2024",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    val totalDuration = albumTracks.sumOf { (it.duration ?: 0).toLong() } / 1000
                    val mins = totalDuration / 60
                    Text(
                        text = "${albumTracks.size} songs • ${mins} min",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            
            // Track List
            itemsIndexed(albumTracks) { index, track ->
                val prevTrack = if (index > 0) albumTracks[index - 1] else null
                val currentDisc = track.discNumber ?: 1
                val prevDisc = prevTrack?.discNumber ?: 1
                val showDiscSeparator = index > 0 && currentDisc != prevDisc

                val alpha by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 400, delayMillis = (index * 50).coerceAtMost(500)),
                    label = "itemAlpha"
                )
                val slide by animateFloatAsState(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 400, delayMillis = (index * 50).coerceAtMost(500)),
                    label = "itemSlide"
                )

                Column {
                    if (showDiscSeparator) {
                        Text(
                            text = "Disc $currentDisc",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = 12.dp)
                        )
                    }
                    
                    Box(modifier = Modifier.graphicsLayer { 
                        this.alpha = alpha
                        this.translationY = slide
                    }) {
                        AlbumTrackRow(
                            index = track.trackNumber ?: (index + 1),
                            track = track,
                            onClick = {
                                viewModel.playTrack(track, albumTracks)
                                onTrackSelected(track)
                            }
                        )
                    }
                }
            }
        }

        // No top bar needed since back button is on the card
    }
    
    // Sort Bottom Sheet
    if (showSortSheet) {
        SortBottomSheet(
            title = "Sort by",
            options = SortOption.TRACKS,
            selectedOption = currentTrackSortOption,
            onDismiss = { showSortSheet = false },
            onOptionSelected = { option ->
                viewModel.setTrackSortOption(option)
                showSortSheet = false
            }
        )
    }
}

@Composable
fun AlbumTrackRow(
    index: Int,
    track: TrackInfo,
    onClick: () -> Unit
) {
    val displayLanguage = LocalDisplayLanguage.current
    val title = track.getDisplayName(displayLanguage)
    val artist = track.getDisplayArtist(displayLanguage)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClickable(onClick = onClick, scaleDown = 0.98f)
            .padding(vertical = 12.dp, horizontal = Dimens.ScreenPadding),
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
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Artist name is redundant if it's the same as album artist, but sometimes it differs (compilations)
            // Keeping it subtle
            Text(
                text = artist,
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
