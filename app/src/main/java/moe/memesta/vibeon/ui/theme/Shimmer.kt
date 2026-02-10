package moe.memesta.vibeon.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Premium Shimmer Effect Modifier - Performance Optimized
 */
fun Modifier.shimmerEffect(
    tintColor: Color? = null
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    
    // Animate translation value
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200, 
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translation"
    )

    // Base color for the shimmer wave
    val baseContentColor = tintColor ?: Color.White

    val shimmerColors = remember(baseContentColor, tintColor) {
        listOf(
            baseContentColor.copy(alpha = 0.0f),
            baseContentColor.copy(alpha = if (tintColor != null) 0.25f else 0.15f), 
            baseContentColor.copy(alpha = 0.0f),
        )
    }

    this.drawBehind {
        val brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(translateAnimation - 200f, translateAnimation - 200f),
            end = Offset(translateAnimation + 200f, translateAnimation + 200f)
        )
        drawRect(brush = brush)
    }
}

