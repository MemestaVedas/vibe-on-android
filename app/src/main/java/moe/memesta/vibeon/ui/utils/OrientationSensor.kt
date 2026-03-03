package moe.memesta.vibeon.ui.utils

import android.view.OrientationEventListener
import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

/**
 * Detects whether the device is physically held in landscape orientation
 * via the raw sensor angle — even though the Activity is locked to portrait.
 *
 * Returns true when the device is rotated ~90° or ~270° (landscape).
 */
@Composable
fun rememberIsLandscape(): Boolean {
    val context = LocalContext.current
    var isLandscape by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val listener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(angle: Int) {
                if (angle == ORIENTATION_UNKNOWN) return
                // Landscape-left (~270°) or Landscape-right (~90°), ±30° tolerance
                isLandscape = angle in 60..120 || angle in 240..300
            }
        }
        if (listener.canDetectOrientation()) {
            listener.enable()
        }
        onDispose {
            listener.disable()
        }
    }

    return isLandscape
}
