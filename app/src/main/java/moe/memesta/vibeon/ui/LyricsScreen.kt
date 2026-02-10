package moe.memesta.vibeon.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import moe.memesta.vibeon.ui.theme.VibeBackground
import moe.memesta.vibeon.ui.theme.Dimens
import moe.memesta.vibeon.ui.theme.bouncyClickable

data class LyricGroup(
    val timestamp: Long,
    val lines: List<String>
)

enum class LyricsViewMode {
    ROMAJI, JP, BOTH
}

@Composable
fun LyricsScreen(
    connectionViewModel: ConnectionViewModel,
    playbackViewModel: PlaybackViewModel,
    onBack: () -> Unit
) {
    val currentTrack by connectionViewModel.currentTrack.collectAsState()
    val playbackState by playbackViewModel.playbackState.collectAsState()
    val lyricsData by connectionViewModel.lyrics.collectAsState()
    
    // Default to ROMAJI as requested
    var viewMode by remember { mutableStateOf(LyricsViewMode.ROMAJI) }

    // Parse lyrics
    val lyrics = remember(lyricsData) {
        if (lyricsData?.hasSynced == true && !lyricsData?.syncedLyrics.isNullOrEmpty()) {
            parseLrc(lyricsData!!.syncedLyrics!!)
        } else if (!lyricsData?.plainLyrics.isNullOrEmpty()) {
             // Fallback for plain text
             lyricsData!!.plainLyrics!!.lines().map { LyricGroup(0, listOf(it)) }
        } else {
             emptyList()
        }
    }

    val isEmpty = lyrics.isEmpty()
    val isInstrumental = lyricsData?.instrumental == true
    
    val currentTimeMs = (playbackState.progress * playbackState.duration).toLong()
    val currentLineIndex = remember(currentTimeMs, lyrics) {
        if (isEmpty) -1 else lyrics.indexOfLast { it.timestamp <= currentTimeMs }.coerceAtLeast(0)
    }
    
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // Auto-scroll
    LaunchedEffect(currentLineIndex, lyrics) {
        if (currentLineIndex >= 0 && !isEmpty) {
            scope.launch {
                listState.animateScrollToItem(
                    index = (currentLineIndex - 2).coerceAtLeast(0),
                    scrollOffset = 0
                )
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VibeBackground)
    ) {
        // --- 1. Top Header (Track Info) ---
        // --- 1. Top Header (Minimal/Floating) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(Dimens.ScreenPadding)
                .zIndex(2f)
        ) {
            // Back Button
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    .bouncyClickable { onBack() }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        }

        // Top gradient fade
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            VibeBackground,
                            VibeBackground.copy(alpha = 0.9f),
                            Color.Transparent
                        )
                    )
                )
                .align(Alignment.TopCenter)
                .zIndex(1f)
        )
        
        // --- 2. Lyrics List ---
        if (!isEmpty && !isInstrumental) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Dimens.SectionSpacing), // Use consistent spacing
                contentPadding = PaddingValues(top = 150.dp, bottom = 200.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                itemsIndexed(lyrics) { index, lyricGroup ->
                    LyricGroupItem(
                        lyricGroup = lyricGroup,
                        isActive = index == currentLineIndex,
                        isPast = index < currentLineIndex,
                        viewMode = viewMode
                    )
                }
            }
        }
        
        // Bottom gradient fade
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            VibeBackground.copy(alpha = 0.9f),
                            VibeBackground
                        )
                    )
                )
                .align(Alignment.BottomCenter)
                .zIndex(1f)
        )
        
        // --- 3. Bottom Toggle (Floating Pill) ---
        if (!isEmpty && !isInstrumental) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .zIndex(2f)
            ) {
                Row(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LyricsViewModeToggleData.entries.forEach { mode ->
                        val isSelected = viewMode == mode.mode
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .bouncyClickable { viewMode = mode.mode }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.label,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
        
        // --- 4. Empty / Status State ---
        val isLoading by connectionViewModel.isLoadingLyrics.collectAsState()
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (isEmpty || isInstrumental) {
             Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isInstrumental) "Instrumental Track" else "No lyrics available",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

private enum class LyricsViewModeToggleData(val mode: LyricsViewMode, val label: String) {
    JP(LyricsViewMode.JP, "JP"),
    ROMAJI(LyricsViewMode.ROMAJI, "Romaji"),
    BOTH(LyricsViewMode.BOTH, "Both")
}

fun parseLrc(lrc: String): List<LyricGroup> {
    val regex = "\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)".toRegex()
    val entries = mutableListOf<TempLyricEntry>()

    lrc.lineSequence().forEach { line ->
        regex.find(line)?.let { match ->
            val (min, sec, ms, textRaw) = match.destructured
            val timestamp = min.toLong() * 60000 + sec.toLong() * 1000 + ms.padEnd(3, '0').take(3).toLong()
            val text = textRaw.trim()
            if (text.isNotBlank()) {
                // Split by " / " to support inline dual lyrics
                if (text.contains(" / ")) {
                    val parts = text.split(" / ")
                    parts.forEach { part ->
                        entries.add(TempLyricEntry(timestamp, part.trim()))
                    }
                } else {
                    entries.add(TempLyricEntry(timestamp, text))
                }
            }
        }
    }
    return entries.groupBy { it.timestamp }
        .map { (timestamp, groupEntries) ->
            LyricGroup(timestamp, groupEntries.map { it.text })
        }
        .sortedBy { it.timestamp }
}

private data class TempLyricEntry(val timestamp: Long, val text: String)

@Composable
fun LyricGroupItem(
    lyricGroup: LyricGroup,
    isActive: Boolean,
    isPast: Boolean,
    viewMode: LyricsViewMode
) {
    val textColor by animateColorAsState(
        targetValue = when {
            isActive -> Color.White
            isPast -> Color.White.copy(alpha = 0.4f)
            else -> Color.White.copy(alpha = 0.3f)
        },
        animationSpec = tween(durationMillis = 300),
        label = "TextColor"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "Scale"
    )
    
    // Logic for what lines to show
    val primaryText = when (viewMode) {
        LyricsViewMode.JP -> lyricGroup.lines.firstOrNull() ?: ""
        // Use 2nd line (Romaji) if available, fall back to 1st
        LyricsViewMode.ROMAJI -> if (lyricGroup.lines.size > 1) lyricGroup.lines[1] else lyricGroup.lines.firstOrNull() ?: ""
        // Both: Primary is Original (JP)
        LyricsViewMode.BOTH -> lyricGroup.lines.firstOrNull() ?: ""
    }
    
    val secondaryText = if (viewMode == LyricsViewMode.BOTH && lyricGroup.lines.size > 1) {
        lyricGroup.lines[1]
    } else null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = 1f
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main Line
        Text(
            text = primaryText,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = 0.5.sp,
                lineHeight = 36.sp
            ),
            color = textColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Secondary Lines (Subtitle)
        if (secondaryText != null) {
            Spacer(modifier = Modifier.height(8.dp))
             Text(
                text = secondaryText,
                style = MaterialTheme.typography.titleMedium,
                color = textColor.copy(alpha = textColor.alpha * 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
