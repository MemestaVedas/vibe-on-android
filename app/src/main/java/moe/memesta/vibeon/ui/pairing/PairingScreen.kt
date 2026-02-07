package moe.memesta.vibeon.ui.pairing

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.memesta.vibeon.data.DiscoveredDevice
import moe.memesta.vibeon.ui.ConnectionState
import moe.memesta.vibeon.ui.theme.VibeAnimations
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
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
    var showManualSheet by remember { mutableStateOf(false) }
    val isSearching = devices.isEmpty()
    val isConnecting = connectionState == ConnectionState.CONNECTING
    val isConnected = connectionState == ConnectionState.CONNECTED

    val infiniteTransition = rememberInfiniteTransition(label = "pairing_background")
    val vinylRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing)
        ),
        label = "vinyl_rotation"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.06f,
        targetValue = 0.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
            MaterialTheme.colorScheme.surface
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        val primaryGlow = MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha)
        val tertiaryGlow = MaterialTheme.colorScheme.tertiary.copy(alpha = glowAlpha * 0.7f)
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primaryGlow, Color.Transparent),
                    center = Offset(size.width * 0.15f, size.height * 0.2f),
                    radius = size.minDimension * 0.7f
                ),
                radius = size.minDimension * 0.7f,
                center = Offset(size.width * 0.15f, size.height * 0.2f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(tertiaryGlow, Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.8f),
                    radius = size.minDimension * 0.65f
                ),
                radius = size.minDimension * 0.65f,
                center = Offset(size.width * 0.85f, size.height * 0.8f)
            )
        }

        VinylRecord(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 80.dp, y = 140.dp)
                .size(520.dp)
                .rotate(vinylRotation)
                .alpha(0.12f),
            mainColor = MaterialTheme.colorScheme.tertiary
        )

        ToneArm(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 40.dp, y = (-20).dp)
                .size(320.dp)
                .alpha(0.18f),
            isConnecting = isConnecting || isConnected
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            CenterAlignedTopAppBar(
                title = { Text("Pairing") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToScan) {
                        Icon(Icons.Rounded.QrCodeScanner, contentDescription = "Scan QR")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    text = "Pair your Vibe-On",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Keep the desktop app open. We will find it on your network.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                PairingStatusPill(
                    deviceCount = devices.size,
                    connectionState = connectionState,
                    connectedDevice = connectedDevice
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(
                visible = isConnecting,
                enter = fadeIn(animationSpec = tween(VibeAnimations.FadeDuration)),
                exit = fadeOut(animationSpec = tween(VibeAnimations.FadeDuration))
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedContent(
                targetState = isSearching,
                transitionSpec = {
                    fadeIn(animationSpec = tween(VibeAnimations.HeroDuration)) togetherWith
                        fadeOut(animationSpec = tween(VibeAnimations.FadeDuration))
                },
                label = "pairing_content",
                modifier = Modifier.weight(1f)
            ) { searching ->
                if (searching) {
                    PairingSearchingState(modifier = Modifier.fillMaxSize())
                } else {
                    PairingDeviceList(
                        devices = devices,
                        connectionState = connectionState,
                        connectedDevice = connectedDevice,
                        onDeviceSelected = onDeviceSelected,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            PairingFooterActions(
                onManualClick = { showManualSheet = true },
                onScanClick = onNavigateToScan
            )
        }
    }

    if (showManualSheet) {
        ManualConnectionSheet(
            onDismiss = { showManualSheet = false },
            onConnect = { ip, port ->
                showManualSheet = false
                onConnect(ip, port)
            }
        )
    }
}

@Composable
private fun PairingStatusPill(
    deviceCount: Int,
    connectionState: ConnectionState,
    connectedDevice: DiscoveredDevice?
) {
    val statusText = when {
        connectionState == ConnectionState.CONNECTED && connectedDevice != null ->
            "Connected to ${connectedDevice.nickname ?: connectedDevice.name}"
        connectionState == ConnectionState.CONNECTING && connectedDevice != null ->
            "Connecting to ${connectedDevice.nickname ?: connectedDevice.name}"
        deviceCount > 0 -> "$deviceCount server${if (deviceCount == 1) "" else "s"} found"
        else -> "Searching for servers"
    }

    val toneColor = when (connectionState) {
        ConnectionState.CONNECTED -> MaterialTheme.colorScheme.primary
        ConnectionState.CONNECTING -> MaterialTheme.colorScheme.tertiary
        ConnectionState.FAILED -> MaterialTheme.colorScheme.error
        ConnectionState.IDLE -> MaterialTheme.colorScheme.outline
    }

    val pulseTransition = rememberInfiniteTransition(label = "status_pulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "status_pulse_alpha"
    )
    val dotAlpha = if (connectionState == ConnectionState.CONNECTED) 1f else pulse

    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        border = BorderStroke(1.dp, toneColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(toneColor.copy(alpha = dotAlpha))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.4.sp
            )
        }
    }
}

@Composable
private fun PairingSearchingState(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "searching_pulse")
    val ringScale by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "searching_ring_scale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .graphicsLayer {
                    scaleX = ringScale
                    scaleY = ringScale
                },
            contentAlignment = Alignment.Center
        ) {
            val primary = MaterialTheme.colorScheme.primary
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = primary.copy(alpha = 0.15f),
                    radius = size.minDimension / 2
                )
                drawCircle(
                    color = primary.copy(alpha = 0.25f),
                    radius = size.minDimension / 3
                )
                drawCircle(
                    color = primary,
                    radius = size.minDimension / 10
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Searching nearby servers",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Make sure the desktop app is running on the same WiFi.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PairingDeviceList(
    devices: List<DiscoveredDevice>,
    connectionState: ConnectionState,
    connectedDevice: DiscoveredDevice?,
    onDeviceSelected: (DiscoveredDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = devices,
            key = { device -> "${device.host}:${device.port}" }
        ) { device ->
            val isActive = connectedDevice?.host == device.host && connectedDevice.port == device.port
            PairingDeviceCard(
                device = device,
                isActive = isActive,
                connectionState = connectionState,
                onClick = { onDeviceSelected(device) }
            )
        }
    }
}

