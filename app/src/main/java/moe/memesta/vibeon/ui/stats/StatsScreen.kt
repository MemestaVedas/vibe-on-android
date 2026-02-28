package moe.memesta.vibeon.ui.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Hearing
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import moe.memesta.vibeon.data.stats.PlaybackStatsCalculator
import moe.memesta.vibeon.data.stats.StatsTimeRange
import moe.memesta.vibeon.ui.components.VibeContainedLoadingIndicator
import moe.memesta.vibeon.ui.theme.Dimens
import kotlin.math.max
import kotlin.math.min

@Composable
fun StatsScreen(
    statsViewModel: StatsViewModel,
    onBackPressed: () -> Unit
) {
    val uiState by statsViewModel.uiState.collectAsStateWithLifecycleCompat()
    val summary = uiState.summary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (uiState.isLoading && summary == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                VibeContainedLoadingIndicator(label = "Loading your stats...")
            }
            return
        }

        LazyColumn(
            contentPadding = PaddingValues(
                start = Dimens.ScreenPadding,
                end = Dimens.ScreenPadding,
                top = Dimens.ScreenPadding,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item(key = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { onBackPressed() },
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Your Statistics",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "See how you've been vibing",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { statsViewModel.forceRefresh() },
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Refresh",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            item(key = "ranges") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatsTimeRange.values().forEach { range ->
                        val selected = uiState.selectedRange == range
                        val chipAlpha by animateFloatAsState(
                            targetValue = if (selected) 1f else 0.9f,
                            animationSpec = tween(220),
                            label = "range-chip-alpha"
                        )
                        FilterChip(
                            selected = selected,
                            onClick = { statsViewModel.onRangeSelected(range) },
                            label = { Text(range.displayName, modifier = Modifier.alpha(chipAlpha)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            if (summary != null) {
                item(key = "summary") {
                    AnimatedSection(index = 0) { SummaryGrid(summary = summary) }
                }

                item(key = "timeline") {
                    AnimatedSection(index = 1) { TimelineSection(summary = summary) }
                }

                item(key = "genres") {
                    AnimatedSection(index = 2) { TopGenresSection(summary = summary) }
                }

                item(key = "daily-rhythm") {
                    AnimatedSection(index = 3) { DailyRhythmSection(summary = summary) }
                }

                item(key = "songs") {
                    AnimatedSection(index = 4) { TopSongsSection(summary = summary) }
                }

                item(key = "artists") {
                    AnimatedSection(index = 5) { TopArtistsSection(summary = summary) }
                }

                item(key = "albums") {
                    AnimatedSection(index = 6) { TopAlbumsSection(summary = summary) }
                }

                item(key = "sessions") {
                    AnimatedSection(index = 7) { SessionsAndStreaksSection(summary = summary) }
                }

                item(key = "peak") {
                    AnimatedSection(index = 8) { PeakListeningSection(summary = summary) }
                }
            }
        }
    }
}

@Composable
private fun AnimatedSection(index: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((index * 60L).coerceAtMost(420L))
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(320)) + slideInVertically(
            initialOffsetY = { it / 5 },
            animationSpec = spring(dampingRatio = 0.9f, stiffness = 350f)
        )
    ) {
        content()
    }
}

@Composable
private fun SummaryGrid(summary: PlaybackStatsCalculator.PlaybackStatsSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Listening Time",
                value = formatDuration(summary.totalDurationMs),
                icon = Icons.Outlined.AccessTime,
                gradient = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Total Plays",
                value = summary.totalPlayCount.toString(),
                icon = Icons.Outlined.Hearing,
                gradient = listOf(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.primary)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Unique Songs",
                value = summary.uniqueSongs.toString(),
                icon = Icons.Outlined.MusicNote,
                gradient = listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary)
            )
            StatCard(
                modifier = Modifier.weight(1f),
                label = "Active Days",
                value = summary.activeDays.toString(),
                icon = Icons.Outlined.CalendarMonth,
                gradient = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradient: List<Color>
) {
    val iconScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f),
        label = "stat-icon-scale"
    )
    Card(
        modifier = modifier
            .height(120.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradient))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size((20f * iconScale).dp)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun TimelineSection(summary: PlaybackStatsCalculator.PlaybackStatsSummary) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Outlined.AutoGraph, contentDescription = null)
                Text("Listening Timeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            TimelineChart(summary.timeline)
            TimelineLabels(summary.timeline)
        }
    }
}

@Composable
private fun TimelineChart(timeline: List<PlaybackStatsCalculator.TimelineEntry>) {
    if (timeline.isEmpty()) {
        Text(
            text = "No listening data yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val max = timeline.maxOf { it.totalDurationMs }.coerceAtLeast(1L)
    val barColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    var chartReady by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (chartReady) 1f else 0f,
        animationSpec = tween(650),
        label = "timeline-progress"
    )
    LaunchedEffect(timeline) { chartReady = true }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        val barWidth = size.width / (timeline.size * 1.4f)
        val gap = barWidth * 0.4f
        timeline.forEachIndexed { index, entry ->
            val barHeight = (entry.totalDurationMs / max.toFloat()) * size.height * progress
            val x = index * (barWidth + gap)
            drawRoundRect(
                color = barColor,
                topLeft = androidx.compose.ui.geometry.Offset(x, size.height - barHeight),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
            )
        }
        drawLine(
            color = gridColor,
            start = androidx.compose.ui.geometry.Offset(0f, size.height),
            end = androidx.compose.ui.geometry.Offset(size.width, size.height),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        )
    }
}

