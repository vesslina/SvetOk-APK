package ru.svetok.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.svetok.app.data.geo.GeoJsonRepository
import ru.svetok.app.data.subscription.HttpSubscriptionRepository
import ru.svetok.app.data.subscription.SubscriptionPrefs

data class StreetInfo(val norm: String, val displayName: String)

data class SettingsUiState(
    val fcmToken: String? = null,
    val searchQuery: String = "",
    val filteredStreets: List<StreetInfo> = emptyList(),
    val subscribedNorms: Set<String> = emptySet(),
    val isBusy: Boolean = false,
    val saveMessage: String? = null,
)

class SettingsViewModel(
    private val subscriptionRepository: HttpSubscriptionRepository,
    private val subscriptionPrefs: SubscriptionPrefs,
    private val fcmTokenProvider: FcmTokenProvider,
    private val geoJsonRepository: GeoJsonRepository,
) : ViewModel() {

    private var allStreets: List<StreetInfo> = emptyList()

    private val _uiState = MutableStateFlow(
        SettingsUiState(subscribedNorms = subscriptionPrefs.subscribedStreetNorms)
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { _uiState.update { it.copy(fcmToken = fcmTokenProvider.getToken()) } }
        viewModelScope.launch(Dispatchers.IO) {
            allStreets = geoJsonRepository.loadStreetFeatures()
                .map { StreetInfo(it.streetNorm, it.displayName) }
                .sortedBy { it.displayName }
        }
    }

    fun onSearchQueryChanged(query: String) {
        val filtered = if (query.isBlank()) emptyList()
        else {
            val lower = query.trim().lowercase()
            allStreets.filter {
                it.displayName.lowercase().contains(lower) || it.norm.contains(lower)
            }.take(20)
        }
        _uiState.update { it.copy(searchQuery = query, filteredStreets = filtered) }
    }

    fun subscribe(street: StreetInfo) {
        val token = _uiState.value.fcmToken ?: return
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            val ok = subscriptionRepository.upsertSubscription(fcmToken = token, streetNorm = street.norm)
            if (ok) subscriptionPrefs.addStreet(street.norm)
            _uiState.update {
                it.copy(
                    isBusy = false,
                    subscribedNorms = subscriptionPrefs.subscribedStreetNorms,
                    saveMessage = if (ok) "Подписка на «${street.displayName}» сохранена." else "Ошибка соединения.",
                )
            }
        }
    }

    fun unsubscribe(street: StreetInfo) {
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            subscriptionRepository.deleteSubscription(streetNorm = street.norm)
            subscriptionPrefs.removeStreet(street.norm)
            _uiState.update {
                it.copy(isBusy = false, subscribedNorms = subscriptionPrefs.subscribedStreetNorms)
            }
        }
    }

    fun clearSaveMessage() { _uiState.update { it.copy(saveMessage = null) } }
}
