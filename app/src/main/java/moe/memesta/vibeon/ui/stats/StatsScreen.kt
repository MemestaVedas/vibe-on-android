package moe.memesta.vibeon.ui.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Hearing
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import android.net.Uri
import kotlinx.coroutines.launch
import moe.memesta.vibeon.data.stats.PlaybackStatsCalculator
import moe.memesta.vibeon.data.stats.StatsTimeRange
import moe.memesta.vibeon.ui.components.VibeContainedLoadingIndicator
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

private enum class TimelineMetric(val displayName: String) {
    ListeningTime("Listening time"),
    PlayCount("Play count"),
    AverageSession("Avg. session")
}

private enum class CategoryDimension(val displayName: String, val cardTitle: String) {
    Song("Song", "Listening by song"),
    Artist("Artist", "Listening by artist"),
    Album("Album", "Listening by album"),
    Genre("Genre", "Listening by genre")
}

private data class CategoryEntry(
    val label: String,
    val supporting: String,
    val durationMs: Long,
    val albumArtUrl: String? = null
)
private data class DimPalette(
    val container: Color,
    val content: Color,
    val accent: Color,
    val accentOn: Color
)

@Composable
fun StatsScreen(
    statsViewModel: StatsViewModel,
    onBackPressed: () -> Unit
) {
    val uiState by statsViewModel.uiState.collectAsStateWithLifecycleCompat()
    val summary = uiState.summary
    val cs = MaterialTheme.colorScheme
    val lazyListState = rememberLazyListState()
    val mediaBaseUrl = statsViewModel.mediaBaseUrl
    var selectedMetric by rememberSaveable { mutableStateOf(TimelineMetric.ListeningTime) }
    var selectedDimension by rememberSaveable { mutableStateOf(CategoryDimension.Song) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Standard header matching Albums/Artists style
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { statsViewModel.forceRefresh() },
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Refresh stats"
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Statistics",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "See how you've been vibing",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        RangeChips(
            summary = summary,
            cs = cs,
            selectedRange = uiState.selectedRange,
            onRangeSelected = { statsViewModel.onRangeSelected(it) }
        )
        if (uiState.isLoading && summary == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                VibeContainedLoadingIndicator(label = "Loading your stats...")
            }
        } else {

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 20.dp,
                    start = 20.dp,
                    end = 20.dp,
                    bottom = 120.dp
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item(key = "hero") {
                    AnimatedSection(index = 0) { StatsHeroSection(summary = summary) }
                }
                item(key = "timeline") {
                    AnimatedSection(index = 1) {
                        ListeningTimelineSection(
                            summary = summary,
                            selectedMetric = selectedMetric,
                            onMetricSelected = { selectedMetric = it }
                        )
                    }
                }
                item(key = "categories") {
                    AnimatedSection(index = 2) {
                        CategoryMetricsSection(
                            summary = summary,
                            selectedDimension = selectedDimension,
                            onDimensionSelected = { selectedDimension = it },
                            mediaBaseUrl = mediaBaseUrl
                        )
                    }
                }
                item(key = "habits") {
                    AnimatedSection(index = 3) { ListeningHabitsCard(summary = summary) }
                }
                item(key = "genres") {
                    AnimatedSection(index = 4) { TopGenresSection(summary = summary) }
                }
                item(key = "daily") {
                    AnimatedSection(index = 5) { DailyRhythmSection(summary = summary) }
                }
            }
        }

    }
}

@Composable
private fun RangeChips(
    summary: PlaybackStatsCalculator.PlaybackStatsSummary?,
    cs: androidx.compose.material3.ColorScheme,
    selectedRange: StatsTimeRange,
    onRangeSelected: (StatsTimeRange) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatsTimeRange.values().forEach { range ->
            AnimatedSelectableChip(
                selected = range == selectedRange,
                onClick = { onRangeSelected(range) },
                label = { Text(range.displayName, style = MaterialTheme.typography.labelMedium) },
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = cs.primaryContainer,
                    selectedLabelColor = cs.onPrimaryContainer
                )
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        HeroStatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.CalendarMonth,
            label = "Active days",
            value = summary?.activeDays?.toString() ?: "--",
            containerColor = cs.primaryContainer.copy(alpha = 0.7f),
            contentColor = cs.onPrimaryContainer
        )
    }
}

