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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeoutOrNull

val AlbumsShape = SvgShape(
    pathString = "M320 172C320 216.72 320 239.08 312.98 256.81C302.81 282.49 282.49 302.81 256.81 312.98C239.08 320 216.72 320 172 320H148C103.28 320 80.9199 320 63.1899 312.98C37.5099 302.81 17.19 282.49 7.02002 256.81C1.95503e-05 239.08 0 216.72 0 172V148C0 103.28 1.95503e-05 80.92 7.02002 63.19C17.19 37.515 37.5099 17.187 63.1899 7.02197C80.9199 -2.71797e-05 103.28 0 148 0H172C216.72 0 239.08 -2.71797e-05 256.81 7.02197C282.49 17.187 302.81 37.515 312.98 63.19C320 80.92 320 103.28 320 148V172Z",
    viewportWidth = 320f,
    viewportHeight = 320f
)

val PlaylistsShape = SvgShape(
    pathString = "M0 81.36C0 36.42 36.42 0 81.36 0H228.64C273.58 0 310 36.42 310 81.36C310 118.41 285.23 149.68 251.34 159.5C251.12 159.57 250.97 159.77 250.97 160C250.97 160.23 251.12 160.43 251.34 160.5C285.23 170.32 310 201.59 310 238.64C310 283.58 273.58 320 228.64 320H81.36C36.42 320 0 283.58 0 238.64C0 201.83 24.45 170.73 58 160.69C58.3 160.6 58.51 160.32 58.51 160C58.51 159.68 58.3 159.4 58 159.31C24.45 149.27 0 118.17 0 81.36Z",
    viewportWidth = 310f,
    viewportHeight = 320f
)

val ArtistsShape = SvgShape(
    pathString = "M304 253.72C304 259.83 304 262.89 303.69 265.46C301.31 285.51 285.51 301.31 265.46 303.69C262.89 304 259.83 304 253.72 304H50.281C44.169 304 41.113 304 38.544 303.69C18.495 301.31 2.68799 285.51 0.304993 265.46C-7.33137e-06 262.89 0 259.83 0 253.72V152C0 68.05 68.053 0 152 0C235.95 0 304 68.05 304 152V253.72Z",
    viewportWidth = 304f,
    viewportHeight = 304f
)

val SettingsShape = SvgShape(
    pathString = "M136.72 13.1925C147.26 -4.3975 172.74 -4.3975 183.28 13.1925L195.12 32.9625C201.27 43.2125 213.4 48.2425 224.99 45.3325L247.35 39.7325C267.24 34.7525 285.25 52.7626 280.27 72.6526L274.67 95.0126C271.76 106.603 276.79 118.733 287.04 124.883L306.81 136.723C324.4 147.263 324.4 172.743 306.81 183.283L287.04 195.123C276.79 201.273 271.76 213.403 274.67 224.993L280.27 247.353C285.25 267.243 267.24 285.253 247.35 280.273L224.99 274.673C213.4 271.763 201.27 276.793 195.12 287.043L183.28 306.813C172.74 324.403 147.26 324.403 136.72 306.813L124.88 287.043C118.73 276.793 106.6 271.763 95.0102 274.673L72.6462 280.273C52.7632 285.253 34.7472 267.243 39.7292 247.353L45.3332 224.993C48.2382 213.403 43.2143 201.273 32.9603 195.123L13.1873 183.283C-4.39575 172.743 -4.39575 147.263 13.1873 136.723L32.9603 124.883C43.2143 118.733 48.2382 106.603 45.3332 95.0126L39.7292 72.6526C34.7472 52.7626 52.7633 34.7525 72.6453 39.7325L95.0102 45.3325C106.6 48.2425 118.73 43.2125 124.88 32.9625L136.72 13.1925Z",
    viewportWidth = 320f,
    viewportHeight = 320f
)

