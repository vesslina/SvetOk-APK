package ru.svetok.app.data.subscription

import android.content.Context

private const val PREFS_NAME = "svetok_subscriptions"
private const val KEY_STREETS = "subscribed_streets"

class SubscriptionPrefs(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val subscribedStreetNorms: Set<String>
        get() = prefs.getStringSet(KEY_STREETS, emptySet())?.toSet() ?: emptySet()

    fun addStreet(norm: String) {
        prefs.edit().putStringSet(KEY_STREETS, subscribedStreetNorms + norm).apply()
    }

    fun removeStreet(norm: String) {
        prefs.edit().putStringSet(KEY_STREETS, subscribedStreetNorms - norm).apply()
    }
}
