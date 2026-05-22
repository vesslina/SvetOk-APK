package ru.svetok.app.data.admin

data class AdminObject(val id: Int, val name: String)

data class AdminObjects(
    val tps: List<AdminObject>,
    val feeders: List<AdminObject>,
    val substations: List<AdminObject>,
)

enum class ScopeType(val apiValue: String, val label: String) {
    TP("tp", "ТП"),
    FEEDER("feeder", "Фидер"),
    SUBSTATION("substation", "ПС"),
}

enum class OutageType(val apiValue: String, val label: String) {
    PLANNED("planned", "Плановое"),
    EMERGENCY("emergency", "Аварийное"),
}

data class AdminActiveOutage(
    val id: Int,
    val outageType: String,
    val scope: String,
    val scopeName: String,
    val status: String,
    val startsAt: String,
    val endsAt: String?,
    val reason: String?,
    val streets: List<String>,
)

sealed class AdminLoginResult {
    data class Success(val adminLogin: String) : AdminLoginResult()
    data object InvalidCredentials : AdminLoginResult()
    data class Error(val message: String) : AdminLoginResult()
}

sealed class AdminCreateResult {
    data object Success : AdminCreateResult()
    data object Unauthorized : AdminCreateResult()
    data class Error(val message: String) : AdminCreateResult()
}
