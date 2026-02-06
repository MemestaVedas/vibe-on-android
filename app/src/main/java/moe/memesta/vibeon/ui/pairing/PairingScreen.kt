package moe.memesta.vibeon.ui.pairing

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.memesta.vibeon.data.DiscoveredDevice
import moe.memesta.vibeon.ui.theme.VibeonTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairingScreen(
    devices: List<DiscoveredDevice>,
    onConnect: (String, Int) -> Unit,
    onDeviceSelected: (DiscoveredDevice) -> Unit,
    onNavigateToScan: () -> Unit
) {
    // State for manual connection dialog
    var showManualDialog by remember { mutableStateOf(false) }

    // Dynamic Colors from Material You
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val onSurface = MaterialTheme.colorScheme.onSurface
    
    // Background Gradient (Dark Pastel)
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            surfaceVariant,
            Color.Black.copy(alpha = 0.8f) // Deepen the bottom
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        // --- Main Content ---
        
        // 1. Turf / Plinth (The Silver Turntable Surface)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val plinthColor = Color(0xFFC0C0C0) 
            val tintedSilver = plinthColor.copy(alpha = 0.1f).compositeOver(surfaceColor)
            
            rotate(degrees = -5f, pivot = center) {
                 drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFE0E0E0), 
                            Color(0xFF888888) 
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    ),
                    topLeft = Offset(-100f, size.height * 0.15f),
                    size = Size(size.width * 1.5f, size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(60f, 60f)
                )
            }
        }

        // 2. Vinyl Record (Close up, bottom left)
        val infiniteTransition = rememberInfiniteTransition(label = "VinylSpin")
        val angle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = LinearEasing)
            ),
            label = "Rotation"
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
             // Record
            VinylRecord(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-120).dp, y = 100.dp) // Bleed off bottom-left
                    .size(600.dp) // Massive close-up
                    .rotate(angle),
                mainColor = tertiaryColor
            )
            
            // Tone Arm
            ToneArm(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 60.dp, y = (-50).dp) // Adjust position relative to record
                    .size(400.dp),
                isConnecting = devices.isNotEmpty()
            )
        }

        // 3. UI Overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
             // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { /* Handle back */ },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        contentColor = onSurface
                    )
                ) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                }
                
                // Connection Status Pill
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pulsing distinct dot
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 0.2f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "Pulse"
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(primaryColor.copy(alpha = alpha))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (devices.isNotEmpty()) "SERVER FOUND" else "SEARCHING...",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = onSurface,
                            letterSpacing = 1.sp
                        )
                    }
                }
                
                IconButton(
                    onClick = { /* Settings */ },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        contentColor = onSurface
                    )
                ) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Interaction Area
             if (devices.isNotEmpty()) {
                val device = devices.first()
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                        color = onSurface
                    )
                    Text(
                        text = "${device.host}:${device.port}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { onDeviceSelected(device) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.height(56.dp),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Text("Connect", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                 Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Vibe-On",
                        style = MaterialTheme.typography.displayLarge, 
                        fontWeight = FontWeight.Black,
                        color = onSurface.copy(alpha = 0.1f) // watermark style
                    )
                 }
            }

            Spacer(modifier = Modifier.weight(0.5f))

             // Footer Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Connect Manually Button
                FlatButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Keyboard,
                    text = "Manual",
                    onClick = { showManualDialog = true }
                )
                
                // Scan QR Button
                FlatButton(
                    modifier = Modifier.width(80.dp), // Square-ish
                    icon = Icons.Rounded.QrCodeScanner,
                    onClick = onNavigateToScan
                )
            }
        }
    }

    // Manual Connection Dialog
    if (showManualDialog) {
        ManualConnectionDialog(
            onDismiss = { showManualDialog = false },
            onConnect = { ip, port -> 
                showManualDialog = false
                onConnect(ip, port)
            }
        )
    }
}

