package moe.memesta.vibeon.ui.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

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


// Standardized Squircle shape for albums to fix clipping bugs
val AlbumSquircleShape = RoundedCornerShape(24.dp)
