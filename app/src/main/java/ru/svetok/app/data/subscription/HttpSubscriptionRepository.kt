package ru.svetok.app.data.subscription

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.svetok.app.data.outage.ApiConfig
import ru.svetok.app.util.DeviceIdProvider

class HttpSubscriptionRepository(
    private val apiConfig: ApiConfig,
    private val httpClient: HttpClient,
    private val deviceIdProvider: DeviceIdProvider,
) {
    suspend fun upsertSubscription(fcmToken: String, streetNorm: String?): Boolean {
        if (!apiConfig.isConfigured) return false
        return runCatching {
            httpClient.post("${apiConfig.baseUrl}/api/subscriptions") {
                header(ApiConfig.CLIENT_TOKEN_HEADER, apiConfig.clientToken)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    SubscriptionUpsertDto(
                        deviceId = deviceIdProvider.getDeviceId(),
                        fcmToken = fcmToken,
                        streetNorm = streetNorm,
                    )
                )
            }
            true
        }.onFailure {
            Log.e(TAG, "Subscription upsert failed", it)
        }.getOrDefault(false)
    }

    suspend fun deleteSubscription(streetNorm: String?): Boolean {
        if (!apiConfig.isConfigured) return false
        return runCatching {
            httpClient.delete("${apiConfig.baseUrl}/api/subscriptions") {
                header(ApiConfig.CLIENT_TOKEN_HEADER, apiConfig.clientToken)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    SubscriptionDeleteDto(
                        deviceId = deviceIdProvider.getDeviceId(),
                        streetNorm = streetNorm,
                    )
                )
            }
            true
        }.onFailure {
            Log.e(TAG, "Subscription delete failed", it)
        }.getOrDefault(false)
    }

    private companion object {
        const val TAG = "HttpSubscriptionRepo"
    }
}

@Serializable
private data class SubscriptionUpsertDto(
    @SerialName("device_id") val deviceId: String,
    @SerialName("fcm_token") val fcmToken: String,
    @SerialName("street_norm") val streetNorm: String? = null,
)

@Serializable
private data class SubscriptionDeleteDto(
    @SerialName("device_id") val deviceId: String,
    @SerialName("street_norm") val streetNorm: String? = null,
)
