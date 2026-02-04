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
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(vertical = 24.dp),
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
         Column(modifier = Modifier.weight(1f)) {
             Text(
                 text = item.title,
                 color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.White,
                 fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                 style = MaterialTheme.typography.bodyLarge
             )
             Text(
                 text = item.artist,
                 color = Color.White.copy(alpha = 0.6f),
                 style = MaterialTheme.typography.bodyMedium
             )
         }
    }
}
