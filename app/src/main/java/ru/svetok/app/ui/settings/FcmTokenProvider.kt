package ru.svetok.app.ui.settings

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

class FcmTokenProvider {
    suspend fun getToken(): String? = runCatching {
        FirebaseMessaging.getInstance().token.await()
    }.getOrNull()
}
