package moe.memesta.vibeon.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import moe.memesta.vibeon.data.ArtistItemData
import moe.memesta.vibeon.data.SortOption
import moe.memesta.vibeon.ui.components.ArtistGridItem
import moe.memesta.vibeon.ui.components.SortBottomSheet
import moe.memesta.vibeon.ui.utils.LocalArtistViewStyle
import moe.memesta.vibeon.data.local.LibraryViewStyle

@androidx.compose.animation.ExperimentalSharedTransitionApi
@Composable
fun ArtistsListScreen(
    viewModel: LibraryViewModel,
    onBackClick: () -> Unit,
    onArtistClick: (String) -> Unit,
    onPlayArtist: (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null
) {
    val tracks by viewModel.tracks.collectAsState()
    val currentSortOption by viewModel.currentArtistSortOption.collectAsState()
    val artistViewStyle = LocalArtistViewStyle.current
    var showSortSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val artists = remember(tracks, searchQuery, currentSortOption) {
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
            .let { items ->
                when (currentSortOption) {
                    SortOption.ArtistNameAZ -> items.sortedBy { it.name.lowercase() }
                    SortOption.ArtistNameZA -> items.sortedByDescending { it.name.lowercase() }
                    SortOption.ArtistTrackCountDesc -> items.sortedByDescending {
                        it.followerCount.substringBefore(' ').toIntOrNull() ?: 0
                    }
                    SortOption.ArtistTrackCountAsc -> items.sortedBy {
                        it.followerCount.substringBefore(' ').toIntOrNull() ?: 0
                    }
                    else -> items.sortedBy { it.name.lowercase() }
                }
            }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (artistViewStyle == LibraryViewStyle.MODERN) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
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
                    text = "Artists",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "A calmer Material 3 gallery for discovery, with modern hierarchy and quick sorting.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null)
                    },
                    placeholder = {
                        Text("Search artists")
                    },
                    shape = RoundedCornerShape(24.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SuggestionChip(
                        onClick = { showSortSheet = true },
                        label = { Text("${artists.size} artists") }
                    )

                    if (artists.isNotEmpty()) {
                        FilledTonalButton(onClick = { onPlayArtist(artists.first().name) }) {
                            Text("Play artist")
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = if (artistViewStyle == LibraryViewStyle.MODERN) 8.dp else 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(artists) { artist ->
                ArtistGridItem(
                    artistName = artist.name,
                    photoUrl = artist.photoUrl,
                    onClick = { onArtistClick(artist.name) },
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope
                )
            }
        }
    }

    if (showSortSheet) {
        SortBottomSheet(
            title = "Sort artists",
            options = SortOption.ARTISTS,
            selectedOption = currentSortOption,
            onDismiss = { showSortSheet = false },
            onOptionSelected = { option ->
                viewModel.setArtistSortOption(option)
                showSortSheet = false
            }
        )
    }
}
