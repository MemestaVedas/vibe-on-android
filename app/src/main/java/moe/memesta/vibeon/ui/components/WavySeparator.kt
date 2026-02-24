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
    waveFrequency: Float = 6.5f,
    showAtTop: Boolean = true // If true, hangs from top. If false, rises from bottom.
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(waveHeight)
    ) {
        val width = size.width
        val height = size.height
        val waveAmplitude = waveHeight.toPx()
        val freq = waveFrequency * 2f * PI.toFloat() / width

        // Fill background first with bottom color
        drawRect(color = colorBottom)

        val path = Path().apply {
            if (showAtTop) {
                // Classic stalactite style hanging from the top (y=0)
                moveTo(0f, 0f)
                for (x in 0..width.toInt() step 5) {
                    val angle = ((x.toFloat() * freq)).toDouble()
                    val sinValue = sin(angle).toFloat()
                    val waveFactor = (sinValue + 1f) * 0.5f 
                    val y = waveFactor * waveAmplitude
                    if (x == 0) lineTo(0f, y) else lineTo(x.toFloat(), y)
                }
                lineTo(width, 0f)
            } else {
                // Rising wave style filling from the curve down to the bottom (y=height)
                moveTo(0f, height)
                for (x in 0..width.toInt() step 5) {
                    val angle = ((x.toFloat() * freq)).toDouble()
                    val sinValue = sin(angle).toFloat()
                    val waveFactor = (sinValue + 1f) * 0.5f 
                    val y = waveFactor * waveAmplitude
                    if (x == 0) lineTo(0f, y) else lineTo(x.toFloat(), y)
                }
                lineTo(width, height)
            }
            close()
        }

        drawPath(
            path = path,
            color = colorTop
        )
    }
}
