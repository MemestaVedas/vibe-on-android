package moe.memesta.vibeon.ui.pairing

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.memesta.vibeon.R
import moe.memesta.vibeon.data.DiscoveredDevice
import moe.memesta.vibeon.ui.ConnectionState
import moe.memesta.vibeon.ui.WavyBottomShape

val NorlineFontFamily = FontFamily(
    Font(R.font.norline_rounded, FontWeight.Normal)
)

@Composable
fun ExpressiveStar(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val path = Path()
        val numPoints = 12
        val radius = size.width / 2f
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        for (i in 0 until numPoints * 2) {
            val angle = i * Math.PI / numPoints
            // Wavy radius pattern
            val r = if (i % 2 == 0) radius else radius * 0.82f
            val x = (centerX + r * Math.cos(angle)).toFloat()
            val y = (centerY + r * Math.sin(angle)).toFloat()
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        drawPath(
            path = path, 
            color = color,
            style = Fill
        )
    }
}

@Composable
fun PairingScreen(
    devices: List<DiscoveredDevice>,
    connectionState: ConnectionState = ConnectionState.IDLE,
    connectedDevice: DiscoveredDevice? = null,
    onConnect: (String, Int) -> Unit,
    onDeviceSelected: (DiscoveredDevice) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToScan: () -> Unit,
    onDismiss: () -> Unit = {}
) {
    val isConnected = connectionState == ConnectionState.CONNECTED
    // Use the first discovered device if none is selected, for the "main" view
    val displayDevice = connectedDevice ?: devices.firstOrNull()
    
    var isRevealing by remember { mutableStateOf(false) }

    val revealProgress by animateFloatAsState(
        targetValue = if (isRevealing) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        finishedListener = { if (it == 1f) onDismiss() },
        label = "reveal_curtain"
    )
    
    val infiniteTransition = rememberInfiniteTransition(label = "logo_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "logo_angle"
    )
    
    // Update to user-provided gradient design (Radial)
    val topBackgroundBrush = Brush.radialGradient(
        colors = listOf(
            Color(0xFFF5B7B4),
            Color(0xFFE4B7F2),
            Color(0xFFD4C0D7),
            Color(0xFF452253)
        ),
        radius = 1800f
    )
    val topContentColor = Color(0xFF452253) // Dark purple
    val bottomBackgroundColor = Color(0xFF141414) // Dark black/gray

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Make the root transparent so the LibraryScreen can be seen behind the curtains
            .background(Color.Transparent)
            .navigationBarsPadding()
    ) {
        // --- BOTTOM CONTAINER (Action area / Whole area clickable) ---
        // Drawn FIRST so it's under the top curtain
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.35f) // Takes the remaining space (overlapped slightly by top container)
                .align(Alignment.BottomCenter)
                .graphicsLayer {
                    translationY = size.height * revealProgress
                }
                .background(bottomBackgroundColor)
                .clickable {
                    if (isConnected && revealProgress == 0f) {
                        isRevealing = true
                    } else if (revealProgress == 0f) {
                        displayDevice?.let { onDeviceSelected(it) }
                    }
                }
                .padding(bottom = 60.dp, start = 16.dp, end = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = if (isConnected) "LET'S GO!" else "CONNECT TO A SERVER",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 52.sp,
                    fontFamily = NorlineFontFamily,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 1.sp,
                    lineHeight = 56.sp,
                    textAlign = TextAlign.Center
                ),
                maxLines = 1,
                softWrap = false,
                color = Color(0xFFF5B7B4)
            )
        }

        // --- TOP CONTAINER (Colored section with wavy bottom) ---
        // Drawn SECOND so its transparent wavy areas overlap the bottom container correctly
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.78f) // Take up ~78% of the screen
                .graphicsLayer {
                    translationY = -size.height * revealProgress
                }
                .clip(WavyBottomShape(10.dp, 8.0f))
                .background(topBackgroundBrush)
        ) {
            // Grainy Texture Overlay
            // Jetpack Compose does not easily support noise without a custom RuntimeShader.
            // As a fallback for a tasteful grain/texture, we use a subtle dark scrim and a blended radial gradient.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.04f))
            )

            // Content within Top Container with Status Bar Padding inside
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding() // Move status bar padding here so the background fills the top area
            ) {
                // Back Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = topContentColor
                        )
                    }
                }

                // Main Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp), // Less horizontal padding to help with one-line text
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.weight(0.7f))

                    // Title
                    Text(
                        text = if (isConnected) "Let's VIBE-ON!" else if (devices.isNotEmpty()) "FOUND SERVERS" else "SEARCHING FOR VIBES",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = if (isConnected || devices.isNotEmpty()) 62.sp else 84.sp,
                            fontFamily = NorlineFontFamily,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 2.sp,
                            lineHeight = if (isConnected || devices.isNotEmpty()) 66.sp else 88.sp,
                            textAlign = TextAlign.Center
                        ),
                        maxLines = 1,
                        softWrap = false,
                        color = topContentColor
                    )
                
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = when {
                            isConnected -> "Connected"
                            devices.isEmpty() -> "Scanning for a server,\nMake sure both the devices are on the same network"
                            else -> "Found ${devices.size} local server${if (devices.size > 1) "s" else ""}"
                        },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            fontFamily = FontFamily.SansSerif, // Keeping Roboto/SansSerif for subtitle
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        ),
                        color = topContentColor.copy(alpha = 0.8f) // Slightly subdued dark purple
                    )

                    Spacer(modifier = Modifier.height(60.dp))

                    // Centerpiece: Logo Blob or PC Icon
                    AnimatedContent(
                        targetState = isConnected || devices.isNotEmpty(),
                        transitionSpec = {
                            fadeIn(tween(800)) togetherWith fadeOut(tween(800))
                        },
                        label = "centerpiece_transition"
                    ) { found ->
                        if (found) {
                            // Desktop icon when found
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(160.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DesktopWindows,
                                    contentDescription = null,
                                    tint = topContentColor,
                                    modifier = Modifier.size(100.dp)
                                )
                            }
                        } else {
                            // Rotating vibe-on logo
                            Image(
                                painter = painterResource(id = R.drawable.ic_vibe_logo),
                                contentDescription = "Searching",
                                modifier = Modifier
                                    .size(240.dp) // Enlarged logo from 200.dp
                                    .graphicsLayer { rotationZ = rotationAngle }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    // Device Info
                    if (displayDevice != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = displayDevice.nickname ?: displayDevice.name,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                ),
                                color = topContentColor
                            )
                            
                            // IP Text (Simpler)
                            Text(
                                text = "${displayDevice.host}",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.4.sp
                                ),
                                color = topContentColor.copy(alpha = 0.7f) // Subdued
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
