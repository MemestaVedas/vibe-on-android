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

    // --- Unified Bottom Bar: Player + Navigation in One Pill ---
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // --- Top Section: Now Playing Info ---
                AnimatedVisibility(
                    visible = currentTrack.title != "No Track",
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
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
                                    .height(56.dp)
                                    .padding(horizontal = 12.dp),
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

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = artist,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Playback Location Toggle
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                                        .clickable {
                                            if (isMobilePlayback) playbackViewModel.stopMobilePlayback()
                                            else playbackViewModel.requestMobilePlayback()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isMobilePlayback) Icons.Rounded.Smartphone else Icons.Rounded.SpeakerGroup,
                                        contentDescription = "Toggle Playback Location",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))

                                // Play/Pause Button
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    IconButton(
                                        onClick = { if (isPlaying) connectionViewModel.pause() else connectionViewModel.play() },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                            contentDescription = "Play/Pause",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // Progress bar
                            val animatedProgress by animateFloatAsState(
                                targetValue = progress,
                                animationSpec = VibeAnimations.SpringStandard,
                                label = "progressAnimation"
                            )
                            val primaryColor = MaterialTheme.colorScheme.primary

                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                            ) {
                                drawRect(Color.White.copy(alpha = 0.06f))
                                drawRect(
                                    color = primaryColor,
                                    size = androidx.compose.ui.geometry.Size(
                                        width = size.width * animatedProgress,
                                        height = size.height
                                    )
                                )
                            }

                            Divider(
                                color = Color.White.copy(alpha = 0.05f),
                                thickness = 1.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                            )
                        }
                    }
                }

                // --- Bottom Section: Navigation Tabs ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val items = listOf(
                        NavigationItem("library", "Home", Icons.Filled.Home, 0),
                        NavigationItem("albums", "Albums", Icons.Filled.Album, 1),
                        NavigationItem("search", "Playlists", Icons.Rounded.QueueMusic, 2),
                        NavigationItem("artists", "Artist", Icons.Filled.Person, 3),
                        NavigationItem("settings", "Settings", Icons.Filled.Settings, 4)
                    )

                    items.forEach { item ->
                        val isSelected = if (pagerState != null && currentRoute == "main") {
                            pagerState.currentPage == item.pageIndex
                        } else {
                            currentRoute == item.route || (item.route == "library" && currentRoute == "discovery")
                        }

                        NavTabItem(
                            item = item,
                            isSelected = isSelected,
                            onClick = {
                                if (pagerState != null && currentRoute == "main") {
                                    scope.launch { pagerState.animateScrollToPage(item.pageIndex) }
                                } else if (currentRoute != item.route) {
                                    navController.navigate(if (item.route == "library") "main" else item.route) {
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
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
            .size(48.dp)
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
                    .size(46.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape)
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