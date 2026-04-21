package moe.memesta.vibeon.ui.navigation

import androidx.navigation.NavBackStackEntry

fun NavBackStackEntry.requireStringArg(key: String): String {
    return requireNotNull(arguments?.getString(key)) { "Missing navigation argument: $key" }
}
