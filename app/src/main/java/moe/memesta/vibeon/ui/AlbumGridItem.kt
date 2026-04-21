@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package moe.memesta.vibeon.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.runtime.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.toShape
import moe.memesta.vibeon.ui.theme.bouncyClickable
import moe.memesta.vibeon.ui.shapes.*
import moe.memesta.vibeon.ui.utils.noiseTexture
import moe.memesta.vibeon.ui.utils.rememberResolvedAlbumMainColor

private data class AlbumCardColors(
    val primary: Color,
    val secondary: Color,
    val onPrimary: Color,
    val onSecondary: Color
)

private fun contentColorFor(color: Color): Color =
    if (color.luminance() > 0.5f) Color.Black else Color.White

private fun deriveSecondaryColor(primary: Color): Color {
    val target = if (primary.luminance() > 0.55f) Color.Black else Color.White
    return lerp(primary, target, 0.22f)
}

@androidx.compose.animation.ExperimentalSharedTransitionApi
@Composable
fun AlbumGridItem(
    albumName: String,
    artistName: String,
    coverUrl: String?,
    albumMainColor: Int?,
    songCount: Int = 0,
    onClick: () -> Unit,
    onAlbumMainColorResolved: (Int) -> Unit,
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null
) {
    val context = LocalContext.current

    val fallbackPrimary = MaterialTheme.colorScheme.primary
    val fallbackPrimaryContainer = MaterialTheme.colorScheme.primaryContainer
    val fallbackOnPrimary = MaterialTheme.colorScheme.onPrimary
    val fallbackOnPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer

    val resolvedMainColor = rememberResolvedAlbumMainColor(
        context = context,
        albumName = albumName,
        artistName = artistName,
        coverUrl = coverUrl,
        storedColor = albumMainColor,
        onPersistColor = onAlbumMainColorResolved
    )

    val cardColors = remember(resolvedMainColor) {
        resolvedMainColor?.let { mainColor ->
            val primary = Color(mainColor)
            val secondary = deriveSecondaryColor(primary)
            AlbumCardColors(
                primary = primary,
                secondary = secondary,
                onPrimary = contentColorFor(primary),
                onSecondary = contentColorFor(secondary)
            )
        }
    }

    val imageModel = remember(coverUrl, context) {
        ImageRequest.Builder(context)
            .data(coverUrl)
            .build()
    }

    val resolvedColors = cardColors ?: AlbumCardColors(
        primary = fallbackPrimary,
        secondary = fallbackPrimaryContainer,
        onPrimary = fallbackOnPrimary,
        onSecondary = fallbackOnPrimaryContainer
    )
    val primaryColor by animateColorAsState(
        targetValue = resolvedColors.primary,
        animationSpec = tween(durationMillis = 280),
        label = "album_primary"
    )
    val sampledSecondary by animateColorAsState(
        targetValue = resolvedColors.secondary,
        animationSpec = tween(durationMillis = 280),
        label = "album_secondary"
    )
    val onPrimaryColor by animateColorAsState(
        targetValue = resolvedColors.onPrimary,
        animationSpec = tween(durationMillis = 220),
        label = "album_on_primary"
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Force square aspect ratio on the entire card
            .then(
                if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                    with(sharedTransitionScope) {
                        Modifier.sharedElement(
                            sharedContentState = rememberSharedContentState(key = "album-${albumName}-grid"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    }
                } else Modifier
            )
            .bouncyClickable(onClick = onClick),
        shape = AlbumSquircleShape.toShape(),
        colors = CardDefaults.cardColors(
            containerColor = primaryColor
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Album Art - takes full space
            if (coverUrl != null) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = albumName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            if (songCount > 0) {
                CornerPeelBadge(
                    songCount = songCount,
                    containerColor = primaryColor,
                    contentColor = onPrimaryColor,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                )
            }
            
            // Gradient overlay and text at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Transparent,
                                    0.40f to Color.Transparent,
                                    0.55f to sampledSecondary.copy(alpha = 0.25f),
                                    0.75f to primaryColor.copy(alpha = 0.78f),
                                    1.0f to primaryColor.copy(alpha = 1.0f)
                                )
                            )
                        )
                        .noiseTexture(alpha = 35)
                )
                
                // Album Info
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                        .align(Alignment.BottomCenter),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = albumName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = onPrimaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Text(
                        text = artistName,
                        style = MaterialTheme.typography.bodySmall,
                        color = onPrimaryColor.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                }
            }
        }
    }
}

@Composable
private fun CornerPeelBadge(
    songCount: Int,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    val foldAccent = remember(containerColor) {
        val target = if (containerColor.luminance() > 0.5f) Color.Black else Color.White
        lerp(containerColor, target, 0.22f)
    }
    val foldShadow = Color.Black.copy(alpha = 0.24f)
    val creaseColor = contentColor.copy(alpha = 0.34f)

    Box(
        modifier = modifier
            .width(60.dp)
            .height(46.dp)
            .drawWithCache {
                val peelWidth = size.width * 0.34f
                val peelDrop = size.height * 0.36f
                val foldInset = size.width * 0.18f
                val foldPath = Path().apply {
                    moveTo(size.width - peelWidth, 0f)
                    lineTo(size.width, peelDrop)
                    lineTo(size.width - foldInset, peelDrop * 1.1f)
                    lineTo(size.width - peelWidth * 1.12f, peelDrop * 0.52f)
                    close()
                }
                val shadowPath = Path().apply {
                    moveTo(size.width - peelWidth, 0f)
                    lineTo(size.width, peelDrop)
                    lineTo(size.width - peelWidth * 0.78f, peelDrop * 1.08f)
                    close()
                }
                val bodyPath = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width - peelWidth, 0f)
                    lineTo(size.width, peelDrop)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }

                onDrawBehind {
                    drawPath(bodyPath, color = containerColor.copy(alpha = 0.96f))
                    drawPath(shadowPath, color = foldShadow)
                    drawPath(foldPath, color = foldAccent.copy(alpha = 0.97f))
                    drawLine(
                        color = creaseColor,
                        start = androidx.compose.ui.geometry.Offset(size.width - peelWidth * 0.92f, peelDrop * 0.62f),
                        end = androidx.compose.ui.geometry.Offset(size.width - peelWidth * 0.08f, peelDrop * 0.04f),
                        strokeWidth = 1.4f
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.12f),
                        start = androidx.compose.ui.geometry.Offset(3f, size.height - 1.5f),
                        end = androidx.compose.ui.geometry.Offset(size.width - peelWidth * 0.55f, size.height - 1.5f),
                        strokeWidth = 1f
                    )
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.padding(start = 7.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.96f),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = songCount.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.W800,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

