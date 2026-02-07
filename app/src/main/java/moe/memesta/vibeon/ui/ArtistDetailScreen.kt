@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package moe.memesta.vibeon.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import moe.memesta.vibeon.data.TrackInfo
import moe.memesta.vibeon.ui.components.AlbumCard
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    artistName: String,
    viewModel: LibraryViewModel,
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- 1. Immersive Background (Blurred) ---
         if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .alpha(0.4f)
                    .blur(50.dp),
                contentScale = ContentScale.Crop
            )
            
            // Gradient Overlay
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
                            AsyncImage(
                                model = photoUrl,
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
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 24.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Artist • ${artistTracks.size} Tracks",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
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
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
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
                             Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(20.dp))
                             Spacer(modifier = Modifier.width(8.dp))
                             Text("Shuffle")
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Tabs
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        indicator = { tabPositions ->
                            TabRowDefaults.Indicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = MaterialTheme.colorScheme.primary,
                                height = 3.dp
                            )
                        },
                        divider = {
                             HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Songs", fontWeight = if(selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Albums", fontWeight = if(selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }
            
            // Content based on tab
            if (selectedTab == 0) {
                 itemsIndexed(artistTracks) { index, track ->
                    AlbumTrackRow(
                        index = index + 1,
                        track = track,
                        onClick = {
                            viewModel.playTrack(track, artistTracks)
                            onTrackSelected(track)
                        }
                    )
                }
            } else {
                 item {
                     Spacer(modifier = Modifier.height(16.dp))
                     
                     // Grid for Albums (using FlowRow or similar manually in Column since we are in LazyColumn)
                     // Or just a vertical list of rows for better scrolling
                     // Given user requested "reuse same layout", I will use a Grid adapter here
                     
                     Column(modifier = Modifier.padding(16.dp)) {
                         val chunkedAlbums = artistAlbums.chunked(2) // 2 columns
                         chunkedAlbums.forEach { rowAlbums ->
                             Row(
                                 modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                 horizontalArrangement = Arrangement.spacedBy(16.dp)
                             ) {
                                 rowAlbums.forEach { (albumName, cover) ->
                                     AlbumCard(
                                         albumName = albumName,
                                         coverUrl = cover,
                                         onClick = { viewModel.playAlbum(albumName) }, // TODO: Navigate to detail
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
        
        // --- 3. Custom Top Bar ---
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
                    Text(decodedArtistName, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = if (showTitle) MaterialTheme.colorScheme.background.copy(alpha = 0.95f) else Color.Transparent,
                scrolledContainerColor = MaterialTheme.colorScheme.background
            ),
             // Removed statusBarsPadding to allow background to fill the status bar area.
             modifier = Modifier
        )
    }
}

