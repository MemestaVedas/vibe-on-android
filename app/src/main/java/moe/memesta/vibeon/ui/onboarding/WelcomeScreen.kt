package moe.memesta.vibeon.ui.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.SwipeRight
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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

@Composable
fun WelcomeScreen(
    onComplete: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    
    val infiniteTransition = rememberInfiniteTransition(label = "welcome_logo")
    val logoRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "logo_rotate"
    )

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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .navigationBarsPadding()
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val scrollProgress = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
        val logoTransitionProgress = scrollProgress.coerceIn(0f, 1f)

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
                val ctaText = when (pagerState.currentPage) {
                    0 -> "GET STARTED"
                    1 -> "NEXT"
                    2 -> "LET'S GO!"
                    else -> ""
                }

                Text(
                    text = ctaText,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 32.sp,
                        fontFamily = NorlineFontFamily,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 1.sp,
                        lineHeight = 36.sp,
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
            // Grain / Dim overlay
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
                    0 -> WelcomePage(contentColor = contentColor)
                    1 -> VibeSetupPage(contentColor = contentColor)
                    2 -> ConnectServerPage(contentColor = contentColor)
                }
            }
        }

        // Floating dynamic Logo overlaid on top (moves to top left on pages > 0)
        val density = LocalDensity.current
        val startSize = 240.dp
        val endSize = 40.dp
        
        Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Image(
                painter = painterResource(id = R.drawable.ic_vibe_logo),
                contentDescription = "Vibe-On",
                modifier = Modifier
                    .size(startSize)
                    .graphicsLayer {
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
                        
                        // Interpolate size via scaling
                        val currentSize = androidx.compose.ui.unit.lerp(startSize, endSize, logoTransitionProgress)
                        val baseScale = currentSize / startSize
                        
                        scaleX = baseScale
                        scaleY = baseScale
                        
                        val startX = (screenWidth - startSize) / 2
                        val endX = 16.dp - (startSize - endSize) / 2
                        val currentX = androidx.compose.ui.unit.lerp(startX, endX, logoTransitionProgress)
                        
                        // Exact mathematical layout matching WelcomePage's 0.20f over 1f weights
                        val topSpace = (screenHeight - 504.dp) / 6f
                        val startY = topSpace + 168.dp
                        val endY = 16.dp - (startSize - endSize) / 2
                        val currentY = androidx.compose.ui.unit.lerp(startY, endY, logoTransitionProgress)
                        
                        translationX = with(density) { currentX.toPx() }
                        translationY = with(density) { currentY.toPx() }
                        
                        // Rotation only active when it transitions (logoTransitionProgress > 0)
                        val rotation = if (logoTransitionProgress > 0.01f) logoRotation else 0f
                        rotationZ = androidx.compose.ui.util.lerp(0f, rotation, logoTransitionProgress)
                    }
            )
        }
    }
}

@Composable
private fun WelcomePage(
    contentColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.20f))

        Text(
            text = "WELCOME TO",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 24.sp,
                fontFamily = NorlineFontFamily,
                fontWeight = FontWeight.Normal,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            ),
            color = contentColor.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "VIBE-ON!",
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

        Spacer(modifier = Modifier.height(64.dp))

        // Replaced Static Image with Spacer of exact size to maintain mathematical layout
        Spacer(modifier = Modifier.size(240.dp))

        Spacer(modifier = Modifier.height(64.dp))

        Text(
            text = "Your music, your way",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 28.sp,
                fontFamily = NorlineFontFamily,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            ),
            color = contentColor
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun VibeSetupPage(contentColor: Color) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.2f))

        Text(
            text = "THE VIBE",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 64.sp,
                fontFamily = NorlineFontFamily,
                fontWeight = FontWeight.Normal,
                letterSpacing = 2.sp,
                lineHeight = 68.sp,
                textAlign = TextAlign.Center
            ),
            color = contentColor
        )

        Spacer(modifier = Modifier.height(48.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(WavyBottomShape(6.dp, 4f)),
            color = contentColor.copy(alpha = 0.08f)
        ) {
            Column(modifier = Modifier.padding(top=24.dp, start=24.dp, end=24.dp, bottom=36.dp)) {
                Text(
                    text = "Connect & Play",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = contentColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Vibe-On connects seamlessly to your PC. Your music library remains right where it is.",
                    color = contentColor.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            color = contentColor.copy(alpha = 0.08f)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Ultimate Control",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = contentColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Stream songs to your phone, or use it as the ultimate desktop media remote.",
                    color = contentColor.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

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
        Spacer(modifier = Modifier.weight(0.4f))

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

        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(contentColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.DesktopWindows,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(40.dp)
                )
            }

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

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(contentColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Smartphone,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

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
                Icon(
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

