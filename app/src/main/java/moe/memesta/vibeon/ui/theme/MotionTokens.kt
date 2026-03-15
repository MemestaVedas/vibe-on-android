package moe.memesta.vibeon.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Central motion tokens for Material 3 Expressive-aligned choreography.
 *
 * Keep animation selection semantic (what it does) instead of raw timing values.
 */
object MotionTokens {

    object Duration {
        const val Fast = 180
        const val Standard = 320
        const val Slow = 520
    }

    object EasingTokens {
        val Standard: Easing = FastOutSlowInEasing
        val EnterEmphasis: Easing = androidx.compose.animation.core.EaseOutCubic
        val ExitEmphasis: Easing = androidx.compose.animation.core.EaseInCubic
    }

    object Effects {
        fun fast(): TweenSpec<Float> = tween(
            durationMillis = Duration.Fast,
            easing = EasingTokens.Standard
        )

        fun standard(): TweenSpec<Float> = tween(
            durationMillis = Duration.Standard,
            easing = EasingTokens.Standard
        )

        fun slow(): TweenSpec<Float> = tween(
            durationMillis = Duration.Slow,
            easing = EasingTokens.Standard
        )
    }

    object Spatial {
        fun fast(): SpringSpec<Float> = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMedium
        )

        fun standard(): SpringSpec<Float> = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMediumLow
        )

        fun slow(): SpringSpec<Float> = spring(
            dampingRatio = 0.8f,
            stiffness = Spring.StiffnessLow
        )
    }

    /**
     * Consistent stagger for list reveal animations.
     */
    fun staggeredEffects(
        index: Int,
        baseDuration: Int = Duration.Standard,
        stepDelay: Int = 36,
        maxDelay: Int = 360
    ): AnimationSpec<Float> = tween(
        durationMillis = baseDuration,
        delayMillis = (index * stepDelay).coerceAtMost(maxDelay),
        easing = EasingTokens.Standard
    )
}

// ─── Stagger orchestration helpers ───────────────────────────────────────────

/**
 * Delay for the nth item in a staggered reveal, capped to avoid long waits.
 *
 * @param index   0-based position in the list.
 * @param step    Milliseconds between each item's reveal. Default 40ms.
 * @param maxDelay Cap to keep the last item from waiting too long. Default 400ms.
 */
fun staggerDelay(index: Int, step: Int = 40, maxDelay: Int = 400): Int =
    (index * step).coerceAtMost(maxDelay)

/**
 * Returns an [AnimationSpec] for a staggered alpha or translation reveal.
 * Uses [MotionTokens.EasingTokens.EnterEmphasis] for a kinetic deceleration feel.
 *
 * @param index        0-based position.
 * @param baseDuration Duration of each item's animation in ms.
 * @param step         Delay step per item in ms.
 * @param maxDelay     Maximum total delay in ms.
 */
fun staggerEnterSpec(
    index: Int,
    baseDuration: Int = MotionTokens.Duration.Standard,
    step: Int = 40,
    maxDelay: Int = 400
): AnimationSpec<Float> = tween(
    durationMillis = baseDuration,
    delayMillis = staggerDelay(index, step, maxDelay),
    easing = MotionTokens.EasingTokens.EnterEmphasis
)
