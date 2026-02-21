package moe.memesta.vibeon.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
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
import moe.memesta.vibeon.ui.components.ArtistGridItem

@Composable
fun ArtistsListScreen(
    viewModel: LibraryViewModel,
    onBackClick: () -> Unit,
    onArtistClick: (String) -> Unit,
    onPlayArtist: (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
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
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = "Artists",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        // Artists Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(artists) { artist ->
                ArtistGridItem(
                    artistName = artist.name,
                    photoUrl = artist.photoUrl,
                    onClick = { onArtistClick(artist.name) }
                )
            }
        }
    }
}
