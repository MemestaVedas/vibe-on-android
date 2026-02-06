package moe.memesta.vibeon.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import moe.memesta.vibeon.data.AlbumInfo
import moe.memesta.vibeon.data.ArtistItemData
import moe.memesta.vibeon.data.TrackInfo

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
                    photoUrl = artistTracks.firstOrNull()?.coverUrl // Use track cover as artist photo fallback
                )
            }
            .sortedBy { it.name }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + 24.dp, 
                bottom = contentPadding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Section: Recently Added / Tracks (Horizontal)
            item {
                SectionHeader("Recently Added")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(tracks.take(10)) { track ->
                        moe.memesta.vibeon.ui.components.SquareTrackCard(
                            track = track,
                            onClick = { 
                                viewModel.playTrack(track) // Play directly
                                onTrackSelected(track) 
                            }
                        )
                    }
                }
            }

            // Section: Albums (Horizontal for now)
            item {
                SectionHeader("Albums")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
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

            // Section: Artists (Vertical simplified or Horizontal)
            item {
                SectionHeader("Artists")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
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
            
            // Fallback list of all tracks if needed, or just keep it clean
        }
        
        // Connection Status Indicator (Top-Right)
        moe.memesta.vibeon.ui.components.ConnectionStatusIndicator(
            connectionState = connectionState,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = contentPadding.calculateTopPadding() + 16.dp, end = 24.dp)
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = "See All",
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        )
    }
}
