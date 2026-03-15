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
import moe.memesta.vibeon.ui.components.VibeContainedLoadingIndicator
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
    lyricsSurfaceColor: Color,
    lyricsTextColor: Color,
    lyricsSubtleTextColor: Color,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true
) {
    val playbackState by playbackViewModel.playbackState.collectAsState()
    val lyricsData by connectionViewModel.lyrics.collectAsState()
    
    // Default to ROMAJI as requested
    var viewMode by remember { mutableStateOf(LyricsViewMode.ROMAJI) }

    val lyrics = remember(lyricsData, viewMode) {
        buildLyricsForView(lyricsData, viewMode)
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
        modifier = modifier
            .fillMaxSize()
            .background(color = lyricsSurfaceColor)
    ) {
        // --- 1. Top Header (Floating over lyrics) ---
        if (showHeader) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.SectionSpacing, vertical = 20.dp)
                    .align(Alignment.TopCenter)
                    .zIndex(3f)
            ) {
                Text(
                    text = "Lyrics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = lyricsTextColor
                )
            }
        }

        // --- 2. Lyrics List ---
        if (!isEmpty && !isInstrumental) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Dimens.SectionSpacing),
                contentPadding = PaddingValues(top = 64.dp, bottom = 200.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                itemsIndexed(
                    items = lyrics,
                    key = { _, lyricGroup -> lyricGroup.timestamp }
                ) { index, lyricGroup ->
                    LyricGroupItem(
                        lyricGroup = lyricGroup,
                        isActive = index == currentLineIndex,
                        isPast = index < currentLineIndex,
                        viewMode = viewMode,
                        baseTextColor = lyricsTextColor,
                        secondaryTextColor = lyricsSubtleTextColor,
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
        
        /* Bottom gradient fade removed for matte look */
        
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
                        .background(lyricsSurfaceColor.copy(alpha = 0.92f), CircleShape)
                        .border(1.dp, lyricsTextColor.copy(alpha = 0.2f), CircleShape)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LyricsViewModeToggleData.entries.forEach { mode ->
                        val isSelected = viewMode == mode.mode
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isSelected) lyricsTextColor.copy(alpha = 0.2f) else Color.Transparent)
                                .bouncyClickable { viewMode = mode.mode }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.label,
                                color = if (isSelected) lyricsTextColor else lyricsSubtleTextColor,
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
                VibeContainedLoadingIndicator(label = "Loading lyrics...")
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
                        tint = lyricsSubtleTextColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isInstrumental) "Instrumental Track" else "No lyrics available",
                        style = MaterialTheme.typography.headlineSmall,
                        color = lyricsSubtleTextColor
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

fun buildLyricsForView(
    lyricsData: moe.memesta.vibeon.data.LyricsData?,
    viewMode: LyricsViewMode
): List<LyricGroup> {
    if (lyricsData == null) return emptyList()

    if (lyricsData.hasSynced && !lyricsData.syncedLyrics.isNullOrEmpty()) {
        val jpLyrics = parseLrc(lyricsData.syncedLyrics!!)
        val romajiLyrics = lyricsData.syncedLyricsRomaji
            ?.takeIf { it.isNotBlank() }
            ?.let(::parseLrc)
            .orEmpty()

        return when (viewMode) {
            LyricsViewMode.JP -> jpLyrics
            LyricsViewMode.ROMAJI -> {
                if (romajiLyrics.isNotEmpty()) {
                    romajiLyrics
                } else {
                    // Fallback: extract the most romaji-like line from each JP group.
                    jpLyrics.map { group ->
                        LyricGroup(group.timestamp, listOf(preferRomajiLine(group.lines)))
                    }
                }
            }
            LyricsViewMode.BOTH -> {
                if (romajiLyrics.isEmpty()) {
                    jpLyrics.map { group ->
                        val jpLine = group.lines.firstOrNull().orEmpty()
                        val romaLine = preferRomajiLine(group.lines)
                        if (romaLine.isNotBlank() && romaLine != jpLine) {
                            LyricGroup(group.timestamp, listOf(jpLine, romaLine))
                        } else {
                            LyricGroup(group.timestamp, listOf(jpLine))
                        }
                    }
                } else {
                    jpLyrics.mapIndexed { index, jpGroup ->
                        val romajiGroup = romajiLyrics.getOrNull(index)
                        if (romajiGroup != null && jpGroup.timestamp == romajiGroup.timestamp) {
                            LyricGroup(jpGroup.timestamp, jpGroup.lines + romajiGroup.lines)
                        } else {
                            jpGroup
                        }
                    }
                }
            }
        }
    }

    return lyricsData.plainLyrics
        ?.takeIf { it.isNotBlank() }
        ?.lines()
        ?.map { LyricGroup(0, listOf(it)) }
        .orEmpty()
}

private fun preferRomajiLine(lines: List<String>): String {
    if (lines.isEmpty()) return ""
    val cleaned = lines.map { it.trim() }.filter { it.isNotEmpty() }
    if (cleaned.isEmpty()) return ""

    // Best candidate: contains Latin letters and avoids CJK glyphs.
    cleaned.firstOrNull { hasLatin(it) && !hasCjk(it) }?.let { return it }
    // Next: any line with Latin letters.
    cleaned.firstOrNull { hasLatin(it) }?.let { return it }
    // Fallback to existing order.
    return cleaned.first()
}

private fun hasLatin(text: String): Boolean = text.any { ch ->
    ch.code in 'A'.code..'Z'.code || ch.code in 'a'.code..'z'.code
}

private fun hasCjk(text: String): Boolean = text.any { ch ->
    when (Character.UnicodeBlock.of(ch)) {
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A,
        Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B,
        Character.UnicodeBlock.HIRAGANA,
        Character.UnicodeBlock.KATAKANA,
        Character.UnicodeBlock.HANGUL_SYLLABLES,
        Character.UnicodeBlock.HANGUL_JAMO,
        Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS -> true
        else -> false
    }
}

private data class TempLyricEntry(val timestamp: Long, val text: String)

@Composable
fun LyricGroupItem(
    lyricGroup: LyricGroup,
    isActive: Boolean,
    isPast: Boolean,
    viewMode: LyricsViewMode,
    baseTextColor: Color,
    secondaryTextColor: Color,
    modifier: Modifier = Modifier
) {
    val textColor by animateColorAsState(
        targetValue = when {
            isActive -> baseTextColor
            isPast -> baseTextColor.copy(alpha = 0.35f)
            else -> baseTextColor.copy(alpha = 0.20f)
        },
        animationSpec = tween(durationMillis = 300),
        label = "TextColor"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.25f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "Scale"
    )
    
    // Logic for what lines to show
    val primaryText = when (viewMode) {
        LyricsViewMode.JP -> lyricGroup.lines.firstOrNull() ?: ""
        // Use script-aware selection to prioritize romaji lines.
        LyricsViewMode.ROMAJI -> preferRomajiLine(lyricGroup.lines)
        // Both: Primary is Original (JP)
        LyricsViewMode.BOTH -> lyricGroup.lines.firstOrNull() ?: ""
    }
    
    val secondaryText = if (viewMode == LyricsViewMode.BOTH && lyricGroup.lines.size > 1) {
        lyricGroup.lines[1]
    } else null

    Column(
        modifier = modifier
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
                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
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
                color = secondaryTextColor.copy(alpha = textColor.alpha * 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
