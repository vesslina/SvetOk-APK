package ru.svetok.app.data.admin

import android.content.Context

sealed class TapResult {
    data object AlreadyUnlocked : TapResult()
    data object Unlocked : TapResult()
    data class Progress(val remaining: Int) : TapResult()
}

class AdminSessionPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("admin_session", Context.MODE_PRIVATE)

    val isLoggedIn: Boolean get() = getToken() != null
    val isAdminAccessUnlocked: Boolean get() = prefs.getBoolean(KEY_UNLOCKED, false)

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)
    fun getLogin(): String? = prefs.getString(KEY_LOGIN, null)

    fun saveSession(token: String, login: String) {
        prefs.edit().putString(KEY_TOKEN, token).putString(KEY_LOGIN, login).apply()
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_LOGIN)
            .apply()
    }

    // ── Secret unlock: tap title 5 times ────────────────────
    private var tapCount = 0
    private var lastTapMs = 0L

    fun onTitleTapped(): TapResult {
        if (isAdminAccessUnlocked) return TapResult.AlreadyUnlocked

        val now = System.currentTimeMillis()
        if (now - lastTapMs > 3_000L) tapCount = 0   // reset if gap > 3s
        lastTapMs = now
        tapCount++

        return if (tapCount >= 5) {
            tapCount = 0
            prefs.edit().putBoolean(KEY_UNLOCKED, true).apply()
            TapResult.Unlocked
        } else {
            TapResult.Progress(remaining = 5 - tapCount)
        }
    }

    companion object {
        private const val KEY_TOKEN    = "admin_token"
        private const val KEY_LOGIN    = "admin_login"
        private const val KEY_UNLOCKED = "admin_unlocked"
    }
}
