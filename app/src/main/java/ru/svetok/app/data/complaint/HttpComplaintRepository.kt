package ru.svetok.app.data.complaint

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.svetok.app.BuildConfig
import ru.svetok.app.data.outage.ApiConfig
import ru.svetok.app.util.DeviceIdProvider

class HttpComplaintRepository(
    private val apiConfig: ApiConfig,
    private val httpClient: HttpClient,
    private val deviceIdProvider: DeviceIdProvider,
) {
    suspend fun submitComplaint(
        street: String?,
        house: String?,
        message: String,
    ): ComplaintSubmitResult {
        if (!apiConfig.isConfigured) {
            return ComplaintSubmitResult(
                isSuccess = false,
                message = "HTTP API не настроен.",
            )
        }

        return try {
            val response: ComplaintCreateResponseDto = httpClient.post("${apiConfig.baseUrl}/api/complaints") {
                header(ApiConfig.CLIENT_TOKEN_HEADER, apiConfig.clientToken)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    ComplaintCreateRequestDto(
                        deviceId = deviceIdProvider.getDeviceId(),
                        street = street?.trim().takeUnless { it.isNullOrBlank() },
                        house = house?.trim().takeUnless { it.isNullOrBlank() },
                        message = message.trim(),
                    )
                )
            }.body()

            ComplaintSubmitResult(
                isSuccess = true,
                message = response.message,
            )
        } catch (error: ClientRequestException) {
            val body = runCatching { error.response.bodyAsText() }.getOrNull().orEmpty()
            Log.w(TAG, "Complaint request failed with ${error.response.status.value}: $body", error)
            val friendlyMessage = when (error.response.status.value) {
                429 -> "Не чаще одной жалобы в минуту с одного устройства."
                422 -> "Проверь заполнение полей жалобы."
                else -> body.extractApiErrorMessage() ?: "Не удалось отправить жалобу."
            }

            ComplaintSubmitResult(
                isSuccess = false,
                message = friendlyMessage,
            )
        } catch (error: ServerResponseException) {
            Log.e(TAG, "Complaint server error", error)
            ComplaintSubmitResult(
                isSuccess = false,
                message = "Сервер временно недоступен. Попробуй позже.",
            )
        } catch (error: Exception) {
            Log.e(TAG, "Complaint request crashed", error)
            ComplaintSubmitResult(
                isSuccess = false,
                message = if (BuildConfig.DEBUG) {
                    "Жалоба не отправлена. ${error::class.java.simpleName}: ${error.message.orEmpty()}".trim()
                } else {
                    "Нет связи с сервером. Жалоба не отправлена."
                },
            )
        }
    }

    private companion object {
        const val TAG = "HttpComplaintRepo"
    }
}

data class ComplaintSubmitResult(
    val isSuccess: Boolean,
    val message: String,
)

@Serializable
private data class ComplaintCreateRequestDto(
    @SerialName("device_id")
    val deviceId: String,
    val street: String? = null,
    val house: String? = null,
    val message: String,
)

@Serializable
private data class ComplaintCreateResponseDto(
    val status: String,
    @SerialName("complaint_id")
    val complaintId: Int,
    val message: String,
)

private fun String.extractApiErrorMessage(): String? =
    Regex(""""detail"\s*:\s*"([^"]+)"""")
        .find(this)
        ?.groupValues
        ?.getOrNull(1)
