package moe.memesta.vibeon.ui.pairing

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.memesta.vibeon.data.DiscoveredDevice
import moe.memesta.vibeon.ui.ConnectionState
import moe.memesta.vibeon.ui.WavyBottomShape

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
    onNavigateToScan: () -> Unit
) {
    val isConnected = connectionState == ConnectionState.CONNECTED
    // Use the first discovered device if none is selected, for the "main" view
    val displayDevice = connectedDevice ?: devices.firstOrNull()
    
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
    
    // Exact colors matching screenshot "vibe-on-mobilenew1.png"
    val topBackgroundColor = Color(0xFFD3C1FA) // Light pastel purple
    val topContentColor = Color(0xFF332353) // Dark purple
    val bottomBackgroundColor = Color(0xFF141414) // Dark black/gray

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bottomBackgroundColor)
            .navigationBarsPadding()
    ) {
        // --- TOP CONTAINER (Colored section with wavy bottom) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.78f) // Take up ~78% of the screen
                .clip(WavyBottomShape(16.dp, 4.5f))
                .background(topBackgroundColor)
                .statusBarsPadding()
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

            // Main Content inside Top Container
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Title
                Text(
                    text = "Ready to VIBE-ON!",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.9).sp,
                        lineHeight = 40.sp,
                        textAlign = TextAlign.Center
                    ),
                    color = topContentColor
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = when {
                        isConnected -> "Connected"
                        devices.isEmpty() -> "Searching for servers..."
                        else -> "Found ${devices.size} local server${if (devices.size > 1) "s" else ""}"
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 18.sp,
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
                        // Rotating vibe-on dark purple blob
                         ExpressiveStar(
                             modifier = Modifier
                                 .size(140.dp)
                                 .graphicsLayer { rotationZ = rotationAngle },
                             color = topContentColor
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
            }
        }

        // --- BOTTOM CONTAINER (Action button area) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.35f) // Takes the remaining space (overlapped slightly by top container)
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp, start = 32.dp, end = 32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Button(
                onClick = { 
                    displayDevice?.let { 
                        if (connectionState != ConnectionState.CONNECTED) {
                            onDeviceSelected(it) 
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp),
                shape = RoundedCornerShape(9999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = topBackgroundColor, // Light purple button
                    contentColor = topContentColor // Dark purple text
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = if (isConnected) "ALREADY VIBING" else "START VIBING",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                )
            }
        }
    }
}
