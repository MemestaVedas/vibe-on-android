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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.drawWithContent
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em
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
import kotlin.random.Random
import moe.memesta.vibeon.data.MediaSessionData
import moe.memesta.vibeon.data.TrackInfo
import moe.memesta.vibeon.data.local.ScrubberMode
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
    val scrubberMode by playbackViewModel.scrubberMode.collectAsState()
    val artGestureHintShown by playbackViewModel.artGestureHintShown.collectAsState()
    val heartBurstEvent by playbackViewModel.heartBurstEvent.collectAsState()
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

    val detectedBpm = remember(currentTrack.title, currentTrack.artist, currentTrack.album) {
        extractBpmFromTrack(currentTrack)
    }
    val bpmHueOffset = remember(detectedBpm) {
        detectedBpm?.let { bpm ->
            when {
                bpm < 80f -> 10f
                bpm > 140f -> -10f
                else -> androidx.compose.ui.util.lerp(10f, -10f, (bpm - 80f) / 60f)
            }
        }
    }
    val scopedPrimaryContainerSeed = when {
        themeColors.vibrant != Color.Transparent -> themeColors.vibrant.copy(alpha = 0.35f).compositeOver(MaterialTheme.colorScheme.surface)
        themeColors.muted != Color.Transparent -> themeColors.muted.copy(alpha = 0.28f).compositeOver(MaterialTheme.colorScheme.surface)
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val scopedPrimaryContainerTarget = if (bpmHueOffset != null) {
        shiftHue(scopedPrimaryContainerSeed, bpmHueOffset)
    } else {
        scopedPrimaryContainerSeed
    }
    val scopedPrimaryContainer by animateColorAsState(
        targetValue = scopedPrimaryContainerTarget,
        animationSpec = tween(600),
        label = "scopedPrimaryContainer"
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
                    MaterialTheme(
                        colorScheme = MaterialTheme.colorScheme.copy(
                            primaryContainer = scopedPrimaryContainer,
                            onPrimaryContainer = if (scopedPrimaryContainer.luminance() > 0.55f) Color.Black else Color.White
                        )
                    ) {
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
                            scrubberMode = scrubberMode,
                            onToggleScrubberMode = { playbackViewModel.toggleScrubberMode() },
                            artGestureHintShown = artGestureHintShown,
                            onMarkArtGestureHintShown = { playbackViewModel.markArtGestureHintShown() },
                            heartBurstEvent = heartBurstEvent,
                            onHeartBurst = { x, y -> playbackViewModel.triggerHeartBurst(x, y) },
                            onHeartBurstComplete = { playbackViewModel.clearHeartBurst() },
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            expandLyrics = {
                                scope.launch { listState.animateScrollToItem(1) }
                            }
                        )
                    }
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
    scrubberMode: ScrubberMode,
    onToggleScrubberMode: () -> Unit,
    artGestureHintShown: Boolean,
    onMarkArtGestureHintShown: () -> Unit,
    heartBurstEvent: HeartBurstEvent?,
    onHeartBurst: (Float, Float) -> Unit,
    onHeartBurstComplete: () -> Unit,
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

    val targetTitleWeight = titleWeightForLength(title.length)
    val animatedTitleWeight by animateIntAsState(
        targetValue = targetTitleWeight,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "titleWeight"
    )
    val animatedTitleSpacing by animateFloatAsState(
        targetValue = letterSpacingForWeight(animatedTitleWeight),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "titleSpacing"
    )

    // Main Container with immersive blurred background
        // Kinetic art stage: only breathe when actively playing and motion is allowed.
        val prefersReducedMotion = rememberPrefersReducedMotion()
        val artBreathScale = if (isPlaying && !prefersReducedMotion) {
            val artInfinite = rememberInfiniteTransition(label = "artBreath")
            val breathingScale by artInfinite.animateFloat(
                initialValue = 1f,
                targetValue = 1.018f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 2800, easing = androidx.compose.animation.core.EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "artBreathScale"
            )
            breathingScale
        } else {
            1f
        }
        val effectiveArtScale by animateFloatAsState(
            targetValue = artBreathScale,
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
        val configuration = LocalConfiguration.current
        val albumCardSize = configuration.screenWidthDp.dp
        val albumCardShape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = 40.dp,
            bottomEnd = 40.dp
        )
        var lastTapOffset by remember { mutableStateOf(Offset.Zero) }
        var showGestureHint by remember { mutableStateOf(!artGestureHintShown) }
        val artHintPulseScale by animateFloatAsState(
            targetValue = if (showGestureHint) 0.97f else 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
            label = "artHintPulse"
        )

        LaunchedEffect(artGestureHintShown) {
            if (!artGestureHintShown) {
                onMarkArtGestureHintShown()
                delay(3000)
                showGestureHint = false
            }
        }

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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, MaterialTheme.colorScheme.surface),
                            startY = with(LocalDensity.current) { configuration.screenHeightDp.dp.toPx() * 0.35f }
                        )
                    )
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
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(albumCardSize)
                        .graphicsLayer { scaleX = effectiveArtScale * artHintPulseScale; scaleY = effectiveArtScale * artHintPulseScale }
                        .shadow(elevation = artStageElevation, shape = albumCardShape, clip = false)
                ) {
                    val ghost1 = pagerItems.getOrNull((currentIndex + 1).coerceAtMost((pagerItems.size - 1).coerceAtLeast(0)))
                    val ghost2 = pagerItems.getOrNull((currentIndex + 2).coerceAtMost((pagerItems.size - 1).coerceAtLeast(0)))

                    // Pager parallax: fraction in [-1, 1]; 0 = settled on a page
                    val swipeFrac = abs(pagerState.currentPageOffsetFraction)

                    if (ghost2 != null && ghost2.stableKey != pagerItems.getOrNull(currentIndex)?.stableKey) {
                        // Card 2 spreads further apart and fades more aggressively on swipe
                        val g2OffsetX = lerp(6f, 14f, swipeFrac).dp
                        val g2OffsetY = lerp(6f, 14f, swipeFrac).dp
                        val g2Alpha = lerp(0.3f, 0.05f, swipeFrac)
                        AsyncImage(
                            model = ghost2.coverUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .offset(x = g2OffsetX, y = g2OffsetY)
                                .scale(0.95f)
                                .alpha(g2Alpha)
                                .clip(albumCardShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    if (ghost1 != null && ghost1.stableKey != pagerItems.getOrNull(currentIndex)?.stableKey) {
                        // Card 1 spreads slightly and fades on swipe
                        val g1OffsetX = lerp(3f, 8f, swipeFrac).dp
                        val g1OffsetY = lerp(3f, 8f, swipeFrac).dp
                        val g1Alpha = lerp(0.6f, 0.15f, swipeFrac)
                        AsyncImage(
                            model = ghost1.coverUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .offset(x = g1OffsetX, y = g1OffsetY)
                                .scale(0.975f)
                                .alpha(g1Alpha)
                                .clip(albumCardShape),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(albumCardShape)
                            .pointerInput(Unit) {
                                detectTapGestures(onPress = { offset ->
                                    lastTapOffset = offset
                                    tryAwaitRelease()
                                })
                            }
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onNavigateToAlbum() },
                                onDoubleClick = {
                                    connectionViewModel.toggleFavorite(currentTrack.path)
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onHeartBurst(lastTapOffset.x, lastTapOffset.y)
                                },
                                onLongClick = { showPlaylistDialog = true }
                            )
                            .semantics {
                                onClick(label = "Go to album") { onNavigateToAlbum(); true }
                                customActions = listOf(
                                    CustomAccessibilityAction("Like track") {
                                        connectionViewModel.toggleFavorite(currentTrack.path)
                                        true
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
                    
                    Box(
                        modifier = Modifier
                            .clip(albumCardShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!displayCover.isNullOrEmpty()) {
                            val context = LocalContext.current
                            val artRequest = remember(displayCover) {
                                ImageRequest.Builder(context)
                                    .data(displayCover)
                                    .crossfade(false)
                                    .build()
                            }
                            AsyncImage(
                                model = artRequest,
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

                    HeartBurstOverlay(
                        event = heartBurstEvent,
                        reduceMotion = prefersReducedMotion,
                        onComplete = onHeartBurstComplete
                    )
                }
            }

            AnimatedVisibility(visible = showGestureHint) {
                Text(
                    text = "Tap for album  ·  Double tap to like",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                ) {
                AnimatedContent(
                    targetState = title.ifEmpty { "No Track" },
                    transitionSpec = {
                        (slideInVertically(initialOffsetY = { it }) + fadeIn(tween(200))) togetherWith
                            (slideOutVertically(targetOffsetY = { -it }) + fadeOut(tween(150)))
                    },
                    label = "titleTrackChange"
                ) { animatedTitle ->
                    Text(
                        text = animatedTitle,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = MPlus1pRoundedFamily,
                            fontWeight = FontWeight(animatedTitleWeight),
                            letterSpacing = animatedTitleSpacing.em
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee(iterations = Int.MAX_VALUE)
                    )
                }
                
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
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = MPlus1pRoundedFamily,
                                fontWeight = FontWeight.W400
                            ),
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
                        label = "Queue"
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
                    // Duration is in milliseconds — convert to seconds for display
                    val durationSecs = duration / 1000L
                    val elapsedSecs = (progress * durationSecs).toLong().coerceIn(0L, durationSecs.coerceAtLeast(0L))
                    val totalSecs = durationSecs.coerceAtLeast(0L)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .align(Alignment.CenterHorizontally),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(elapsedSecs),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.weight(1f))
                        IconButton(
                            onClick = onToggleScrubberMode,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (scrubberMode == ScrubberMode.WAVEFORM) Icons.Outlined.GraphicEq else Icons.Outlined.HorizontalRule,
                                contentDescription = if (scrubberMode == ScrubberMode.WAVEFORM) "Switch to classic progress bar" else "Switch to waveform",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = formatTime(totalSecs),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedContent(
                        targetState = scrubberMode,
                        transitionSpec = { fadeIn(tween(120)) togetherWith fadeOut(tween(100)) },
                        label = "scrubberModeSwap"
                    ) { mode ->
                        if (mode == ScrubberMode.WAVEFORM) {
                            WaveformScrubber(
                                amplitudes = rememberWaveformAmplitudes(currentTrack.path),
                                progress = progress,
                                onSeek = onSeek,
                                modifier = Modifier
                                    .fillMaxWidth(0.88f)
                                    .height(48.dp)
                                    .align(Alignment.CenterHorizontally)
                            )
                        } else {
                            SquigglyProgressBar(
                                progress = progress,
                                isPlaying = isPlaying,
                                thumbColor = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f),
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                onSeek = onSeek,
                                modifier = Modifier
                                    .fillMaxWidth(0.88f)
                                    .align(Alignment.CenterHorizontally)
                                    .height(30.dp)
                            )
                        }
                    }
                
                Spacer(modifier = Modifier.height(32.dp))

                // Main Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterHorizontally)
                ) {
                    IconButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            connectionViewModel.toggleShuffle()
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                if (isShuffled) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f),
                                CircleShape
                            )
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (isShuffled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSkipPrevious()
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f), CircleShape)
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous track",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    FloatingActionButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPlayPauseToggle()
                        },
                        modifier = Modifier.size(96.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    IconButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSkipNext()
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f), CircleShape)
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            Icons.Rounded.SkipNext,
                            contentDescription = "Next track",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    val repeatActive = repeatMode == "all" || repeatMode == "one"
                    IconButton(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            connectionViewModel.toggleRepeat()
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                if (repeatActive) MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f),
                                CircleShape
                            )
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            if (repeatMode == "one") Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                            contentDescription = "Repeat",
                            tint = if (repeatActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTogglePlaybackLocation()
                        },
                        shape = RoundedCornerShape(22.dp),
                        color = if (isMobilePlayback) {
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.26f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                        },
                        contentColor = if (isMobilePlayback) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .height(44.dp)
                            .minimumInteractiveComponentSize()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isMobilePlayback) Icons.Rounded.PhoneAndroid else Icons.Rounded.Computer,
                                contentDescription = if (isMobilePlayback) "Mobile playback" else "Computer playback",
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (isMobilePlayback) "Mobile" else "PC",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    val volumeValue = volume.toFloat().coerceIn(0f, 1f)
                    Icon(
                        imageVector = Icons.Rounded.VolumeDown,
                        contentDescription = "Volume down",
                        modifier = Modifier
                            .size(24.dp)
                            .minimumInteractiveComponentSize()
                            .clickable {
                                val newVolume = (volumeValue - 0.1f).coerceIn(0f, 1f)
                                connectionViewModel.setVolume(newVolume.toDouble())
                            },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Slider(
                        value = volumeValue,
                        onValueChange = { connectionViewModel.setVolume(it.coerceIn(0f, 1f).toDouble()) },
                        valueRange = 0f..1f,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
                        )

                        )

                        

                    Icon(
                        imageVector = Icons.Rounded.VolumeUp,
                        contentDescription = "Volume up",
                        modifier = Modifier
                            .size(24.dp)
                            .minimumInteractiveComponentSize()
                            .clickable {
                                val newVolume = (volumeValue + 0.1f).coerceIn(0f, 1f)
                                connectionViewModel.setVolume(newVolume.toDouble())
                            },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
        val trackCounterText = if (queue.isNotEmpty()) {
            "${(currentIndex + 1).coerceIn(1, queue.size)} / ${queue.size}"
        } else {
            "0 / 0"
        }

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
                    .size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                MorphDismissButton(onClick = onBackToLibrary)
            }

            Text(
                text = trackCounterText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

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
    onDragEnd: () -> Unit = {},
    modifier: Modifier = Modifier
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
        modifier = modifier
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

@Composable
fun WaveformScrubber(
    amplitudes: List<Float>,
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeBarColor = MaterialTheme.colorScheme.primary
    val inactiveBarColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.30f)
    val thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.95f)
    var widthPx by remember { mutableFloatStateOf(1f) }
    var fineSeek by remember { mutableStateOf(false) }
    val fineScale by animateFloatAsState(
        targetValue = if (fineSeek) 2f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "fineSeekScale"
    )
    val clamped = progress.coerceIn(0f, 1f)

    Canvas(
        modifier = modifier
            .graphicsLayer { scaleX = fineScale }
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { fineSeek = true },
                    onPress = {
                        tryAwaitRelease()
                        fineSeek = false
                    },
                    onTap = { offset ->
                        onSeek((offset.x / widthPx).coerceIn(0f, 1f))
                    }
                )
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    onSeek((change.position.x / widthPx).coerceIn(0f, 1f))
                }
            }
            .semantics {
                progressBarRangeInfo = androidx.compose.ui.semantics.ProgressBarRangeInfo(clamped, 0f..1f)
                setProgress(label = "Seek") { newValue ->
                    onSeek(newValue.coerceIn(0f, 1f))
                    true
                }
            }
    ) {
        val bars = amplitudes.ifEmpty { List(56) { 0.2f } }
        val barSlot = size.width / bars.size
        val barWidth = barSlot * 0.6f
        val progressX = clamped * size.width
        bars.forEachIndexed { index, amp ->
            val x = index * barSlot
            val h = (size.height * amp.coerceIn(0f, 1f)).coerceAtLeast(size.height * 0.12f)
            val y = (size.height - h) / 2f
            drawRoundRect(
                color = if (x <= progressX) activeBarColor else inactiveBarColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }
        drawCircle(
            color = thumbColor,
            radius = 10.dp.toPx(),
            center = Offset(progressX.coerceIn(0f, size.width), size.height / 2f)
        )
    }
}

