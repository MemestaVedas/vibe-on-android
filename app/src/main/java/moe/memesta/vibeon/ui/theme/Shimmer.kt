package moe.memesta.vibeon.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Premium Shimmer Effect Modifier - Performance Optimized
 * Uses drawWithCache to avoid per-frame gradient recreation.
 */
fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    
    // Animate translation value
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200, // Slightly faster for snappier feel
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translation"
    )

    // Use drawWithCache to cache the drawing operations
    this.drawWithCache {
        val shimmerColors = listOf(
            Color.White.copy(alpha = 0.0f),
            Color.White.copy(alpha = 0.15f), // Reduced intensity
            Color.White.copy(alpha = 0.0f),
        )
        
        val brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(translateAnimation - 200f, translateAnimation - 200f),
            end = Offset(translateAnimation + 200f, translateAnimation + 200f)
        )
        
        onDrawBehind {
            drawRect(brush = brush)
        }
    }
}

