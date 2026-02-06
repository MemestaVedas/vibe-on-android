@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package moe.memesta.vibeon.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import moe.memesta.vibeon.data.TrackInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    albumName: String,
    viewModel: LibraryViewModel,
    onBackClick: () -> Unit,
    onTrackSelected: (TrackInfo) -> Unit,
    contentPadding: PaddingValues
) {
    val tracks by viewModel.tracks.collectAsState()
    val albumTracks = remember(tracks, albumName) {
        tracks.filter { it.album == albumName }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(albumName, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(
                bottom = contentPadding.calculateBottomPadding() + 24.dp
            )
        ) {
            items(albumTracks) { track ->
                TrackListItem(
                    track = track,
                    onTrackClick = {
                        viewModel.playTrack(track)
                        onTrackSelected(track)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
