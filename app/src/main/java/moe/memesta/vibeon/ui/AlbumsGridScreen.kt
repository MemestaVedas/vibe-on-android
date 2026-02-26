package moe.memesta.vibeon.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import moe.memesta.vibeon.ui.theme.Dimens
import moe.memesta.vibeon.ui.AlbumGridItem
import moe.memesta.vibeon.ui.components.SortBottomSheet
import moe.memesta.vibeon.data.SortOption

@Composable
fun AlbumsGridScreen(
    viewModel: LibraryViewModel,
    onBackClick: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onPlayAlbum: (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val albums by viewModel.filteredAlbums.collectAsState()
    val currentSortOption by viewModel.currentAlbumSortOption.collectAsState()
    var showSortSheet by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = Dimens.ScreenPadding, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        CircleShape
                    )
                ) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Text(
                    text = "Albums",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            
            // Sort button
            IconButton(
                onClick = { showSortSheet = true },
                modifier = Modifier.background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    CircleShape
                )
            ) {
                Icon(
                    Icons.Rounded.Sort,
                    contentDescription = "Sort",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = Dimens.ScreenPadding, 
                end = Dimens.ScreenPadding, 
                top = Dimens.ScreenPadding, 
                bottom = contentPadding.calculateBottomPadding() + Dimens.SectionSpacing
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                items = albums,
                key = { it.name }
            ) { album ->
                AlbumGridItem(
                    albumName = album.name,
                    artistName = album.artist,
                    coverUrl = album.coverUrl,
                    songCount = album.songCount,
                    onClick = { onAlbumClick(album.name) }
                )
            }
        }
    }
    
    // Sort Bottom Sheet
    if (showSortSheet) {
        SortBottomSheet(
            title = "Sort by",
            options = SortOption.ALBUMS,
            selectedOption = currentSortOption,
            onDismiss = { showSortSheet = false },
            onOptionSelected = { option ->
                viewModel.setAlbumSortOption(option)
                showSortSheet = false
            }
        )
    }
}
