package moe.memesta.vibeon.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material3.LinearProgressIndicator
import coil.compose.AsyncImage
import moe.memesta.vibeon.ui.ConnectionViewModel
import moe.memesta.vibeon.ui.PlaybackViewModel
import moe.memesta.vibeon.ui.theme.VibeAnimations
import moe.memesta.vibeon.ui.theme.Dimens
import moe.memesta.vibeon.ui.theme.bouncyClickable
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import kotlin.math.abs

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
    
    val progress = playbackState.progress

    // Stitch Colors (Approximate from screenshot)
    val accentColor = Color(0xFFE57373) // Salmon/Red

    Box(
        modifier = modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .shadow(elevation = 24.dp, shape = RoundedCornerShape(28.dp), spotColor = Color.Black.copy(alpha = 0.5f))
            .clip(RoundedCornerShape(28.dp))
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(Color(0xFF2E2E2E), Color(0xFF222222))
                )
            )
            .fillMaxWidth()
    ) {
        Column {
            // --- Mini Player Section ---
            AnimatedVisibility(visible = currentTrack.title != "No Track") {
                Column(
                    modifier = Modifier.pointerInput(Unit) {
                        detectVerticalDragGestures { change, dragAmount ->
                            val threshold = 20.dp.toPx()
                            if (dragAmount < -threshold) { // Swipe Up
                                change.consume()
                                onNavigateToPlayer()
                            }
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clickable { onNavigateToPlayer() }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Art + Info
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            with(sharedTransitionScope) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .sharedElement(
                                            state = rememberSharedContentState(key = "album_art_shared"),
                                            animatedVisibilityScope = animatedVisibilityScope,
                                            boundsTransform = { _, _ ->
                                                androidx.compose.animation.core.tween(durationMillis = 500)
                                            }
                                        )
                                        .clip(CircleShape)
                                        .background(Color.DarkGray)
                                ) {
                                    if (currentTrack.coverUrl != null) {
                                        val context = androidx.compose.ui.platform.LocalContext.current
                                        val request = remember(currentTrack.coverUrl) {
                                            coil.request.ImageRequest.Builder(context)
                                                .data(currentTrack.coverUrl)
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
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column {
                                Text(
                                    text = currentTrack.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = currentTrack.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFE57373),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { /* TODO: Toggle Heart */ }) {
                                Icon(
                                    imageVector = Icons.Rounded.FavoriteBorder,
                                    contentDescription = "Like",
                                    tint = Color.Gray
                                )
                            }
                            
                            IconButton(
                                onClick = { if (isPlaying) connectionViewModel.pause() else connectionViewModel.play() },
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(accentColor, CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                    
                    // Smooth animated progress (no shimmer - was causing perpetual animation)
                    val animatedProgress by animateFloatAsState(
                        targetValue = progress,
                        animationSpec = VibeAnimations.SpringStandard,
                        label = "progressAnimation"
                    )

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                    ) {
                        // Track
                        drawRect(Color.White.copy(alpha = 0.1f))
                        // Progress - solid accent color
                        drawRect(
                            color = accentColor,
                            size = androidx.compose.ui.geometry.Size(
                                width = size.width * animatedProgress,
                                height = size.height
                            )
                        )
                    }
                }
            }

            // --- Navigation Bar Section ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .background(Color(0xFF1A1A1A)),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val items = listOf(
                    NavigationItem("library", "Home", Icons.Filled.Home, 0),
                    NavigationItem("albums", "Albums", Icons.Filled.Album, 1),
                    NavigationItem("search", "Search", Icons.Filled.Search, 2),
                    NavigationItem("artists", "Artist", Icons.Filled.Person, 3),
                    NavigationItem("settings", "Settings", Icons.Filled.Settings, 4)
                )

                items.forEach { item ->
                    val isSelected = if (pagerState != null && currentRoute == "main") {
                        pagerState.currentPage == item.pageIndex
                    } else {
                        currentRoute == item.route || (item.route == "library" && currentRoute == "discovery")
                    }
                    
                    // Smooth color transition
                    val iconColor by animateColorAsState(
                        targetValue = if (isSelected) accentColor else Color.Gray,
                        animationSpec = VibeAnimations.springStandardGeneric(),
                        label = "navItemColor"
                    )
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .bouncyClickable(onClick = {
                                if (pagerState != null && currentRoute == "main") {
                                    scope.launch {
                                        pagerState.animateScrollToPage(item.pageIndex)
                                    }
                                } else if (currentRoute != item.route) {
                                    navController.navigate(if (item.route == "library") "main" else item.route) {
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            })
                            .padding(8.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .height(32.dp)
                                .width(if (isSelected) 50.dp else 32.dp)
                                .clip(RoundedCornerShape(Dimens.CornerRadiusLarge))
                                .background(if (isSelected) Color(0xFF3E2C2C) else Color.Transparent)
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = iconColor,
                                modifier = Modifier.size(Dimens.IconSize)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = iconColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

data class NavigationItem(val route: String, val label: String, val icon: ImageVector, val pageIndex: Int)
