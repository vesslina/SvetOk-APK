package ru.svetok.app.util

import android.content.Context
import java.util.UUID

class DeviceIdProvider(
    context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDeviceId(): String {
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) {
            return existing
        }

        val generated = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, generated).apply()
        return generated
    }

    private companion object {
        const val PREFS_NAME = "svetok-device"
        const val KEY_DEVICE_ID = "device_id"
    }
}
