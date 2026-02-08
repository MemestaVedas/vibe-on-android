@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package moe.memesta.vibeon.ui

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.Canvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.launch
import moe.memesta.vibeon.ui.theme.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlin.math.abs

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NowPlayingScreen(
    playbackViewModel: PlaybackViewModel,
    connectionViewModel: ConnectionViewModel,
    onBackPressed: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val playbackState by playbackViewModel.playbackState.collectAsState()
    val currentTrack by connectionViewModel.currentTrack.collectAsState()
    val isPlaying by connectionViewModel.isPlaying.collectAsState()
    val isMobilePlayback by playbackViewModel.isMobilePlayback.collectAsState()

    // 3 Pages: 0=Queue, 1=NowPlaying, 2=Lyrics
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VibeBackground)
    ) {
        // State for drag interaction
        var isDraggingSeek by remember { mutableStateOf(false) }

        // Pager handles horizontal swipes for Queue/Lyrics
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !isDraggingSeek, // Disable swipe gestures when dragging seek bar
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> QueueScreen(connectionViewModel)
                1 -> {
                    // Main Player - No gesture wrapper that would block buttons
                    NowPlayingView(
                        title = currentTrack.title,
                        artist = currentTrack.artist,
                        isPlaying = isPlaying,
                        progress = playbackState.progress,
                        duration = playbackState.duration,
                        coverUrl = currentTrack.coverUrl,
                        isMobilePlayback = isMobilePlayback,
                        onPlayPauseToggle = {
                            if (isPlaying) connectionViewModel.pause() else connectionViewModel.play()
                        },
                        onSkipNext = { 
                            android.util.Log.i("NowPlayingScreen", "⏭️ Skip Next button pressed")
                            connectionViewModel.next() 
                        },
                        onSkipPrevious = { 
                            android.util.Log.i("NowPlayingScreen", "⏮️ Skip Previous button pressed")
                            connectionViewModel.previous() 
                        },
                        onSeek = { progress -> 
                            // Duration is in MS, seek expects S
                            val positionSecs = (progress * playbackState.duration) / 1000.0
                            connectionViewModel.seek(positionSecs)
                        },
                        onBackToLibrary = onBackPressed,
                        onTogglePlaybackLocation = {
                            if (isMobilePlayback) playbackViewModel.stopMobilePlayback()
                            else playbackViewModel.requestMobilePlayback()
                        },
                        onLyricsClick = {
                            scope.launch { pagerState.animateScrollToPage(2) }
                        },
                        onQueueClick = {
                            scope.launch { pagerState.animateScrollToPage(0) }
                        },
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onSeekDragStart = { isDraggingSeek = true },
                        onSeekDragEnd = { isDraggingSeek = false }
                    )
                }
                2 -> LyricsScreen(
                    connectionViewModel, 
                    playbackViewModel,
                    onBack = { scope.launch { pagerState.animateScrollToPage(1) } }
                )
            }
        }
        
        // Page Indicator
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(3) { iteration ->
                val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f)
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(if (pagerState.currentPage == iteration) 8.dp else 6.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NowPlayingView(
    title: String,
    artist: String,
    isPlaying: Boolean,
    progress: Float,
    duration: Long = 0,
    coverUrl: String? = null,
    isMobilePlayback: Boolean = false,
    onPlayPauseToggle: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeek: (Float) -> Unit,
    onBackToLibrary: () -> Unit,
    onTogglePlaybackLocation: () -> Unit,
    onLyricsClick: () -> Unit,
    onQueueClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onSeekDragStart: () -> Unit = {},
    onSeekDragEnd: () -> Unit = {}
) {
    
    // Derived Colors from MaterialTheme (set by DynamicTheme in MainActivity)
    val vibrantColor = MaterialTheme.colorScheme.primary
    val bgGradientStart = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
    val primaryControlColor = vibrantColor
    val onPrimaryControlColor = MaterialTheme.colorScheme.onPrimary
    
    // Main Container with Background
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // blurred background
        if (!coverUrl.isNullOrEmpty()) {
             AsyncImage(
                model = coverUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(radius = 80.dp)
                    .scale(1.2f)
                    .alpha(0.5f), // Reduced alpha for better text contrast
                contentScale = ContentScale.Crop
            )
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.4f),
                                bgGradientStart,
                                VibeBackground
                            )
                        )
                    )
            )
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackToLibrary) {
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Dismiss",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PLAYING FROM LIBRARY",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                        letterSpacing = 1.sp
                    )
                }
                
                IconButton(onClick = { /* More options */ }) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = "More",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

                // Album Art (Bigger)
            with(sharedTransitionScope) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth(1f) // Max width
                        .aspectRatio(1f)
                        .sharedElement(
                            state = rememberSharedContentState(key = "album_art_shared"),
                            animatedVisibilityScope = animatedVisibilityScope,
                            boundsTransform = { _, _ ->
                                androidx.compose.animation.core.tween(durationMillis = 500)
                            }
                        )
                        .graphicsLayer {
                            shadowElevation = 24.dp.toPx()
                            shape = RoundedCornerShape(32.dp) // Slightly tighter corners for bigger art
                            clip = true
                        }
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                     // Glow effect behind
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(y = 12.dp)
                            .blur(32.dp)
                            .background(vibrantColor.copy(alpha = 0.3f))
                    )
                    
                    if (!coverUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = coverUrl,
                            contentDescription = "Album Art",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(32.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(32.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(VibeSurfaceContainer)
                                .clip(RoundedCornerShape(32.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(80.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // Track Info
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = title.ifEmpty { "No Track" },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = artist.ifEmpty { "Unknown Artist" },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Badges
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // FLAC Badge should use prominent color but ensure it's visible
                    Badge(text = "FLAC", color = vibrantColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "44.1 kHz • 16 bit",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    // Lyrics Button
                    Surface(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = CircleShape,
                        modifier = Modifier.clickable { onLyricsClick() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Lyrics,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Lyrics",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Squiggly Progress Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                SquigglyProgressBar(
                    progress = progress,
                    thumbColor = vibrantColor,
                    trackColor = Color.White.copy(alpha = 0.2f),
                    activeTrackColor = vibrantColor,
                    onSeek = onSeek,
                    onDragStart = onSeekDragStart,
                    onDragEnd = onSeekDragEnd
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime((progress * duration).toLong()),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Text(
                        text = formatTime(duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp)) // Reduced spacing to bring controls closer

            // Main Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = { /* Shuffle */ }) {
                    Icon(Icons.Rounded.Shuffle, null, tint = Color.White.copy(alpha = 0.6f))
                }
                // Previous
                Surface(
                    onClick = onSkipPrevious,
                    modifier = Modifier.size(64.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Play/Pause
                Surface(
                    onClick = onPlayPauseToggle,
                    modifier = Modifier.size(88.dp),
                    shape = RoundedCornerShape(32.dp),
                    color = primaryControlColor
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = onPrimaryControlColor,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                // Next
                Surface(
                    onClick = onSkipNext,
                    modifier = Modifier.size(64.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                
                IconButton(onClick = { /* Repeat */ }) {
                    Icon(Icons.Rounded.Repeat, null, tint = Color.White.copy(alpha = 0.6f))
                }
            }

            Spacer(modifier = Modifier.height(24.dp)) // Fixed spacing after controls

            // Bottom Controls (Volume / Output)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                 Row(
                   modifier = Modifier
                       .clip(RoundedCornerShape(16.dp))
                       .background(Color.White.copy(alpha = 0.1f))
                       .padding(horizontal = 16.dp, vertical = 12.dp)
                       .clickable { onTogglePlaybackLocation() },
                   verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isMobilePlayback) Icons.Rounded.Smartphone else Icons.Rounded.SpeakerGroup,
                        contentDescription = null,
                        tint = vibrantColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isMobilePlayback) "Mobile" else "PC",
                        color = vibrantColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Row(
                   modifier = Modifier
                       .clip(RoundedCornerShape(16.dp))
                       .background(Color.White.copy(alpha = 0.1f))
                       .padding(horizontal = 16.dp, vertical = 12.dp)
                       .clickable { onQueueClick() },
                   verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.QueueMusic,
                        contentDescription = null,
                        tint = vibrantColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Queue",
                        color = vibrantColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Custom Waveform-like Progress Bar
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SquigglyProgressBar(
    progress: Float,
    thumbColor: Color,
    trackColor: Color,
    activeTrackColor: Color,
    onSeek: (Float) -> Unit,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {}
) {
    val density = LocalDensity.current
    var width by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(progress) }

    // Use dragProgress while dragging, otherwise use the external progress
    val currentProgress = (if (isDragging) dragProgress else progress).let { if (it.isNaN()) 0f else it.coerceIn(0f, 1f) }

    val barHeight = 30.dp
    val amplitude = with(density) { 4.dp.toPx() } // Height of the wave
    val frequency = 0.06f // Slightly increased squiggles (was 0.04f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(barHeight)
            // Gesture handling for seek
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek(newProgress)
                }
            }
            .pointerInput(Unit) {
               detectHorizontalDragGestures(
                   onDragStart = { 
                       isDragging = true 
                       onDragStart()
                   },
                   onDragEnd = { 
                       isDragging = false 
                       onSeek(dragProgress)
                       onDragEnd()
                   },
                   onDragCancel = { 
                       isDragging = false 
                       onDragEnd()
                   }
               ) { change, _ ->
                   change.consume()
                   val newProgress = (change.position.x / width).coerceIn(0f, 1f)
                   dragProgress = newProgress
                   // onSeek(newProgress) // Removed to prevent network spam during drag
               }
            }
            .onSizeChanged { width = it.width.toFloat() }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerY = size.height / 2
            val path = androidx.compose.ui.graphics.Path()
            
            // Draw the full sine wave (inactive track)
            path.moveTo(0f, centerY)
            for (x in 0..size.width.toInt() step 2) {
                val xPos = x.toFloat()
                // Minimize value at start to join smoothly? 
                // Just standard sine: y = A * sin(wx)
                val yPos = centerY + amplitude * kotlin.math.sin(xPos * frequency)
                path.lineTo(xPos, yPos.toFloat())
            }
            
            // Draw Inactive Track
            drawPath(
                path = path,
                color = trackColor,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 4.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )

            // Draw Active Track (Clipped sine wave)
            clipRect(right = width * currentProgress) {
                 drawPath(
                    path = path,
                    color = activeTrackColor,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 4.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                )
            }
            
            // Draw Thumb (Circle at the end of the progress)
            val thumbX = width * currentProgress
            val thumbY = centerY + amplitude * kotlin.math.sin(thumbX * frequency)
            
            drawCircle(
                color = thumbColor,
                radius = 8.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(thumbX, thumbY.toFloat())
            )
        }
    }
}


@Composable
fun Badge(text: String, color: Color) {
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 1f)), // Full opacity for visibility
        modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp) // Ensure not collapsed
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 1f), // Full opacity
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
    }
}

// Utility extension for alpha
fun Modifier.alpha(alpha: Float) = this.then(Modifier.graphicsLayer(alpha = alpha))

// Utility to enforce luminance
fun Color.ensureLuminance(minLuminance: Float): Color {
    if (this.luminance() >= minLuminance) return this
    
    // Simple brightening strategy
    var adjusted = this
    var safety = 0
    while (adjusted.luminance() < minLuminance && safety < 10) {
        adjusted = adjusted.lighter()
        safety++
    }
    return adjusted
}

fun Color.lighter(factor: Float = 1.1f): Color {
    val argb = this.toArgb()
    val r = (Color(argb).red * factor).coerceAtMost(1f)
    val g = (Color(argb).green * factor).coerceAtMost(1f)
    val b = (Color(argb).blue * factor).coerceAtMost(1f)
    return Color(red = r, green = g, blue = b, alpha = this.alpha)
}

// Helper to format time
private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val secs = totalSeconds % 60
    return String.format("%d:%02d", minutes, secs)
}
