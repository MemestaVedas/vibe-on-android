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
import coil.compose.AsyncImage
import coil.request.ImageRequest
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
    val firstTrack = albumTracks.firstOrNull()
    val displayAlbumName = firstTrack?.getDisplayAlbum(displayLanguage) ?: decodedAlbumName
    val coverUrl = firstTrack?.coverUrl
    val artistName = firstTrack?.artist ?: "Unknown Artist"
    val displayArtistName = firstTrack?.getDisplayArtist(displayLanguage) ?: artistName
    
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
                bottom = contentPadding.calculateBottomPadding() + Dimens.SectionSpacing,
                top = 0.dp // Start at top for transparent status bar effect
            )
        ) {
            // Header Space
            item {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Album Art Card with overlays - Edge to edge square
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f) // Square aspect ratio
                            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (coverUrl != null) {
                            val context = LocalContext.current
                            val request = remember(coverUrl) {
                                ImageRequest.Builder(context)
                                    .data(coverUrl)
                                    .crossfade(true)
                                    .build()
                            }
                            AsyncImage(
                                model = request,
                                contentDescription = decodedAlbumName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                        
                        // Gradient overlay for better button visibility
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.3f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.4f)
                                        )
                                    )
                                )
                        )
                        
                        // Back button (top left)
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = 10.dp, top = 20.dp)
                                .size(40.dp)
                                .background(
                                    Color.Black.copy(alpha = 0.3f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                Icons.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        
                        // Play button (bottom right)
                        FloatingActionButton(
                            onClick = {
                                if (albumTracks.isNotEmpty()) {
                                    viewModel.playAlbum(decodedAlbumName)
                                    onTrackSelected(albumTracks.first())
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                                .size(56.dp),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ) {
                            Icon(
                                Icons.Rounded.PlayArrow,
                                contentDescription = "Play",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    
                    // Text content with padding
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimens.ScreenPadding)
                    ) {
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Album Title
                        Text(
                            text = displayAlbumName,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Artist Name (clickable)
                        Text(
                            text = displayArtistName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.bouncyClickable(onClick = {
                                navController.navigate("artist/${java.net.URLEncoder.encode(artistName, "UTF-8")}")
                            })
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Metadata row: Album • Year    Songs • Duration
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Album • ${albumTracks.size} songs",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
            
            // Track List
            itemsIndexed(albumTracks) { index, track ->
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
                
                Box(modifier = Modifier.graphicsLayer { 
                    this.alpha = alpha
                    this.translationY = slide
                }) {
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
        }

        // No top bar needed since back button is on the card
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