@Composable
fun HeartBurstOverlay(
    event: HeartBurstEvent?,
    reduceMotion: Boolean,
    onComplete: () -> Unit
) {
    if (event == null) return

    if (reduceMotion) {
        var visible by remember(event.timestampMs) { mutableStateOf(true) }
        LaunchedEffect(event.timestampMs) {
            delay(300)
            visible = false
            onComplete()
        }
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(100)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.fillMaxSize().semantics { invisibleToUser() }
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(84.dp)
                )
            }
        }
        return
    }

    data class Particle(val angle: Float, val distance: Float, val size: Float, val rotation: Float, val secondary: Boolean, val delayMs: Int)
    val particles = remember(event.timestampMs) {
        List(10) { index ->
            Particle(
                angle = Random.nextFloat() * (2f * PI.toFloat()),
                distance = Random.nextInt(80, 160).toFloat(),
                size = Random.nextInt(16, 40).toFloat(),
                rotation = Random.nextFloat() * 60f - 30f,
                secondary = index >= 7,
                delayMs = index * 40
            )
        }
    }
    val progressAnims = remember(event.timestampMs) { particles.map { Animatable(0f) } }
    LaunchedEffect(event.timestampMs) {
        progressAnims.forEachIndexed { index, anim ->
            launch {
                delay(particles[index].delayMs.toLong())
                anim.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
            }
        }
        delay(980)
        onComplete()
    }

    Box(modifier = Modifier.fillMaxSize().semantics { invisibleToUser() }) {
        particles.forEachIndexed { index, particle ->
            val p = progressAnims[index].value
            val eased = 1f - (1f - p) * (1f - p) * (1f - p)
            val tx = particle.distance * eased * kotlin.math.cos(particle.angle)
            val ty = particle.distance * eased * kotlin.math.sin(particle.angle)
            val scale = when {
                p < 0.4f -> 1.2f * (p / 0.4f)
                p < 0.6f -> 1.2f - ((p - 0.4f) / 0.2f) * 0.2f
                else -> 1f
            }
            val alpha = if (p <= 0.6f) 1f else (1f - (p - 0.6f) / 0.4f).coerceIn(0f, 1f)
            Icon(
                Icons.Filled.Favorite,
                contentDescription = null,
                tint = if (particle.secondary) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset { IntOffset((event.x + tx).roundToInt(), (event.y + ty).roundToInt()) }
                    .size(particle.size.dp)
                    .graphicsLayer {
                        rotationZ = particle.rotation
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
            )
        }
    }
}

