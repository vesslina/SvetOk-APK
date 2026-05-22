package ru.svetok.app.data.admin

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.svetok.app.data.outage.ApiConfig

class HttpAdminRepository(
    private val apiConfig: ApiConfig,
    private val httpClient: HttpClient,
    private val sessionPrefs: AdminSessionPrefs,
) {
    suspend fun login(login: String, password: String): AdminLoginResult {
        return try {
            val response: LoginResponseDto = httpClient
                .post("${apiConfig.baseUrl}/api/admin/login") {
                    header(ApiConfig.CLIENT_TOKEN_HEADER, apiConfig.clientToken)
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(LoginRequestDto(login = login.trim(), password = password))
                }
                .body()
            sessionPrefs.saveSession(response.token, response.adminLogin)
            AdminLoginResult.Success(response.adminLogin)
        } catch (e: ClientRequestException) {
            when (e.response.status.value) {
                401 -> AdminLoginResult.InvalidCredentials
                else -> AdminLoginResult.Error("Ошибка сервера: ${e.response.status.value}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login failed", e)
            AdminLoginResult.Error("Нет связи с сервером.")
        }
    }

    suspend fun getObjects(): AdminObjects? {
        val token = sessionPrefs.getToken() ?: return null
        return try {
            val dto: ObjectsResponseDto = httpClient
                .get("${apiConfig.baseUrl}/api/admin/objects") {
                    header(ApiConfig.CLIENT_TOKEN_HEADER, apiConfig.clientToken)
                    header(ADMIN_TOKEN_HEADER, token)
                }
                .body()
            AdminObjects(
                tps = dto.tps.map { AdminObject(it.id, it.name) },
                feeders = dto.feeders.map { AdminObject(it.id, it.name) },
                substations = dto.substations.map { AdminObject(it.id, it.name) },
            )
        } catch (e: ClientRequestException) {
            if (e.response.status.value == 401) sessionPrefs.clearSession()
            null
        } catch (e: Exception) {
            Log.e(TAG, "getObjects failed", e)
            null
        }
    }

    suspend fun getActiveOutages(): List<AdminActiveOutage> {
        val token = sessionPrefs.getToken() ?: return emptyList()
        return try {
            val dto: AdminOutagesResponseDto = httpClient
                .get("${apiConfig.baseUrl}/api/admin/outages") {
                    header(ApiConfig.CLIENT_TOKEN_HEADER, apiConfig.clientToken)
                    header(ADMIN_TOKEN_HEADER, token)
                }
                .body()
            dto.outages.map { it.toDomain() }
        } catch (e: ClientRequestException) {
            if (e.response.status.value == 401) sessionPrefs.clearSession()
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "getActiveOutages failed", e)
            emptyList()
        }
    }

    suspend fun createOutage(
        outageType: OutageType,
        scope: ScopeType,
        scopeRefId: Int,
        reason: String?,
        startsAt: String?,
        endsAt: String?,
    ): AdminCreateResult {
        val token = sessionPrefs.getToken() ?: return AdminCreateResult.Unauthorized
        return try {
            httpClient.post("${apiConfig.baseUrl}/api/admin/outages") {
                header(ApiConfig.CLIENT_TOKEN_HEADER, apiConfig.clientToken)
                header(ADMIN_TOKEN_HEADER, token)
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(
                    CreateOutageRequestDto(
                        outageType = outageType.apiValue,
                        scope = scope.apiValue,
                        scopeRefId = scopeRefId,
                        reason = reason?.takeUnless { it.isBlank() },
                        startsAt = startsAt?.takeUnless { it.isBlank() },
                        endsAt = endsAt?.takeUnless { it.isBlank() },
                    )
                )
            }
            AdminCreateResult.Success
        } catch (e: ClientRequestException) {
            when (e.response.status.value) {
                401 -> { sessionPrefs.clearSession(); AdminCreateResult.Unauthorized }
                else -> AdminCreateResult.Error("Ошибка сервера: ${e.response.status.value}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "createOutage failed", e)
            AdminCreateResult.Error("Нет связи с сервером.")
        }
    }

    suspend fun endOutage(outageId: Int): Boolean {
        val token = sessionPrefs.getToken() ?: return false
        return try {
            httpClient.delete("${apiConfig.baseUrl}/api/admin/outages/$outageId") {
                header(ApiConfig.CLIENT_TOKEN_HEADER, apiConfig.clientToken)
                header(ADMIN_TOKEN_HEADER, token)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "endOutage($outageId) failed", e)
            false
        }
    }

    companion object {
        const val ADMIN_TOKEN_HEADER = "X-Admin-Token"
        private const val TAG = "HttpAdminRepo"
    }
}

// ─── DTOs ─────────────────────────────────────────────────────

@Serializable
private data class LoginRequestDto(val login: String, val password: String)

@Serializable
private data class LoginResponseDto(
    val token: String,
    @SerialName("admin_login") val adminLogin: String,
)

@Serializable
private data class ObjectItemDto(val id: Int, val name: String)

@Serializable
private data class ObjectsResponseDto(
    val tps: List<ObjectItemDto>,
    val feeders: List<ObjectItemDto>,
    val substations: List<ObjectItemDto>,
)

@Serializable
private data class AdminOutagesResponseDto(val outages: List<AdminOutageDto>)

@Serializable
private data class AdminOutageDto(
    val id: Int,
    @SerialName("outage_type") val outageType: String,
    val scope: String,
    @SerialName("scope_name") val scopeName: String,
    val status: String,
    @SerialName("starts_at") val startsAt: String,
    @SerialName("ends_at") val endsAt: String? = null,
    val reason: String? = null,
    val streets: List<String>,
)

private fun AdminOutageDto.toDomain() = AdminActiveOutage(
    id = id,
    outageType = outageType,
    scope = scope,
    scopeName = scopeName,
    status = status,
    startsAt = startsAt,
    endsAt = endsAt,
    reason = reason,
    streets = streets,
)

@Serializable
private data class CreateOutageRequestDto(
    @SerialName("outage_type") val outageType: String,
    val scope: String,
    @SerialName("scope_ref_id") val scopeRefId: Int,
    val reason: String? = null,
    @SerialName("starts_at") val startsAt: String? = null,
    @SerialName("ends_at") val endsAt: String? = null,
)
