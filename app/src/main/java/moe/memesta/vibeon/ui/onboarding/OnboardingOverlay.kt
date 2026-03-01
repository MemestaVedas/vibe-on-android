package moe.memesta.vibeon.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwipeRight
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.memesta.vibeon.ui.pairing.NorlineFontFamily

/**
 * One-time walkthrough overlay that explains the home screen layout.
 *
 * Matches PairingScreen design: dark scrim with spotlight tooltips,
 * NorlineFont headlines, pink accent color.
 *
 * Steps:
 *   1. "Search" — Explain nav button = search tap
 *   2. "Navigate" — Explain hold gesture for page switching
 *   3. "Swipe" — Explain player pill swipe gestures
 */
@Composable
fun OnboardingOverlay(
    onDismiss: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    val totalSteps = 3
    val accentPink = Color(0xFFF5B7B4)
    val contentColor = Color(0xFF452253)

    // Entrance animation
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(400)),
        exit = fadeOut(tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable {
                    if (currentStep < totalSteps - 1) {
                        currentStep++
                    } else {
                        onDismiss()
                    }
                }
                .navigationBarsPadding()
                .statusBarsPadding()
        ) {
            // Animated step content
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    (fadeIn(tween(400)) + slideInHorizontally { it / 4 })
                        .togetherWith(fadeOut(tween(200)) + slideOutHorizontally { -it / 4 })
                },
                label = "walkthrough_step"
            ) { step ->
                when (step) {
                    0 -> WalkthroughStep(
                        title = "SEARCH",
                        subtitle = "Tap the nav button to search your library",
                        icon = Icons.Rounded.Search,
                        accentPink = accentPink,
                        contentColor = contentColor,
                        stepNumber = 1,
                        totalSteps = totalSteps,
                        alignment = Alignment.BottomEnd
                    )
                    1 -> WalkthroughStep(
                        title = "NAVIGATE",
                        subtitle = "Hold the nav button to switch between pages.\nRelease over a page to jump there.",
                        icon = Icons.Rounded.TouchApp,
                        accentPink = accentPink,
                        contentColor = contentColor,
                        stepNumber = 2,
                        totalSteps = totalSteps,
                        alignment = Alignment.BottomEnd
                    )
                    2 -> WalkthroughStep(
                        title = "SWIPE",
                        subtitle = "Swipe the player pill left or right to skip tracks.\nSwipe up to open the full player.",
                        icon = Icons.Rounded.SwipeRight,
                        accentPink = accentPink,
                        contentColor = contentColor,
                        stepNumber = 3,
                        totalSteps = totalSteps,
                        alignment = Alignment.BottomStart
                    )
                }
            }

            // Skip button
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Skip",
                    color = accentPink.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun WalkthroughStep(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentPink: Color,
    contentColor: Color,
    stepNumber: Int,
    totalSteps: Int,
    alignment: Alignment
) {
    val infiniteTransition = rememberInfiniteTransition(label = "step_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_pulse"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Icon with glow ring
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                // Glow ring
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                        .clip(CircleShape)
                        .background(accentPink.copy(alpha = 0.1f))
                )
                // Inner circle
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(accentPink.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentPink,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 52.sp,
                    fontFamily = NorlineFontFamily,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 3.sp,
                    textAlign = TextAlign.Center
                ),
                color = accentPink
            )

            // Subtitle
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                ),
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        // Step indicator + tap hint at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Step dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(totalSteps) { index ->
                    val isActive = index == stepNumber - 1
                    val width by animateDpAsState(
                        targetValue = if (isActive) 24.dp else 8.dp,
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
                        label = "dot_$index"
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

            Text(
                text = if (stepNumber < totalSteps) "Tap anywhere to continue" else "Tap to start vibing",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}
