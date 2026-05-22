package ru.svetok.app.ui.outages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.svetok.app.data.admin.AdminSessionPrefs
import ru.svetok.app.data.admin.HttpAdminRepository
import ru.svetok.app.data.outage.OutageMapStatus
import ru.svetok.app.data.outage.OutageRepository

data class OutageListItem(
    val id: String,
    val intId: Int?,
    val displayTitle: String,
    val timeLabel: String,
    val reason: String?,
    val streets: List<String>,
    val status: OutageMapStatus,
)

data class OutagesListUiState(
    val isLoading: Boolean = false,
    val isAdmin: Boolean = false,
    val outages: List<OutageListItem> = emptyList(),
    val error: String? = null,
)

class OutagesListViewModel(
    private val outageRepo: OutageRepository,
    private val adminRepo: HttpAdminRepository,
    private val sessionPrefs: AdminSessionPrefs,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OutagesListUiState())
    val uiState: StateFlow<OutagesListUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val isAdmin = sessionPrefs.isLoggedIn
            val items: List<OutageListItem> = if (isAdmin) loadAdminOutages() else loadPublicOutages()
            _uiState.update { it.copy(isLoading = false, isAdmin = isAdmin, outages = items) }
        }
    }

    fun endOutage(intId: Int) {
        viewModelScope.launch {
            if (adminRepo.endOutage(intId)) refresh()
        }
    }

    private suspend fun loadPublicOutages(): List<OutageListItem> {
        return try {
            val result = outageRepo.loadCurrentOutages()
            result.outages.mapIndexed { idx, o ->
                val type = if (o.status == OutageMapStatus.ACTIVE) "Аварийное" else "Плановое"
                OutageListItem(
                    id = o.id,
                    intId = o.id.toIntOrNull(),
                    displayTitle = "$type отключение #${idx + 1}",
                    timeLabel = o.timeLabel,
                    reason = o.reason.takeUnless { it.isBlank() || it == "Причина не указана" },
                    streets = o.streetLabels,
                    status = o.status,
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    private suspend fun loadAdminOutages(): List<OutageListItem> {
        return try {
            adminRepo.getActiveOutages().map { o ->
                val type = if (o.outageType == "emergency") "Аварийное" else "Плановое"
                OutageListItem(
                    id = o.id.toString(),
                    intId = o.id,
                    displayTitle = "$type — ${o.scopeName}",
                    timeLabel = "${o.startsAt} → ${o.endsAt ?: "до распоряжения"}",
                    reason = o.reason?.takeUnless { it.isBlank() },
                    streets = o.streets,
                    status = if (o.status == "active") OutageMapStatus.ACTIVE else OutageMapStatus.PLANNED,
                )
            }
        } catch (_: Exception) { emptyList() }
    }
}
