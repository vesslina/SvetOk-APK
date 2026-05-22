package ru.svetok.app.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.svetok.app.data.admin.AdminActiveOutage
import ru.svetok.app.data.admin.AdminCreateResult
import ru.svetok.app.data.admin.AdminLoginResult
import ru.svetok.app.data.admin.AdminObjects
import ru.svetok.app.data.admin.AdminSessionPrefs
import ru.svetok.app.data.admin.HttpAdminRepository
import ru.svetok.app.data.admin.OutageType
import ru.svetok.app.data.admin.ScopeType
import ru.svetok.app.data.admin.TapResult

data class AdminUiState(
    val isLoggedIn: Boolean = false,
    val adminLogin: String = "",
    val isAdminAccessUnlocked: Boolean = false,
    // login dialog
    val isLoggingIn: Boolean = false,
    val loginError: String? = null,
    // outage list
    val outages: List<AdminActiveOutage> = emptyList(),
    val isLoadingOutages: Boolean = false,
    // objects (for create form)
    val objects: AdminObjects? = null,
    val isLoadingObjects: Boolean = false,
    // create form
    val isCreating: Boolean = false,
    val createError: String? = null,
    val createSuccess: Boolean = false,
)

class AdminViewModel(
    private val adminRepo: HttpAdminRepository,
    private val sessionPrefs: AdminSessionPrefs,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AdminUiState(
            isLoggedIn = sessionPrefs.isLoggedIn,
            adminLogin = sessionPrefs.getLogin().orEmpty(),
            isAdminAccessUnlocked = sessionPrefs.isAdminAccessUnlocked,
        )
    )
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    fun login(login: String, password: String) {
        if (_uiState.value.isLoggingIn) return
        _uiState.update { it.copy(isLoggingIn = true, loginError = null) }
        viewModelScope.launch {
            when (val result = adminRepo.login(login, password)) {
                is AdminLoginResult.Success -> _uiState.update {
                    it.copy(isLoggingIn = false, isLoggedIn = true, adminLogin = result.adminLogin, loginError = null)
                }
                is AdminLoginResult.InvalidCredentials -> _uiState.update {
                    it.copy(isLoggingIn = false, loginError = "Неверный логин или пароль.")
                }
                is AdminLoginResult.Error -> _uiState.update {
                    it.copy(isLoggingIn = false, loginError = result.message)
                }
            }
        }
    }

    fun logout() {
        sessionPrefs.clearSession()
        _uiState.update { it.copy(isLoggedIn = false, adminLogin = "", outages = emptyList()) }
    }

    fun clearLoginError() {
        _uiState.update { it.copy(loginError = null) }
    }

    /** Returns how many taps remain, or null when just unlocked. */
    fun onTitleTapped(): TapResult {
        val result = sessionPrefs.onTitleTapped()
        if (result is TapResult.Unlocked) {
            _uiState.update { it.copy(isAdminAccessUnlocked = true) }
        }
        return result
    }

    fun loadOutages() {
        _uiState.update { it.copy(isLoadingOutages = true) }
        viewModelScope.launch {
            val outages = adminRepo.getActiveOutages()
            _uiState.update { it.copy(isLoadingOutages = false, outages = outages) }
        }
    }

    fun endOutage(outageId: Int) {
        viewModelScope.launch {
            if (adminRepo.endOutage(outageId)) loadOutages()
        }
    }

    fun loadObjects() {
        if (_uiState.value.objects != null || _uiState.value.isLoadingObjects) return
        _uiState.update { it.copy(isLoadingObjects = true) }
        viewModelScope.launch {
            val objects = adminRepo.getObjects()
            _uiState.update { it.copy(isLoadingObjects = false, objects = objects) }
        }
    }

    fun createOutage(
        outageType: OutageType,
        scope: ScopeType,
        scopeRefId: Int,
        reason: String,
        startsNow: Boolean,
        startsAtText: String,
        endsNever: Boolean,
        endsAtText: String,
    ) {
        if (_uiState.value.isCreating) return
        _uiState.update { it.copy(isCreating = true, createError = null, createSuccess = false) }
        viewModelScope.launch {
            val startsAt = if (startsNow) null else startsAtText.trim().takeUnless { it.isEmpty() }
            val endsAt = if (endsNever) null else endsAtText.trim().takeUnless { it.isEmpty() }
            when (val result = adminRepo.createOutage(outageType, scope, scopeRefId, reason, startsAt, endsAt)) {
                is AdminCreateResult.Success -> _uiState.update {
                    it.copy(isCreating = false, createSuccess = true, createError = null)
                }
                is AdminCreateResult.Unauthorized -> {
                    logout()
                    _uiState.update { it.copy(isCreating = false, createError = "Сессия истекла. Войдите снова.") }
                }
                is AdminCreateResult.Error -> _uiState.update {
                    it.copy(isCreating = false, createError = result.message)
                }
            }
        }
    }

    fun consumeCreateSuccess() { _uiState.update { it.copy(createSuccess = false) } }
    fun consumeCreateError() { _uiState.update { it.copy(createError = null) } }
}
