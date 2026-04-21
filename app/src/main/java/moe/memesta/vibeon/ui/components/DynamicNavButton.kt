@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package moe.memesta.vibeon.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.graphics.shapes.RoundedPolygon
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeoutOrNull
import moe.memesta.vibeon.ui.theme.rememberPrefersReducedMotion

import moe.memesta.vibeon.ui.shapes.*


data class NavPage(
    val route: String,
    val label: String,
    val shape: RoundedPolygon,
    val pageIndex: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

val NavPages = listOf(
    NavPage("settings", "Settings", SettingsShape, 4, Icons.Rounded.Settings),
    NavPage("artists", "Artists", ArtistsShape, 3, Icons.Rounded.Person),
    NavPage("stats", "Stats", PlaylistsShape, 2, Icons.Rounded.BarChart),
    NavPage("albums", "Albums", AlbumsShape, 1, Icons.Rounded.Album),
    NavPage("library", "Home", MaterialShapes.Circle, 0, Icons.Rounded.Home)
)

@Composable
fun DynamicNavButton(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onSearchTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    var hoveredIndex by remember { mutableStateOf(-1) }
    val prefersReducedMotion = rememberPrefersReducedMotion()
    
    val currentPage = NavPages.find { it.route == currentRoute || (it.route == "library" && currentRoute == "discovery") } ?: NavPages.last()
    
    // The menu items are all pages
    val menuItems = NavPages
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd
    ) {
        // The popup menu - positioned to the LEFT side
        if (isMenuOpen) {
            Popup(
                alignment = Alignment.BottomStart,
                properties = PopupProperties(focusable = true, dismissOnBackPress = true, dismissOnClickOutside = true),
                onDismissRequest = { isMenuOpen = false }
            ) {
                Row(
                    modifier = Modifier
                        .padding(bottom = 80.dp, start = 24.dp)
                        .fillMaxWidth(0.5f),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Column for shapes with icons inside
                    Column(
                        modifier = Modifier
                            .width(64.dp)
                            .clip(ShapeCache.rounded24)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        menuItems.forEachIndexed { index, page ->
                            val isHovered = index == hoveredIndex
                            val scale by animateFloatAsState(if (isHovered) 1.2f else 1.0f, 
                                animationSpec = if (prefersReducedMotion) {
                                    androidx.compose.animation.core.snap()
                                } else {
                                    spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                },
                                label = "scale"
                            )
                            
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .scale(scale)
                                    .clip(page.shape.toShape())
                                    .background(if (isHovered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        isMenuOpen = false
                                        onNavigate(page.route)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                // Icon appears when hovered
                                if (isHovered) {
                                    Icon(
                                        imageVector = page.icon,
                                        contentDescription = page.label,
                                        tint = if (isHovered) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Column for labels on the left
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .padding(start = 8.dp)
                    ) {
                        menuItems.forEachIndexed { index, page ->
                            Box(
                                modifier = Modifier.height(44.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                val isHovered = index == hoveredIndex
                                if (isHovered) {
                                    Text(
                                        text = page.label,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // The main button (Search) - shape changes based on current page
        val mainScale by animateFloatAsState(
            targetValue = if (isMenuOpen) 1.15f else 1.0f,
            animationSpec = if (prefersReducedMotion) {
                androidx.compose.animation.core.snap()
            } else {
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            },
            label = "mainScale"
        )
        
        Box(
            modifier = Modifier
                .size(56.dp)
                .scale(mainScale)
                .clip(currentPage.shape.toShape())
                .background(MaterialTheme.colorScheme.primary)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        
                        // Decreased activation time from 300ms to 100ms for faster response
                        val upBeforeTimeout = withTimeoutOrNull(100L) {
                            var result: PointerInputChange? = null
                            while (result == null) {
                                val event = awaitPointerEvent()
                                result = event.changes.firstOrNull { it.changedToUp() }
                            }
                            result
                        }
                        
                        if (upBeforeTimeout != null) {
                            // Quick tap - released before timeout
                            onSearchTap()
                        } else {
                            // Long press - timeout occurred
                            isMenuOpen = true
                            hoveredIndex = -1
                            var hasMoved = false
                            
                            // Track drag
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                
                                // Calculate hovered item if finger moved
                                if (change.positionChange() != androidx.compose.ui.geometry.Offset.Zero) {
                                    hasMoved = true
                                    val yOffset = change.position.y - down.position.y
                                    val itemHeightPx = 44.dp.toPx()
                                    val gapPx = 40.dp.toPx()  // Reduced gap for better finger positioning
                                    val relativeY = -yOffset
                                    
                                    if (relativeY > gapPx) {
                                        val indexOffset = ((relativeY - gapPx) / itemHeightPx).toInt()
                                        val newIndex = (menuItems.size - 1) - indexOffset
                                        hoveredIndex = newIndex.coerceIn(0, menuItems.size - 1)
                                    } else {
                                        hoveredIndex = -1
                                    }
                                }
                                
                                // Check for release
                                if (change.changedToUp()) {
                                    isMenuOpen = false
                                    
                                    if (hoveredIndex in menuItems.indices) {
                                        onNavigate(menuItems[hoveredIndex].route)
                                    } else if (!hasMoved) {
                                        onSearchTap()
                                    }
                                    hoveredIndex = -1
                                    break
                                }
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
