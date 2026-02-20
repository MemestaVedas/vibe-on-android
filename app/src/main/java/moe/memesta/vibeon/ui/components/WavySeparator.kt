package moe.memesta.vibeon.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * A static wavy section separator forming stalactites hanging from the top.
 */
@Composable
fun WavySeparator(
    colorTop: Color,
    colorBottom: Color,
    modifier: Modifier = Modifier,
    waveHeight: Dp = 12.dp,
    waveFrequency: Float = 6.5f
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(waveHeight)
    ) {
        val width = size.width
        val height = size.height
        val waveAmplitude = height
        val freq = waveFrequency * 2f * PI.toFloat() / width

        // Fill background first with bottom color
        drawRect(color = colorBottom)

        // Draw top color hanging down
        val path = Path().apply {
            moveTo(0f, 0f) 

            for (x in 0..width.toInt() step 5) {
                val angle: Double = ((x.toFloat() * freq)).toDouble()
                val sinValue: Float = sin(angle).toFloat()
                val waveFactor: Float = (sinValue + 1f) * 0.5f 
                val y: Float = waveFactor * waveAmplitude
                if (x == 0) {
                    lineTo(0f, y) 
                } else {
                    lineTo(x.toFloat(), y)
                }
            }
            
            lineTo(width, 0f) 
            close()
        }

        drawPath(
            path = path,
            color = colorTop
        )
    }
}
