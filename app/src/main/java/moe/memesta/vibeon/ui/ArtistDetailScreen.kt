package moe.memesta.vibeon.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.navigation.NavController
import moe.memesta.vibeon.data.TrackInfo
import moe.memesta.vibeon.ui.components.AlbumCard
import moe.memesta.vibeon.ui.theme.Dimens
import moe.memesta.vibeon.ui.theme.MotionTokens
import moe.memesta.vibeon.ui.utils.PaletteUtils
import moe.memesta.vibeon.ui.utils.ThemeColors
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import moe.memesta.vibeon.ui.utils.LocalDisplayLanguage
import moe.memesta.vibeon.ui.utils.getDisplayArtist
import moe.memesta.vibeon.ui.utils.getDisplayAlbum

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun ArtistDetailScreen(
    artistName: String,
    viewModel: LibraryViewModel,
    navController: NavController,
    onBackClick: () -> Unit,
    onTrackSelected: (TrackInfo) -> Unit,
    contentPadding: PaddingValues,
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null
) {
    val displayLanguage = LocalDisplayLanguage.current
    val tracks by viewModel.tracks.collectAsState()
    val homeArtists by viewModel.homeArtists.collectAsState()
    
    // Decode URL-encoded artist name
    val decodedArtistName = remember(artistName) {
        try {
            URLDecoder.decode(artistName, StandardCharsets.UTF_8.toString())
        } catch (e: Exception) {
            artistName
        }
    }
    
    val artistTracks = remember(tracks, decodedArtistName) {
        tracks.filter { it.artist == decodedArtistName }
    }
    
    // Get artist photo from HomeArtists list
    val artistInfo = remember(homeArtists, decodedArtistName) {
        homeArtists.find { it.name == decodedArtistName }
    }
    val photoUrl = artistInfo?.photoUrl ?: artistTracks.firstOrNull()?.coverUrl
    val displayArtistName = artistTracks.firstOrNull()?.getDisplayArtist(displayLanguage) ?: decodedArtistName
    
    // Derived Albums
    val artistAlbums = remember(artistTracks, displayLanguage) {
        artistTracks.groupBy { it.album }
            .map { (albumName, groupedTracks) ->
                val first = groupedTracks.firstOrNull()
                ArtistAlbumGroup(
                    albumId = albumName,
                    displayAlbum = first?.getDisplayAlbum(displayLanguage) ?: albumName,
                    coverUrl = first?.coverUrl,
                    tracks = groupedTracks
                )
            }
    }

    val scrollState = rememberLazyListState()
    var titleLayout by remember(displayArtistName) { mutableStateOf<TextLayoutResult?>(null) }
    
    // Dynamic Theming
    var themeColors by remember { mutableStateOf(ThemeColors()) }
    val animatedVibrant by animateColorAsState(
        targetValue = if (themeColors.vibrant != Color.Transparent) themeColors.vibrant else MaterialTheme.colorScheme.primary,
        animationSpec = tween(1000),
        label = "themeVibrant"
    )
    val animatedMuted by animateColorAsState(
        targetValue = if (themeColors.muted != Color.Transparent) themeColors.muted else MaterialTheme.colorScheme.secondary,
        animationSpec = tween(1000),
        label = "themeMuted"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- 1. Immersive Background (Blurred) ---
         if (photoUrl != null) {
            val context = LocalContext.current
            val request = remember(photoUrl) {
                ImageRequest.Builder(context)
                    .data(photoUrl)
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
                    .alpha(0.35f),
                contentScale = ContentScale.Crop
            )
            
            // Solid Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp)
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.65f))
            )
        }

        // --- 2. Content ---
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = contentPadding.calculateBottomPadding() + Dimens.SectionSpacing,
                top = 0.dp
            )
        ) {
            // Header Space
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 100.dp, bottom = 0.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Artist Photo
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .then(
                                if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                                    with(sharedTransitionScope) {
                                        Modifier.sharedElement(
                                            sharedContentState = rememberSharedContentState(key = "artist-${decodedArtistName}"),
                                            animatedVisibilityScope = animatedVisibilityScope
                                        )
                                    }
                                } else Modifier
                            )
                            .clip(RoundedCornerShape(100.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (photoUrl != null) {
                            val context = LocalContext.current
                            val request = remember(photoUrl) {
                                ImageRequest.Builder(context)
                                    .data(photoUrl)
                                    .crossfade(true)
                                    .build()
                            }
                            AsyncImage(
                                model = request,
                                contentDescription = decodedArtistName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = decodedArtistName.take(1).uppercase(),
                                    style = MaterialTheme.typography.displayLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val titleStyle = MaterialTheme.typography.displayLarge
                        val maxLineWidthPx = constraints.maxWidth * 0.88f
                        val measuredLineWidth = titleLayout?.getLineRight(0) ?: 0f
                        val isWideTitle = measuredLineWidth > maxLineWidthPx
                        val dynamicWeight = if (isWideTitle) FontWeight.Medium else FontWeight.Black
                        val dynamicLetterSpacing = if (isWideTitle) (-0.2).sp else (-1.2).sp

                        Text(
                            text = displayArtistName,
                            style = titleStyle,
                            fontWeight = dynamicWeight,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
                            textAlign = TextAlign.Center,
                            letterSpacing = dynamicLetterSpacing,
                            onTextLayout = { titleLayout = it }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Surface(
                        color = animatedVibrant.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, animatedVibrant.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "${artistAlbums.size} Albums • ${artistTracks.size} Tracks",
                            style = MaterialTheme.typography.labelLarge,
                            color = animatedVibrant,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Buttons
                     Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                         Button(
                            onClick = {
                                if (artistTracks.isNotEmpty()) {
                                    viewModel.playArtist(decodedArtistName)
                                    onTrackSelected(artistTracks.first())
                                }
                            },
                             contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Play")
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                         OutlinedButton(
                            onClick = {
                                 if (artistTracks.isNotEmpty()) {
                                    viewModel.playTrack(artistTracks.random(), artistTracks)
                                    onTrackSelected(artistTracks.first())
                                }
                            }
                        ) {
                             Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(20.dp))
                             Spacer(modifier = Modifier.width(8.dp))
                             Text("Shuffle")
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // Albums Zone (top)
            item {
                Text(
                    text = "Albums",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = Dimens.ScreenPadding)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            itemsIndexed(artistAlbums.chunked(3)) { rowIndex, albumChunk ->
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
                ) {
                    items(albumChunk) { album ->
                        AlbumCard(
                            albumName = album.displayAlbum,
                            coverUrl = album.coverUrl,
                            onClick = {
                                navController.navigate("album/${java.net.URLEncoder.encode(album.albumId, "UTF-8")}")
                            },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    }
                }

                if (rowIndex == 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Albums -> Songs handoff
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        ),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Text(
                        text = "Songs",
                        style = MaterialTheme.typography.titleLarge,
                        color = animatedMuted,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = 8.dp)
                    )
                }
            }

            // Songs Zone (bottom), grouped subtly by album
            artistAlbums.forEach { album ->
                item(key = "album-header-${album.albumId}") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimens.ScreenPadding, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = album.displayAlbum,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)
                        )
                    }
                }

                itemsIndexed(
                    items = album.tracks,
                    key = { _, track -> track.path }
                ) { index, track ->
                    val alpha by animateFloatAsState(
                        targetValue = 1f,
                        animationSpec = MotionTokens.staggeredEffects(index),
                        label = "itemAlpha"
                    )
                    val slide by animateFloatAsState(
                        targetValue = 0f,
                        animationSpec = MotionTokens.staggeredEffects(index),
                        label = "itemSlide"
                    )
                    
                    Box(modifier = Modifier.graphicsLayer { 
                        this.alpha = alpha
                        this.translationY = slide
                    }) {
                        AlbumTrackRow(
                            index = track.trackNumber ?: (index + 1),
                            track = track,
                            onClick = {
                                viewModel.playTrack(track, artistTracks)
                                onTrackSelected(track)
                            }
                        )
                    }
                }
            }
        }
        
        // --- 3. Glassmorphic Top Bar ---
            val showTitle by remember {
                derivedStateOf { scrollState.firstVisibleItemIndex > 0 || scrollState.firstVisibleItemScrollOffset > 300 }
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
                        if (showTitle) Modifier.background(MaterialTheme.colorScheme.surface)
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
                            displayArtistName, 
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

private data class ArtistAlbumGroup(
    val albumId: String,
    val displayAlbum: String,
    val coverUrl: String?,
    val tracks: List<TrackInfo>
)

