package moe.memesta.vibeon.ui

import android.view.WindowManager
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage

/**
 * Immersive landscape dock mode screen.
 *
 * Full-screen, edge-to-edge view shown when the device is physically held in landscape.
 * Hides system chrome, shows full-bleed album art with a frosted glass control panel.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ImmersiveView(
    connectionViewModel: ConnectionViewModel,
    playbackViewModel: PlaybackViewModel,
) {
    val currentTrack by connectionViewModel.currentTrack.collectAsState()
    val playbackState by playbackViewModel.playbackState.collectAsState()
    val lyricsData by connectionViewModel.lyrics.collectAsState()

    // Hide system bars while immersive view is visible
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? android.app.Activity)?.window ?: return@DisposableEffect onDispose {}
        val activity = view.context as? android.app.Activity
        
        // Force landscape orientation
        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, view)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // Keep screen on in dock mode
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onDispose {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            controller.show(WindowInsetsCompat.Type.systemBars())
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // --- Full-bleed blurred album art background ---
        AsyncImage(
            model = currentTrack.coverUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(24.dp),
            contentScale = ContentScale.Crop
        )

        // Dark gradient scrim so controls are readable
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.65f),
                            Color.Black.copy(alpha = 0.40f),
                            Color.Black.copy(alpha = 0.65f)
                        )
                    )
                )
        )

        // --- Main landscape layout: 3 columns ---
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(36.dp)
        ) {
            // 1. Album art card — Left side (Bigger column)
            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                val animatedProgress by animateFloatAsState(
                    targetValue = playbackState.progress,
                    animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                    label = "progress"
                )
                
                val waveSpeed by animateDpAsState(
                    targetValue = if (playbackState.isPlaying) 60.dp else 0.dp,
                    label = "waveSpeed"
                )
                
                val thickStrokeWidth = with(LocalDensity.current) { 8.dp.toPx() }
                val thickStroke = remember(thickStrokeWidth) { Stroke(width = thickStrokeWidth, cap = StrokeCap.Round) }

                AsyncImage(
                    model = currentTrack.coverUrl,
                    contentDescription = "Album art",
                    modifier = Modifier
                        .fillMaxHeight(0.60f)
                        .aspectRatio(1f)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                CircularWavyProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxHeight(0.80f)
                        .aspectRatio(1f),
                    stroke = thickStroke,
                    trackStroke = thickStroke,
                    amplitude = { 1f },
                    gapSize = 1.dp,
                    wavelength = 50.dp,
                    waveSpeed = 30.dp
                )
            }

            // 2. Controls panel — Middle side (Smaller column)
            Column(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Track info
                Text(
                    text = playbackState.title,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp
                    ),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = playbackState.artist,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.70f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Playback controls row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Previous
                    FilledTonalIconButton(
                        onClick = { connectionViewModel.previous() },
                        modifier = Modifier.size(52.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color.White.copy(alpha = 0.15f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous",
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Play / Pause — large tonal button
                    FilledIconButton(
                        onClick = {
                            if (playbackState.isPlaying) connectionViewModel.pause()
                            else connectionViewModel.play()
                        },
                        modifier = Modifier.size(68.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Next
                    FilledTonalIconButton(
                        onClick = { connectionViewModel.next() },
                        modifier = Modifier.size(52.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color.White.copy(alpha = 0.15f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Dock mode label
                Text(
                    text = "DOCK MODE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 3.sp
                    ),
                    color = Color.White.copy(alpha = 0.35f)
                )
            }

            // 3. Lyrics panel — Right side (Medium column)
            Box(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                // Parse lyrics
                val lyrics = remember(lyricsData) {
                    if (lyricsData?.hasSynced == true && !lyricsData?.syncedLyrics.isNullOrEmpty()) {
                        val jpLyrics = parseLrc(lyricsData!!.syncedLyrics!!)
                        
                        // Merge Romaji if available
                        if (!lyricsData?.syncedLyricsRomaji.isNullOrEmpty()) {
                            val romajiLyrics = parseLrc(lyricsData!!.syncedLyricsRomaji!!)
                            jpLyrics.mapIndexed { index, jpGroup ->
                                val romajiGroup = romajiLyrics.getOrNull(index)
                                if (romajiGroup != null && jpGroup.timestamp == romajiGroup.timestamp) {
                                    LyricGroup(jpGroup.timestamp, jpGroup.lines + romajiGroup.lines)
                                } else jpGroup
                            }
                        } else {
                            jpLyrics
                        }
                    } else if (!lyricsData?.plainLyrics.isNullOrEmpty()) {
                        lyricsData!!.plainLyrics!!.lines().map { LyricGroup(0, listOf(it)) }
                    } else {
                        emptyList()
                    }
                }

                val isEmpty = lyrics.isEmpty()
                val isInstrumental = lyricsData?.instrumental == true
                val currentTimeMs = (playbackState.progress * playbackState.duration).toLong()
                val currentLineIndex = remember(currentTimeMs, lyrics) {
                    if (isEmpty) -1 else lyrics.indexOfLast { it.timestamp <= currentTimeMs }.coerceAtLeast(0)
                }
                
                val listState = rememberLazyListState()
                
                // Auto-scroll
                LaunchedEffect(currentLineIndex, lyrics) {
                    if (currentLineIndex >= 0 && !isEmpty) {
                        listState.animateScrollToItem(
                            index = (currentLineIndex - 2).coerceAtLeast(0),
                            scrollOffset = 0
                        )
                    }
                }

                if (isEmpty || isInstrumental) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isInstrumental) "Instrumental Track" else "No lyrics available",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.4f)
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 80.dp, bottom = 120.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        itemsIndexed(lyrics) { index, lyricGroup ->
                            LyricGroupItem(
                                lyricGroup = lyricGroup,
                                isActive = index == currentLineIndex,
                                isPast = index < currentLineIndex,
                                viewMode = LyricsViewMode.ROMAJI,
                                baseTextColor = Color.White,
                                secondaryTextColor = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        // Subtle top hint: "rotate to exit"
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.12f))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Rotate phone to exit",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.50f)
            )
        }
    }
}

