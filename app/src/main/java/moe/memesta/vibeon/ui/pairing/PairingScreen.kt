package moe.memesta.vibeon.ui.pairing

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.memesta.vibeon.data.DiscoveredDevice
import moe.memesta.vibeon.ui.ConnectionState
import moe.memesta.vibeon.ui.theme.VibeAnimations

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
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // High-contrast black as requested
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // --- TOP BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onNavigateBack,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // --- MAIN CONTENT ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 35.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Ready to VIBE-ON!",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.9).sp,
                        lineHeight = 40.sp,
                        textAlign = TextAlign.Center
                    ),
                    color = Color.White
                )
                
                Text(
                    text = when {
                        isConnected -> "Connected"
                        devices.isEmpty() -> "Searching for devices..."
                        else -> "Found ${devices.size} local client${if (devices.size > 1) "s" else ""}"
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    ),
                    color = Color(0xFF9AA0A6) // From CSS .Text color
                )
            }

            Spacer(modifier = Modifier.height(60.dp))

            // Centerpiece: The Star/Blob
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(278.dp)
            ) {
                ExpressiveStar(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary
                )
                
                Icon(
                    imageVector = Icons.Rounded.DesktopWindows,
                    contentDescription = null,
                    tint = Color.Black, // Icons inside the primary blob should be black for contrast
                    modifier = Modifier.size(80.dp)
                )
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
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        color = Color.White
                    )
                    
                    // IP Pill
                    Surface(
                        shape = RoundedCornerShape(9999.dp),
                        color = Color(0xFF1E1E1E), // From CSS .Background
                        modifier = Modifier.wrapContentSize()
                    ) {
                        Text(
                            text = "${displayDevice.host}",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.4.sp
                            ),
                            color = MaterialTheme.colorScheme.primary // Use primary for accent text
                        )
                    }
                }
            }
        }

        // --- FOOTER ACTION ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
                .padding(horizontal = 35.dp)
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
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.Black
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = if (isConnected) "ALREADY STREAMING" else "START STREAMING",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                )
            }
        }
    }
}

@Composable
fun ExpressiveStar(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "star_animation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Canvas(modifier = modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }) {
        val path = Path()
        val centerX = size.width / 2
        val centerY = size.height / 2
        val outerRadius = size.minDimension / 2
        val innerRadius = outerRadius * 0.82f
        val points = 12
        
        for (i in 0 until points * 2) {
            val angle = i * Math.PI / points - Math.PI / 2
            val r = if (i % 2 == 0) outerRadius else innerRadius
            val x = centerX + r * Math.cos(angle).toFloat()
            val y = centerY + r * Math.sin(angle).toFloat()
            
            if (i == 0) path.moveTo(x, y)
            else {
                // Approximate rounded corners with quadratic bezier
                val prevAngle = (i - 1) * Math.PI / points - Math.PI / 2
                val prevR = if ((i - 1) % 2 == 0) outerRadius else innerRadius
                val prevX = centerX + prevR * Math.cos(prevAngle).toFloat()
                val prevY = centerY + prevR * Math.sin(prevAngle).toFloat()
                
                val midX = (prevX + x) / 2
                val midY = (prevY + y) / 2
                
                path.quadraticBezierTo(prevX, prevY, midX, midY)
            }
        }
        path.close()
        drawPath(path, color)
    }
}
