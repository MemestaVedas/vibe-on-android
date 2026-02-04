package moe.memesta.vibeon.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import moe.memesta.vibeon.data.AlbumInfo
import moe.memesta.vibeon.ui.AlbumGridItem

@Composable
fun AlbumsGridScreen(
    viewModel: LibraryViewModel,
    onBackClick: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onPlayAlbum: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tracks by viewModel.tracks.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    val albums = remember(tracks, searchQuery) {
        tracks.groupBy { it.album }
            .map { (album, tracks) ->
                AlbumInfo(
                    name = album,
                    artist = tracks.firstOrNull()?.artist ?: "",
                    coverUrl = tracks.firstOrNull()?.coverUrl
                )
            }
            .filter {
                searchQuery.isEmpty() || 
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true)
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
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Albums",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            IconButton(onClick = { /* Open search */ }) {
                Icon(Icons.Rounded.Search, contentDescription = "Search")
            }
        }
        
        // Albums Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(albums) { album ->
                AlbumGridItem(
                    albumName = album.name,
                    artistName = album.artist,
                    coverUrl = album.coverUrl,
                    onClick = { onAlbumClick(album.name) },
                    onPlayClick = { onPlayAlbum(album.name) }
                )
            }
        }
    }
}
