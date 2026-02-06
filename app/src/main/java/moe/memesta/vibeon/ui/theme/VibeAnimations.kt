package moe.memesta.vibeon.ui.theme

import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.SpringSpec

/**
 * Centralized animation configuration for organic, polished UI feel.
 * Inspired by Material 3 Expressive and Apple HIG principles.
 */
object VibeAnimations {
    
    // ===== SPRING SPECS =====
    
    /**
     * Organic spring with subtle bounce (Material 3 Expressive style).
     * Use for press/release effects, nav transitions, interactive elements.
     */
    val SpringExpressive = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,  // 0.75f - subtle overshoot
        stiffness = Spring.StiffnessMediumLow         // 200f - relaxed, not snappy
    )
    
    /**
     * Standard spring with no bounce.
     * Use for utility animations where bounce would feel distracting.
     */
    val SpringStandard = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,   // 1.0f - no overshoot
        stiffness = Spring.StiffnessMedium            // 400f - responsive
    )

    /**
     * Helper to create a standard spring for any type (e.g., Color).
     */
    fun <T> springStandardGeneric(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /**
     * Helper to create an expressive spring for any type.
     */
    fun <T> springExpressiveGeneric(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
    
    /**
     * Fast, snappy spring for micro-interactions.
     */
    val SpringSnappy = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy, // 0.5f
        stiffness = Spring.StiffnessHigh                // 10000f
    )
    
    // ===== TWEEN SPECS (Easing Curves) =====
    
    /**
     * Screen enter transition - slow start, fast end.
     */
    val ScreenEnterDuration = 350
    val ScreenEnterSpec = tween<Float>(
        durationMillis = ScreenEnterDuration,
        easing = EaseOutExpo
    )
    
    /**
     * Screen exit transition - faster, gets out of the way.
     */
    val ScreenExitDuration = 250
    val ScreenExitSpec = tween<Float>(
        durationMillis = ScreenExitDuration,
        easing = EaseInCubic
    )
    
    // ===== SCALE VALUES =====
    
    /** Scale factor when element is pressed */
    const val PressScale = 0.92f
    
    /** Scale factor for subtle hover/focus effect */
    const val FocusScale = 1.02f
    
    // ===== DURATION CONSTANTS =====
    
    const val FadeDuration = 200
    const val MicroDuration = 100
    const val HeroDuration = 450
}