@Composable
private fun PairingDeviceCard(
    device: DiscoveredDevice,
    isActive: Boolean,
    connectionState: ConnectionState,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val targetScale = if (pressed) 0.98f else 1f
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = VibeAnimations.SpringExpressive,
        label = "card_scale"
    )
    val targetElevation = when {
        pressed -> 1.dp
        isActive -> 6.dp
        else -> 2.dp
    }
    val elevation by animateDpAsState(
        targetValue = targetElevation,
        animationSpec = VibeAnimations.springStandardGeneric(),
        label = "card_elevation"
    )
    val containerColor = if (isActive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }
    val borderColor = if (isActive) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current
            ) { onClick() },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Link,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.nickname ?: device.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${device.host}:${device.port}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isActive && connectionState == ConnectionState.CONNECTED) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Connected",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            val statusText = when {
                isActive && connectionState == ConnectionState.CONNECTED -> "Connected"
                isActive && connectionState == ConnectionState.CONNECTING -> "Connecting..."
                else -> "Tap to connect"
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AnimatedVisibility(
                visible = isActive && connectionState == ConnectionState.CONNECTING,
                enter = fadeIn(animationSpec = tween(VibeAnimations.FadeDuration)),
                exit = fadeOut(animationSpec = tween(VibeAnimations.FadeDuration))
            ) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

@Composable
private fun PairingFooterActions(
    onManualClick: () -> Unit,
    onScanClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onManualClick,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Keyboard,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Enter IP",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        TextButton(
            onClick = onScanClick,
            modifier = Modifier.height(56.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.QrCodeScanner,
                contentDescription = "Scan QR"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Scan")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualConnectionSheet(
    onDismiss: () -> Unit,
    onConnect: (String, Int) -> Unit
) {
    var ip by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("5000") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Manual Connect",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Enter the IP shown on the desktop app.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = ip,
                onValueChange = { ip = it },
                label = { Text("IP Address") },
                placeholder = { Text("192.168.1.100") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = port,
                onValueChange = { port = it },
                label = { Text("Port") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    val portInt = port.toIntOrNull() ?: 5000
                    if (ip.isNotBlank()) {
                        onConnect(ip.trim(), portInt)
                    }
                },
                enabled = ip.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(text = "Connect")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun VinylRecord(
    modifier: Modifier = Modifier,
    mainColor: Color
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val radius = size.minDimension / 2

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    mainColor.copy(alpha = 0.75f),
                    mainColor.copy(alpha = 0.9f),
                    mainColor.copy(alpha = 0.6f)
                ),
                center = center,
                radius = radius
            ),
            radius = radius
        )

        val grooveColor = Color.Black.copy(alpha = 0.15f)
        for (i in 0..12) {
            drawCircle(
                color = grooveColor,
                radius = radius * (0.94f - (i * 0.04f)),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        drawArc(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.0f),
                    Color.White.copy(alpha = 0.12f),
                    Color.White.copy(alpha = 0.0f)
                ),
                start = Offset(0f, 0f),
                end = Offset(size.width, size.height)
            ),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = true
        )

        drawCircle(
            color = Color(0xFFF4F4F4),
            radius = radius * 0.32f
        )
        drawCircle(
            color = mainColor.copy(alpha = 0.25f),
            radius = radius * 0.14f,
            style = Stroke(width = 18f)
        )
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
    val targetRotation = if (isConnecting) 28f else 12f
    val rotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = VibeAnimations.SpringExpressive,
        label = "tonearm_rotation"
    )

    Canvas(modifier = modifier) {
        val pivotX = size.width * 0.85f
        val pivotY = size.height * 0.15f

        rotate(degrees = rotation, pivot = Offset(pivotX, pivotY)) {
            val tubeGradient = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF8C8C8C),
                    Color(0xFFEDEDED),
                    Color(0xFF9C9C9C)
                ),
                start = Offset(pivotX, pivotY),
                end = Offset(size.width * 0.2f, size.height * 0.8f)
            )

            drawLine(
                brush = tubeGradient,
                start = Offset(pivotX, pivotY),
                end = Offset(size.width * 0.25f, size.height * 0.75f),
                strokeWidth = 22f,
                cap = StrokeCap.Round
            )

            val headStart = Offset(size.width * 0.25f, size.height * 0.75f)
            rotate(degrees = 20f, pivot = headStart) {
                drawRoundRect(
                    color = Color(0xFF222222),
                    topLeft = Offset(headStart.x - 20f, headStart.y),
                    size = Size(48f, 82f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                )
                drawLine(
                    color = Color(0xFFEEEEEE),
                    start = Offset(headStart.x + 22f, headStart.y + 18f),
                    end = Offset(headStart.x + 36f, headStart.y + 8f),
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFDDDDDD), Color(0xFF555555))
                ),
                center = Offset(pivotX, pivotY),
                radius = 46f
            )

            drawRect(
                color = Color(0xFF333333),
                topLeft = Offset(pivotX - 28f, pivotY - 86f),
                size = Size(56f, 56f)
            )
        }
    }
}
