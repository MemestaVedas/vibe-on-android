package moe.memesta.vibeon.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Centralized animation configuration for organic, polished UI feel.
 * Inspired by Material 3 Expressive and Apple HIG principles.
 */
object VibeAnimations {
    
    // ===== SPRING SPECS =====
    
    /**
     * Organic spring with subtle bounce (Material 3 Expressive style).
     * Use for press/release effects, nav transitions, interactive elements.
     * Tuned for "very smooth" feel (low stiffness).
     */
    val SpringExpressive = spring<Float>(
        dampingRatio = 0.8f,                  // Slightly less bouncy than LowBouncy (0.75f) for stability
        stiffness = 110f                      // Much softer than MediumLow (200f) for fluid motion
    )
    
    /**
     * Standard spring with no bounce.
     * Use for utility animations where bounce would feel distracting.
     */
    val SpringStandard = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 300f                      // Slightly softer than Medium (400f)
    )

    /**
     * Helper to create a standard spring for any type (e.g., Color).
     */
    fun <T> springStandardGeneric(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 300f
    )

    /**
     * Helper to create an expressive spring for any type.
     */
    fun <T> springExpressiveGeneric(): SpringSpec<T> = spring(
        dampingRatio = 0.8f,
        stiffness = 110f
    )
    
    /**
     * Fast, snappy spring for micro-interactions.
     */
    val SpringSnappy = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
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

/**
 * A modifier that adds a bouncy click effect.
 * Handles scale, rotation (subtle), and alpha changes on press.
 */
fun Modifier.bouncyClickable(
    enabled: Boolean = true,
    scaleDown: Float = 0.92f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = VibeAnimations.SpringExpressive,
        label = "bouncyScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            enabled = enabled,
            onClick = onClick
        )
}
