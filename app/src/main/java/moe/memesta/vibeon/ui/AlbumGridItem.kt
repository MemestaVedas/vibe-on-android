package moe.memesta.vibeon.ui

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.* 
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.memesta.vibeon.ui.theme.Dimens
import moe.memesta.vibeon.ui.theme.bouncyClickable
import moe.memesta.vibeon.ui.shapes.*

@androidx.compose.animation.ExperimentalSharedTransitionApi
@Composable
fun AlbumGridItem(
    albumName: String,
    artistName: String,
    coverUrl: String?,
    songCount: Int = 0,
    onClick: () -> Unit,
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope? = null,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope? = null
) {
    // Dynamic color state for each album
    var dominantColor by remember(coverUrl) { mutableStateOf<Color?>(null) }
    val defaultColor = MaterialTheme.colorScheme.primary
    val primaryColor = dominantColor ?: defaultColor
    
    // Calculate onPrimary color based on dominant color luminance
    val onPrimaryColor = dominantColor?.let { color ->
        // Calculate luminance of the dominant color
        val r = (color.red * 255).toInt()
        val g = (color.green * 255).toInt()
        val b = (color.blue * 255).toInt()
        val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255
        
        // Use white text on dark colors, black on light colors
        if (luminance > 0.5) Color.Black else Color.White
    } ?: MaterialTheme.colorScheme.onPrimary
    
    // Extract dominant color from album art
    val context = LocalContext.current
    LaunchedEffect(coverUrl) {
        if (coverUrl != null) {
            try {
                withContext(Dispatchers.IO) {
                    val loader = context.imageLoader
                    val request = ImageRequest.Builder(context)
                        .data(coverUrl)
                        .allowHardware(false)
                        .build()
                    
                    val result = loader.execute(request)
                    if (result is SuccessResult) {
                        val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                        bitmap?.let {
                            val palette = Palette.from(it).generate()
                            palette.dominantSwatch?.let { swatch ->
                                dominantColor = Color(swatch.rgb)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback to default color on error
            }
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Force square aspect ratio on the entire card
            .then(
                if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                    with(sharedTransitionScope) {
                        Modifier.sharedElement(
                            sharedContentState = rememberSharedContentState(key = "album-${albumName}-grid"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                    }
                } else Modifier
            )
            .bouncyClickable(onClick = onClick),
        shape = AlbumSquircleShape,
        colors = CardDefaults.cardColors(
            containerColor = primaryColor
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Album Art - takes full space
            if (coverUrl != null) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = albumName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
            
            // Gradient overlay and text at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    primaryColor
                                )
                            )
                        )
                )
                
                // Album Info
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                        .align(Alignment.BottomCenter),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = albumName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = onPrimaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Text(
                        text = artistName,
                        style = MaterialTheme.typography.bodySmall,
                        color = onPrimaryColor.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (songCount > 0) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$songCount Songs",
                            style = MaterialTheme.typography.bodySmall,
                            color = onPrimaryColor.copy(alpha = 0.8f),
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

