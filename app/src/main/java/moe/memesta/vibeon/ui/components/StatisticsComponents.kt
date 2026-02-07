package moe.memesta.vibeon.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatisticsSection(
    stats: moe.memesta.vibeon.data.LibraryStats?
) {
    if (stats == null) {
        // Show loading skeleton or return
        return
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Header
        Text(
            text = "Your Statistics",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "See how you've been vibing ✨",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 20.dp)
        )
        
        // Statistics Grid (2x2)
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatisticsBadge(
                    label = "Total Songs",
                    value = stats.totalSongs.toString(),
                    color = Color(0xFFE85D75), // Pink/Rose
                    modifier = Modifier.weight(1f)
                )
                StatisticsBadge(
                    label = "Times Played",
                    value = "—", // Placeholder for future play count tracking
                    color = Color(0xFF9B8B9E), // Mauve/Purple
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatisticsBadge(
                    label = "Total Time",
                    value = "${stats.totalDurationHours.toInt()}h",
                    color = Color(0xFFB8955C), // Gold
                    modifier = Modifier.weight(1f)
                )
                StatisticsBadge(
                    label = "Artists",
                    value = stats.totalArtists.toString(),
                    color = Color(0xFFE8936B), // Coral/Salmon
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StatisticsBadge(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Star background
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = moe.memesta.vibeon.R.drawable.ic_star_badge),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(color)
        )
        
        // Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 40.sp
                ),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}
