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
    val coverUrl = firstTrack?.coverUrl
    val artistName = firstTrack?.artist ?: "Unknown Artist"
    
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
        // --- 1. Immersive Background (Blurred) ---
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
                contentDescription = null,
                onSuccess = { result ->
                    themeColors = PaletteUtils.extractColors(result.result.drawable)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp)
                    .alpha(0.35f)
                    .blur(60.dp),
                contentScale = ContentScale.Crop
            )
            
            // Gradient Overlay to fade into background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background.copy(alpha = 0.1f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
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
                bottom = contentPadding.calculateBottomPadding() + Dimens.SectionSpacing,
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
                            .size(260.dp)
                            .shadow(32.dp, RoundedCornerShape(Dimens.CornerRadiusLarge))
                            .clip(RoundedCornerShape(Dimens.CornerRadiusLarge))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            // Basic clickable for now, maybe expand later
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
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.PlayArrow, // Fallback icon
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
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        letterSpacing = (-1).sp
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = artistName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = animatedVibrant,
                        modifier = Modifier.bouncyClickable(onClick = { 
                            navController.navigate("artist/${java.net.URLEncoder.encode(artistName, "UTF-8")}")
                        })
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Surface(
                        color = animatedVibrant.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, animatedVibrant.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "Album • ${albumTracks.size} Songs",
                            style = MaterialTheme.typography.labelMedium,
                            color = animatedVibrant,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                    
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
                                if (albumTracks.isNotEmpty()) {
                                    viewModel.playTrack(albumTracks.random(), albumTracks)
                                    onTrackSelected(albumTracks.first())
                                }
                            },
                        ) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
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
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Play")
                        }
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

        // --- 3. Glassmorphic Top Bar ---
        val showTitle by remember {
            derivedStateOf { scrollState.firstVisibleItemIndex > 0 || scrollState.firstVisibleItemScrollOffset > 400 }
        }
        
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .then(
                        if (showTitle) Modifier
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f))
                            .blur(25.dp)
                        else Modifier
                    ),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    
                    AnimatedVisibility(
                        visible = showTitle,
                        enter = fadeIn() + slideInVertically { 20 },
                        exit = fadeOut()
                    ) {
                        Text(
                            decodedAlbumName, 
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1, 
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
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