val AlbumArtStarShape = SvgShape(
    pathString = "M136.697 9.84752C137.237 9.31752 137.508 9.0475 137.738 8.8275C150.248 -2.9425 169.748 -2.9425 182.258 8.8275C182.488 9.0475 182.758 9.31752 183.298 9.84752C183.628 10.1575 183.787 10.3174 183.937 10.4674C191.947 18.1074 203.278 21.1375 214.028 18.5275C214.238 18.4775 214.458 18.4175 214.898 18.3075C215.628 18.1175 215.998 18.0274 216.308 17.9474C233.018 14.0074 249.918 23.7574 254.858 40.2074C254.948 40.5174 255.048 40.8775 255.258 41.6075C255.378 42.0475 255.438 42.2674 255.498 42.4774C258.608 53.0874 266.908 61.3874 277.518 64.4974C277.728 64.5574 277.947 64.6174 278.387 64.7374C279.117 64.9474 279.478 65.0473 279.788 65.1373C296.238 70.0773 305.988 86.9774 302.048 103.687C301.968 103.997 301.878 104.368 301.688 105.098C301.578 105.538 301.518 105.757 301.468 105.967C298.858 116.717 301.888 128.047 309.528 136.057C309.678 136.207 309.837 136.367 310.147 136.697C310.677 137.237 310.947 137.507 311.167 137.737C322.937 150.247 322.937 169.747 311.167 182.257C310.947 182.487 310.677 182.757 310.147 183.297C309.837 183.627 309.678 183.787 309.528 183.937C301.888 191.947 298.858 203.277 301.468 214.027C301.518 214.237 301.578 214.457 301.688 214.897C301.878 215.627 301.968 215.997 302.048 216.307C305.988 233.017 296.238 249.918 279.788 254.858C279.478 254.948 279.117 255.047 278.387 255.257C277.947 255.377 277.728 255.437 277.518 255.497C266.908 258.607 258.608 266.907 255.498 277.517C255.438 277.727 255.378 277.947 255.258 278.387C255.048 279.117 254.948 279.477 254.858 279.787C249.918 296.237 233.018 305.987 216.308 302.047C215.998 301.967 215.628 301.877 214.898 301.687C214.458 301.577 214.238 301.517 214.028 301.467C203.278 298.857 191.947 301.887 183.937 309.527C183.787 309.677 183.628 309.837 183.298 310.147C182.758 310.677 182.488 310.947 182.258 311.167C169.748 322.937 150.248 322.937 137.738 311.167C137.508 310.947 137.237 310.677 136.697 310.147C136.367 309.837 136.208 309.677 136.058 309.527C128.048 301.887 116.718 298.857 105.968 301.467C105.758 301.517 105.538 301.577 105.098 301.687C104.368 301.877 103.997 301.967 103.687 302.047C86.9775 305.987 70.0776 296.237 65.1376 279.787C65.0476 279.477 64.9475 279.117 64.7375 278.387C64.6175 277.947 64.5575 277.727 64.4975 277.517C61.3875 266.907 53.0875 258.607 42.4775 255.497C42.2675 255.437 42.0475 255.377 41.6075 255.257C40.8775 255.047 40.5175 254.948 40.2075 254.858C23.7575 249.918 14.0075 233.017 17.9475 216.307C18.0275 215.997 18.1176 215.627 18.3076 214.897C18.4176 214.457 18.4776 214.237 18.5276 214.027C21.1376 203.277 18.1075 191.947 10.4675 183.937C10.3175 183.787 10.1575 183.627 9.84752 183.297C9.31752 182.757 9.0475 182.487 8.8275 182.257C-2.9425 169.747 -2.9425 150.247 8.8275 137.737C9.0475 137.507 9.31752 137.237 9.84752 136.697C10.1575 136.367 10.3175 136.207 10.4675 136.057C18.1075 128.047 21.1376 116.717 18.5276 105.967C18.4776 105.757 18.4176 105.538 18.3076 105.098C18.1176 104.368 18.0275 103.997 17.9475 103.687C14.0075 86.9774 23.7575 70.0773 40.2075 65.1373C40.5175 65.0473 40.8775 64.9474 41.6075 64.7374C42.0475 64.6174 42.2675 64.5574 42.4775 64.4974C53.0875 61.3874 61.3875 53.0874 64.4975 42.4774C64.5575 42.2674 64.6175 42.0475 64.7375 41.6075C64.9475 40.8775 65.0476 40.5174 65.1376 40.2074C70.0776 23.7574 86.9775 14.0074 103.687 17.9474C103.997 18.0274 104.368 18.1175 105.098 18.3075C105.538 18.4175 105.758 18.4775 105.968 18.5275C116.718 21.1375 128.048 18.1074 136.058 10.4674C136.208 10.3174 136.367 10.1575 136.697 9.84752Z",
    viewportWidth = 320f,
    viewportHeight = 320f
)

data class NavPage(val route: String, val label: String, val shape: Shape, val pageIndex: Int)

val NavPages = listOf(
    NavPage("settings", "Settings", SettingsShape, 4),
    NavPage("artists", "Artists", ArtistsShape, 3),
    NavPage("playlists", "Playlists", PlaylistsShape, 2),
    NavPage("albums", "Albums", AlbumsShape, 1),
    NavPage("library", "Home", CircleShape, 0)
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
    
    val currentPage = NavPages.find { it.route == currentRoute || (it.route == "library" && currentRoute == "discovery") } ?: NavPages.last()
    
    // The menu items are all pages
    val menuItems = NavPages
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd
    ) {
        // The popup menu
        if (isMenuOpen) {
            Popup(
                alignment = Alignment.BottomEnd,
                properties = PopupProperties(focusable = true, dismissOnBackPress = true, dismissOnClickOutside = true),
                onDismissRequest = { isMenuOpen = false }
            ) {
                Row(
                    modifier = Modifier.padding(bottom = 80.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Column for texts
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(vertical = 8.dp).padding(end = 12.dp)
                    ) {
                        menuItems.forEachIndexed { index, page ->
                            Box(
                                modifier = Modifier.height(36.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                val isHovered = index == hoveredIndex
                                if (isHovered) {
                                    androidx.compose.material3.Text(
                                        text = page.label,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    
                    // Column for shapes
                    Column(
                        modifier = Modifier
                            .width(48.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        menuItems.forEachIndexed { index, page ->
                            val isHovered = index == hoveredIndex
                            val scale by animateFloatAsState(if (isHovered) 1.15f else 1.0f, label = "scale")
                            
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .scale(scale)
                                    .clip(page.shape)
                                    .background(if (isHovered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        isMenuOpen = false
                                        onNavigate(page.route)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                // Optional: Add icon or text here if needed, but user image shows just shapes
                            }
                        }
                    }
                }
            }
        }
        
        // The main button (Search) - shape changes based on current page
        val mainScale by animateFloatAsState(
            targetValue = if (isMenuOpen) 1.15f else 1.0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "mainScale"
        )
        
        Box(
            modifier = Modifier
                .size(64.dp)
                .scale(mainScale)
                .clip(currentPage.shape) // Dynamic shape based on current page
                .background(MaterialTheme.colorScheme.primary)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        
                        // Try to wait for up within 300ms
                        val upBeforeTimeout = withTimeoutOrNull(300L) {
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
                                    val itemHeightPx = 40.dp.toPx()
                                    val gapPx = 80.dp.toPx()
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
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
