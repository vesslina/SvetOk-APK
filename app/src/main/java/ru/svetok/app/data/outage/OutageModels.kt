package ru.svetok.app.data.outage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

enum class OutageMapStatus {
    PLANNED,
    ACTIVE,
}

enum class OutageSource {
    DEMO,
    API,
    CACHE,
}

data class MapOutage(
    val id: String,
    val title: String,
    val status: OutageMapStatus,
    val reason: String,
    val timeLabel: String,
    val streetNorms: List<String>,
    val streetLabels: List<String>,
)

data class OutageLoadResult(
    val source: OutageSource,
    val outages: List<MapOutage>,
    val infoMessage: String? = null,
    val updatedAtMs: Long? = null,
)

interface OutageRepository {
    fun observeCachedOutages(): Flow<OutageLoadResult?> = emptyFlow()

    suspend fun refreshCurrentOutages(): OutageLoadResult = loadCurrentOutages()

    suspend fun loadCurrentOutages(): OutageLoadResult
}