@Composable
private fun TimelineLabels(timeline: List<PlaybackStatsCalculator.TimelineEntry>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        for (entry in timeline.take(4)) {
            Text(
                text = entry.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TopSongsSection(summary: PlaybackStatsCalculator.PlaybackStatsSummary) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Top Songs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            for ((index, song) in summary.topSongs.withIndex()) {
                val rowAlpha by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = tween(220 + (index * 55)),
                    label = "song-row-$index"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(rowAlpha),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "${index + 1}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                        Text(song.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = "${song.playCount} plays",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (summary.topSongs.isEmpty()) {
                Text(
                    text = "Start playing music to see your top songs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TopArtistsSection(summary: PlaybackStatsCalculator.PlaybackStatsSummary) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Top Artists", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            for ((index, artist) in summary.topArtists.withIndex()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.Person, contentDescription = null)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(artist.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "${artist.playCount} plays · ${artist.uniqueSongs} songs",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = formatDuration(artist.totalDurationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (summary.topArtists.isEmpty()) {
                Text(
                    text = "Play some music to see your top artists",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TopAlbumsSection(summary: PlaybackStatsCalculator.PlaybackStatsSummary) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Top Albums", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            for (album in summary.topAlbums) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AsyncImage(
                        model = album.albumArtUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(album.album, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "${album.playCount} plays · ${album.uniqueSongs} songs",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = formatDuration(album.totalDurationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (summary.topAlbums.isEmpty()) {
                Text(
                    text = "Albums will show up once you listen more",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TopGenresSection(summary: PlaybackStatsCalculator.PlaybackStatsSummary) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Top Genres", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            val maxDuration = summary.topGenres.maxOfOrNull { it.totalDurationMs }?.coerceAtLeast(1L) ?: 1L
            summary.topGenres.forEachIndexed { index, genre ->
                val progressTarget = (genre.totalDurationMs.toFloat() / maxDuration.toFloat()).coerceIn(0f, 1f)
                val progress by animateFloatAsState(
                    targetValue = progressTarget,
                    animationSpec = tween(350 + (index * 70)),
                    label = "genre-progress-$index"
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = genre.genre,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = formatDuration(genre.totalDurationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
            if (summary.topGenres.isEmpty()) {
                Text(
                    text = "Keep listening to discover your genre profile",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DailyRhythmSection(summary: PlaybackStatsCalculator.PlaybackStatsSummary) {
    val distribution = summary.dayListeningDistribution
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Daily Rhythm", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (distribution == null || distribution.buckets.isEmpty()) {
                Text(
                    text = "Not enough listening sessions yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            val topBuckets = distribution.buckets
                .sortedByDescending { it.totalDurationMs }
                .take(4)

            topBuckets.forEachIndexed { index, bucket ->
                val share = (bucket.totalDurationMs.toFloat() / distribution.maxBucketDurationMs.coerceAtLeast(1L).toFloat())
                    .coerceIn(0f, 1f)
                val animatedShare by animateFloatAsState(
                    targetValue = share,
                    animationSpec = tween(350 + (index * 60)),
                    label = "rhythm-share-$index"
                )
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = formatMinuteLabel(bucket.startMinute),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1f)
                        )
                        LinearProgressIndicator(
                            progress = { animatedShare },
                            modifier = Modifier
                                .weight(1f)
                                .height(7.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surface
                        )
                        Text(
                            text = formatDuration(bucket.totalDurationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionsAndStreaksSection(summary: PlaybackStatsCalculator.PlaybackStatsSummary) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Sessions & Streaks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricChip(
                    label = "Sessions",
                    value = summary.totalSessions.toString()
                )
                MetricChip(
                    label = "Longest Streak",
                    value = "${summary.longestStreakDays} days"
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricChip(
                    label = "Avg Session",
                    value = formatDuration(summary.averageSessionDurationMs)
                )
                MetricChip(
                    label = "Avg / Day",
                    value = String.format("%.1f", summary.averageSessionsPerDay)
                )
            }
        }
    }
}

@Composable
private fun PeakListeningSection(summary: PlaybackStatsCalculator.PlaybackStatsSummary) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Peak Listening", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                text = summary.peakDayLabel?.let { "Peak day: $it" } ?: "Peak day: --",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Peak duration: ${formatDuration(summary.peakDayDurationMs)}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun MetricChip(label: String, value: String) {
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(300),
        label = "metric-chip-alpha"
    )
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .alpha(alpha)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = value, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatMinuteLabel(startMinute: Int): String {
    val safeStart = startMinute.coerceIn(0, 23 * 60 + 59)
    val hour = safeStart / 60
    val minute = safeStart % 60
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val period = if (hour < 12) "AM" else "PM"
    return String.format("%02d:%02d %s", displayHour, minute, period)
}

private fun formatDuration(durationMs: Long): String {
    val totalMinutes = durationMs / 60000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
