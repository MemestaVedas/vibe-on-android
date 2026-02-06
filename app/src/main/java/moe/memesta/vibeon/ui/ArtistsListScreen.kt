package moe.memesta.vibeon.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import moe.memesta.vibeon.data.ArtistItemData
import moe.memesta.vibeon.ui.components.ArtistListItem

@Composable
fun ArtistsListScreen(
    viewModel: LibraryViewModel,
    onBackClick: () -> Unit,
    onArtistClick: (String) -> Unit,
    onPlayArtist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tracks by viewModel.tracks.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    val artists = remember(tracks, searchQuery) {
        tracks.groupBy { it.artist }
            .map { (artist, artistTracks) ->
                ArtistItemData(
                    name = artist,
                    followerCount = "${artistTracks.size} Tracks",
                    photoUrl = artistTracks.firstOrNull()?.coverUrl
                )
            }
            .filter {
                searchQuery.isEmpty() || 
                it.name.contains(searchQuery, ignoreCase = true)
            }
            .sortedBy { it.name }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 12.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Artists",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            IconButton(onClick = { /* Open search */ }) {
                Icon(Icons.Rounded.Search, contentDescription = "Search")
            }
        }
        
        // Artists List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(artists) { artist ->
                ArtistListItem(
                    artistName = artist.name,
                    followerCount = artist.followerCount,
                    photoUrl = artist.photoUrl,
                    onClick = { onArtistClick(artist.name) },
                    onPlayClick = { onPlayArtist(artist.name) }
                )
            }
        }
    }
}
