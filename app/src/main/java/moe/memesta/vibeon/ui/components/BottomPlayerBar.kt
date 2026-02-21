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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
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

// Design tokens for non-accent colors
private val NavBarBg = Color(0x0F0F14E0)  // rgba(15,15,20,0.88)
private val NavBorderColor = Color.White.copy(alpha = 0.1f)
// NavAccent is now MaterialTheme.colorScheme.primary for dynamic theming
private val NavInactive = Color(0xFF8A8A9A)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BottomPlayerBar(
    navController: NavController,
    connectionViewModel: ConnectionViewModel,
    playbackViewModel: PlaybackViewModel,
    onNavigateToPlayer: () -> Unit,
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
    val displayLanguage = LocalDisplayLanguage.current
    val title = currentTrack.getDisplayName(displayLanguage)
    val artist = currentTrack.getDisplayArtist(displayLanguage)

    val progress = playbackState.progress

    // Determine effective route for the nav button
    val effectiveRoute = if (currentRoute == "main" && pagerState != null) {
        when (pagerState.currentPage) {
            0 -> "library"
            1 -> "albums"
            2 -> "playlists"
            3 -> "artists"
            4 -> "settings"
            else -> "library"
        }
    } else {
        currentRoute
    }

    // --- Unified Bottom Bar: Player + Navigation in One Pill ---
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- Left Pill: Now Playing Info ---
        AnimatedVisibility(
            visible = currentTrack.title != "No Track",
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(40.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(40.dp))
                    .clickable { onNavigateToPlayer() }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { change, dragAmount ->
                            val threshold = 20.dp.toPx()
                            if (dragAmount < -threshold) {
                                change.consume()
                                onNavigateToPlayer()
                            }
                        }
                    }
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
                                    isPlaying = isPlaying,
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = artist,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Play/Pause Button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = { if (isPlaying) connectionViewModel.pause() else connectionViewModel.play() },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
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
                navController.navigate("search") {
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
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
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
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
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    // Pulsing glow animation for album art when playing
    val infiniteTransition = rememberInfiniteTransition(label = "albumArtPulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.0f,
        targetValue = if (isPlaying) 0.55f else 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isPlaying) 1.08f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    Box(
        modifier = Modifier
            .size(52.dp)
            .scale(glowScale),
        contentAlignment = Alignment.Center
    ) {
        // Glow ring (removed for matte look)
        /*
        if (isPlaying) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha),
                        CircleShape
                    )
            )
        }
        */

        with(sharedTransitionScope) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(moe.memesta.vibeon.ui.components.AlbumArtStarShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), moe.memesta.vibeon.ui.components.AlbumArtStarShape)
                    .background(Color.DarkGray)
            ) {
                if (coverUrl != null) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val request = remember(coverUrl) {
                        coil.request.ImageRequest.Builder(context)
                            .data(coverUrl)
                            .crossfade(true)
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