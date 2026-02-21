package moe.memesta.vibeon.ui.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

class SvgShape(private val pathString: String, private val viewportWidth: Float, private val viewportHeight: Float) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = PathParser().parsePathString(pathString).toPath()
        
        val scaleX = size.width / viewportWidth
        val scaleY = size.height / viewportHeight
        
        val scaledPath = Path().apply {
            addPath(path)
            // Scale the path to fit the size
            val matrix = androidx.compose.ui.graphics.Matrix()
            matrix.scale(scaleX, scaleY)
            transform(matrix)
        }
        
        return Outline.Generic(scaledPath)
    }
}

// Custom Squircle shape for albums
val AlbumSquircleShape = SvgShape(
    pathString = "M320 172C320 216.72 320 239.08 312.98 256.81C302.81 282.49 282.49 302.81 256.81 312.98C239.08 320 216.72 320 172 320H148C103.28 320 80.9199 320 63.1899 312.98C37.5099 302.81 17.19 282.49 7.02002 256.81C1.95503e-05 239.08 0 216.72 0 172V148C0 103.28 1.95503e-05 80.92 7.02002 63.19C17.19 37.515 37.5099 17.187 63.1899 7.02197C80.9199 -2.71797e-05 103.28 0 148 0H172C216.72 0 239.08 -2.71797e-05 256.81 7.02197C282.49 17.187 302.81 37.515 312.98 63.19C320 80.92 320 103.28 320 148V172Z",
    viewportWidth = 320f,
    viewportHeight = 320f
)
