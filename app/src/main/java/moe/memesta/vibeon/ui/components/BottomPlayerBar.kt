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
    modifier: Modifier = Modifier
) {
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
                                        AsyncImage(
                                            model = currentTrack.coverUrl,
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
                    NavigationItem("library", "Home", Icons.Filled.Home),
                    NavigationItem("albums", "Albums", Icons.Filled.Album),
                    NavigationItem("search", "Search", Icons.Filled.Search),
                    NavigationItem("artists", "Artist", Icons.Filled.Person),
                    NavigationItem("settings", "Settings", Icons.Filled.Settings)
                )

                items.forEach { item ->
                    val isSelected = currentRoute == item.route || 
                                     (item.route == "library" && currentRoute == "discovery")
                    
                    // Press animation with organic spring
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) VibeAnimations.PressScale else 1f,
                        animationSpec = VibeAnimations.SpringExpressive,
                        label = "navItemScale"
                    )
                    
                    // Smooth color transition
                    val iconColor by animateColorAsState(
                        targetValue = if (isSelected) accentColor else Color.Gray,
                        animationSpec = VibeAnimations.springStandardGeneric(),
                        label = "navItemColor"
                    )
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .scale(scale)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null  // Remove ripple for cleaner press effect
                            ) {
                                if (currentRoute != item.route) {
                                    // Proper back navigation: removed popUpTo so back respects history
                                    navController.navigate(item.route) {
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                            .padding(8.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .height(32.dp)
                                .width(if (isSelected) 50.dp else 32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) Color(0xFF3E2C2C) else Color.Transparent)
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = iconColor,
                                modifier = Modifier.size(24.dp)
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

data class NavigationItem(val route: String, val label: String, val icon: ImageVector)
