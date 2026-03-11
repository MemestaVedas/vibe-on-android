package moe.memesta.vibeon.ui.shapes

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A wavy shape that creates a sinusoidal wave at the bottom of the container.
 */
class WavyBottomShape(
    private val waveHeight: Dp,
    private val waveFrequency: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val width = size.width
        val height = size.height
        val amplitude = with(density) { waveHeight.toPx() }
        val freq = waveFrequency * 2f * PI.toFloat() / width

        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(width, 0f)
            
            val waveBottomBase = height - amplitude
            for (x in width.toInt() downTo 0 step 5) {
                val angle: Double = ((x.toFloat() * freq)).toDouble()
                val sinValue: Float = sin(angle).toFloat()
                val waveFactor: Float = (sinValue + 1f) * 0.5f 
                val y: Float = waveBottomBase + (waveFactor * amplitude)
                
                if (x == width.toInt()) {
                    lineTo(width, y)
                } else {
                    lineTo(x.toFloat(), y)
                }
            }
            lineTo(0f, waveBottomBase + ((sin(0.0).toFloat() + 1f) * 0.5f * amplitude))
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * A petal-like shape with specified number of lobes and depth.
 */
class PetalShape(
    private val petals: Int = 8,
    private val depth: Float = 0.15f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val baseRadius = size.width / 2f * (1f - depth)
        val variation = size.width / 2f * depth

        for (i in 0..360 step 5) {
            val angle = Math.toRadians(i.toDouble())
            val r = baseRadius + variation * sin(angle * petals).toFloat()
            val x = cx + r * cos(angle).toFloat()
            val y = cy + r * sin(angle).toFloat()
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        return Outline.Generic(path)
    }
}
