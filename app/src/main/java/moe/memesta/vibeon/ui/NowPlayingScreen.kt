@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package moe.memesta.vibeon.ui

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.Canvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sign
import moe.memesta.vibeon.ui.theme.bouncyClickable
import moe.memesta.vibeon.ui.theme.Dimens
import moe.memesta.vibeon.ui.theme.*
import moe.memesta.vibeon.ui.utils.LocalDisplayLanguage
import moe.memesta.vibeon.ui.utils.getDisplayName
import moe.memesta.vibeon.ui.utils.getDisplayArtist
import moe.memesta.vibeon.ui.utils.getDisplayAlbum
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlin.math.abs
import moe.memesta.vibeon.data.TrackInfo
import moe.memesta.vibeon.data.MediaSessionData

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NowPlayingScreen(
    playbackViewModel: PlaybackViewModel,
    connectionViewModel: ConnectionViewModel,
    onBackPressed: () -> Unit,
    onNavigateToAlbum: (String) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val playbackState by playbackViewModel.playbackState.collectAsState()
    val displayLanguage = LocalDisplayLanguage.current
    val currentTrack by connectionViewModel.currentTrack.collectAsState()
    val isPlaying by connectionViewModel.isPlaying.collectAsState()
    val isMobilePlayback by playbackViewModel.isMobilePlayback.collectAsState()
    
    // Shuffle and Repeat from server state
    val isShuffled by connectionViewModel.isShuffled.collectAsState()
    val repeatMode by connectionViewModel.repeatMode.collectAsState()
    val volume by connectionViewModel.volume.collectAsState()
    val favorites by connectionViewModel.favorites.collectAsState()

    val isLiked = currentTrack.path.let { favorites.contains(it) }
    val scope = rememberCoroutineScope()

    // Modal sheet for Queue
    var showQueueSheet by remember { mutableStateOf(false) }
    
    val listState = rememberLazyListState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VibeBackground)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Box(modifier = Modifier.fillParentMaxHeight()) {
                    NowPlayingContent(
                        title = currentTrack.getDisplayName(displayLanguage),
                        artist = currentTrack.getDisplayArtist(displayLanguage),
                        titleRomaji = currentTrack.titleRomaji,
                        artistRomaji = currentTrack.artistRomaji,
                        originalTitle = currentTrack.title,
                        originalArtist = currentTrack.artist,
                        isPlaying = isPlaying,
                        progress = playbackState.progress,
                        duration = playbackState.duration,
                        coverUrl = currentTrack.coverUrl,
                        isMobilePlayback = isMobilePlayback,
                        currentTrack = currentTrack,
                        isLiked = isLiked,
                        connectionViewModel = connectionViewModel,
                        isShuffled = isShuffled,
                        repeatMode = repeatMode,
                        volume = volume,
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
                            val positionSecs = (progress * playbackState.duration / 1000.0)
                            connectionViewModel.seek(positionSecs)
                        },
                        onBackToLibrary = onBackPressed,
                        onTogglePlaybackLocation = {
                            if (isMobilePlayback) playbackViewModel.stopMobilePlayback()
                            else playbackViewModel.requestMobilePlayback()
                        },
                        onLyricsClick = {
                            scope.launch { listState.animateScrollToItem(1) }
                        },
                        onQueueClick = { showQueueSheet = true },
                        onNavigateToAlbum = { onNavigateToAlbum(currentTrack.album.ifEmpty { currentTrack.artist }) },
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        expandLyrics = {
                            scope.launch { listState.animateScrollToItem(1) }
                        }
                    )
                }
            }
            item {
                Box(
                    modifier = Modifier
                        .fillParentMaxHeight()
                        .background(VibeSurfaceContainer, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                ) {
                    LyricsScreen(
                        connectionViewModel = connectionViewModel,
                        playbackViewModel = playbackViewModel
                    )
                }
            }
        }
        
        // Compact Header shown when scrolling down
        val showCompactHeader by remember {
            derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 300 }
        }

        AnimatedVisibility(
            visible = showCompactHeader,
            enter = fadeIn() + androidx.compose.animation.slideInVertically(),
            exit = fadeOut() + androidx.compose.animation.slideOutVertically(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            CompactNowPlayingHeader(
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                onPlayPauseToggle = {
                    if (isPlaying) connectionViewModel.pause() else connectionViewModel.play()
                },
                onHeaderClick = {
                    scope.launch { listState.animateScrollToItem(0) }
                }
            )
        }
    }

    if (showQueueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            containerColor = VibeBackground,
            modifier = Modifier.fillMaxHeight(0.85f)
        ) {
            QueueScreen(connectionViewModel)
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NowPlayingContent(
    title: String,
    artist: String,
    titleRomaji: String? = null,
    artistRomaji: String? = null,
    originalTitle: String = "",
    originalArtist: String = "",
    isPlaying: Boolean,
    progress: Float,
    duration: Long = 0,
    coverUrl: String? = null,
    isMobilePlayback: Boolean = false,
    currentTrack: MediaSessionData,
    isLiked: Boolean,
    connectionViewModel: ConnectionViewModel,
    isShuffled: Boolean,
    repeatMode: String,
    volume: Double,
    onPlayPauseToggle: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeek: (Float) -> Unit,
    onBackToLibrary: () -> Unit,
    onTogglePlaybackLocation: () -> Unit,
    onLyricsClick: () -> Unit,
    onQueueClick: () -> Unit,
    onNavigateToAlbum: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    expandLyrics: () -> Unit = {}
) {
    
    // Derived Colors from MaterialTheme (set by DynamicTheme in MainActivity)
    val vibrantColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val primaryControlColor = vibrantColor
    val onPrimaryControlColor = MaterialTheme.colorScheme.onPrimary
    val hapticFeedback = LocalHapticFeedback.current
    
    // Context for gestures
    var showPlaylistDialog by remember { mutableStateOf(false) }
    
    // Note: Shuffle/Repeat state now comes from connectionViewModel
    
    // Breathing animation for album art — pauses when not playing
    val breathingTransition = rememberInfiniteTransition(label = "breathing")
    val breathingScale by breathingTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )
    val albumScale = if (isPlaying) breathingScale else 1f

    // Pulse halo visualizer around album art
    val haloTransition = rememberInfiniteTransition(label = "halo")
    val haloScaleOuter by haloTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "haloScaleOuter"
    )
    val haloScaleInner by haloTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(1100)
        ),
        label = "haloScaleInner"
    )
    val haloAlphaOuter by haloTransition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "haloAlphaOuter"
    )
    val haloAlphaInner by haloTransition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(1100)
        ),
        label = "haloAlphaInner"
    )
    
    // Japanese dual-text: show romaji subtitle if the display title differs from original
    val showRomajiSubtitle = !titleRomaji.isNullOrBlank() && !titleRomaji.equals(title, ignoreCase = true) && !titleRomaji.equals(originalTitle, ignoreCase = true)
    val romajiSubtitle = if (showRomajiSubtitle) titleRomaji else null
    val showArtistRomajiSubtitle = !artistRomaji.isNullOrBlank() && !artistRomaji.equals(artist, ignoreCase = true) && !artistRomaji.equals(originalArtist, ignoreCase = true)
    val artistRomajiSubtitle = if (showArtistRomajiSubtitle) artistRomaji else null
    
    val bgGradient = Brush.verticalGradient(
        colors = listOf(
            vibrantColor.copy(alpha = 0.4f),
            secondaryColor.copy(alpha = 0.2f),
            tertiaryColor.copy(alpha = 0.05f),
            VibeBackground,
            VibeBackground
        )
    )

    // Main Container with standard app background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
    ) {

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = Dimens.SectionSpacing) // 24.dp
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header (Minimal/Floating)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        .size(36.dp)
                        .bouncyClickable(onClick = onBackToLibrary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Dismiss",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        .size(36.dp)
                        .bouncyClickable(onClick = { showPlaylistDialog = true }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.PlaylistAdd,
                        contentDescription = "Add to Playlist",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // Album Art (Bigger) & Gestures
            var offsetX by remember { mutableFloatStateOf(0f) }
            val swipeThreshold = 100f

            with(sharedTransitionScope) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(1f) // Max width
                        .weight(1f, fill = false)
                        .aspectRatio(1f)
                        .offset(x = offsetX.dp)
                        .graphicsLayer {
                            scaleX = albumScale
                            scaleY = albumScale
                            shadowElevation = 0.dp.toPx()
                            clip = false
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = {
                                    when {
                                        offsetX < -swipeThreshold -> {
                                            onSkipNext()
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                        offsetX > swipeThreshold -> {
                                            onSkipPrevious()
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                    // Reset offset
                                    offsetX = 0f
                                }
                            ) { change, dragAmount ->
                                change.consume()
                                // Drag right/left for tracks
                                if (abs(dragAmount.x) > abs(dragAmount.y)) {
                                    offsetX += (dragAmount.x * 0.5f)
                                } else if (dragAmount.y > 20) {
                                    // Swipe down logic for lyrics
                                    expandLyrics()
                                }
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    connectionViewModel.toggleFavorite(currentTrack.path)
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                onTap = {
                                    onNavigateToAlbum()
                                }
                            )
                        }
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent<String?>(
                        targetState = coverUrl,
                        transitionSpec = {
                            val isNext = targetState != initialState
                            val direction = if (isNext) 1 else -1
                            (slideInHorizontally(
                                animationSpec = tween(400, easing = FastOutSlowInEasing),
                                initialOffsetX = { fullWidth -> fullWidth * direction }
                            ) + fadeIn()).togetherWith(
                                slideOutHorizontally(
                                    animationSpec = tween(400, easing = FastOutSlowInEasing),
                                    targetOffsetX = { fullWidth -> -fullWidth * direction }
                                ) + fadeOut()
                            )
                        },
                        label = "AlbumArtSlide"
                    ) { currentCoverUrl ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(32.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(32.dp))
                        ) {
                            if (isPlaying) {
                                // Blurred aura behind
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .scale(haloScaleOuter)
                                        .blur(24.dp)
                                        .background(vibrantColor.copy(alpha = haloAlphaOuter * 2f), CircleShape)
                                )
                                // Inner crisp ring
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .scale(haloScaleInner)
                                        .border(
                                            width = 1.5.dp,
                                            color = vibrantColor.copy(alpha = haloAlphaInner),
                                            shape = RoundedCornerShape(32.dp)
                                        )
                                )
                            }
                            
                            if (!currentCoverUrl.isNullOrEmpty()) {
                                val context = LocalContext.current
                                val request = remember(currentCoverUrl) {
                                    ImageRequest.Builder(context)
                                        .data(currentCoverUrl)
                                        .crossfade(true)
                                        .build()
                                }
                                AsyncImage(
                                    model = request,
                                    contentDescription = "Album Art",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(VibeSurfaceContainer),
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
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // Track Info
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title.ifEmpty { "No Track" },
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    animationMode = MarqueeAnimationMode.Immediately,
                                    velocity = 30.dp
                                )
                        )
                        // Romaji subtitle for Japanese titles
                        if (romajiSubtitle != null) {
                            Text(
                                text = romajiSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.45f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    // Heart/Like Button
                    IconButton(
                        onClick = {
                            connectionViewModel.toggleFavorite(currentTrack.path)
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) vibrantColor else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = artist.ifEmpty { "Unknown Artist" },
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            animationMode = MarqueeAnimationMode.Immediately,
                            velocity = 30.dp
                        )
                )
                // Romaji subtitle for Japanese artist names
                if (artistRomajiSubtitle != null) {
                    Text(
                        text = artistRomajiSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.35f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Badges
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(modifier = Modifier.weight(1f))
                    // Lyrics Button
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .bouncyClickable(onClick = onLyricsClick)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                var isDraggingSeek by remember { mutableStateOf(false) }
                SquigglyProgressBar(
                    progress = progress,
                    thumbColor = vibrantColor,
                    trackColor = Color.White.copy(alpha = 0.2f),
                    activeTrackColor = vibrantColor,
                    onSeek = onSeek,
                    onDragStart = { isDraggingSeek = true },
                    onDragEnd = { isDraggingSeek = false }
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Duration is in milliseconds — convert to seconds for display
                val durationSecs = duration / 1000L
                val elapsedSecs = (progress * durationSecs).toLong().coerceIn(0L, durationSecs.coerceAtLeast(0L))
                val totalSecs = durationSecs.coerceAtLeast(0L)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(elapsedSecs),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Text(
                        text = formatTime(totalSecs),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            // Main Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = {
                    connectionViewModel.toggleShuffle()
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }) {
                    Icon(
                        Icons.Rounded.Shuffle,
                        null,
                        tint = if (isShuffled) vibrantColor else Color.White.copy(alpha = 0.6f)
                    )
                }
                
                // Previous
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                        .bouncyClickable(onClick = onSkipPrevious),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Play/Pause
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(primaryControlColor, RoundedCornerShape(32.dp))
                        .bouncyClickable(onClick = onPlayPauseToggle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = onPrimaryControlColor,
                        modifier = Modifier.size(48.dp)
                    )
                }

                // Next
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                        .bouncyClickable(onClick = onSkipNext),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                IconButton(onClick = {
                    connectionViewModel.toggleRepeat()
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }) {
                    Icon(
                        if (repeatMode != "off") Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        null,
                        tint = if (repeatMode != "off") vibrantColor else Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Volume Control
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Rounded.VolumeDown,
                    contentDescription = "Volume Down",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
                
                CustomVolumeBar(
                    volume = volume.toFloat(),
                    onVolumeChange = { newVolume ->
                        connectionViewModel.setVolume(newVolume.toDouble())
                    },
                    activeColor = vibrantColor,
                    modifier = Modifier.weight(1f)
                )
                
                Icon(
                    Icons.Rounded.VolumeUp,
                    contentDescription = "Volume Up",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bottom Controls (Volume / Output)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                 Row(
                   modifier = Modifier
                       .clip(RoundedCornerShape(20.dp))
                       .background(Color.White.copy(alpha = 0.05f))
                       .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                       .bouncyClickable(onClick = onTogglePlaybackLocation)
                       .padding(horizontal = 20.dp, vertical = 14.dp),
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
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Row(
                   modifier = Modifier
                       .clip(RoundedCornerShape(20.dp))
                       .background(Color.White.copy(alpha = 0.05f))
                       .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                       .bouncyClickable(onClick = onQueueClick)
                       .padding(horizontal = 20.dp, vertical = 14.dp),
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
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showPlaylistDialog) {
        val playlists by connectionViewModel.playlists.collectAsState()
        
        LaunchedEffect(Unit) {
            connectionViewModel.getPlaylists()
        }

        AlertDialog(
            onDismissRequest = { showPlaylistDialog = false },
            title = { Text("Add to Playlist", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(playlists) { playlist ->
                        Text(
                            text = playlist.name,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    connectionViewModel.addToPlaylist(playlist.id, currentTrack.path)
                                    showPlaylistDialog = false
                                }
                                .padding(16.dp)
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPlaylistDialog = false }) {
                    Text("Cancel", color = vibrantColor)
                }
            },
            containerColor = VibeSurfaceContainer,
            titleContentColor = Color.White
        )
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

@Composable
fun CustomVolumeBar(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    var width by remember { mutableFloatStateOf(0f) }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onVolumeChange((offset.x / width).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    onVolumeChange((change.position.x / width).coerceIn(0f, 1f))
                }
            }
            .onSizeChanged { width = it.width.toFloat() },
        contentAlignment = Alignment.CenterStart
    ) {
        // Background track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f))
        )
        // Active track
        Box(
            modifier = Modifier
                .fillMaxWidth(volume.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(CircleShape)
                .background(activeColor)
        )
        // Thumb
        Box(
            modifier = Modifier
                .offset(x = with(LocalDensity.current) { (volume.coerceIn(0f, 1f) * width).toDp() } - 8.dp)
                .size(16.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(2.dp, activeColor, CircleShape)
        )
    }
}

@Composable
fun CompactNowPlayingHeader(
    currentTrack: MediaSessionData,
    isPlaying: Boolean,
    onPlayPauseToggle: () -> Unit,
    onHeaderClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayLanguage = LocalDisplayLanguage.current
    val title = currentTrack.getDisplayName(displayLanguage)
    val artist = currentTrack.getDisplayArtist(displayLanguage)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(VibeSurfaceContainer.copy(alpha = 0.95f))
            .statusBarsPadding()
            .clickable { onHeaderClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = currentTrack.coverUrl,
            contentDescription = "Album Cover",
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = artist,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .bouncyClickable { onPlayPauseToggle() }
                .padding(8.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
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

// Helper to format time (Input is SECONDS)
private fun formatTime(seconds: Long): String {
    val totalSeconds = seconds
    val minutes = totalSeconds / 60
    val secs = totalSeconds % 60
    return String.format("%d:%02d", minutes, secs)
}
