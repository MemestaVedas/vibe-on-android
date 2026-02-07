package moe.memesta.vibeon.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import moe.memesta.vibeon.data.TrackInfo
import moe.memesta.vibeon.ui.theme.VibeAnimations
import moe.memesta.vibeon.ui.theme.shimmerEffect

@Composable
fun SquareTrackCard(
    track: TrackInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlight: Float = 1f
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val targetScale = if (pressed) 0.98f else highlight
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = VibeAnimations.SpringExpressive,
        label = "trackCardScale"
    )
    val emphasis = ((highlight - 0.94f) / 0.06f).coerceIn(0f, 1f)
    val targetAlpha = 0.86f + (emphasis * 0.14f)
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = VibeAnimations.SpringStandard,
        label = "trackCardAlpha"
    )

    Column(
        modifier = modifier
            .width(140.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current
            ) { onClick() }
    ) {
        // Art
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        ) {
            if (track.coverUrl != null) {
                AsyncImage(
                    model = track.coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = track.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artist,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun GridTrackCard(
    track: TrackInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp) // Fixed height for consistency
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Art
        Box(
            modifier = Modifier
                .size(56.dp) // Smaller fixed size
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        ) {
            if (track.coverUrl != null) {
                AsyncImage(
                    model = track.coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodySmall, // Smaller text
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AlbumCard(
    albumName: String,
    coverUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlight: Float = 1f
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val targetScale = if (pressed) 0.98f else highlight
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = VibeAnimations.SpringExpressive,
        label = "albumCardScale"
    )
    val emphasis = ((highlight - 0.94f) / 0.06f).coerceIn(0f, 1f)
    val targetAlpha = 0.86f + (emphasis * 0.14f)
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = VibeAnimations.SpringStandard,
        label = "albumCardAlpha"
    )

    Column(
        modifier = modifier
            .width(140.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current
            ) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            if (coverUrl != null) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = albumName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = albumName.take(1).uppercase(),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = albumName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ArtistPill(
    artistName: String,
    photoUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlight: Float = 1f
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val targetScale = if (pressed) 0.98f else highlight
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = VibeAnimations.SpringExpressive,
        label = "artistPillScale"
    )
    val emphasis = ((highlight - 0.94f) / 0.06f).coerceIn(0f, 1f)
    val targetAlpha = 0.86f + (emphasis * 0.14f)
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = VibeAnimations.SpringStandard,
        label = "artistPillAlpha"
    )

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = CircleShape,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current
            ) { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (photoUrl != null) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = artistName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = artistName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = artistName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun SkeletonSquareCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.width(140.dp)
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .height(12.dp)
                .fillMaxWidth(0.8f)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .height(10.dp)
                .fillMaxWidth(0.55f)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .shimmerEffect()
        )
    }
}

@Composable
fun SkeletonAlbumCard(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.width(140.dp)
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .height(12.dp)
                .fillMaxWidth(0.7f)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .shimmerEffect()
        )
    }
}

@Composable
fun SkeletonArtistPill(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(48.dp)
            .width(160.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .shimmerEffect()
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .height(12.dp)
                .fillMaxWidth(0.6f)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
        )
    }
}
