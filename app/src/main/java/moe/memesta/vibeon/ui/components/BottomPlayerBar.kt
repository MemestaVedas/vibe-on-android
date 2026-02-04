package moe.memesta.vibeon.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import moe.memesta.vibeon.ui.ConnectionViewModel
import moe.memesta.vibeon.ui.PlaybackViewModel

@Composable
fun BottomPlayerBar(
    navController: NavController,
    connectionViewModel: ConnectionViewModel,
    playbackViewModel: PlaybackViewModel,
    onNavigateToPlayer: () -> Unit,
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
            .background(Color(0xFF222222))
            .fillMaxWidth()
    ) {
        Column {
            // --- Mini Player Section ---
            AnimatedVisibility(visible = currentTrack.title != "No Track") {
                Column {
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
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
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
                    
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = accentColor,
                        trackColor = Color.Transparent
                    )
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
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo("library") { saveState = true }
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
                                tint = if (isSelected) accentColor else Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) accentColor else Color.Gray,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

data class NavigationItem(val route: String, val label: String, val icon: ImageVector)
