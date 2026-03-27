package moe.memesta.vibeon.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.graphics.TransformOrigin
import moe.memesta.vibeon.ui.theme.MotionTokens

private val TRANSITION_DURATION = MotionTokens.Duration.Slow
// Asymmetric easing: entering content decelerates into frame, exiting accelerates out.
private val ENTER_EASING = MotionTokens.EasingTokens.EnterEmphasis
private val EXIT_EASING  = MotionTokens.EasingTokens.ExitEmphasis

// Push: Enter from Right
fun enterTransition() = slideInHorizontally(
    animationSpec = tween(TRANSITION_DURATION, easing = ENTER_EASING),
    initialOffsetX = { it / 4 } // More subtle slide than full screen width
) + fadeIn(
    animationSpec = tween(TRANSITION_DURATION, easing = ENTER_EASING)
)

// Push: Exit to Left with Fade & Scale
fun exitTransition() = fadeOut(
    animationSpec = tween(TRANSITION_DURATION, easing = EXIT_EASING)
) + scaleOut(
    animationSpec = tween(TRANSITION_DURATION, easing = EXIT_EASING),
    targetScale = 0.96f
)

// Pop: Enter from Left with Parallax (slide in from left, slight zoom in for depth)
fun popEnterTransition() = scaleIn(
    animationSpec = tween(TRANSITION_DURATION, easing = ENTER_EASING),
    initialScale = 0.96f
) + fadeIn(
    animationSpec = tween(TRANSITION_DURATION, easing = ENTER_EASING)
)

// Pop: Exit to Right with Scale Down (current page slides out while scaling)
fun popExitTransition() = slideOutHorizontally(
    animationSpec = tween(TRANSITION_DURATION, easing = EXIT_EASING),
    targetOffsetX = { it / 4 }
) + fadeOut(
    animationSpec = tween(TRANSITION_DURATION, easing = EXIT_EASING)
)


// ─── Vertical lift transitions ────────────────────────────────────────────────
// Use for detail/overlay screens (album detail, artist detail, now playing).

fun verticalEnterTransition() = androidx.compose.animation.slideInVertically(
    animationSpec = tween(MotionTokens.Duration.Standard, easing = ENTER_EASING),
    initialOffsetY = { (it * 0.12f).toInt() }
) + scaleIn(
    animationSpec = tween(MotionTokens.Duration.Standard, easing = ENTER_EASING),
    initialScale = 0.94f
) + fadeIn(
    animationSpec = tween(MotionTokens.Duration.Standard, easing = ENTER_EASING)
)

fun verticalExitTransition() = androidx.compose.animation.slideOutVertically(
    animationSpec = tween(MotionTokens.Duration.Fast, easing = EXIT_EASING),
    targetOffsetY = { (it * 0.08f).toInt() }
) + scaleOut(
    animationSpec = tween(MotionTokens.Duration.Fast, easing = EXIT_EASING),
    targetScale = 0.96f
) + fadeOut(
    animationSpec = tween(MotionTokens.Duration.Fast, easing = EXIT_EASING)
)

// ─── Bottom-sheet transitions ─────────────────────────────────────────────────
// Full-height slide from the bottom edge — used for Now Playing.

fun sheetEnterTransition() = androidx.compose.animation.slideInVertically(
    animationSpec = tween(420, easing = ENTER_EASING),
    initialOffsetY = { it }           // start fully off-screen below
) + fadeIn(
    animationSpec = tween(320, easing = ENTER_EASING)
)

fun sheetExitTransition() = androidx.compose.animation.slideOutVertically(
    animationSpec = tween(340, easing = EXIT_EASING),
    targetOffsetY = { it }            // slide back down off-screen
) + fadeOut(
    animationSpec = tween(240, easing = EXIT_EASING)
)
