package ru.svetok.app.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.svetok.app.data.geo.GeoJsonRepository
import ru.svetok.app.data.geo.StreetFeature
import ru.svetok.app.data.outage.MapOutage
import ru.svetok.app.data.outage.OutageLoadResult
import ru.svetok.app.data.outage.OutageMapStatus
import ru.svetok.app.data.outage.OutageRepository
import ru.svetok.app.data.outage.OutageSource  // needed for source comparison in refreshFromNetwork

class MapViewModel(
    private val geoJsonRepository: GeoJsonRepository,
    private val outageRepository: OutageRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var cachedStreets: List<StreetFeature>? = null
    private var latestOutageLoad: OutageLoadResult? = null
    private var autoRefreshJob: Job? = null
    private var isRefreshing = false
    private var isOnline = false          // set only by network refreshes, not by Room cache reads
    private var lastUpdatedMs: Long? = null

    init {
        observeCachedOutages()
        loadStreets()
        startAutoRefresh()
    }

    fun loadStreets() {
        val previousState = _uiState.value
        _uiState.value = previousState.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            runCatching {
                cachedStreets ?: async(Dispatchers.IO) {
                    geoJsonRepository.loadStreetFeatures()
                }.await().also { cachedStreets = it }
            }.onSuccess {
                renderState(outageLoad = latestOutageLoad, isLoading = latestOutageLoad == null)
            }.onFailure { error ->
                _uiState.value = previousState.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Не удалось загрузить карту города.",
                )
            }
        }
    }

    fun onStreetTapped(streetNorm: String) {
        val state = _uiState.value
        _uiState.value = state.copy(
            selectedStreet = buildSelectedStreet(
                streetNorm = streetNorm,
                streets = state.streets,
                outages = state.outages,
                highlightByStreetNorm = state.highlightByStreetNorm,
            ),
        )
    }

    fun clearStreetSelection() {
        _uiState.value = _uiState.value.copy(selectedStreet = null)
    }

    fun refreshNow() {
        viewModelScope.launch { refreshFromNetwork(showLoading = true) }
    }

    private fun observeCachedOutages() {
        viewModelScope.launch {
            outageRepository.observeCachedOutages().collect { cached ->
                cached ?: return@collect
                latestOutageLoad = cached
                renderState(outageLoad = cached, isLoading = cachedStreets == null)
            }
        }
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            refreshFromNetwork(showLoading = true)
            while (isActive) {
                delay(AUTO_REFRESH_INTERVAL_MS)
                refreshFromNetwork(showLoading = false)
            }
        }
    }

    private suspend fun refreshFromNetwork(showLoading: Boolean) {
        if (isRefreshing) return
        isRefreshing = true

        if (showLoading && latestOutageLoad == null) {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        }

        runCatching {
            outageRepository.refreshCurrentOutages()
        }.onSuccess { fresh ->
            // Track online status only from real network results
            val apiSuccess = fresh.source == OutageSource.API
            if (apiSuccess) {
                isOnline = true
                lastUpdatedMs = fresh.updatedAtMs ?: System.currentTimeMillis()
            } else {
                isOnline = false
            }
            latestOutageLoad = fresh
            renderState(outageLoad = fresh, isLoading = false)
        }.onFailure { error ->
            isOnline = false
            if (latestOutageLoad == null) {
                runCatching { outageRepository.loadCurrentOutages() }
                    .onSuccess { fallback ->
                        latestOutageLoad = fallback
                        renderState(outageLoad = fallback, isLoading = false)
                    }
                    .onFailure {
                        renderState(
                            outageLoad = null,
                            isLoading = false,
                            errorMessage = error.message ?: "Не удалось загрузить данные.",
                        )
                    }
            } else {
                renderState(outageLoad = latestOutageLoad, isLoading = false)
            }
        }

        isRefreshing = false
    }

    private fun renderState(
        outageLoad: OutageLoadResult?,
        isLoading: Boolean,
        errorMessage: String? = null,
    ) {
        val streets = cachedStreets.orEmpty()
        val outages = outageLoad?.outages.orEmpty()
        val highlightByStreetNorm = buildHighlightMap(
            outages = outages,
            knownStreetNorms = streets.mapTo(linkedSetOf()) { it.streetNorm },
        )

        _uiState.value = MapUiState(
            isLoading = isLoading,
            streets = streets,
            outages = outages,
            highlightByStreetNorm = highlightByStreetNorm,
            selectedStreet = _uiState.value.selectedStreet?.streetNorm?.let { streetNorm ->
                buildSelectedStreet(
                    streetNorm = streetNorm,
                    streets = streets,
                    outages = outages,
                    highlightByStreetNorm = highlightByStreetNorm,
                )
            },
            errorMessage = errorMessage,
            isOnline = isOnline,
            lastUpdatedLabel = lastUpdatedMs?.toHHmm()?.let { "Обновлено в $it" },
        )
    }

    private fun buildHighlightMap(
        outages: List<MapOutage>,
        knownStreetNorms: Set<String>,
    ): Map<String, OutageMapStatus> {
        val map = linkedMapOf<String, OutageMapStatus>()
        outages.forEach { outage ->
            outage.streetNorms.filter(knownStreetNorms::contains).forEach { norm ->
                val current = map[norm]
                map[norm] = when {
                    current == OutageMapStatus.ACTIVE -> OutageMapStatus.ACTIVE
                    outage.status == OutageMapStatus.ACTIVE -> OutageMapStatus.ACTIVE
                    else -> OutageMapStatus.PLANNED
                }
            }
        }
        return map
    }

    private fun buildSelectedStreet(
        streetNorm: String,
        streets: List<StreetFeature>,
        outages: List<MapOutage>,
        highlightByStreetNorm: Map<String, OutageMapStatus>,
    ): SelectedStreetUi? {
        val status = highlightByStreetNorm[streetNorm] ?: return null
        val street = streets.firstOrNull { it.streetNorm == streetNorm } ?: return null
        val streetOutages = outages.filter { streetNorm in it.streetNorms }
        if (streetOutages.isEmpty()) return null
        return SelectedStreetUi(
            streetNorm = streetNorm,
            streetName = street.displayName,
            status = status,
            outages = streetOutages,
        )
    }

    private companion object {
        const val AUTO_REFRESH_INTERVAL_MS = 30_000L
    }
}

private val HH_MM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    .withZone(ZoneId.systemDefault())

private fun Long.toHHmm(): String = HH_MM.format(Instant.ofEpochMilli(this))
