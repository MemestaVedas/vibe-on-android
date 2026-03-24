package moe.memesta.vibeon.ui.pairing

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import moe.memesta.vibeon.R
import moe.memesta.vibeon.data.DiscoveredDevice
import moe.memesta.vibeon.ui.ConnectionState
import moe.memesta.vibeon.ui.shapes.*
import moe.memesta.vibeon.ui.components.AlbumPalette
import moe.memesta.vibeon.ui.components.extractAlbumPalette
import moe.memesta.vibeon.ui.theme.MPlusRoundedFont
import moe.memesta.vibeon.ui.utils.ContrastGuard

val NorlineFontFamily = MPlusRoundedFont

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
    albumArtUrl: String? = null,
    albumArtBitmap: Bitmap? = null,
    onConnect: (String, Int) -> Unit,
    onDeviceSelected: (DiscoveredDevice) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToScan: () -> Unit,
    onNavigateToOffline: () -> Unit = {},
    onDismiss: () -> Unit = {},
    onTroubleshoot: () -> Unit = {}
) {
    val isConnected = connectionState == ConnectionState.CONNECTED
    val isFailed = connectionState == ConnectionState.FAILED
    val isConnecting = connectionState == ConnectionState.CONNECTING
    val displayDevice = connectedDevice ?: devices.firstOrNull()
    
    var isRevealing by remember { mutableStateOf(false) }
    var pendingOfflineNav by remember { mutableStateOf(false) }

    val revealProgress by animateFloatAsState(
        targetValue = if (isRevealing) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        finishedListener = { 
            if (it == 1f) {
                if (pendingOfflineNav) {
                    onNavigateToOffline()
                }
                onDismiss() 
            }
        },
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
    
    // --- Dynamic palette from album art ---
    val defaultPalette = remember { AlbumPalette() }
    var palette by remember { mutableStateOf(defaultPalette) }
    LaunchedEffect(albumArtBitmap) {
        palette = if (albumArtBitmap != null) {
            extractAlbumPalette(albumArtBitmap)
        } else {
            defaultPalette
        }
    }
    
    // Animate palette colors for smooth transitions
    val animPrimary by animateColorAsState(palette.primary, tween(1200), label = "pal_p")
    val animSecondary by animateColorAsState(palette.secondary, tween(1200), label = "pal_s")
    val animTertiary by animateColorAsState(palette.tertiary, tween(1200), label = "pal_t")
    val animOnPrimary by animateColorAsState(palette.onPrimary, tween(1200), label = "pal_op")
    
    // Radial gradient with animated palette
    val topBackgroundBrush = remember(animPrimary, animSecondary, animTertiary, animOnPrimary) {
        Brush.radialGradient(
            colors = listOf(animPrimary, animSecondary, animTertiary, animOnPrimary),
            radius = 1800f
        )
    }
    
    // Contrast-guarded content color
    val topContentColor = remember(animPrimary) {
        ContrastGuard.ensureContrast(animPrimary, Color(0xFF452253), minRatio = 3.0f)
    }
    val bottomBackgroundColor = Color(0xFF141414)
    
    // Radial expansion animation
    val radiusPulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radius_pulse"
    )
    
    // --- Connection timeout → Troubleshoot ---
    var showTroubleshoot by remember { mutableStateOf(false) }
    LaunchedEffect(devices.isEmpty(), connectionState) {
        showTroubleshoot = false
        if (devices.isEmpty() && connectionState == ConnectionState.IDLE) {
            delay(5000)
            if (devices.isEmpty()) {
                showTroubleshoot = true
            }
        }
    }

    // --- Logo-to-Album-Art morph state ---
    val showAlbumArt = isConnected && albumArtUrl != null
    val morphProgress by animateFloatAsState(
        targetValue = if (showAlbumArt) 1f else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "morph"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .navigationBarsPadding()
    ) {
        // --- BOTTOM CONTAINER ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.35f)
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Troubleshoot button — appears after 5s with no servers
                AnimatedVisibility(
                    visible = showTroubleshoot && !isConnected,
                    enter = fadeIn(tween(400)) + slideInVertically { it / 2 },
                    exit = fadeOut(tween(200))
                ) {
                    Surface(
                        onClick = onTroubleshoot,
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Wifi,
                                contentDescription = null,
                                tint = animPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Troubleshoot connection",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = animPrimary
                            )
                        }
                    }
                }
                
                // CTA text
                val ctaText = when {
                    isFailed -> "RETRY"
                    isConnected -> "LET'S GO!"
                    isConnecting -> "CONNECTING..."
                    else -> "CONNECT TO A SERVER"
                }
                val ctaUsesNorline = ctaText == "CONNECT TO A SERVER"
                Text(
                    text = ctaText,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 52.sp,
                        fontFamily = if (ctaUsesNorline) NorlineFontFamily else MPlusRoundedFont,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 1.sp,
                        lineHeight = 56.sp,
                        textAlign = TextAlign.Center
                    ),
                    maxLines = 1,
                    softWrap = false,
                    color = animPrimary
                )
            }
        }

        // --- TOP CONTAINER (Gradient with wavy bottom + animated radial) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.78f)
                .graphicsLayer {
                    translationY = -size.height * revealProgress
                }
                .clip(WavyBottomShape(10.dp, 8.0f))
                .drawBehind {
                    val center = Offset(size.width / 2f, size.height * 0.4f)
                    val baseRadius = size.maxDimension * 0.8f
                    
                    // Animated radial gradient from center outward
                    drawRect(
                        brush = Brush.radialGradient(
                            colorStops = arrayOf(
                                0.0f to animPrimary,
                                0.3f to animSecondary,
                                0.6f to animTertiary,
                                1.0f to animOnPrimary
                            ),
                            center = center,
                            radius = baseRadius * radiusPulse
                        ),
                        size = size
                    )
                    
                    // Grain overlay
                    drawRect(
                        color = Color.Black.copy(alpha = 0.04f),
                        size = size
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Back Button + Connection Status Indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { 
                        if (isConnected) {
                            onNavigateBack() 
                        } else {
                            pendingOfflineNav = true
                            isRevealing = true
                        }
                    }) {
                        Icon(
                            imageVector = if (isConnected) Icons.Rounded.ArrowBack else Icons.Rounded.Folder,
                            contentDescription = if (isConnected) "Back" else "Local Files",
                            tint = topContentColor
                        )
                    }
                    
                    // Real-time connection status chip
                    AnimatedVisibility(
                        visible = connectionState != ConnectionState.IDLE,
                        enter = fadeIn() + scaleIn(initialScale = 0.8f),
                        exit = fadeOut() + scaleOut(targetScale = 0.8f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = when (connectionState) {
                                ConnectionState.CONNECTING -> topContentColor.copy(alpha = 0.15f)
                                ConnectionState.CONNECTED -> Color(0xFF2E7D32).copy(alpha = 0.2f)
                                ConnectionState.FAILED -> Color(0xFFB71C1C).copy(alpha = 0.2f)
                                else -> Color.Transparent
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Pulsing dot for connecting state
                                if (isConnecting) {
                                    val dotAlpha by infiniteTransition.animateFloat(
                                        initialValue = 0.3f,
                                        targetValue = 1f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(800),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "dot_pulse"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(topContentColor.copy(alpha = dotAlpha))
                                    )
                                } else if (isFailed) {
                                    Icon(
                                        imageVector = Icons.Rounded.ErrorOutline,
                                        contentDescription = null,
                                        tint = Color(0xFFEF5350),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                
                                Text(
                                    text = when (connectionState) {
                                        ConnectionState.CONNECTING -> "Connecting..."
                                        ConnectionState.CONNECTED -> "Connected"
                                        ConnectionState.FAILED -> "Failed"
                                        else -> ""
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = topContentColor
                                )
                            }
                        }
                    }
                }

                // Main Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.weight(0.7f))

                    // Title
                    val heroTitle = when {
                        isFailed -> "CONNECTION LOST"
                        isConnected -> "Let's VIBE-ON!"
                        devices.isNotEmpty() -> "FOUND SERVERS"
                        else -> "SEARCHING FOR VIBES"
                    }
                    val heroUsesNorline = heroTitle == "SEARCHING FOR VIBES"
                    Text(
                        text = heroTitle,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = if (isConnected || devices.isNotEmpty() || isFailed) 62.sp else 84.sp,
                            fontFamily = if (heroUsesNorline) NorlineFontFamily else MPlusRoundedFont,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 2.sp,
                            lineHeight = if (isConnected || devices.isNotEmpty() || isFailed) 66.sp else 88.sp,
                            textAlign = TextAlign.Center
                        ),
                        maxLines = 1,
                        softWrap = false,
                        color = topContentColor
                    )
                
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = when {
                            isFailed -> "Check that your desktop app is running\nand both devices are on the same network"
                            isConnected -> "Connected"
                            devices.isEmpty() -> "Scanning for a server,\nMake sure both the devices are on the same network"
                            else -> "Found ${devices.size} local server${if (devices.size > 1) "s" else ""}"
                        },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            fontFamily = MPlusRoundedFont,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        ),
                        color = topContentColor.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(60.dp))

                    // Centerpiece: Logo → Album Art morph (or Desktop icon / rotating logo)
                    AnimatedContent(
                        targetState = Triple(isConnected, devices.isNotEmpty(), isFailed),
                        transitionSpec = {
                            fadeIn(tween(800)) togetherWith fadeOut(tween(800))
                        },
                        label = "centerpiece_transition"
                    ) { (connected, found, failed) ->
                        when {
                            connected && albumArtUrl != null -> {
                                // Logo → Album Art morph
                                Box(
                                    modifier = Modifier.size(220.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Logo layer (fades out)
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_vibe_logo),
                                        contentDescription = "Vibe-On",
                                        modifier = Modifier
                                            .size(200.dp)
                                            .graphicsLayer {
                                                alpha = (1f - morphProgress).coerceIn(0f, 1f)
                                                scaleX = 1f + (1f - morphProgress) * 0.1f
                                                scaleY = 1f + (1f - morphProgress) * 0.1f
                                                rotationZ = (1f - morphProgress) * 10f
                                            }
                                    )
                                    // Album art layer (fades in with star clip)
                                    val context = LocalContext.current
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(albumArtUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Now Playing",
                                        modifier = Modifier
                                            .size(200.dp)
                                            .clip(AlbumArtStarShape)
                                            .graphicsLayer {
                                                alpha = morphProgress.coerceIn(0f, 1f)
                                                scaleX = 0.85f + morphProgress * 0.15f
                                                scaleY = 0.85f + morphProgress * 0.15f
                                            },
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                            connected -> {
                                // Connected but no album art
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
                            }
                            found -> {
                                // Found servers — desktop icon
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
                            }
                            failed -> {
                                // Error state
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(160.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ErrorOutline,
                                        contentDescription = null,
                                        tint = topContentColor.copy(alpha = 0.6f),
                                        modifier = Modifier.size(80.dp)
                                    )
                                }
                            }
                            else -> {
                                // Searching — rotating logo
                                Image(
                                    painter = painterResource(id = R.drawable.ic_vibe_logo),
                                    contentDescription = "Searching",
                                    modifier = Modifier
                                        .size(240.dp)
                                        .graphicsLayer { rotationZ = rotationAngle }
                                )
                            }
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
                            
                            Text(
                                text = "${displayDevice.host}",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.4.sp
                                ),
                                color = topContentColor.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
