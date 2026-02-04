package moe.memesta.vibeon.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.memesta.vibeon.data.WebSocketClient

@Composable
fun LyricsScreen(
    viewModel: ConnectionViewModel
) {
    // TODO: Expose lyrics state from ViewModel (we added the backend handler, need frontend state)
    // For now, placeholder or simple assumption that we might need to add lyrics state to ConnectionViewModel/WebSocketClient
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Lyrics support coming soon\n(Backend ready, wiring pending)",
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}
