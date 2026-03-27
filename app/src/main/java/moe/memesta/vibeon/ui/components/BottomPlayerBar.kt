package moe.memesta.vibeon.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.SpeakerGroup
import androidx.compose.material3.*
import androidx.compose.foundation.pager.PagerState
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.foundation.Canvas
import kotlin.math.abs
import kotlin.math.sin
import coil.compose.AsyncImage
import moe.memesta.vibeon.ui.ConnectionViewModel
import moe.memesta.vibeon.ui.PlaybackViewModel
import moe.memesta.vibeon.ui.theme.VibeAnimations
import moe.memesta.vibeon.ui.theme.Dimens
import moe.memesta.vibeon.ui.theme.bouncyClickable
import moe.memesta.vibeon.ui.utils.LocalDisplayLanguage
import moe.memesta.vibeon.ui.utils.getDisplayArtist
import moe.memesta.vibeon.ui.utils.getDisplayName
import android.os.Build
import moe.memesta.vibeon.ui.shapes.*
import moe.memesta.vibeon.ui.theme.OrbitPlayButton

// Design tokens for non-accent colors
private val NavBarBg = Color(0xFF0F0F14)
private val NavBorderColor = Color.White
// NavAccent is now MaterialTheme.colorScheme.primary for dynamic theming
private val NavInactive = Color(0xFF8A8A9A)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BottomPlayerBar(
    navController: NavController,
    connectionViewModel: ConnectionViewModel,
    playbackViewModel: PlaybackViewModel,
    onNavigateToPlayer: () -> Unit,
    onNavigateToSearch: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    pagerState: PagerState? = null,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Player State
    val playbackState by playbackViewModel.playbackState.collectAsState()
    val currentTrack by connectionViewModel.currentTrack.collectAsState()
    val isPlaying by connectionViewModel.isPlaying.collectAsState()
    val isMobilePlayback by playbackViewModel.isMobilePlayback.collectAsState()
    val effectiveIsPlaying = if (isMobilePlayback) playbackState.isPlaying else isPlaying
    val displayLanguage = LocalDisplayLanguage.current
    val title = currentTrack.getDisplayName(displayLanguage)
    val artist = currentTrack.getDisplayArtist(displayLanguage)

    val progress = playbackState.progress
    val sharedKeyBase = currentTrack.path.ifEmpty { "no-track" }

    // Determine effective route for the nav button
    val effectiveRoute = if (currentRoute == "main" && pagerState != null) {
        when (pagerState.currentPage) {
            0 -> "library"
            1 -> "albums"
            2 -> "stats"
            3 -> "artists"
            4 -> "settings"
            else -> "library"
        }
    } else {
        currentRoute
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // --- Unified Bottom Bar: Player + Navigation in One Pill ---
        Row(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .padding(top = 12.dp, bottom = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
        // --- Left Pill: Now Playing Info ---
        AnimatedVisibility(
            visible = currentTrack.title != "No Track",
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.weight(1f).widthIn(max = 280.dp)
        ) {
            // State for swipe gesture animations
            val offsetXState = remember { mutableFloatStateOf(0f) }
            val animatedOffsetX by animateFloatAsState(
                targetValue = offsetXState.floatValue,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "pillSwipeOffset"
            )
            
            // Scale effect for feedback
            val scale by animateFloatAsState(
                targetValue = if (abs(offsetXState.floatValue) > 50f) 0.95f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessHigh
                ),
                label = "pillScale"
            )
            
            // Track transition animation
            var trackKey by remember { mutableStateOf(currentTrack.path) }
            val trackChanged = trackKey != currentTrack.path
            if (trackChanged) {
                trackKey = currentTrack.path
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .graphicsLayer {
                        // Apply swipe offset for visual feedback
                        translationX = animatedOffsetX
                        // Add slight rotation for dynamic effect
                        rotationZ = animatedOffsetX * 0.01f
                        // Scale effect when swiping
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .shadow(elevation = 8.dp, shape = CircleShape, spotColor = Color.Black)
                    .pointerInput(connectionViewModel) {
                        detectDragGestures(
                            onDragStart = { },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                offsetXState.floatValue += dragAmount.x
                                offsetXState.floatValue = offsetXState.floatValue.coerceIn(-200f, 200f)
                            },
                            onDragEnd = {
                                val threshold = 80.dp.toPx()
                                val currentOffset = offsetXState.floatValue
                                when {
                                    currentOffset > threshold -> {
                                        scope.launch {
                                            android.util.Log.i("BottomPlayerBar", "Swipe detected: previous (offset=$currentOffset)")
                                            connectionViewModel.previous()
                                        }
                                    }
                                    currentOffset < -threshold -> {
                                        scope.launch {
                                            android.util.Log.i("BottomPlayerBar", "Swipe detected: next (offset=$currentOffset)")
                                            connectionViewModel.next()
                                        }
                                    }
                                }
                                offsetXState.floatValue = 0f
                            },
                            onDragCancel = {
                                offsetXState.floatValue = 0f
                            }
                        )
                    }
                        .clickable(onClick = onNavigateToPlayer)
                    ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Player Content
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Album Art + Info
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            with(sharedTransitionScope) {
                                AlbumArtWithPulse(
                                    coverUrl = currentTrack.coverUrl,
                                    isPlaying = effectiveIsPlaying,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope,
                                    sharedKey = sharedKeyBase
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Animated track info
                            AnimatedContent(
                                targetState = currentTrack.path,
                                transitionSpec = {
                                    (fadeIn(animationSpec = tween(300)) + 
                                     slideInHorizontally { it / 2 })
                                        .togetherWith(
                                            fadeOut(animationSpec = tween(200)) +
                                            slideOutHorizontally { -it / 2 }
                                        )
                                },
                                label = "trackInfoTransition"
                            ) { _ ->
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .basicMarquee(iterations = Int.MAX_VALUE)
                                    )
                                    Text(
                                        text = artist,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Play/Pause Button
                        // Play/Pause — OrbitPlayButton with orbit arc while playing
                        OrbitPlayButton(
                            isPlaying = effectiveIsPlaying,
                            onClick = {
                                if (isMobilePlayback) {
                                    val nextPlayState = !effectiveIsPlaying
                                    playbackViewModel.setPlayerPlayWhenReady(nextPlayState)
                                    playbackViewModel.updateIsPlaying(nextPlayState)
                                } else {
                                    if (effectiveIsPlaying) connectionViewModel.pause() else connectionViewModel.play()
                                }
                            },
                            playIcon = Icons.Rounded.PlayArrow,
                            pauseIcon = Icons.Rounded.Pause,
                            size = 44.dp
                        )
                    }
                }
            }
        }

        // --- Right Pill: Dynamic Nav Button ---
        DynamicNavButton(
            currentRoute = effectiveRoute,
            onNavigate = { route ->
                val pageIndex = NavPages.find { it.route == route }?.pageIndex ?: 0
                if (pagerState != null && currentRoute == "main") {
                    scope.launch { pagerState.animateScrollToPage(pageIndex) }
                } else if (currentRoute != route) {
                    navController.navigate(if (route == "library") "main" else route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            onSearchTap = {
                onNavigateToSearch()
            }
        )
    }
}
}

@Composable
private fun NavTabItem(
    item: NavigationItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val iconScale by animateFloatAsState(
        targetValue = when {
            isSelected -> 1.2f
            isPressed -> 0.9f
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "navIconScale"
    )

    val iconColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else NavInactive,
        animationSpec = VibeAnimations.springStandardGeneric(),
        label = "navIconColor"
    )

    val labelColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else NavInactive,
        animationSpec = VibeAnimations.springStandardGeneric(),
        label = "navLabelColor"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .then(
                    if (isSelected) Modifier
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(12.dp)
                        )
                    else Modifier
                )
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = iconColor,
                modifier = Modifier
                    .size(Dimens.IconSize)
                    .scale(iconScale)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall,
            color = labelColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun AlbumArtWithPulse(
    coverUrl: String?,
    isPlaying: Boolean,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    sharedKey: String
) {
    val miniArtShape = RoundedCornerShape(14.dp)

    Box(
        modifier = Modifier.size(52.dp),
        contentAlignment = Alignment.Center
    ) {
        with(sharedTransitionScope) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "album-$sharedKey"),
                        animatedVisibilityScope = animatedVisibilityScope,
                        clipInOverlayDuringTransition = OverlayClip(miniArtShape)
                    )
                    .clip(miniArtShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary, miniArtShape)
                    .background(Color.DarkGray)
            ) {
                if (coverUrl != null) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val request = remember(coverUrl) {
                        coil.request.ImageRequest.Builder(context)
                            .data(coverUrl)
                            .crossfade(false)
                            .build()
                    }
                    AsyncImage(
                        model = request,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

private val EaseInOutQuad = CubicBezierEasing(0.45f, 0f, 0.55f, 1f)

data class NavigationItem(val route: String, val label: String, val icon: ImageVector, val pageIndex: Int)
fun Modifier.glassmorphicBackground(): Modifier = composed {
    this.then(
        Modifier.background(MaterialTheme.colorScheme.surface)
    )
}