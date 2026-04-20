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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import moe.memesta.vibeon.R
import moe.memesta.vibeon.ui.shapes.*
import moe.memesta.vibeon.ui.pairing.NorlineFontFamily
import moe.memesta.vibeon.ui.theme.GoogleSansFlexFamily
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwipeRight
import moe.memesta.vibeon.ui.utils.noiseTexture

@Composable
fun NextActionArea(
    text: String,
    color: Color,
    currentIndex: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Page Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0..2) {
                    val isActive = i == currentIndex
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (isActive) 16.dp else 6.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = if (isActive) 1f else 0.3f))
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = NorlineFontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontSize = 32.sp
                ),
                color = color
            )
        }
    }
}

/**
 * Welcome to VIBE-ON! — first-launch tutorial.
 */
@Composable
fun WelcomeScreen(
    onComplete: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    var isExiting by remember { mutableStateOf(false) }

    val summaries = listOf(
        "Welcome to Vibe-On",
        "Connect to a Server",
        "Navigate with Gestures"
    )
    
    val gradients = listOf(
        Brush.radialGradient(
            colors = listOf(Color(0xFFF5B7B4), Color(0xFFE4B7F2), Color(0xFFD4C0D7), Color(0xFF452253)),
            radius = 1800f
        ),
        Brush.radialGradient(
            colors = listOf(Color(0xFFB4E4F5), Color(0xFFB4F2D6), Color(0xFFC0D7CD), Color(0xFF224553)),
            radius = 1800f
        ),
        Brush.radialGradient(
            colors = listOf(Color(0xFFF5DEB4), Color(0xFFF2C8B4), Color(0xFFD7CCC0), Color(0xFF533822)),
            radius = 1800f
        )
    )

    val contentColors = listOf(
        Color(0xFF452253), // Dark Purple Pop
        Color(0xFF224553), // Dark Teal Pop
        Color(0xFF533822)  // Dark Brown Pop
    )

    val infiniteTransition = rememberInfiniteTransition(label = "welcome_anim")
    val logoRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart), label = "logo_rotate"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF141414)) // Base background
            .noiseTexture(alpha = 40)
            .navigationBarsPadding()
    ) {
        val totalHeight = maxHeight
        val collapsedHeight = 76.dp

        val curtainTopOffset by animateDpAsState(
            targetValue = if (isExiting) -maxHeight else 0.dp,
            animationSpec = tween(600, easing = FastOutSlowInEasing),
            label = "curtain_top",
            finishedListener = { if (it < 0.dp) onComplete() }
        )

        val curtainBottomOffset by animateDpAsState(
            targetValue = if (isExiting) 200.dp else 0.dp,
            animationSpec = tween(600, easing = FastOutSlowInEasing),
            label = "curtain_bottom"
        )

        // 1. Base Layer (Visible constantly behind everything, seen when on last step)
        Box(
            modifier = Modifier.fillMaxSize().offset(y = curtainBottomOffset)
        ) {
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                NextActionArea(
                    text = "LET'S GO!",
                    color = Color(0xFFF5B7B4), // Pink pops beautifully against Dark Background
                    currentIndex = 2,
                    onClick = { isExiting = true }
                )
            }
        }

        // 2. The stacked layout pages over the top (the top curtain)
        Box(modifier = Modifier.fillMaxSize().offset(y = curtainTopOffset)) {
            summaries.forEachIndexed { index, summary -> 
                if (index > currentStep + 1) return@forEachIndexed // Load exactly current and the 1 next waiting card
    
                val isCollapsed = index < currentStep
                val isActive = index == currentStep
                val isNext = index == currentStep + 1
                
                val cardHeight by animateDpAsState(
                    targetValue = when {
                        isCollapsed -> collapsedHeight * (index + 1)
                        isActive -> totalHeight - 130.dp // Leave exact room for NextActionArea peeking
                        else -> totalHeight // Background filling element
                    },
                    animationSpec = tween(400, easing = FastOutSlowInEasing),
                    label = "card_height_$index"
                )
    
                // Avoid wavy bottom gap on isNext creating black lines at background edges
                val currentShape = if (isNext) androidx.compose.ui.graphics.RectangleShape else WavyBottomShape(10.dp, 8.0f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(cardHeight)
                        .zIndex(10f - index)
                        .clip(currentShape)
                        .background(gradients[index])
                ) {
                    // Subtle overlay
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.04f)))
    
                    // Padded content container so tops overlap correctly
                    Box(modifier = Modifier.fillMaxSize().padding(top = collapsedHeight * index)) {
                        
                        // Collapsed Summary (Title peek on the top edges)
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isCollapsed,
                            enter = fadeIn(tween(300)),
                            exit = fadeOut(tween(150))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(collapsedHeight)
                                    .padding(bottom = 12.dp)
                                    .statusBarsPadding()
                                    .clickable { currentStep = index }, // Make previous cards clickable!
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = summary,
                                    modifier = Modifier.padding(horizontal = 24.dp),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = NorlineFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = contentColors[index].copy(alpha = 0.7f)
                                )
                            }
                        }
    
                        // Active Page Flow Content
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isActive,
                            enter = fadeIn(tween(300, delayMillis = 150)),
                            exit = fadeOut(tween(150))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .statusBarsPadding()
                                    .padding(bottom = 24.dp) 
                            ) {
                                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    when (index) {
                                        0 -> WelcomePage(contentColor = contentColors[index], logoRotation = logoRotation, pulseScale = pulseScale)
                                        1 -> ConnectServerPage(contentColor = contentColors[index])
                                        2 -> GestureIntroPage(contentColor = contentColors[index])
                                    }
                                }
                            }
                        }
    
                        // Peeking NextActionArea embedded inside the NEXT card (Slides up from the bottom)
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isNext,
                            enter = fadeIn(tween(300)),
                            exit = fadeOut(tween(150)),
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            NextActionArea(
                                text = "CONTINUE",
                                color = contentColors[index], // Using the next page's high contrast color!
                                currentIndex = currentStep,
                                onClick = { currentStep++ }
                            )
                        }
                    }
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
                fontFamily = GoogleSansFlexFamily,
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
                fontFamily = GoogleSansFlexFamily
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
                fontFamily = NorlineFontFamily,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            ),
            color = contentColor.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Press and hold the nav button\nto switch between pages",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = NorlineFontFamily,
                textAlign = TextAlign.Center
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
                fontFamily = NorlineFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            ),
            color = contentColor.copy(alpha = 0.7f)
        )
    }
}
