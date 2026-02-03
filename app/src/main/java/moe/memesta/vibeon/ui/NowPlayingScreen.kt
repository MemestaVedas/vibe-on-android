package moe.memesta.vibeon.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import coil.compose.AsyncImage
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.memesta.vibeon.ui.theme.VibeBlue
import moe.memesta.vibeon.ui.theme.VibePurple

@Composable
fun NowPlayingScreen(
    title: String,
    artist: String,
    isPlaying: Boolean,
    progress: Float,
    coverUrl: String? = null,
    baseUrl: String = "http://192.168.1.34:5000",
    isMobilePlayback: Boolean = false,
    onPlayPauseToggle: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeek: (Float) -> Unit,
    onBackToLibrary: () -> Unit = {},
    onTogglePlaybackLocation: () -> Unit = {}
) {
    var sliderValue by remember { mutableStateOf(progress) }
    var isDragging by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header with back button
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackToLibrary) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back to Library",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "Now Playing",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Box(modifier = Modifier.size(24.dp)) // Spacer for alignment
        }

        // Album Art
        Box(
            modifier = Modifier
                .size(280.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (!coverUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = "$baseUrl$coverUrl",
                    contentDescription = "Album Art",
                    modifier = Modifier
                        .size(280.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            }
        }

        // Track Info
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title.ifEmpty { "No Track" },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )
            Text(
                text = artist.ifEmpty { "Unknown Artist" },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                maxLines = 1
            )
        }

        // Progress Bar and Time
        Column(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = if (isDragging) sliderValue else progress,
                onValueChange = { 
                    isDragging = true
                    sliderValue = it 
                },
                onValueChangeFinished = { 
                    isDragging = false
                    onSeek(sliderValue) 
                },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = VibePurple,
                    activeTrackColor = VibePurple,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(sliderValue.toDouble()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                // Total duration placeholder
                Text(
                    text = "--:--",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        // Controls
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Playback location indicator & toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onTogglePlaybackLocation,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isMobilePlayback) VibePurple.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                        contentColor = if (isMobilePlayback) VibePurple else MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.fillMaxWidth(0.6f)
                ) {
                    Icon(
                        imageVector = if (isMobilePlayback) Icons.Default.PhoneAndroid else Icons.Default.Computer,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isMobilePlayback) "Playing on Mobile" else "Playing on Desktop",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onSkipPrevious,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        Icons.Filled.SkipPrevious,
                        contentDescription = "Previous",
                        modifier = Modifier.size(32.dp),
                        tint = VibeBlue
                    )
                }

                FloatingActionButton(
                    onClick = onPlayPauseToggle,
                    shape = RoundedCornerShape(50),
                    containerColor = VibePurple,
                    contentColor = Color.White,
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(40.dp)
                    )
                }

                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        modifier = Modifier.size(32.dp),
                        tint = VibeBlue
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun formatTime(seconds: Double): String {
    val totalSeconds = seconds.toInt()
    val minutes = totalSeconds / 60
    val secs = totalSeconds % 60
    return String.format("%d:%02d", minutes, secs)
}