@Composable
fun VinylRecord(
    modifier: Modifier = Modifier,
    mainColor: Color
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val center = center
        val radius = size.minDimension / 2
        
        // 1. Disc Body (Dynamic Gradient based on Wallpaper/Theme)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    mainColor.copy(alpha = 0.8f),
                    mainColor,
                    mainColor.copy(alpha = 0.6f) // Darker Edge
                ),
                center = center,
                radius = radius
            ),
            radius = radius
        )
        
        // 2. Grooves
        val grooveColor = Color.Black.copy(alpha = 0.15f)
        for (i in 0..15) {
            drawCircle(
                color = grooveColor,
                radius = radius * (0.95f - (i * 0.03f)),
                style = Stroke(width = 1.dp.toPx())
            )
        }
        
        // 3. Highlight
        drawArc(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.0f),
                    Color.White.copy(alpha = 0.1f),
                    Color.White.copy(alpha = 0.0f)
                ),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height)
            ),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = true
        )

        // 4. Center Label Area
        drawCircle(
            color = Color(0xFFF0F0F0),
            radius = radius * 0.35f
        )
        
        // Label Ring (Theme Color)
        drawCircle(
            color = mainColor.copy(alpha = 0.3f), // Inner ring
            radius = radius * 0.15f,
            style = Stroke(width = 20f)
        )
        
        // 5. Center Hole
        drawCircle(
            color = Color(0xFF111111),
            radius = 6.dp.toPx()
        )
    }
}

@Composable
fun ToneArm(
    modifier: Modifier = Modifier,
    isConnecting: Boolean
) {
    // Animate rotation based on state
    val targetRotation = if (isConnecting) 35f else 15f
    val rotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "ToneArmRotation"
    )

    Canvas(modifier = modifier) {
        val pivotX = size.width * 0.85f
        val pivotY = size.height * 0.15f
        
        rotate(degrees = rotation, pivot = Offset(pivotX, pivotY)) {
             // 1. Arm Tube (Silver Gradient)
             val tubeGradient = Brush.linearGradient(
                 colors = listOf(
                     Color(0xFF888888),
                     Color(0xFFEEEEEE), // Shine
                     Color(0xFF999999)
                 ),
                 start = Offset(pivotX, pivotY),
                 end = Offset(size.width * 0.2f, size.height * 0.8f)
             )
            
            // Main Tube
            drawLine(
                brush = tubeGradient,
                start = Offset(pivotX, pivotY),
                end = Offset(size.width * 0.25f, size.height * 0.75f),
                strokeWidth = 24f,
                cap = StrokeCap.Round
            )
            
            // 2. Headshell (Black/Dark Grey)
            val headStart = Offset(size.width * 0.25f, size.height * 0.75f)
            
            // Angled headshell
            rotate(degrees = 20f, pivot = headStart) {
                drawRoundRect(
                    color = Color(0xFF222222),
                    topLeft = Offset(headStart.x - 20f, headStart.y),
                    size = Size(50f, 90f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                )
                 drawLine(
                    color = Color(0xFFEEEEEE),
                    start = Offset(headStart.x + 25f, headStart.y + 20f),
                    end = Offset(headStart.x + 40f, headStart.y + 10f),
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
            }

            // 3. Gimbal/Pivot Assembly
            drawCircle(
                brush = Brush.radialGradient(
                     colors = listOf(Color(0xFFDDDDDD), Color(0xFF555555))
                ),
                center = Offset(pivotX, pivotY),
                radius = 50f
            )
            
             // 4. Counterweight
            drawRect(
                color = Color(0xFF333333),
                topLeft = Offset(pivotX - 30f, pivotY - 90f), // Behind pivot
                size = Size(60f, 60f)
            )
        }
    }
}

@Composable
fun FlatButton(
    modifier: Modifier = Modifier,
    icon:  androidx.compose.ui.graphics.vector.ImageVector,
    text: String? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(24.dp), // Rounded M3 Expressive
        modifier = modifier.height(64.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            if (text != null) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun ManualConnectionDialog(
    onDismiss: () -> Unit,
    onConnect: (String, Int) -> Unit
) {
    var ip by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("5000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manual Connect") },
        text = {
            Column {
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text("IP Address") },
                    placeholder = { Text("192.168.1.X") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Port") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    val portInt = port.toIntOrNull() ?: 5000
                    if (ip.isNotBlank()) {
                         onConnect(ip, portInt)
                    }
                }
            ) {
                Text("Connect")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
