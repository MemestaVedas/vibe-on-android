package moe.memesta.vibeon.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages onboarding state persistence.
 * Tracks whether the user has completed the welcome tutorial
 * and the home screen walkthrough.
 */
class OnboardingManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "vibe_on_onboarding",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_WELCOME_COMPLETED = "welcome_completed"
        private const val KEY_WALKTHROUGH_COMPLETED = "walkthrough_completed"
        private const val KEY_HOLD_GESTURE_SEEN = "hold_gesture_seen"
    }

    /** Whether the initial "Welcome to VIBE-ON!" tutorial has been completed */
    var isWelcomeCompleted: Boolean
        get() = prefs.getBoolean(KEY_WELCOME_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_WELCOME_COMPLETED, value).apply()

    /** Whether the one-time home screen walkthrough has been shown */
    var isWalkthroughCompleted: Boolean
        get() = prefs.getBoolean(KEY_WALKTHROUGH_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_WALKTHROUGH_COMPLETED, value).apply()

    /** Whether the hold-to-navigate gesture tooltip has been shown */
    var isHoldGestureSeen: Boolean
        get() = prefs.getBoolean(KEY_HOLD_GESTURE_SEEN, false)
        set(value) = prefs.edit().putBoolean(KEY_HOLD_GESTURE_SEEN, value).apply()

    /** Reset all onboarding state (for testing/debug) */
    fun resetAll() {
        prefs.edit()
            .remove(KEY_WELCOME_COMPLETED)
            .remove(KEY_WALKTHROUGH_COMPLETED)
            .remove(KEY_HOLD_GESTURE_SEEN)
            .apply()
    }
}
