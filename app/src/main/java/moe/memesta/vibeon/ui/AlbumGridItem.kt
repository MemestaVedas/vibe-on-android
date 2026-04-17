package moe.memesta.vibeon.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import coil.compose.AsyncImage
import moe.memesta.vibeon.ui.theme.bouncyClickable
import moe.memesta.vibeon.ui.shapes.*
import moe.memesta.vibeon.ui.utils.AlbumArtColorCache
import moe.memesta.vibeon.ui.utils.PaletteUtils

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
    songCount: Int = 0,
    onClick: () -> Unit,
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null
) {
    val fallbackPrimary = MaterialTheme.colorScheme.primary
    val fallbackPrimaryContainer = MaterialTheme.colorScheme.primaryContainer
    val fallbackOnPrimary = MaterialTheme.colorScheme.onPrimary
    val fallbackOnPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer

    var cardColors by remember(coverUrl) {
        mutableStateOf(
            coverUrl?.let { url ->
                AlbumArtColorCache.get(url)?.let { cachedPrimary ->
                    val cachedSecondary = deriveSecondaryColor(cachedPrimary)
                    AlbumCardColors(
                        primary = cachedPrimary,
                        secondary = cachedSecondary,
                        onPrimary = contentColorFor(cachedPrimary),
                        onSecondary = contentColorFor(cachedSecondary)
                    )
                }
            }
        )
    }

    val resolvedColors = cardColors ?: AlbumCardColors(
        primary = fallbackPrimary,
        secondary = fallbackPrimaryContainer,
        onPrimary = fallbackOnPrimary,
        onSecondary = fallbackOnPrimaryContainer
    )
    val primaryColor = resolvedColors.primary
    val sampledSecondary = resolvedColors.secondary
    val onPrimaryColor = resolvedColors.onPrimary
    val pillContainerColor = remember(primaryColor) {
        val target = if (primaryColor.luminance() > 0.5f) Color.Black else Color.White
        lerp(primaryColor, target, 0.26f)
    }
    val onPillContainerColor = contentColorFor(pillContainerColor)
    
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
        shape = AlbumSquircleShape,
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
                    model = coverUrl,
                    contentDescription = albumName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onSuccess = { success ->
                        if (cardColors == null) {
                            val extractedColors = PaletteUtils.extractColors(success.result.drawable)
                            val extractedPrimary = when {
                                extractedColors.vibrant.alpha > 0f -> extractedColors.vibrant
                                extractedColors.muted.alpha > 0f -> extractedColors.muted
                                else -> fallbackPrimary
                            }
                            val extractedSecondary = when {
                                extractedColors.muted.alpha > 0f -> extractedColors.muted
                                extractedColors.vibrant.alpha > 0f -> deriveSecondaryColor(extractedPrimary)
                                else -> fallbackPrimaryContainer
                            }
                            cardColors = AlbumCardColors(
                                primary = extractedPrimary,
                                secondary = extractedSecondary,
                                onPrimary = if (extractedColors.onVibrant.alpha > 0f) extractedColors.onVibrant else contentColorFor(extractedPrimary),
                                onSecondary = if (extractedColors.onMuted.alpha > 0f) extractedColors.onMuted else contentColorFor(extractedSecondary)
                            )
                            coverUrl?.let { url -> AlbumArtColorCache.put(url, extractedPrimary) }
                        }
                    }
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
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp),
                    shape = RoundedCornerShape(999.dp),
                    color = pillContainerColor.copy(alpha = 0.94f),
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(start = 4.dp, end = 6.dp, top = 3.dp, bottom = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = onPillContainerColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Surface(
                            shape = CircleShape,
                            color = primaryColor,
                            tonalElevation = 0.dp
                        ) {
                            Box(
                                modifier = Modifier.size(26.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = songCount.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.W700,
                                    color = onPrimaryColor,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
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
                                    0.18f to Color.Transparent,
                                    0.40f to sampledSecondary.copy(alpha = 0.18f),
                                    0.68f to primaryColor.copy(alpha = 0.78f),
                                    1.0f to primaryColor.copy(alpha = 0.98f)
                                )
                            )
                        )
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

