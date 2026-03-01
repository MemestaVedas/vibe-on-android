package moe.memesta.vibeon.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.memesta.vibeon.R
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
                        subtitle = "Tap the vibe button to search your library",
                        icon = Icons.Rounded.Search,
                        accentPink = accentPink,
                        contentColor = contentColor,
                        stepNumber = 1,
                        totalSteps = totalSteps,
                        alignment = Alignment.BottomEnd
                    )
                    1 -> WalkthroughStep(
                        title = "VIBE",
                        subtitle = "Hold the vibe button to switch between pages.\nRelease over a page to jump there.",
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
private fun GhostHandAnimation(
    stepNumber: Int,
    accentPink: Color,
    contentColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ghost_anim_$stepNumber")
    val handAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ghost_anim"
    )

    // Base item (Vibe button for steps 1 & 2, Player Pill for step 3)
    Box(contentAlignment = Alignment.Center) {
        if (stepNumber == 1 || stepNumber == 2) {
            // Mock Vibe button (Matching DynamicNavButton Search)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(accentPink)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp).align(Alignment.Center)
                )
            }
        } else {
            // Mock Player Pill
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(64.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(contentColor)
            ) {
                // simple pill representation
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(accentPink.copy(alpha=0.5f)))
                    Box(modifier = Modifier.weight(1f).height(8.dp).padding(horizontal = 12.dp).clip(RoundedCornerShape(4.dp)).background(accentPink.copy(alpha=0.2f)))
                }
            }
        }

        // Ghost Hand Logic based on step
        val animValues = when (stepNumber) {
            1 -> { // Search (Tap)
                // Phases: 0.0-0.3 Move in, 0.3-0.5 Hold (Tap), 0.5-0.7 Ripple scale, 0.7-1.0 Move out
                val x = when {
                    handAnimation < 0.3f -> androidx.compose.ui.util.lerp(120f, 0f, handAnimation / 0.3f)
                    handAnimation < 0.7f -> 0f
                    else -> androidx.compose.ui.util.lerp(0f, 120f, (handAnimation - 0.7f) / 0.3f)
                }
                val y = x
                val scale = if (handAnimation in 0.3f..0.7f) 0.85f else 1f
                val hAlpha = when {
                    handAnimation < 0.2f -> handAnimation / 0.2f
                    handAnimation < 0.8f -> 1f
                    else -> 1f - ((handAnimation - 0.8f) / 0.2f)
                }
                val rAlpha = if (handAnimation in 0.4f..0.7f) 1f - ((handAnimation - 0.4f) / 0.3f) else 0f
                val rScale = if (handAnimation in 0.4f..0.7f) androidx.compose.ui.util.lerp(1f, 2.0f, (handAnimation - 0.4f) / 0.3f) else 0f
                listOf(x, y, scale, hAlpha, rAlpha, rScale, 0f)
            }
            2 -> { // Vibe (Hold)
                // Phases: 0.0-0.2 Move in, 0.2-0.8 Hold, 0.8-1.0 Move out
                val x = when {
                    handAnimation < 0.2f -> androidx.compose.ui.util.lerp(120f, 0f, handAnimation / 0.2f)
                    handAnimation < 0.8f -> 0f
                    else -> androidx.compose.ui.util.lerp(0f, 120f, (handAnimation - 0.8f) / 0.2f)
                }
                val y = x
                val scale = if (handAnimation in 0.2f..0.8f) 0.85f else 1f
                val hAlpha = when {
                    handAnimation < 0.1f -> handAnimation / 0.1f
                    handAnimation < 0.9f -> 1f
                    else -> 1f - ((handAnimation - 0.9f) / 0.1f)
                }
                // Continuous rippling while holding
                val rAlpha = if (handAnimation in 0.3f..0.8f) {
                    val progress = ((handAnimation - 0.3f) / 0.5f) * 3f // 3 ripples
                    1f - (progress % 1f)
                } else 0f
                val rScale = if (handAnimation in 0.3f..0.8f) {
                    val progress = ((handAnimation - 0.3f) / 0.5f) * 3f
                    androidx.compose.ui.util.lerp(1f, 1.8f, progress % 1f)
                } else 0f
                listOf(x, y, scale, hAlpha, rAlpha, rScale, 0f)
            }
            else -> { // Swipe (Left/Right)
                // Phases: 0.0-0.2 Move to right edge of pill, 0.2-0.3 Press (scale down), 0.3-0.6 Swipe to left edge, 0.6-0.7 Release (scale up), 0.7-1.0 Move out
                val x = when {
                    handAnimation < 0.2f -> androidx.compose.ui.util.lerp(120f, 60f, handAnimation / 0.2f)
                    handAnimation < 0.3f -> 60f
                    handAnimation < 0.6f -> androidx.compose.ui.util.lerp(60f, -60f, (handAnimation - 0.3f) / 0.3f)
                    handAnimation < 0.7f -> -60f
                    else -> androidx.compose.ui.util.lerp(-60f, -120f, (handAnimation - 0.7f) / 0.3f)
                }
                val y = when {
                    handAnimation < 0.2f -> androidx.compose.ui.util.lerp(120f, 0f, handAnimation / 0.2f)
                    handAnimation < 0.7f -> 0f
                    else -> androidx.compose.ui.util.lerp(0f, 120f, (handAnimation - 0.7f) / 0.3f)
                }
                val scale = if (handAnimation in 0.2f..0.7f) 0.85f else 1f
                val hAlpha = when {
                    handAnimation < 0.1f -> handAnimation / 0.1f
                    handAnimation < 0.9f -> 1f
                    else -> 1f - ((handAnimation - 0.9f) / 0.1f)
                }
                
                // Track swipe trail
                val rAlpha = if (handAnimation in 0.3f..0.6f) 0.6f else 0f
                val rScale = 1f
                val rx = x
                listOf(x, y, scale, hAlpha, rAlpha, rScale, rx)
            }
        }
        val handX = animValues[0]
        val handY = animValues[1]
        val handScale = animValues[2]
        val handAlpha = animValues[3]
        val rippleAlpha = animValues[4]
        val rippleScale = animValues[5]
        val rippleOffsetX = animValues[6]

        // Ripple Effect (depends on step)
        if (stepNumber == 1 || stepNumber == 2) {
            if (rippleAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .graphicsLayer {
                            scaleX = rippleScale
                            scaleY = rippleScale
                            alpha = rippleAlpha
                        }
                        .clip(CircleShape)
                        .border(4.dp, accentPink, CircleShape)
                )
            }
        } else {
            // Swipe Trail
            if (rippleAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .graphicsLayer {
                            translationX = rippleOffsetX
                            alpha = rippleAlpha
                        }
                        .clip(CircleShape)
                        .background(accentPink.copy(alpha = 0.5f))
                )
            }
        }

        // The Ghost Hand
        Icon(
            imageVector = Icons.Rounded.TouchApp,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    translationX = handX
                    translationY = handY
                    alpha = handAlpha
                    scaleX = handScale
                    scaleY = handScale
                }
        )
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
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Animated Ghost Hand
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                GhostHandAnimation(
                    stepNumber = stepNumber,
                    accentPink = accentPink,
                    contentColor = contentColor
                )
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
                .padding(bottom = 140.dp),
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
