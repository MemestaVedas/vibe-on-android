package moe.memesta.vibeon.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.memesta.vibeon.ui.theme.bouncyClickable
import moe.memesta.vibeon.ui.theme.Dimens
import moe.memesta.vibeon.ui.theme.VibeBackground
import moe.memesta.vibeon.ui.theme.VibeSurface
import moe.memesta.vibeon.ui.utils.LocalDisplayLanguage
import moe.memesta.vibeon.ui.utils.getDisplayName
import moe.memesta.vibeon.ui.utils.getDisplayArtist

/**
 * PlaylistsScreen - Displays the queue as playlists (upcoming feature: user-created playlists)
 * Currently shows "Up Next" queue with track details and playing indicator
 * 
 * Future: This will be extended to show user-created playlists when playlist management is added
 */
@Composable
fun PlaylistsScreen(
    viewModel: ConnectionViewModel,
    contentPadding: PaddingValues = PaddingValues()
) {
    val queue by viewModel.wsClient.queue.collectAsState()
    val currentIndex by viewModel.wsClient.currentIndex.collectAsState()

    if (queue.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VibeBackground)
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Filled.QueueMusic,
                    contentDescription = "Empty queue",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    "Queue is empty",
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
                .padding(contentPadding),
            contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding, vertical = Dimens.SectionSpacing),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
        ) {
            item {
                Text(
                    text = "Up Next",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = Dimens.SectionSpacing)
                )
            }
            
            itemsIndexed(queue) { index, item ->
                PlaylistQueueItemRow(item, isCurrent = index == currentIndex)
            }
            
            // Bottom padding for floating nav bar
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun PlaylistQueueItemRow(item: moe.memesta.vibeon.data.QueueItem, isCurrent: Boolean) {
    val displayLanguage = LocalDisplayLanguage.current
    val title = item.getDisplayName(displayLanguage)
    val artist = item.getDisplayArtist(displayLanguage)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClickable(onClick = {}, enabled = false)
            .padding(vertical = 12.dp)
            .background(
                color = if (isCurrent) VibeSurface.copy(alpha = 0.8f) else Color.Transparent,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isCurrent) {
            androidx.compose.material3.Icon(
                androidx.compose.material.icons.Icons.Rounded.GraphicEq,
                contentDescription = "Playing",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(20.dp)
                    .padding(end = 12.dp)
            )
        } else {
            Spacer(modifier = Modifier.width(28.dp))
        }

        // Track Cover
        coil.compose.AsyncImage(
            model = item.coverUrl,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = artist,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}
