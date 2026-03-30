package moe.memesta.vibeon.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import moe.memesta.vibeon.data.TrackInfo
import moe.memesta.vibeon.ui.theme.Dimens
import moe.memesta.vibeon.ui.theme.bouncyClickable
import moe.memesta.vibeon.ui.utils.LocalDisplayLanguage
import moe.memesta.vibeon.ui.utils.getDisplayName
import moe.memesta.vibeon.ui.utils.getDisplayArtist
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    viewModel: LibraryViewModel,
    connectionViewModel: ConnectionViewModel,
    onBackClick: () -> Unit,
    onTrackSelected: (TrackInfo) -> Unit,
    contentPadding: PaddingValues
) {
    val displayLanguage = LocalDisplayLanguage.current
    val playlistTracks by connectionViewModel.currentPlaylistTracks.collectAsState()
    val playlists by connectionViewModel.playlists.collectAsState()
    
    val decodedPlaylistId = remember(playlistId) {
        try {
            URLDecoder.decode(playlistId, StandardCharsets.UTF_8.toString())
        } catch (e: Exception) {
            playlistId
        }
    }

    val playlistName = playlists.find { it.id == decodedPlaylistId }?.name ?: "Playlist"
    val scrollState = rememberLazyListState()
    var pendingRemoveTrack by remember { mutableStateOf<TrackInfo?>(null) }

    // Refresh playlist tracks on enter
    LaunchedEffect(decodedPlaylistId) {
        connectionViewModel.getPlaylistTracks(decodedPlaylistId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = contentPadding.calculateBottomPadding() + Dimens.SectionSpacing,
                top = 0.dp
            )
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        )
                        
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = Dimens.ScreenPadding, top = 20.dp)
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }

                        IconButton(
                            onClick = { connectionViewModel.getPlaylistTracks(decodedPlaylistId) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = Dimens.ScreenPadding, top = 20.dp)
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = Color.White)
                        }
                        
                        FloatingActionButton(
                            onClick = {
                                if (playlistTracks.isNotEmpty()) {
                                    connectionViewModel.wsClient.sendSetQueue(playlistTracks.map { it.path })
                                    connectionViewModel.wsClient.sendPlayTrack(playlistTracks.first().path)
                                    onTrackSelected(playlistTracks.first())
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                                .size(56.dp),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Play", modifier = Modifier.size(28.dp))
                        }
                    }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimens.ScreenPadding)
                    ) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = playlistName,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${playlistTracks.size} songs",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
            
            itemsIndexed(playlistTracks) { index, track ->
                val alpha by animateFloatAsState(targetValue = 1f, animationSpec = tween(durationMillis = 400), label = "alpha")
                
                Box(modifier = Modifier.graphicsLayer { this.alpha = alpha }) {
                    PlaylistTrackRow(
                        index = index + 1,
                        track = track,
                        onMoveUp = {
                            if (index > 0) {
                                val newList = playlistTracks.toMutableList()
                                val temp = newList[index - 1]
                                newList[index - 1] = newList[index]
                                newList[index] = temp
                                
                                val ids = newList.mapNotNull { it.playlistTrackId }
                                if (ids.size == newList.size) {
                                    connectionViewModel.reorderPlaylistTracks(decodedPlaylistId, ids)
                                    connectionViewModel.getPlaylistTracks(decodedPlaylistId)
                                }
                            }
                        },
                        onMoveDown = {
                            if (index < playlistTracks.lastIndex) {
                                val newList = playlistTracks.toMutableList()
                                val temp = newList[index + 1]
                                newList[index + 1] = newList[index]
                                newList[index] = temp

                                val ids = newList.mapNotNull { it.playlistTrackId }
                                if (ids.size == newList.size) {
                                    connectionViewModel.reorderPlaylistTracks(decodedPlaylistId, ids)
                                    connectionViewModel.getPlaylistTracks(decodedPlaylistId)
                                }
                            }
                        },
                        onRemove = {
                            pendingRemoveTrack = track
                        },
                        onClick = {
                            connectionViewModel.wsClient.sendSetQueue(playlistTracks.map { it.path })
                            connectionViewModel.wsClient.sendPlayTrack(track.path)
                            onTrackSelected(track)
                        }
                    )
                }
            }
        }

        if (pendingRemoveTrack != null) {
            val removeTitle = pendingRemoveTrack?.getDisplayName(displayLanguage) ?: "this track"
            AlertDialog(
                onDismissRequest = { pendingRemoveTrack = null },
                title = { Text("Remove Track?") },
                text = { Text("Remove \"$removeTitle\" from this playlist?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val trackId = pendingRemoveTrack?.playlistTrackId
                            if (trackId != null) {
                                connectionViewModel.removeFromPlaylist(decodedPlaylistId, trackId)
                                connectionViewModel.getPlaylistTracks(decodedPlaylistId)
                            }
                            pendingRemoveTrack = null
                        }
                    ) {
                        Text("Remove")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingRemoveTrack = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun PlaylistTrackRow(
    index: Int,
    track: TrackInfo,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onClick: () -> Unit
) {
    val displayLanguage = LocalDisplayLanguage.current
    val title = track.getDisplayName(displayLanguage)
    val artist = track.getDisplayArtist(displayLanguage)
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClickable(onClick = onClick, scaleDown = 0.98f)
            .padding(vertical = 12.dp, horizontal = Dimens.ScreenPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = index.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(32.dp)
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(artist, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    Icons.Rounded.MoreVert,
                    contentDescription = "More",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                DropdownMenuItem(
                    text = { Text("Move Up", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = {
                        showMenu = false
                        onMoveUp()
                    },
                    leadingIcon = { Icon(Icons.Rounded.ArrowUpward, "Up", tint = MaterialTheme.colorScheme.primary) }
                )
                DropdownMenuItem(
                    text = { Text("Move Down", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = {
                        showMenu = false
                        onMoveDown()
                    },
                    leadingIcon = { Icon(Icons.Rounded.ArrowDownward, "Down", tint = MaterialTheme.colorScheme.primary) }
                )
                DropdownMenuItem(
                    text = { Text("Remove from Playlist", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        showMenu = false
                        onRemove()
                    },
                    leadingIcon = { Icon(Icons.Rounded.Delete, "Remove", tint = MaterialTheme.colorScheme.error) }
                )
            }
        }
    }
}
