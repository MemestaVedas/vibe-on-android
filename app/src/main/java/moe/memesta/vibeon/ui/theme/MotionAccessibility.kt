package moe.memesta.vibeon.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Reads system animator scale and exposes reduced-motion preference.
 */
fun Context.prefersReducedMotion(): Boolean {
    val scale = Settings.Global.getFloat(
        contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f
    )
    return scale == 0f
}

@Composable
fun rememberPrefersReducedMotion(): Boolean {
    val context = LocalContext.current
    var reducedMotion by remember { mutableStateOf(context.prefersReducedMotion()) }

    LaunchedEffect(context) {
        reducedMotion = context.prefersReducedMotion()
    }

    return reducedMotion
}
