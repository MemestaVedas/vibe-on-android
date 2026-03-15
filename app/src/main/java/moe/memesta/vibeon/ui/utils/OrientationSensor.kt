package moe.memesta.vibeon.ui.utils

import android.view.OrientationEventListener
import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Detects whether the device is physically held in landscape orientation
 * via the raw sensor angle — even though the Activity is locked to portrait.
 *
 * Returns true when the device is rotated to a deliberate landscape angle (±20° from 90°/270°).
 * A 600 ms debounce prevents accidental triggers from brief tilts.
 */
@Composable
fun rememberIsLandscape(): Boolean {
    val context = LocalContext.current
    var isLandscape by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    DisposableEffect(context) {
        var debounceJob: Job? = null
        val listener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(angle: Int) {
                if (angle == ORIENTATION_UNKNOWN) return
                // Require ±20° of true landscape (70–110° or 250–290°) to reduce accidental triggers
                val nowLandscape = angle in 70..110 || angle in 250..290
                when {
                    nowLandscape && !isLandscape -> {
                        // Delay before setting landscape to avoid momentary tilts triggering it
                        if (debounceJob?.isActive != true) {
                            debounceJob = scope.launch {
                                delay(600)
                                isLandscape = true
                            }
                        }
                    }
                    !nowLandscape -> {
                        // Cancel any pending landscape activation and return to portrait immediately
                        debounceJob?.cancel()
                        debounceJob = null
                        if (isLandscape) isLandscape = false
                    }
                }
            }
        }
        if (listener.canDetectOrientation()) {
            listener.enable()
        }
        onDispose {
            debounceJob?.cancel()
            listener.disable()
        }
    }

    return isLandscape
}
