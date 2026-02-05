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
    val currentTrack by viewModel.currentTrack.collectAsState()
    val scrollState = rememberScrollState()
    val lyrics = currentTrack.lyrics

    // Auto-fetch lyrics if empty when screen is shown or track changes
    androidx.compose.runtime.LaunchedEffect(currentTrack.title, currentTrack.artist) {
        if (currentTrack.lyrics.isEmpty() && currentTrack.title != "No Track") {
            android.util.Log.i("LyricsScreen", "🔍 Requesting lyrics for ${currentTrack.title}")
            viewModel.getLyrics()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding() // Notch Support
            .padding(horizontal = 24.dp)
            .padding(bottom = 80.dp), // Space for Pager Indicator
        contentAlignment = Alignment.TopCenter
    ) {
        if (lyrics.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Lyrics",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = lyrics,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineSmall.copy(
                         lineHeight = 32.sp
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(48.dp))
            }
        } else {
            // Empty State
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No Lyrics Available",
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Queue up a song with lyrics!",
                    color = Color.White.copy(alpha = 0.3f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
