package ru.svetok.app.data.outage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class RoomOutageRepository(
    private val remoteRepository: HttpOutageRepository,
    private val localOutageRepository: LocalOutageRepository,
    private val outageCacheDao: OutageCacheDao,
) : OutageRepository {

    override fun observeCachedOutages(): Flow<OutageLoadResult?> =
        combine(
            outageCacheDao.observeMeta(),
            outageCacheDao.observeOutages(),
        ) { meta, outages ->
            buildCachedResult(meta = meta, outages = outages)
        }

    override suspend fun loadCurrentOutages(): OutageLoadResult =
        readCachedResult() ?: refreshCurrentOutages()

    override suspend fun refreshCurrentOutages(): OutageLoadResult {
        val remoteResult = remoteRepository.loadCurrentOutages()

        return when (remoteResult.source) {
            OutageSource.API -> {
                persistRemoteResult(remoteResult)
                remoteResult.copy(updatedAtMs = System.currentTimeMillis())
            }
            OutageSource.CACHE -> remoteResult
            OutageSource.DEMO -> {
                // API unavailable — try local Room cache, otherwise return empty state
                readCachedResult() ?: OutageLoadResult(
                    source = OutageSource.CACHE,
                    outages = emptyList(),
                )
            }
        }
    }

    private suspend fun persistRemoteResult(result: OutageLoadResult) {
        outageCacheDao.replaceCache(
            meta = OutageCacheMetaEntity(
                source = result.source.name,
                infoMessage = result.infoMessage,
                updatedAtEpochMs = System.currentTimeMillis(),
            ),
            outages = result.outages.mapIndexed { index, outage ->
                outage.toEntity(sortOrder = index)
            },
        )
    }

    private suspend fun readCachedResult(): OutageLoadResult? =
        buildCachedResult(
            meta = outageCacheDao.getMeta(),
            outages = outageCacheDao.getOutages(),
        )

    private fun buildCachedResult(
        meta: OutageCacheMetaEntity?,
        outages: List<CachedOutageEntity>,
    ): OutageLoadResult? {
        if (meta == null && outages.isEmpty()) return null
        return OutageLoadResult(
            source = OutageSource.CACHE,
            outages = outages.map(CachedOutageEntity::toMapOutage),
            updatedAtMs = meta?.updatedAtEpochMs,
        )
    }
}

private fun MapOutage.toEntity(sortOrder: Int): CachedOutageEntity = CachedOutageEntity(
    id = id,
    title = title,
    status = status.name,
    reason = reason,
    timeLabel = timeLabel,
    streetNorms = streetNorms,
    streetLabels = streetLabels,
    sortOrder = sortOrder,
)

private fun CachedOutageEntity.toMapOutage(): MapOutage = MapOutage(
    id = id,
    title = title,
    status = status.toOutageStatus(),
    reason = reason,
    timeLabel = timeLabel,
    streetNorms = streetNorms,
    streetLabels = streetLabels,
)

private fun String.toOutageStatus(): OutageMapStatus =
    runCatching { OutageMapStatus.valueOf(this) }.getOrDefault(OutageMapStatus.PLANNED)
