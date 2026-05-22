package ru.svetok.app.ui.map

import ru.svetok.app.data.geo.StreetFeature
import ru.svetok.app.data.outage.MapOutage
import ru.svetok.app.data.outage.OutageMapStatus

data class MapUiState(
    val isLoading: Boolean = true,
    val streets: List<StreetFeature> = emptyList(),
    val outages: List<MapOutage> = emptyList(),
    val highlightByStreetNorm: Map<String, OutageMapStatus> = emptyMap(),
    val selectedStreet: SelectedStreetUi? = null,
    val errorMessage: String? = null,
    val isOnline: Boolean = false,
    val lastUpdatedLabel: String? = null,
)

data class SelectedStreetUi(
    val streetNorm: String,
    val streetName: String,
    val status: OutageMapStatus,
    val outages: List<MapOutage>,
)
