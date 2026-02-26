package moe.memesta.vibeon.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import moe.memesta.vibeon.ui.theme.bouncyClickable
import moe.memesta.vibeon.ui.theme.Dimens
import moe.memesta.vibeon.ui.theme.VibeBackground
import moe.memesta.vibeon.ui.theme.VibeSurface
import moe.memesta.vibeon.ui.utils.LocalDisplayLanguage
import moe.memesta.vibeon.ui.utils.getDisplayName
import moe.memesta.vibeon.ui.utils.getDisplayArtist

/**
 * PlaylistsScreen - Displays user-created playlists from the PC
 * Shows playlist cards with track count and allows navigation to view tracks
 */
@Composable
fun PlaylistsScreen(
    viewModel: ConnectionViewModel,
    libraryViewModel: LibraryViewModel,
    contentPadding: PaddingValues = PaddingValues(),
    onPlaylistSelected: (String) -> Unit = {}
) {
    val playlists by viewModel.playlists.collectAsState()
    val allTracks by libraryViewModel.tracks.collectAsState()
    var showCreateWizard by remember { mutableStateOf(false) }
    
    // Fetch playlists when screen loads
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.getPlaylists()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("New Playlist") },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                onClick = { showCreateWizard = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.padding(16.dp)
            )
        }
    ) { paddingValues ->
        if (playlists.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(VibeBackground)
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Filled.QueueMusic,
                        contentDescription = "No playlists",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        "No playlists",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        fontSize = 16.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(VibeBackground)
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Dimens.SectionSpacing),
                verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
            ) {
                item {
                    Text(
                        text = "Playlists",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = Dimens.SectionSpacing)
                    )
                }
                
                itemsIndexed(playlists) { index, playlist ->
                    PlaylistCard(
                        playlist = playlist,
                        onPlaylistClick = { onPlaylistSelected(playlist.id) }
                    )
                }
                
                // Bottom padding for floating nav bar
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    if (showCreateWizard) {
        PlaylistCreationWizard(
            songs = allTracks,
            onCreatePlaylist = { name, songPaths, customization ->
                // Send to server
                viewModel.createPlaylist(name, songPaths, customization)
                showCreateWizard = false
                viewModel.getPlaylists()
            },
            onDismiss = { showCreateWizard = false }
        )
    }
}


@Composable
fun PlaylistCard(
    playlist: moe.memesta.vibeon.data.PlaylistInfo,
    onPlaylistClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClickable(onClick = onPlaylistClick)
            .padding(vertical = 12.dp)
            .background(
                color = VibeSurface.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Album Art Placeholder for Playlist
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.QueueMusic,
                contentDescription = "Playlist",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${playlist.trackCount} tracks",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        
        androidx.compose.material3.Icon(
            androidx.compose.material.icons.Icons.Rounded.GraphicEq,
            contentDescription = "View playlist",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
    }
}
