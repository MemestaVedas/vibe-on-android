@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
package moe.memesta.vibeon.ui

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin
import moe.memesta.vibeon.data.MediaSessionData
import moe.memesta.vibeon.data.TrackInfo
import moe.memesta.vibeon.ui.theme.Dimens
import moe.memesta.vibeon.ui.theme.VibeBackground
import moe.memesta.vibeon.ui.theme.VibeSurfaceContainer
import moe.memesta.vibeon.ui.theme.bouncyClickable
import moe.memesta.vibeon.ui.theme.ensureLuminance
import moe.memesta.vibeon.ui.theme.*
import moe.memesta.vibeon.ui.image.AppImageLoader
import moe.memesta.vibeon.ui.utils.LocalDisplayLanguage
import moe.memesta.vibeon.ui.utils.PaletteUtils
import moe.memesta.vibeon.ui.utils.ThemeColors
import moe.memesta.vibeon.ui.utils.getDisplayAlbum
import moe.memesta.vibeon.ui.utils.getDisplayArtist
import moe.memesta.vibeon.ui.utils.getDisplayName
import moe.memesta.vibeon.ui.shapes.*

@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
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
    val effectiveIsPlaying = if (isMobilePlayback) playbackState.isPlaying else isPlaying

    // Shuffle and Repeat from server state
    val isShuffled by connectionViewModel.isShuffled.collectAsState()
    val repeatMode by connectionViewModel.repeatMode.collectAsState()
    val volume by connectionViewModel.volume.collectAsState()
    val favorites by connectionViewModel.favorites.collectAsState()

    val isLiked = currentTrack.path.let { favorites.contains(it) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Dynamic palette for lyrics surfaces and text
    val coverUrl = currentTrack.coverUrl?.takeIf { it.isNotBlank() }
    var themeColors by remember { mutableStateOf(ThemeColors()) }
    val lyricsSurfaceColor by animateColorAsState(
        targetValue = if (themeColors.vibrant != Color.Transparent) {
            themeColors.vibrant.copy(alpha = 0.18f).compositeOver(VibeBackground)
        } else {
            VibeSurfaceContainer
        },
        animationSpec = tween(600),
        label = "lyricsSurfaceColor"
    )
    val lyricsTextColor by animateColorAsState(
        targetValue = if (themeColors.vibrant != Color.Transparent) {
            themeColors.vibrant.ensureLuminance(0.85f)
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(600),
        label = "lyricsTextColor"
    )
    val lyricsSubtleTextColor by animateColorAsState(
        targetValue = if (themeColors.muted != Color.Transparent) {
            themeColors.muted.ensureLuminance(0.75f)
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        },
        animationSpec = tween(600),
        label = "lyricsSubtleTextColor"
    )

    LaunchedEffect(coverUrl) {
        if (coverUrl != null) {
            val loader = AppImageLoader.get(context)
            val request = ImageRequest.Builder(context)
                .data(coverUrl)
                .allowHardware(false)
                .build()
            val result = withContext(kotlinx.coroutines.Dispatchers.IO) { loader.execute(request) }
            if (result is SuccessResult) {
                themeColors = PaletteUtils.extractColors(result.drawable)
            }
        } else {
            themeColors = ThemeColors()
        }
    }

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
                        isPlaying = effectiveIsPlaying,
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
                            if (isMobilePlayback) {
                                playbackViewModel.setPlayerPlayWhenReady(!effectiveIsPlaying)
                                playbackViewModel.updateIsPlaying(!effectiveIsPlaying)
                            } else {
                                if (effectiveIsPlaying) connectionViewModel.pause() else connectionViewModel.play()
                            }
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
                        onQueueClick = { showQueueSheet = !showQueueSheet },
                        isQueueSheetVisible = showQueueSheet,
                        onNavigateToAlbum = { onNavigateToAlbum(currentTrack.album.ifEmpty { currentTrack.artist }) },
                        lyricsSurfaceColor = lyricsSurfaceColor,
                        lyricsTextColor = lyricsTextColor,
                        lyricsSubtleTextColor = lyricsSubtleTextColor,
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
                        .background(color = lyricsSurfaceColor)
                ) {
                    LyricsScreen(
                        connectionViewModel = connectionViewModel,
                        playbackViewModel = playbackViewModel,
                        lyricsSurfaceColor = lyricsSurfaceColor,
                        lyricsTextColor = lyricsTextColor,
                        lyricsSubtleTextColor = lyricsSubtleTextColor,
                        showHeader = false
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
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            CompactNowPlayingHeader(
                currentTrack = currentTrack,
                isPlaying = effectiveIsPlaying,
                onPlayPauseToggle = {
                    if (isMobilePlayback) {
                        playbackViewModel.setPlayerPlayWhenReady(!effectiveIsPlaying)
                        playbackViewModel.updateIsPlaying(!effectiveIsPlaying)
                    } else {
                        if (effectiveIsPlaying) connectionViewModel.pause() else connectionViewModel.play()
                    }
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
            QueueScreen(
                viewModel = connectionViewModel,
                showCloseButton = true,
                onClose = { showQueueSheet = false }
            )
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
    isQueueSheetVisible: Boolean,
    onNavigateToAlbum: () -> Unit,
    lyricsSurfaceColor: Color,
    lyricsTextColor: Color,
    lyricsSubtleTextColor: Color,
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
    val displayLanguage = LocalDisplayLanguage.current
    
    // Context for gestures
    var showPlaylistDialog by remember { mutableStateOf(false) }

    val queue by connectionViewModel.queue.collectAsState()
    val currentIndex by connectionViewModel.currentIndex.collectAsState()
    val pagerItems = remember(queue, currentTrack, displayLanguage, title, artist) {
        if (queue.isNotEmpty()) {
            queue.mapIndexed { index, item ->
                val stableKey = item.path.takeIf { it.isNotBlank() }
                    ?: listOf(item.title, item.artist, item.album, item.coverUrl.orEmpty(), index.toString()).joinToString("|")
                TrackPagerItem(
                    path = item.path,
                    stableKey = stableKey,
                    coverUrl = item.coverUrl,
                    title = item.getDisplayName(displayLanguage),
                    artist = item.getDisplayArtist(displayLanguage)
                )
            }
        } else {
            val fallbackKey = currentTrack.path.takeIf { it.isNotBlank() }
                ?: listOf(title, artist, coverUrl.orEmpty()).joinToString("|")
            listOf(
                TrackPagerItem(
                    path = currentTrack.path,
                    stableKey = fallbackKey,
                    coverUrl = currentTrack.coverUrl,
                    title = title,
                    artist = artist
                )
            )
        }
    }
    val initialPage = currentIndex.coerceIn(0, (pagerItems.size - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { pagerItems.size }
    )
    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
    var lastDispatchedPage by remember { mutableIntStateOf(-1) }

    LaunchedEffect(currentIndex, pagerItems.size, isDragged) {
        if (!isDragged && pagerItems.isNotEmpty()) {
            val target = currentIndex.coerceIn(0, pagerItems.lastIndex)
            if (pagerState.currentPage != target) {
                pagerState.animateScrollToPage(target)
            } else {
                lastDispatchedPage = target
            }
        }
    }

    LaunchedEffect(isDragged, pagerItems, currentIndex) {
        if (!isDragged && pagerItems.isNotEmpty()) {
            val page = pagerState.currentPage
            if (page != currentIndex && page != lastDispatchedPage) {
                lastDispatchedPage = page
                val selectedPath = pagerItems[page].path
                if (selectedPath.isNotBlank()) {
                    connectionViewModel.playTrack(selectedPath)
                }
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            } else if (page == currentIndex) {
                lastDispatchedPage = page
            }
        }
    }
    
    // Japanese dual-text: show romaji subtitle if the display title differs from original
    val showRomajiSubtitle = !titleRomaji.isNullOrBlank() && !titleRomaji.equals(title, ignoreCase = true) && !titleRomaji.equals(originalTitle, ignoreCase = true)
    val romajiSubtitle = if (showRomajiSubtitle) titleRomaji else null
    val showArtistRomajiSubtitle = !artistRomaji.isNullOrBlank() && !artistRomaji.equals(artist, ignoreCase = true) && !artistRomaji.equals(originalArtist, ignoreCase = true)
    val artistRomajiSubtitle = if (showArtistRomajiSubtitle) artistRomaji else null

    // Main Container with immersive blurred background
        // Kinetic art stage: subtle breathing scale while playing
        val artInfinite = rememberInfiniteTransition(label = "artBreath")
        val artBreathScale by artInfinite.animateFloat(
            initialValue = 1f,
            targetValue = 1.018f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2800, easing = androidx.compose.animation.core.EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "artBreathScale"
        )
        val effectiveArtScale by animateFloatAsState(
            targetValue = if (isPlaying) artBreathScale else 1f,
            animationSpec = MotionTokens.Effects.slow(),
            label = "artScale"
        )
        val artStageElevation by animateDpAsState(
            targetValue = if (isPlaying) 28.dp else 16.dp,
            animationSpec = tween(
                durationMillis = MotionTokens.Duration.Standard,
                easing = MotionTokens.EasingTokens.Standard
            ),
            label = "artStageElevation"
        )
        val artGlowAlpha by animateFloatAsState(
            targetValue = if (isPlaying) 0.3f else 0.18f,
            animationSpec = MotionTokens.Effects.slow(),
            label = "artGlowAlpha"
        )

        // Double-tap like state
        var showLikeHeart by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Height reserved for the edge-to-edge lyrics pill at bottom.
        // Increase if the pill appears taller on some devices.
        val lyricsPillHeight = 88.dp

        // Immersive Blurred Background
        if (!coverUrl.isNullOrEmpty()) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(radius = 100.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    .alpha(0.6f),
                contentScale = ContentScale.Crop
            )
        }

        // Main content Column
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Album Art - Immersive Header Style
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f) // Restore full 1:1 square layout
                    .shadow(
                        elevation = artStageElevation,
                        shape = RoundedCornerShape(bottomStart = 56.dp, bottomEnd = 56.dp),
                        clip = false
                    )
                    .clip(RoundedCornerShape(bottomStart = 56.dp, bottomEnd = 56.dp))
                        .graphicsLayer { scaleX = effectiveArtScale; scaleY = effectiveArtScale }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { change, dragAmount ->
                            if (dragAmount > 20f) {
                                change.consume()
                                expandLyrics()
                            }
                        }
                    }
                    .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { onNavigateToAlbum() },
                                onDoubleTap = {
                                    connectionViewModel.toggleFavorite(currentTrack.path)
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showLikeHeart = true
                                }
                            )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    vibrantColor.copy(alpha = artGlowAlpha),
                                    tertiaryColor.copy(alpha = artGlowAlpha * 0.55f),
                                    Color.Transparent
                                ),
                                radius = 1200f
                            )
                        )
                )

                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = pagerItems.size > 1,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val item = pagerItems.getOrNull(page) ?: return@HorizontalPager
                    val displayCover = if (page == currentIndex && !coverUrl.isNullOrBlank()) {
                        coverUrl
                    } else {
                        item.coverUrl
                    }
                    
                    with(sharedTransitionScope) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .sharedElement(
                                    sharedContentState = rememberSharedContentState(key = "album-${item.stableKey}"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!displayCover.isNullOrEmpty()) {
                                AsyncImage(
                                    model = displayCover,
                                    contentDescription = "Album Art",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop // Immersive crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.MusicNote,
                                        null,
                                        tint = Color.White.copy(alpha = 0.15f),
                                        modifier = Modifier.size(100.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                // Heart burst — double-tap like feedback
                androidx.compose.animation.AnimatedVisibility(
                    visible = showLikeHeart,
                    enter = fadeIn(animationSpec = tween(80)) +
                            scaleIn(initialScale = 0.35f, animationSpec = MotionTokens.Spatial.fast()),
                    exit  = fadeOut(animationSpec = tween(280)) +
                            scaleOut(targetScale = 1.5f, animationSpec = tween(280))
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFFF4081),
                            modifier = Modifier.size(96.dp)
                        )
                    }
                }
            }

            // Heart auto-resets after burst animation
            LaunchedEffect(showLikeHeart) {
                if (showLikeHeart) {
                    kotlinx.coroutines.delay(700)
                    showLikeHeart = false
                }
            }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                ) {
                Text(
                    text = title.ifEmpty { "No Track" },
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee(iterations = Int.MAX_VALUE)
                )
                
                if (romajiSubtitle != null) {
                    Text(
                        text = romajiSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = artist.ifEmpty { "Unknown Artist" },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        if (artistRomajiSubtitle != null) {
                            Text(
                                text = artistRomajiSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Queue Button aligned with new kinetic button system
                    FluxPill(
                        selected = isQueueSheetVisible,
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onQueueClick()
                        },
                        icon = Icons.Rounded.QueueMusic,
                        label = if (queue.isNotEmpty()) {
                            "${(currentIndex + 1).coerceIn(1, queue.size)}/${queue.size}"
                        } else {
                            "Queue"
                        }
                    )
                }
            }
                    
                    
                // Compacted spacing to bring controls closer to metadata
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Squiggly Progress Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Duration is in milliseconds — convert to seconds for display
                    val durationSecs = duration / 1000L
                    val elapsedSecs = (progress * durationSecs).toLong().coerceIn(0L, durationSecs.coerceAtLeast(0L))
                    val totalSecs = durationSecs.coerceAtLeast(0L)

                    Text(
                        text = formatTime(elapsedSecs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(48.dp)
                    )
                    
                    var isDraggingSeek by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                        SquigglyProgressBar(
                            progress = progress,
                            isPlaying = isPlaying,
                            thumbColor = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            onSeek = onSeek,
                            onDragStart = { isDraggingSeek = true },
                            onDragEnd = { isDraggingSeek = false }
                        )
                    }
                    
                    Text(
                        text = formatTime(totalSecs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(48.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))

                // Main Controls — futuristic transport cluster
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Previous — PrismIconButton
                    PrismIconButton(
                        onClick = onSkipPrevious,
                        icon = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous track",
                        size = 64.dp,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Play/Pause — OrbitButton (orbit arc active while playing)
                    OrbitButton(
                        onClick = onPlayPauseToggle,
                        isActive = isPlaying,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Next — PrismIconButton (mirrored prism via rotationY)
                    PrismIconButton(
                        onClick = onSkipNext,
                        icon = Icons.Rounded.SkipNext,
                        contentDescription = "Next track",
                        size = 64.dp,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.graphicsLayer { rotationY = 180f }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Material 3 Enhanced Controls Bar (Two Islands)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Island 1: Controls (Device, Shuffle, Repeat)
                    Row(
                        modifier = Modifier
                            .weight(1.8f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(32.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FluxPill(
                            selected = isMobilePlayback,
                            onClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTogglePlaybackLocation()
                            },
                            modifier = Modifier.weight(1f),
                            icon = if (isMobilePlayback) Icons.Rounded.Smartphone else Icons.Rounded.Computer
                        )

                        FluxPill(
                            selected = isShuffled,
                            onClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                connectionViewModel.toggleShuffle()
                            },
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.Shuffle
                        )

                        FluxPill(
                            selected = repeatMode == "all" || repeatMode == "one",
                            onClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                connectionViewModel.toggleRepeat()
                            },
                            modifier = Modifier.weight(1f),
                            icon = if (repeatMode == "one") Icons.Rounded.RepeatOne else Icons.Rounded.Repeat
                        )
                    }

                    // Island 2: Volume Control
                    var width by remember { mutableFloatStateOf(0f) }
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(32.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    if (width > 0) {
                                        connectionViewModel.setVolume((offset.x / width).coerceIn(0f, 1f).toDouble())
                                    }
                                }
                            }
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures { change, _ ->
                                    change.consume()
                                    if (width > 0) {
                                        connectionViewModel.setVolume((change.position.x / width).coerceIn(0f, 1f).toDouble())
                                    }
                                }
                            }
                            .onSizeChanged { width = it.width.toFloat() },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        // Active volume fill
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(volume.toFloat().coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.tertiaryContainer)
                        )
                        
                        // Volume Icon
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (volume == 0.0) Icons.Rounded.VolumeOff 
                                else if (volume < 0.5) Icons.Rounded.VolumeDown 
                                else Icons.Rounded.VolumeUp,
                                contentDescription = "Volume",
                                tint = if (volume > 0) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                } // Closes Enhanced Bar Row
            } // Closes inner controls Column

            Spacer(modifier = Modifier.weight(1f))
        } // Closes Main content Column

        // Unified Lyrics Sheet - full-bleed at bottom (edge-to-edge)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(color = lyricsSurfaceColor)
                .clickable(enabled = true) { expandLyrics() }
                .padding(horizontal = Dimens.SectionSpacing, vertical = 20.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Text(
                text = "Lyrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = lyricsTextColor
            )
        }

        // Header (Floating) moved to top over album art and column content
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f), CircleShape)
                    .size(40.dp)
                    .bouncyClickable(onClick = onBackToLibrary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }

            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f), CircleShape)
                    .size(40.dp)
                    .bouncyClickable(onClick = { showPlaylistDialog = true }),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.PlaylistAdd,
                    contentDescription = "Add to Playlist",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        } // Closes the Header Row
    } // Closes the main Box (e.g., PlayerScreen content)
    
    if (showPlaylistDialog) {
        val playlists by connectionViewModel.playlists.collectAsState()
        
        LaunchedEffect(Unit) {
            connectionViewModel.getPlaylists()
        }

        AlertDialog(
            onDismissRequest = { showPlaylistDialog = false },
            title = { Text("Add to Playlist", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(playlists) { playlist ->
                        Text(
                            text = playlist.name,
                            color = MaterialTheme.colorScheme.onSurface,
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
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    }
}

private data class TrackPagerItem(
    val path: String,
    val stableKey: String,
    val coverUrl: String?,
    val title: String,
    val artist: String
)

// Custom Waveform-like Progress Bar
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SquigglyProgressBar(
    progress: Float,
    isPlaying: Boolean = true,
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

    val clampedProgress = progress.let { if (it.isNaN()) 0f else it.coerceIn(0f, 1f) }
    val currentTarget = if (isDragging) dragProgress else clampedProgress

    val renderedProgress = animateFloatAsState(
        targetValue = currentTarget,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "SquigglyAnim"
    )

    val thumbInteractionFraction by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "ThumbInteractionAnim"
    )

    val shouldShowWave = isPlaying && !isDragging
    val amplitudeFactor by animateFloatAsState(
        targetValue = if (shouldShowWave) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "WaveAmplitude"
    )

    val waveLength = 30.dp
    val waveSpeed = waveLength / 2
    val waveLengthPx = with(density) { waveLength.toPx() }
    val waveSpeedPx = with(density) { waveSpeed.toPx() }
    val waveFrequency = if (waveLengthPx > 0f) ((2 * PI) / waveLengthPx).toFloat() else 0f

    val phaseShiftAnim = remember { Animatable(0f) }
    val phaseShift = phaseShiftAnim.value

    LaunchedEffect(shouldShowWave, waveLengthPx, waveSpeedPx) {
        if (shouldShowWave && waveLengthPx > 0f && waveSpeedPx > 0f) {
            val fullRotation = (2 * PI).toFloat()
            val durationMs = ((waveLengthPx / waveSpeedPx) * 1000f).roundToInt().coerceAtLeast(800)
            while (shouldShowWave) {
                val start = (phaseShiftAnim.value % fullRotation).let { if (it < 0f) it + fullRotation else it }
                phaseShiftAnim.snapTo(start)
                phaseShiftAnim.animateTo(
                    targetValue = start + fullRotation,
                    animationSpec = tween(durationMillis = durationMs, easing = LinearEasing)
                )
            }
        }
    }

    val barHeight = 30.dp
    val trackHeight = 5.dp
    val thumbRadius = 8.dp
    val thumbLineHeight = 24.dp
    val thumbGap = 4.dp

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
            val trackHeightPx = trackHeight.toPx()
            val thumbRadiusPx = thumbRadius.toPx()
            val thumbLineHeightPx = thumbLineHeight.toPx()
            val thumbGapPx = thumbGap.toPx()
            val waveAmplitudePx = 4.dp.toPx() * amplitudeFactor

            val centerY = size.height / 2f
            val trackStart = thumbRadiusPx
            val trackEnd = (size.width - thumbRadiusPx).coerceAtLeast(trackStart)
            val trackWidth = (trackEnd - trackStart).coerceAtLeast(0f)
            val progressValue = renderedProgress.value.coerceIn(0f, 1f)
            val progressPxEnd = trackStart + (trackWidth * progressValue)

            if (progressPxEnd < trackEnd) {
                drawLine(
                    color = trackColor,
                    start = Offset(progressPxEnd, centerY),
                    end = Offset(trackEnd, centerY),
                    strokeWidth = trackHeightPx,
                    cap = StrokeCap.Round
                )
            }

            val activeTrackEnd = (progressPxEnd - (thumbGapPx * thumbInteractionFraction)).coerceAtLeast(trackStart)
            if (activeTrackEnd > trackStart) {
                if (waveAmplitudePx > 0.01f && waveFrequency > 0f) {
                    val wavePath = Path()
                    val periodPx = ((2 * PI) / waveFrequency).toFloat()
                    val samplesPerCycle = 20f
                    val waveStep = (periodPx / samplesPerCycle).coerceAtLeast(1.2f).coerceAtMost(trackHeightPx)

                    fun yAt(x: Float): Float {
                        val y = centerY + waveAmplitudePx * sin(waveFrequency * x + phaseShift)
                        return y.coerceIn(
                            centerY - waveAmplitudePx - trackHeightPx / 2f,
                            centerY + waveAmplitudePx + trackHeightPx / 2f
                        )
                    }

                    var prevX = trackStart
                    var prevY = yAt(prevX)
                    wavePath.moveTo(prevX, prevY)

                    var x = prevX + waveStep
                    while (x < activeTrackEnd) {
                        val y = yAt(x)
                        val midX = (prevX + x) * 0.5f
                        val midY = (prevY + y) * 0.5f
                        wavePath.quadraticTo(prevX, prevY, midX, midY)
                        prevX = x
                        prevY = y
                        x += waveStep
                    }
                    val endY = yAt(activeTrackEnd)
                    wavePath.quadraticTo(prevX, prevY, activeTrackEnd, endY)

                    drawPath(
                        path = wavePath,
                        color = activeTrackColor,
                        style = Stroke(
                            width = trackHeightPx,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            miter = 1f
                        )
                    )
                } else {
                    drawLine(
                        color = activeTrackColor,
                        start = Offset(trackStart, centerY),
                        end = Offset(activeTrackEnd, centerY),
                        strokeWidth = trackHeightPx,
                        cap = StrokeCap.Round
                    )
                }
            }

            val thumbWidth = lerp(thumbRadiusPx * 2f, trackHeightPx * 1.2f, thumbInteractionFraction)
            val thumbHeight = lerp(thumbRadiusPx * 2f, thumbLineHeightPx, thumbInteractionFraction)
            val minThumbCenter = (thumbWidth / 2f).coerceAtMost(size.width / 2f)
            val maxThumbCenter = (size.width - thumbWidth / 2f).coerceAtLeast(minThumbCenter)
            val thumbX = progressPxEnd.coerceIn(minThumbCenter, maxThumbCenter)

            drawRoundRect(
                color = thumbColor,
                topLeft = Offset(thumbX - thumbWidth / 2f, centerY - thumbHeight / 2f),
                size = Size(thumbWidth, thumbHeight),
                cornerRadius = CornerRadius(thumbWidth / 2f)
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
            .background(MaterialTheme.colorScheme.surfaceVariant)
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
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

// End of NowPlayingScreen.kt
