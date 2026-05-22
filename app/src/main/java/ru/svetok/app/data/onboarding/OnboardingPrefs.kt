package ru.svetok.app.data.onboarding

import android.content.Context

class OnboardingPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("onboarding", Context.MODE_PRIVATE)

    val isCompleted: Boolean get() = prefs.getBoolean(KEY_DONE, false)

    fun markCompleted() {
        prefs.edit().putBoolean(KEY_DONE, true).apply()
    }

    companion object {
        private const val KEY_DONE = "done"
    }
}
