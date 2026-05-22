package ru.svetok.app.data.outage

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class HttpOutageRepository(
    private val apiConfig: ApiConfig,
    private val httpClient: HttpClient,
    private val localOutageRepository: LocalOutageRepository,
) : OutageRepository {

    override suspend fun loadCurrentOutages(): OutageLoadResult {
        if (!apiConfig.isConfigured) {
            return localOutageRepository.loadCurrentOutages()
        }

        return runCatching {
            val payload: ActiveOutagesResponseDto = httpClient
                .get("${apiConfig.baseUrl}/api/outages/active") {
                    header(ApiConfig.CLIENT_TOKEN_HEADER, apiConfig.clientToken)
                }
                .body()

            OutageLoadResult(
                source = OutageSource.API,
                outages = payload.outages.map(OutageDto::toMapOutage),
            )
        }.getOrElse {
            localOutageRepository.loadCurrentOutages()
        }
    }
}

@Serializable
private data class ActiveOutagesResponseDto(
    val source: String,
    val outages: List<OutageDto>,
    @SerialName("info_message")
    val infoMessage: String? = null,
)

@Serializable
private data class OutageDto(
    val id: String,
    val title: String,
    val status: String,
    val reason: String,
    @SerialName("time_label")
    val timeLabel: String,
    @SerialName("street_norms")
    val streetNorms: List<String>,
    @SerialName("street_labels")
    val streetLabels: List<String>,
)

private fun OutageDto.toMapOutage(): MapOutage = MapOutage(
    id = id,
    title = title,
    status = if (status == "active") OutageMapStatus.ACTIVE else OutageMapStatus.PLANNED,
    reason = reason,
    timeLabel = timeLabel,
    streetNorms = streetNorms,
    streetLabels = streetLabels,
)
