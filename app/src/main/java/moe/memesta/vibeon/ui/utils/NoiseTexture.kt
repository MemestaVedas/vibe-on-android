package moe.memesta.vibeon.ui.utils

import android.graphics.Bitmap
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.random.Random

private fun generateNoiseBitmap(width: Int = 256, height: Int = 256, alpha: Int = 40): Bitmap {
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(width * height)
    val rng = Random(seed = 42)
    for (i in pixels.indices) {
        val gray = rng.nextInt(256)
        pixels[i] = android.graphics.Color.argb(alpha, gray, gray, gray)
    }
    bmp.setPixels(pixels, 0, width, 0, 0, width, height)
    return bmp
}

fun Modifier.noiseTexture(alpha: Int = 40): Modifier = composed {
    val noiseBitmap = remember(alpha) { generateNoiseBitmap(alpha = alpha).asImageBitmap() }

    drawWithContent {
        drawContent()

        val tileWidth = noiseBitmap.width.toFloat()
        val tileHeight = noiseBitmap.height.toFloat()

        var y = 0f
        while (y < size.height) {
            var x = 0f
            while (x < size.width) {
                drawImage(
                    image = noiseBitmap,
                    topLeft = Offset(x, y),
                    blendMode = BlendMode.Overlay
                )
                x += tileWidth
            }
            y += tileHeight
        }
    }
}
