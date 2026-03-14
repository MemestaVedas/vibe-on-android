package moe.memesta.vibeon.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import moe.memesta.vibeon.data.SortOption
import moe.memesta.vibeon.ui.theme.Dimens
import moe.memesta.vibeon.ui.components.SortBottomSheet
import moe.memesta.vibeon.ui.utils.LocalAlbumViewStyle
import moe.memesta.vibeon.data.local.LibraryViewStyle

@androidx.compose.animation.ExperimentalSharedTransitionApi
@Composable
fun AlbumsGridScreen(
    viewModel: LibraryViewModel,
    onBackClick: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onPlayAlbum: (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null
) {
    val albums by viewModel.filteredAlbums.collectAsState()
    val currentSortOption by viewModel.currentAlbumSortOption.collectAsState()
    val albumViewStyle = LocalAlbumViewStyle.current
    var showSortSheet by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (albumViewStyle == LibraryViewStyle.MODERN) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = Dimens.ScreenPadding, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.background(
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            CircleShape
                        )
                    ) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }

                    AssistChip(
                        onClick = { showSortSheet = true },
                        label = { Text(currentSortOption.displayName) },
                        leadingIcon = {
                            Icon(Icons.Rounded.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Albums",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Artwork-first browsing with expressive Material 3 surfaces and fast sorting.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SuggestionChip(
                        onClick = { showSortSheet = true },
                        label = { Text("${albums.size} albums") }
                    )

                    if (albums.isNotEmpty()) {
                        FilledTonalButton(onClick = { onPlayAlbum(albums.first().name) }) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Play something")
                        }
                    }
                }
            }
        } else {
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
        }
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = Dimens.ScreenPadding, 
                end = Dimens.ScreenPadding, 
                top = if (albumViewStyle == LibraryViewStyle.MODERN) 8.dp else Dimens.ScreenPadding, 
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
                    onClick = { onAlbumClick(album.name) },
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope
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
