package moe.memesta.vibeon.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import moe.memesta.vibeon.data.stats.PlaybackStatsCalculator
import moe.memesta.vibeon.ui.theme.Dimens
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun StatisticsSection(
    summary: PlaybackStatsCalculator.PlaybackStatsSummary?,
    onViewStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (summary == null) {
        PlaceholderStatisticsContent()
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = Dimens.ScreenPadding, top = 8.dp, end = Dimens.ScreenPadding)
            .clickable(onClick = onViewStats),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header row stays lightweight so the module blends into page background.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Listening stats",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = summary.range.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        SineWaveLine(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            animate = true,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
            alpha = 0.92f,
            strokeWidth = 3.dp,
            amplitude = 3.dp,
            waves = 7.6f,
            phase = 0f
        )

        // Content
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main listening duration
            Text(
                text = formatListeningDurationLong(summary.totalDurationMs),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Stats row: Total Plays and Avg per day
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Total plays",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = summary.totalPlayCount.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Avg per day",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatListeningDurationCompact(summary.averageDailyDurationMs),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Top track if available
            val topTrack = summary.topSongs.firstOrNull()
            if (topTrack != null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Top track",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = topTrack.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${topTrack.artist} • ${topTrack.playCount} plays",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Mini timeline
            MiniListeningTimeline(summary)
        }
    }
}

@Composable
private fun SineWaveLine(
    modifier: Modifier = Modifier,
    color: Color = Color.Black,
    alpha: Float = 1f,
    strokeWidth: androidx.compose.ui.unit.Dp = 2.dp,
    amplitude: androidx.compose.ui.unit.Dp = 8.dp,
    waves: Float = 2f,
    phase: Float = 0f,
    animate: Boolean = false,
    animationDurationMillis: Int = 2000,
    samples: Int = 400,
    cap: StrokeCap = StrokeCap.Round
) {
    val density = LocalDensity.current

    val currentPhase = if (animate) {
        val infiniteTransition = rememberInfiniteTransition(label = "StatsSineWave")
        val animatedPhase = infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 2f * PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = animationDurationMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "statsPhase"
        )
        animatedPhase.value
    } else {
        phase
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val centerY = h / 2f
        val strokePx = with(density) { strokeWidth.toPx() }
        val ampPx = with(density) { amplitude.toPx() }

        if (w <= 0f || samples < 2) return@Canvas

        val path = Path().apply {
            val step = w / (samples - 1)
            moveTo(0f, centerY + (ampPx * sin(currentPhase)))
            for (i in 1 until samples) {
                val x = i * step
                val theta = (x / w) * (2f * PI.toFloat() * waves) + currentPhase
                val y = centerY + ampPx * sin(theta)
                lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokePx, cap = cap, join = StrokeJoin.Round),
            alpha = alpha
        )
    }
}

@Composable
private fun PlaceholderStatisticsContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenPadding, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        VibeContainedLoadingIndicator(label = "Loading stats...")
    }
}

@Composable
private fun MiniListeningTimeline(summary: PlaybackStatsCalculator.PlaybackStatsSummary?) {
    val timeline = summary?.timeline ?: emptyList()
    val maxDuration = timeline.maxOfOrNull { it.totalDurationMs }?.takeIf { it > 0 } ?: 1L

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val entries = if (timeline.isEmpty()) {
            List(5) { null }
        } else {
            timeline.takeLast(minOf(7, timeline.size))
        }

        entries.forEach { entry ->
            val heightFraction = entry?.let { it.totalDurationMs.toFloat() / maxDuration.toFloat() }?.coerceIn(0f, 1f) ?: 0.1f
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((70.dp * heightFraction).coerceAtLeast(10.dp))
                        .clip(CircleShape)
                        .background(color = MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = entry?.label ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatListeningDurationLong(durationMs: Long): String {
    val totalMinutes = durationMs / 60000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

private fun formatListeningDurationCompact(durationMs: Long): String {
    val totalMinutes = durationMs / 60000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}