@Composable
fun MorphDismissButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val progress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "dismissMorph"
    )
    val iconColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .size(40.dp)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .semantics { contentDescription = "Close player" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            val topY = lerp(0.30f, 0.26f, progress)
            val midY = lerp(0.58f, 0.66f, progress)
            val path = Path().apply {
                moveTo(size.width * 0.2f, size.height * topY)
                lineTo(size.width * 0.5f, size.height * midY)
                lineTo(size.width * 0.8f, size.height * topY)
            }
            drawPath(
                path = path,
                color = iconColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

private fun titleWeightForLength(charCount: Int): Int = when {
    charCount <= 12 -> 800
    charCount <= 24 -> lerp(800f, 300f, (charCount - 12) / 12f).toInt()
    else -> 300
}

private fun letterSpacingForWeight(weight: Int): Float {
    val t = (weight - 300f) / 500f
    return lerp(-0.02f, 0.01f, 1f - t)
}

private fun Modifier.fadingEdgeMask(): Modifier = drawWithContent {
    drawContent()
    drawRect(
        brush = Brush.horizontalGradient(
            0f to Color.Transparent,
            0.05f to Color.White,
            0.95f to Color.White,
            1f to Color.Transparent
        ),
        blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
    )
}

private fun rememberWaveformAmplitudes(seedKey: String): List<Float> {
    val seed = seedKey.hashCode().toLong()
    val random = Random(seed)
    return List(56) { random.nextFloat().coerceIn(0.1f, 1f) }
}

private fun extractBpmFromTrack(track: MediaSessionData): Float? {
    val combined = listOf(track.title, track.artist, track.album).joinToString(" ")
    val regex = Regex("(\\d{2,3})\\s*bpm", RegexOption.IGNORE_CASE)
    val bpm = regex.find(combined)?.groupValues?.getOrNull(1)?.toFloatOrNull()
    return bpm?.coerceIn(40f, 240f)
}

private fun shiftHue(color: Color, delta: Float): Color {
    val hsl = FloatArray(3)
    androidx.core.graphics.ColorUtils.colorToHSL(color.toArgb(), hsl)
    hsl[0] = (hsl[0] + delta + 360f) % 360f
    val shifted = androidx.core.graphics.ColorUtils.HSLToColor(hsl)
    return Color(shifted).copy(alpha = color.alpha)
}

@Composable
private fun PixelStyleToggleRow(
    isMobilePlayback: Boolean,
    isShuffled: Boolean,
    repeatMode: String,
    onTogglePlaybackLocation: () -> Unit,
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rowShape = RoundedCornerShape(30.dp)

    Surface(
        modifier = modifier,
        shape = rowShape,
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PixelStyleToggleButton(
                modifier = Modifier.weight(1f),
                active = isMobilePlayback,
                icon = if (isMobilePlayback) Icons.Rounded.PhoneAndroid else Icons.Rounded.Computer,
                contentDescription = if (isMobilePlayback) "Switch to computer playback" else "Switch to mobile playback",
                activeColor = MaterialTheme.colorScheme.tertiary,
                activeContentColor = MaterialTheme.colorScheme.onTertiary,
                inactiveColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onTogglePlaybackLocation
            )

            PixelStyleToggleButton(
                modifier = Modifier.weight(1f),
                active = isShuffled,
                icon = Icons.Rounded.Shuffle,
                contentDescription = "Shuffle",
                activeColor = MaterialTheme.colorScheme.primary,
                activeContentColor = MaterialTheme.colorScheme.onPrimary,
                inactiveColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onShuffleToggle
            )

            val repeatActive = repeatMode == "all" || repeatMode == "one"
            PixelStyleToggleButton(
                modifier = Modifier.weight(1f),
                active = repeatActive,
                icon = if (repeatMode == "one") Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                contentDescription = "Repeat",
                activeColor = MaterialTheme.colorScheme.secondary,
                activeContentColor = MaterialTheme.colorScheme.onSecondary,
                inactiveColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onRepeatToggle
            )
        }
    }
}

@Composable
private fun PixelStyleToggleButton(
    modifier: Modifier,
    active: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    activeColor: Color,
    activeContentColor: Color,
    inactiveColor: Color,
    inactiveContentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(52.dp)
            .minimumInteractiveComponentSize(),
        shape = RoundedCornerShape(26.dp),
        color = if (active) activeColor else inactiveColor,
        contentColor = if (active) activeContentColor else inactiveContentColor,
        tonalElevation = if (active) 2.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// End of NowPlayingScreen.kt
