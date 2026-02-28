package moe.memesta.vibeon.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MorphingShapesLoader(
    modifier: Modifier = Modifier,
    showLabel: Boolean = true,
    label: String = "Loading..."
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    val transition = rememberInfiniteTransition(label = "morph-loader")

    val rotation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulse = transition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val orbit = transition.animateFloat(
        initialValue = -22f,
        targetValue = 22f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbit"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(96.dp)) {
                val center = center
                val base = size.minDimension * 0.20f
                val dynamicRadius = base * pulse.value

                rotate(rotation.value) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primary.copy(alpha = 0.95f),
                                primary.copy(alpha = 0.25f)
                            ),
                            center = center
                        ),
                        radius = dynamicRadius * 1.28f,
                        center = center.copy(x = center.x + orbit.value, y = center.y - orbit.value * 0.35f),
                        blendMode = BlendMode.SrcOver
                    )

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                secondary.copy(alpha = 0.92f),
                                secondary.copy(alpha = 0.20f)
                            ),
                            center = center
                        ),
                        radius = dynamicRadius,
                        center = center.copy(x = center.x - orbit.value * 0.85f, y = center.y + orbit.value * 0.45f)
                    )

                    drawCircle(
                        color = tertiary.copy(alpha = 0.30f),
                        radius = dynamicRadius * 0.78f,
                        center = center.copy(x = center.x, y = center.y + orbit.value * 0.25f)
                    )
                }

                drawCircle(
                    color = Color.White.copy(alpha = 0.14f),
                    radius = dynamicRadius * 0.46f,
                    center = center
                )
            }
        }

        if (showLabel) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.alpha(0.95f)
            )
        }
    }
}
