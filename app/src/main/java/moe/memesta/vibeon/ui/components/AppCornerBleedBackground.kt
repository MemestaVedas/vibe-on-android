package moe.memesta.vibeon.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Subtle app-wide corner bleed similar to YouTube Music.
 * Drawn once at the root so all screens inherit the same ambient tone.
 */
@Composable
fun AppCornerBleedBackground(
    albumMainColor: Int?,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val backgroundColor = colorScheme.background
    val fallbackPalette = AlbumPalette(
        primary = colorScheme.primary,
        secondary = colorScheme.tertiary,
        tertiary = colorScheme.secondary,
        onPrimary = colorScheme.onBackground,
        surface = colorScheme.background
    )

    var palette by remember { mutableStateOf(fallbackPalette) }

    LaunchedEffect(albumMainColor) {
        palette = if (albumMainColor != null) {
            extractAlbumPalette(albumMainColor)
        } else {
            fallbackPalette
        }
    }

    val topLeftColor by animateColorAsState(
        targetValue = palette.primary.copy(alpha = 0.14f),
        animationSpec = tween(durationMillis = 900),
        label = "bleed_top_left"
    )
    val topRightColor by animateColorAsState(
        targetValue = palette.secondary.copy(alpha = 0.12f),
        animationSpec = tween(durationMillis = 900),
        label = "bleed_top_right"
    )
    val bottomRightColor by animateColorAsState(
        targetValue = palette.tertiary.copy(alpha = 0.10f),
        animationSpec = tween(durationMillis = 900),
        label = "bleed_bottom_right"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(color = backgroundColor)

                val radius = size.maxDimension * 0.78f

                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(topLeftColor, Color.Transparent),
                        center = Offset(0f, 0f),
                        radius = radius
                    )
                )

                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(topRightColor, Color.Transparent),
                        center = Offset(size.width, 0f),
                        radius = radius
                    )
                )

                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(bottomRightColor, Color.Transparent),
                        center = Offset(size.width, size.height),
                        radius = radius * 0.85f
                    )
                )
            }
    )
}
