package ru.svetok.app.data.outage

import ru.svetok.app.BuildConfig

data class ApiConfig(
    val baseUrl: String,
    val clientToken: String,
) {
    val isConfigured: Boolean
        get() = baseUrl.isNotBlank() && clientToken.isNotBlank()

    companion object {
        const val CLIENT_TOKEN_HEADER = "X-Svetok-Api-Key"
    }
}

fun loadApiConfig(): ApiConfig = ApiConfig(
    baseUrl = BuildConfig.API_BASE_URL.trim().removeSuffix("/"),
    clientToken = assembleToken(),
)

// Токен собирается из двух частей в разных местах кода.
// С R8 эта функция и имена полей обфусцируются.
private fun assembleToken(): String {
    val a = BuildConfig.SVC_KEY_PRIMARY.trim()
    val b = BuildConfig.SVC_KEY_SUFFIX.trim()
    return a + b
}
