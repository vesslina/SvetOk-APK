package ru.svetok.app.data.outage

class LocalOutageRepository : OutageRepository {
    override suspend fun loadCurrentOutages(): OutageLoadResult = OutageLoadResult(
        source = OutageSource.DEMO,
        outages = emptyList(),
    )
}
