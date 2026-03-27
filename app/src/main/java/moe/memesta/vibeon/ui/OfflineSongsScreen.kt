package moe.memesta.vibeon.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import moe.memesta.vibeon.data.model.UnifiedSong
import moe.memesta.vibeon.ui.theme.Dimens

@Composable
fun OfflineSongsScreen(
    contentPadding: PaddingValues,
    playbackViewModel: PlaybackViewModel? = null,
    connectionViewModel: ConnectionViewModel? = null,
    viewModel: OfflineSongsViewModel = viewModel()
) {
    val context = LocalContext.current
    val songs by viewModel.songs.collectAsState()
    val unifiedSongs by viewModel.unifiedSongs.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    // Merge offline + server library whenever the server library changes
    val serverLibrary by connectionViewModel?.library?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    LaunchedEffect(serverLibrary) {
        viewModel.mergeWithServerLibrary(serverLibrary)
    }

    var hasPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            viewModel.loadSongs()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            permissionLauncher.launch(permission)
        } else {
            viewModel.loadSongs()
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        if (!hasPermission) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Permission required to access offline music.")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_AUDIO
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                    permissionLauncher.launch(permission)
                }) {
                    Text("Grant Permission")
                }
            }
        } else if (unifiedSongs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No offline songs found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = Dimens.ScreenPadding,
                    bottom = Dimens.ScreenPadding + (if (currentSong != null) 90.dp else 20.dp),
                    start = Dimens.ScreenPadding,
                    end = Dimens.ScreenPadding
                )
            ) {
                item {
                    Text(
                        "Offline Songs",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    // Stats row
                    val matchedCount = unifiedSongs.count { it.serverPath != null }
                    if (matchedCount > 0) {
                        Text(
                            "$matchedCount of ${unifiedSongs.size} synced with server",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                items(
                    items = unifiedSongs,
                    key = { it.id }
                ) { song ->
                    UnifiedSongItem(
                        song = song,
                        onClick = { viewModel.playUnified(song, connectionViewModel) }
                    )
                }
            }
        }

        // Mini player — shown while an offline (ExoPlayer) song is playing
        if (currentSong != null) {
            OfflineMiniPlayer(
                song = currentSong!!,
                isPlaying = isPlaying,
                onPlayPauseClick = { viewModel.togglePlayPause() },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = Dimens.ScreenPadding)
            )
        }
    }
}

/** Song row supporting both offline-only and server-matched songs. */
@Composable
fun UnifiedSongItem(song: UnifiedSong, onClick: () -> Unit) {
    val fallbackBaseUrl = "content://media/external/audio/albumart"
    val albumArtUri = "$fallbackBaseUrl/${song.albumId}"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            val painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Rounded.MusicNote)
            // Prefer server cover URL; fall back to local MediaStore album art
            val artModel = if (song.coverUrl != null) {
                ImageRequest.Builder(LocalContext.current)
                    .data(song.coverUrl)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .crossfade(true)
                    .build()
            } else {
                ImageRequest.Builder(LocalContext.current)
                    .data(android.net.Uri.parse(albumArtUri))
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .crossfade(true)
                    .build()
            }
            AsyncImage(
                model = artModel,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                error = painter
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Offline badge: shown when song is offline-only (no server match)
        if (!song.isOfflineAvailable || song.serverPath == null) {
            Icon(
                imageVector = Icons.Rounded.WifiOff,
                contentDescription = "Offline only",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        } else {
            // Small "synced" indicator
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Text(
                    text = "SYNC",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun OfflineMiniPlayer(
    song: OfflineSong,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fallbackBaseUrl = "content://media/external/audio/albumart"
    val albumArtUri = "$fallbackBaseUrl/${song.albumId}"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), androidx.compose.foundation.shape.RoundedCornerShape(32.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(modifier = Modifier.size(56.dp).clip(CircleShape)) {
                        val painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Rounded.MusicNote)
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(android.net.Uri.parse(albumArtUri))
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            error = painter,
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = onPlayPauseClick,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OfflineSongItem(song: OfflineSong, onClick: () -> Unit) {
    val fallbackBaseUrl = "content://media/external/audio/albumart"
    val albumArtUri = "$fallbackBaseUrl/${song.albumId}"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            val painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Rounded.MusicNote)
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(android.net.Uri.parse(albumArtUri))
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                error = painter
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
