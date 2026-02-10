package moe.memesta.vibeon.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.*
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import moe.memesta.vibeon.data.TrackInfo
import moe.memesta.vibeon.ui.components.AlbumCard
import moe.memesta.vibeon.ui.theme.Dimens
import moe.memesta.vibeon.ui.utils.PaletteUtils
import moe.memesta.vibeon.ui.utils.ThemeColors
import androidx.navigation.NavController
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ArtistDetailScreen(
    artistName: String,
    viewModel: LibraryViewModel,
    navController: NavController,
    onBackClick: () -> Unit,
    onTrackSelected: (TrackInfo) -> Unit,
    contentPadding: PaddingValues
) {
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
    
    // Derived Albums
    val artistAlbums = remember(artistTracks) {
        artistTracks.groupBy { it.album }
            .map { (albumName, tracks) ->
                Pair(albumName, tracks.firstOrNull()?.coverUrl)
            }
            .distinctBy { it.first }
    }

    val scrollState = rememberLazyListState()
    var selectedTab by remember { mutableStateOf(0) } // 0: Songs, 1: Albums
    
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
                    .alpha(0.35f)
                    .blur(60.dp),
                contentScale = ContentScale.Crop
            )
            
            // Gradient Overlay
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
                            .shadow(24.dp, RoundedCornerShape(100.dp)) // Circle
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
                    
                    Text(
                        text = decodedArtistName,
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = Dimens.ScreenPadding),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        letterSpacing = (-1.5).sp
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Surface(
                        color = animatedVibrant.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, animatedVibrant.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "Artist • ${artistTracks.size} Tracks",
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
            
            // Sticky Tab Row
            stickyHeader {
                val isAtTop by remember {
                    derivedStateOf { 
                        scrollState.firstVisibleItemIndex > 0 || scrollState.firstVisibleItemScrollOffset > 400 
                    }
                }
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isAtTop) MaterialTheme.colorScheme.background.copy(alpha = 0.85f) else Color.Transparent,
                    tonalElevation = if (isAtTop) 4.dp else 0.dp
                ) {
                    Box(modifier = Modifier.then(
                        if (isAtTop) Modifier.blur(20.dp) else Modifier
                    )) {
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.primary,
                            indicator = { tabPositions ->
                                TabRowDefaults.Indicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = animatedVibrant,
                                    height = 3.dp
                                )
                            },
                            divider = {
                                HorizontalDivider(color = animatedVibrant.copy(alpha = 0.2f))
                            }
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { 
                                    Text(
                                        "Songs", 
                                        fontWeight = if(selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                        color = if(selectedTab == 0) animatedVibrant else MaterialTheme.colorScheme.onSurfaceVariant
                                    ) 
                                }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { 
                                    Text(
                                        "Albums", 
                                        fontWeight = if(selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                        color = if(selectedTab == 1) animatedVibrant else MaterialTheme.colorScheme.onSurfaceVariant
                                    ) 
                                }
                            )
                        }
                    }
                }
            }
            
            // Content based on tab
            if (selectedTab == 0) {
                 itemsIndexed(artistTracks) { index, track ->
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
                                viewModel.playTrack(track, artistTracks)
                                onTrackSelected(track)
                            }
                        )
                    }
                }
            } else {
                 item {
                     Spacer(modifier = Modifier.height(16.dp))
                     
                     // Grid for Albums
                     Column(modifier = Modifier.padding(Dimens.ScreenPadding)) {
                         val chunkedAlbums = artistAlbums.chunked(2) // 2 columns
                         chunkedAlbums.forEach { rowAlbums ->
                             Row(
                                 modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                 horizontalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
                             ) {
                                 rowAlbums.forEach { (albumName, cover) ->
                                     AlbumCard(
                                         albumName = albumName,
                                         coverUrl = cover,
                                         onClick = { 
                                             navController.navigate("album/${java.net.URLEncoder.encode(albumName, "UTF-8")}")
                                         },
                                         modifier = Modifier.weight(1f)
                                     )
                                 }
                                 if (rowAlbums.size < 2) {
                                     Spacer(modifier = Modifier.weight(1f))
                                 }
                             }
                         }
                     }
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
                        if (showTitle) Modifier.blur(20.dp).background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f))
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
                            decodedArtistName, 
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

