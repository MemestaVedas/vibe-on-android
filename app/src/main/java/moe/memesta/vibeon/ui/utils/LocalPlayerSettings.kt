package moe.memesta.vibeon.ui.utils

import androidx.compose.runtime.compositionLocalOf
import moe.memesta.vibeon.data.local.DisplayLanguage

val LocalDisplayLanguage = compositionLocalOf { DisplayLanguage.ORIGINAL }
