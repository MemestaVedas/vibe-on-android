package moe.memesta.vibeon.ui.components

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Hold-to-Navigate gesture modifier.
 *
 * Shows a visual progress indicator (circle filling around touch point)
 * and triggers haptic feedback when the hold threshold is reached.
 *
 * Design spec:
 * - "Add haptic feedback (slight vibration) and a visual progress indicator
 *    (like a circle filling up around the touch point)"
 */
@Composable
fun Modifier.holdToNavigate(
    holdDurationMs: Long = 500L,
    onHoldComplete: () -> Unit,
    enabled: Boolean = true
): Modifier {
    val context = LocalContext.current
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var isHolding by remember { mutableStateOf(false) }
    var touchPosition by remember { mutableStateOf(Offset.Zero) }

    val animatedProgress by animateFloatAsState(
        targetValue = holdProgress,
        animationSpec = tween(
            durationMillis = if (isHolding) holdDurationMs.toInt() else 150,
            easing = if (isHolding) LinearEasing else FastOutSlowInEasing
        ),
        label = "hold_progress"
    )

    return this
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                touchPosition = down.position
                isHolding = true
                holdProgress = 1f

                // Wait for either release or hold completion
                val startTime = System.currentTimeMillis()
                var completed = false

                try {
                    while (true) {
                        val event = awaitPointerEvent()
                        val elapsed = System.currentTimeMillis() - startTime

                        if (elapsed >= holdDurationMs && !completed) {
                            completed = true
                            // Haptic feedback
                            triggerHaptic(context)
                            onHoldComplete()
                        }

                        if (event.changes.all { it.changedToUp() }) {
                            break
                        }
                    }
                } catch (_: Exception) {
                    // Gesture cancelled
                }

                isHolding = false
                holdProgress = 0f
            }
        }
}

/**
 * Visual indicator for hold-to-navigate gesture.
 * Draws a circle that fills around the touch point.
 */
@Composable
fun HoldProgressIndicator(
    progress: Float,
    position: Offset,
    color: Color,
    modifier: Modifier = Modifier
) {
    if (progress <= 0f) return

    Canvas(modifier = modifier) {
        // Draw progress arc around touch point
        val radius = 28.dp.toPx()
        val strokeWidth = 3.dp.toPx()

        drawArc(
            color = color.copy(alpha = 0.3f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(position.x - radius, position.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            topLeft = Offset(position.x - radius, position.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Center dot
        drawCircle(
            color = color.copy(alpha = 0.5f * progress),
            radius = 4.dp.toPx(),
            center = position
        )
    }
}

private fun triggerHaptic(context: android.content.Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(30)
            }
        }
    } catch (_: Exception) {
        // Haptic not available
    }
}