@Composable
private fun HeroStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    containerColor: Color,
    contentColor: Color
) {
    Box(
        modifier = modifier
            .height(118.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(containerColor)
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor.copy(alpha = 0.45f),
            modifier = Modifier
                .size(32.dp)
                .align(Alignment.TopEnd)
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.75f)
            )
        }
    }
}

// â”€â”€ Timeline section â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun ListeningTimelineSection(
    summary: PlaybackStatsCalculator.PlaybackStatsSummary?,
    selectedMetric: TimelineMetric,
    onMetricSelected: (TimelineMetric) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Listening timeline",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimelineMetric.values().forEach { metric ->
                AnimatedSelectableChip(
                    selected = metric == selectedMetric,
                    onClick = { onMetricSelected(metric) },
                    label = { Text(metric.displayName, style = MaterialTheme.typography.labelSmall) },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
        val timeline = summary?.timeline.orEmpty()
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            )
        ) {
            if (timeline.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No listening data yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                TimelineVerticalBarChart(
                    timeline = timeline,
                    selectedMetric = selectedMetric,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun TimelineVerticalBarChart(
    timeline: List<PlaybackStatsCalculator.TimelineEntry>,
    selectedMetric: TimelineMetric,
    modifier: Modifier = Modifier
) {
    val maxValue = timeline.maxOf { metricValue(it, selectedMetric) }.coerceAtLeast(1L)
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(timeline, selectedMetric) {
        ready = false
        kotlinx.coroutines.delay(50)
        ready = true
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val barWidth = ((maxWidth - 16.dp) / timeline.size.coerceAtLeast(1)) - 4.dp
            LazyRow(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                itemsIndexed(timeline) { index, entry ->
                    val fraction = metricValue(entry, selectedMetric).toFloat() / maxValue.toFloat()
                    val isPeak = fraction >= 0.98f
                    val animProg by animateFloatAsState(
                        targetValue = if (ready) fraction else 0f,
                        animationSpec = spring(
                            dampingRatio = 0.75f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "bar-$index"
                    )
                    Box(
                        modifier = Modifier
                            .width(barWidth.coerceAtLeast(8.dp))
                            .fillMaxHeight(animProg.coerceAtLeast(0.02f))
                            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                            .background(
                                if (isPeak) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val step = (timeline.size / 4).coerceAtLeast(1)
            timeline.filterIndexed { idx, _ -> idx % step == 0 || idx == timeline.lastIndex }
                .take(5)
                .forEach { entry ->
                    Text(
                        text = entry.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
        }
    }
}

private fun metricValue(entry: PlaybackStatsCalculator.TimelineEntry, metric: TimelineMetric): Long =
    entry.totalDurationMs

// â”€â”€ Category metrics section â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun CategoryMetricsSection(
    summary: PlaybackStatsCalculator.PlaybackStatsSummary?,
    selectedDimension: CategoryDimension,
    onDimensionSelected: (CategoryDimension) -> Unit,
    mediaBaseUrl: String
) {
    val palette = dimensionPalette(selectedDimension)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Top categories",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CategoryDimension.values().forEach { dim ->
                val dp = dimensionPalette(dim)
                AnimatedSelectableChip(
                    selected = dim == selectedDimension,
                    onClick = { onDimensionSelected(dim) },
                    label = { Text(dim.displayName, style = MaterialTheme.typography.labelSmall) },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = dp.accent,
                        selectedLabelColor = dp.accentOn,
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
        val entries = buildCategoryEntries(summary, selectedDimension)
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = palette.container)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = selectedDimension.cardTitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.content.copy(alpha = 0.65f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (entries.isEmpty()) {
                    Text(
                        text = "Play more music to see stats",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.content.copy(alpha = 0.55f),
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                } else {
                    CategoryHorizontalBarChart(
                        entries = entries,
                        palette = palette,
                        mediaBaseUrl = mediaBaseUrl
                    )
                }
            }
        }
    }
}

private fun buildCategoryEntries(
    summary: PlaybackStatsCalculator.PlaybackStatsSummary?,
    dimension: CategoryDimension
): List<CategoryEntry> {
    summary ?: return emptyList()
    return when (dimension) {
        CategoryDimension.Song -> summary.topSongs.map { s ->
            CategoryEntry(s.title, s.artist, s.totalDurationMs, s.albumArtUrl)
        }
        CategoryDimension.Artist -> summary.topArtists.map { a ->
            CategoryEntry(a.artist, "${a.playCount} plays Â· ${a.uniqueSongs} songs", a.totalDurationMs)
        }
        CategoryDimension.Album -> summary.topAlbums.map { al ->
            CategoryEntry(al.album, "${al.playCount} plays", al.totalDurationMs, al.albumArtUrl)
        }
        CategoryDimension.Genre -> summary.topGenres.map { g ->
            CategoryEntry(g.genre, formatDuration(g.totalDurationMs), g.totalDurationMs)
        }
    }
}

@Composable
private fun dimensionPalette(dimension: CategoryDimension): DimPalette {
    val cs = MaterialTheme.colorScheme
    return when (dimension) {
        CategoryDimension.Song -> DimPalette(
            cs.primaryContainer.copy(alpha = 0.6f), cs.onPrimaryContainer,
            cs.primaryContainer, cs.onPrimaryContainer
        )
        CategoryDimension.Artist -> DimPalette(
            cs.tertiaryContainer.copy(alpha = 0.6f), cs.onTertiaryContainer,
            cs.tertiaryContainer, cs.onTertiaryContainer
        )
        CategoryDimension.Album -> DimPalette(
            cs.secondaryContainer.copy(alpha = 0.6f), cs.onSecondaryContainer,
            cs.secondaryContainer, cs.onSecondaryContainer
        )
        CategoryDimension.Genre -> DimPalette(
            cs.surfaceContainerHigh, cs.onSurface,
            cs.primary, cs.onPrimary
        )
    }
}

@Composable
private fun CategoryHorizontalBarChart(
    entries: List<CategoryEntry>,
    palette: DimPalette,
    mediaBaseUrl: String
) {
    val maxDuration = entries.maxOfOrNull { it.durationMs }?.coerceAtLeast(1L) ?: 1L
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        entries.take(5).forEachIndexed { index, entry ->
            val targetProgress = (entry.durationMs.toFloat() / maxDuration.toFloat()).coerceIn(0f, 1f)
            val progress by animateFloatAsState(
                targetValue = targetProgress,
                animationSpec = tween(380 + index * 60),
                label = "cat-bar-$index"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(palette.container, palette.accent.copy(alpha = 0.08f))
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val artModel = remember(entry.albumArtUrl, mediaBaseUrl) {
                        resolveStatsAlbumArtModel(entry.albumArtUrl, mediaBaseUrl)
                    }
                    if (artModel != null) {
                        AsyncImage(
                            model = artModel,
                            contentDescription = null,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            alpha = 0.96f,
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(
                            modifier = Modifier.size(28.dp),
                            shape = CircleShape,
                            color = palette.accent.copy(alpha = 0.25f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.accentOn
                                )
                            }
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = entry.label,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.content,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (entry.supporting.isNotBlank()) {
                            Text(
                                text = entry.supporting,
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.content.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = palette.accent,
                            trackColor = palette.container
                        )
                    }
                    Text(
                        text = formatDuration(entry.durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.content.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

// â”€â”€ Listening habits card â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun ListeningHabitsCard(summary: PlaybackStatsCalculator.PlaybackStatsSummary?) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Listening habits",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            HabitMetric(
                icon = Icons.Outlined.History,
                label = "Sessions",
                value = summary?.totalSessions?.toString() ?: "--"
            )
            HabitMetric(
                icon = Icons.Outlined.Hearing,
                label = "Avg session",
                value = if (summary != null) formatDuration(summary.averageSessionDurationMs) else "--"
            )
            HabitMetric(
                icon = Icons.Outlined.CalendarMonth,
                label = "Longest streak",
                value = if (summary != null) "${summary.longestStreakDays} days" else "--"
            )
            HabitMetric(
                icon = Icons.Outlined.AutoGraph,
                label = "Sessions / day",
                value = if (summary != null) String.format(Locale.US, "%.1f", summary.averageSessionsPerDay) else "--"
            )
            if (summary?.peakDayLabel != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Bolt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Peak day Â· ${summary.peakDayLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = formatDuration(summary.peakDayDurationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun HabitMetric(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// â”€â”€ Top genres section â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun TopGenresSection(summary: PlaybackStatsCalculator.PlaybackStatsSummary?) {
    if (summary == null || summary.topGenres.isEmpty()) return
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Top genres",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            val maxDuration = summary.topGenres.maxOf { it.totalDurationMs }.coerceAtLeast(1L)
            summary.topGenres.take(5).forEachIndexed { index, genre ->
                val target = (genre.totalDurationMs.toFloat() / maxDuration.toFloat()).coerceIn(0f, 1f)
                val progress by animateFloatAsState(
                    targetValue = target,
                    animationSpec = tween(370 + index * 65),
                    label = "genre-$index"
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = genre.genre,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = formatDuration(genre.totalDurationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = MaterialTheme.colorScheme.tertiary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                }
            }
        }
    }
}

// â”€â”€ Daily rhythm section â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun DailyRhythmSection(summary: PlaybackStatsCalculator.PlaybackStatsSummary?) {
    val distribution = summary?.dayListeningDistribution
    if (distribution == null || distribution.buckets.isEmpty()) return
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Daily rhythm",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            val topBuckets = distribution.buckets
                .sortedByDescending { it.totalDurationMs }
                .take(4)
            topBuckets.forEachIndexed { index, bucket ->
                val share = (bucket.totalDurationMs.toFloat() /
                        distribution.maxBucketDurationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                val animated by animateFloatAsState(
                    targetValue = share,
                    animationSpec = tween(350 + index * 55),
                    label = "rhythm-$index"
                )
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
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
                            modifier = Modifier.widthIn(min = 64.dp)
                        )
                        LinearProgressIndicator(
                            progress = { animated },
                            modifier = Modifier
                                .weight(1f)
                                .height(7.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                        Text(
                            text = formatDuration(bucket.totalDurationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

// â”€â”€ Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

private fun resolveStatsAlbumArtModel(
    rawAlbumArtUrl: String?,
    mediaBaseUrl: String
): Any? {
    val source = rawAlbumArtUrl?.trim().orEmpty()
    if (source.isBlank()) return null

    return when {
        source.startsWith("http://", ignoreCase = true) ||
            source.startsWith("https://", ignoreCase = true) ||
            source.startsWith("content://", ignoreCase = true) ||
            source.startsWith("file://", ignoreCase = true) ||
            source.startsWith("android.resource://", ignoreCase = true) -> Uri.parse(source)

        source.startsWith("/") -> {
            val normalizedBase = mediaBaseUrl.trimEnd('/')
            Uri.parse("$normalizedBase$source")
        }

        else -> {
            val localFile = File(source)
            if (localFile.isAbsolute) localFile else source
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalMinutes = durationMs / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

private fun formatMinuteLabel(startMinute: Int): String {
    val safe = startMinute.coerceIn(0, 23 * 60 + 59)
    val hour = safe / 60
    val minute = safe % 60
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val period = if (hour < 12) "AM" else "PM"
    return String.format("%02d:%02d %s", displayHour, minute, period)
}

@Composable
private fun AnimatedSelectableChip(
    selected: Boolean = false,
    onClick: () -> Unit = {},
    label: @Composable () -> Unit = {},
    shape: androidx.compose.ui.graphics.Shape = CircleShape,
    colors: androidx.compose.material3.SelectableChipColors = FilterChipDefaults.filterChipColors()
) {
    val scale by animateFloatAsState(targetValue = if (selected) 1f else 0.96f)
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        shape = shape,
        colors = colors,
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    )
}

@Composable
private fun AnimatedSection(index: Int, content: @Composable () -> Unit) {
    val enter = fadeIn(tween(300 + index * 40)) + slideInVertically(tween(300 + index * 40)) { it / 6 }
    AnimatedVisibility(
        visible = true,
        enter = enter
    ) {
        content()
    }
}

@Composable
private fun StatsHeroSection(summary: PlaybackStatsCalculator.PlaybackStatsSummary?) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        HeroStatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.MusicNote,
            label = "Top song",
            value = summary?.topSongs?.firstOrNull()?.title ?: "--",
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
        HeroStatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.Album,
            label = "Top album",
            value = summary?.topAlbums?.firstOrNull()?.album ?: "--",
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
        HeroStatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.AutoGraph,
            label = "Total time",
            value = summary?.totalDurationMs?.let { formatDuration(it) } ?: "--",
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}
