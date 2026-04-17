package moe.memesta.vibeon.ui.shapes

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
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
            .coerceIn(0f, height * 0.5f)
        val normalizedFrequency = waveFrequency.coerceAtLeast(0.25f)
        val freq = normalizedFrequency * 2f * PI.toFloat() / width
        val sampleStep = (width / 60f).toInt().coerceIn(2, 10)

        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(width, 0f)
            
            val waveBottomBase = height - amplitude
            var x = width.toInt()
            while (x >= 0) {
                val angle: Double = ((x.toFloat() * freq)).toDouble()
                val sinValue: Float = sin(angle).toFloat()
                val waveFactor: Float = (sinValue + 1f) * 0.5f 
                val y: Float = waveBottomBase + (waveFactor * amplitude)
                
                if (x == width.toInt()) {
                    lineTo(width, y)
                } else {
                    lineTo(x.toFloat(), y)
                }
                x -= sampleStep
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
        val normalizedPetals = petals.coerceAtLeast(2)
        val normalizedDepth = depth.coerceIn(0.05f, 0.45f)
        val cx = size.width / 2f
        val cy = size.height / 2f
        val baseRadius = size.width / 2f * (1f - normalizedDepth)
        val variation = size.width / 2f * normalizedDepth

        for (i in 0..360 step 5) {
            val angle = Math.toRadians(i.toDouble())
            val r = baseRadius + variation * sin(angle * normalizedPetals).toFloat()
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

val OrganicBlobVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "OrganicBlobVector",
        defaultWidth = 31.6.dp,
        defaultHeight = 27.8.dp,
        viewportWidth = 316f,
        viewportHeight = 278f
    ).apply {
        addPath(
            pathData = addPathNodes(
                "M271.57 122.2C257.552 100.62 243.535 79.0103 229.517 57.4303C220.423 43.4203 211.167 29.2204 198.872 18.0904C186.576 6.94042 170.648 -0.939579 154.316 0.0904215C139.976 1.01042 126.684 8.72037 116.191 18.7904C105.698 28.8604 97.5464 41.2604 89.5284 53.5404C67.8424 86.7204 46.1303 119.9 24.4443 153.1C14.1393 168.86 3.56535 185.31 0.713353 204.09C-2.73065 226.78 6.55235 249.89 23.0183 264.98C40.2373 280.76 68.1384 279.48 89.0984 275.16C112.075 270.41 134.541 261.48 157.975 261.51C178.047 261.51 197.446 268.11 216.979 272.91C236.485 277.68 257.445 280.62 276.279 273.52C299.659 264.73 316.448 239.73 315.991 214.07C315.56 190.66 302.457 169.75 289.839 150.27C283.758 140.92 277.678 131.55 271.597 122.2H271.57Z"
            ),
            fill = SolidColor(Color(0xFFD0BCFF)),
            stroke = null
        )
    }.build()
}
