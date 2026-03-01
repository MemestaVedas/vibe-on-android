package moe.memesta.vibeon.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import moe.memesta.vibeon.R
import moe.memesta.vibeon.ui.WavyBottomShape
import moe.memesta.vibeon.ui.pairing.NorlineFontFamily
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwipeRight

/**
 * Welcome to VIBE-ON! — first-launch tutorial.
 *
 * Design language matches PairingScreen:
 * - Warm pink→purple radial gradient
 * - NorlineFont display text
 * - Wavy clip shape dividers
 * - Dark bottom area with CTA
 *
 * 3-page walkthrough:
 *   1. Welcome splash
 *   2. "Connect to a server" instructions
 *   3. "Navigate with gestures" intro
 */
@Composable
fun WelcomeScreen(
    onComplete: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    // Background gradient matching PairingScreen
    val gradientBrush = Brush.radialGradient(
        colors = listOf(
            Color(0xFFF5B7B4),
            Color(0xFFE4B7F2),
            Color(0xFFD4C0D7),
            Color(0xFF452253)
        ),
        radius = 1800f
    )
    val contentColor = Color(0xFF452253) // Dark purple content
    val bottomBg = Color(0xFF141414)
    val accentPink = Color(0xFFF5B7B4)

    // Logo rotation for page 0
    val infiniteTransition = rememberInfiniteTransition(label = "welcome_anim")
    val logoRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "logo_rotate"
    )

    // Pulse for decorative elements
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
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
                .background(bottomBg)
                .clickable {
                    if (pagerState.currentPage < 2) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        onComplete()
                    }
                }
                .padding(bottom = 60.dp, start = 16.dp, end = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Page indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { index ->
                        val isActive = index == pagerState.currentPage
                        val width by animateDpAsState(
                            targetValue = if (isActive) 24.dp else 8.dp,
                            animationSpec = spring(
                                dampingRatio = 0.8f,
                                stiffness = 300f
                            ),
                            label = "dot_width_$index"
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(width)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isActive) accentPink else accentPink.copy(alpha = 0.3f)
                                )
                        )
                    }
                }

                // CTA text
                Text(
                    text = if (pagerState.currentPage < 2) "CONTINUE" else "LET'S GO!",
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
                    color = accentPink
                )
            }
        }

        // --- TOP CONTAINER (Gradient with wavy bottom) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.78f)
                .clip(WavyBottomShape(10.dp, 8.0f))
                .background(gradientBrush)
        ) {
            // Grain overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.04f))
            )

            // Pager content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) { page ->
                when (page) {
                    0 -> WelcomePage(
                        contentColor = contentColor,
                        logoRotation = logoRotation,
                        pulseScale = pulseScale
                    )
                    1 -> ConnectServerPage(contentColor = contentColor)
                    2 -> GestureIntroPage(contentColor = contentColor)
                }
            }
        }
    }
}

@Composable
private fun WelcomePage(
    contentColor: Color,
    logoRotation: Float,
    pulseScale: Float
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.5f))

        // Title
        Text(
            text = "WELCOME TO",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 36.sp,
                fontFamily = NorlineFontFamily,
                fontWeight = FontWeight.Normal,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center
            ),
            color = contentColor.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "VIBE-ON!",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 84.sp,
                fontFamily = NorlineFontFamily,
                fontWeight = FontWeight.Normal,
                letterSpacing = 2.sp,
                lineHeight = 88.sp,
                textAlign = TextAlign.Center
            ),
            maxLines = 1,
            softWrap = false,
            color = contentColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your music, everywhere",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            ),
            color = contentColor.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Rotating logo
        Image(
            painter = painterResource(id = R.drawable.ic_vibe_logo),
            contentDescription = "Vibe-On",
            modifier = Modifier
                .size(200.dp)
                .graphicsLayer {
                    rotationZ = logoRotation
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ConnectServerPage(contentColor: Color) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.5f))

        Text(
            text = "CONNECT",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 72.sp,
                fontFamily = NorlineFontFamily,
                fontWeight = FontWeight.Normal,
                letterSpacing = 2.sp,
                lineHeight = 76.sp,
                textAlign = TextAlign.Center
            ),
            maxLines = 1,
            softWrap = false,
            color = contentColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Connect to a server",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            ),
            color = contentColor.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Keep the desktop app running",
            style = MaterialTheme.typography.bodyMedium.copy(
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.SansSerif
            ),
            color = contentColor.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Desktop + phone illustration
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Desktop icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(contentColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Rounded.DesktopWindows,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(40.dp)
                )
            }

            // Connection dots
            val infiniteTransition = rememberInfiniteTransition(label = "connect_dots")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { i ->
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.2f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, delayMillis = i * 200),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot_$i"
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(contentColor.copy(alpha = alpha))
                    )
                }
            }

            // Phone icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(contentColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Rounded.Smartphone,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Same WiFi reminder card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = contentColor.copy(alpha = 0.08f)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Rounded.Wifi,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Both devices must be on the same Wi-Fi network",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun GestureIntroPage(contentColor: Color) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.5f))

        Text(
            text = "NAVIGATE",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 72.sp,
                fontFamily = NorlineFontFamily,
                fontWeight = FontWeight.Normal,
                letterSpacing = 2.sp,
                lineHeight = 76.sp,
                textAlign = TextAlign.Center
            ),
            maxLines = 1,
            softWrap = false,
            color = contentColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Hold to navigate",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            ),
            color = contentColor.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Press and hold the nav button\nto switch between pages",
            style = MaterialTheme.typography.bodyMedium.copy(
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.SansSerif
            ),
            color = contentColor.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Gesture demonstration
        val infiniteTransition = rememberInfiniteTransition(label = "gesture_demo")
        val holdProgress by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "hold_demo"
        )

        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            // Progress ring
            Canvas(modifier = Modifier.size(100.dp)) {
                val strokeWidth = 4.dp.toPx()
                // Background ring
                drawArc(
                    color = contentColor.copy(alpha = 0.15f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = strokeWidth,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                )
                // Progress ring
                drawArc(
                    color = contentColor,
                    startAngle = -90f,
                    sweepAngle = 360f * holdProgress,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = strokeWidth,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                )
            }

            // Center finger icon
            androidx.compose.material3.Icon(
                imageVector = Icons.Rounded.TouchApp,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Gesture hints
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GestureHint(
                icon = Icons.Rounded.Search,
                text = "Tap the nav button to search",
                contentColor = contentColor
            )
            GestureHint(
                icon = Icons.Rounded.SwipeRight,
                text = "Swipe the player pill to skip tracks",
                contentColor = contentColor
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun GestureHint(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    contentColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(contentColor.copy(alpha = 0.06f))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Medium
            ),
            color = contentColor.copy(alpha = 0.7f)
        )
    }
}
