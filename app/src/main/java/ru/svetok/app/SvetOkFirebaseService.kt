package ru.svetok.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import ru.svetok.app.data.subscription.HttpSubscriptionRepository
import ru.svetok.app.data.subscription.SubscriptionPrefs

class SvetOkFirebaseService : FirebaseMessagingService() {

    private val subscriptionRepository: HttpSubscriptionRepository by inject()
    private val subscriptionPrefs: SubscriptionPrefs by inject()

    override fun onNewToken(token: String) {
        val streets = subscriptionPrefs.subscribedStreetNorms
        if (streets.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            streets.forEach { norm ->
                subscriptionRepository.upsertSubscription(fcmToken = token, streetNorm = norm)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val notification = message.notification ?: return
        showNotification(
            title = notification.title ?: "СветОк",
            body = notification.body ?: "",
        )
    }

    private fun showNotification(title: String, body: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Отключения электроэнергии",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Уведомления о плановых и аварийных отключениях"
        }
        manager.createNotificationChannel(channel)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        private const val CHANNEL_ID = "outages"
    }
}
