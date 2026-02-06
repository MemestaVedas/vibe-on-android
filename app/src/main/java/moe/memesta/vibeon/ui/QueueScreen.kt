package moe.memesta.vibeon.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.memesta.vibeon.data.QueueItem

@Composable
fun QueueScreen(
    viewModel: ConnectionViewModel
) {
    val queue by viewModel.wsClient.queue.collectAsState()
    val currentIndex by viewModel.wsClient.currentIndex.collectAsState()

    if (queue.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
             Text("Queue is empty", color = Color.White.copy(alpha = 0.5f))
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                 Text(
                    text = "Up Next",
                    style = MaterialTheme.typography.headlineSmall,
                     color = Color.White,
                     fontWeight = FontWeight.Bold,
                     modifier = Modifier.padding(bottom = 16.dp)
                 )
            }
            
            itemsIndexed(queue) { index, item ->
                QueueItemRow(item, isCurrent = index == currentIndex)
            }
        }
    }
}

@Composable
fun QueueItemRow(item: QueueItem, isCurrent: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
         if (isCurrent) {
             androidx.compose.material3.Icon(
                 androidx.compose.material.icons.Icons.Rounded.GraphicEq, // Active indicator
                 contentDescription = "Playing",
                 tint = MaterialTheme.colorScheme.primary,
                 modifier = Modifier.size(20.dp).padding(end = 8.dp)
             )
         } else {
             Spacer(modifier = Modifier.width(28.dp)) // Indent non-active tracks to align titles
         }
         
         Column(modifier = Modifier.weight(1f)) {
             Text(
                 text = item.title,
                 color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.White,
                 fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                 style = MaterialTheme.typography.bodyLarge,
                 maxLines = 1
             )
             Text(
                 text = item.artist,
                 color = Color.White.copy(alpha = 0.6f),
                 style = MaterialTheme.typography.bodyMedium,
                 maxLines = 1
             )
         }
    }
}
