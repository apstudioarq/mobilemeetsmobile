package com.mobilemeetsmobile.data.remote

object BackendConfig {
    private const val LEGACY_BASE_URL = "https://api.mobilemeetsmobile-clone.dev/v1"

    private var supabaseProjectUrl: String = ""

    private var supabaseAnonKey: String = ""

    private var firebaseFunctionsUrl: String = ""

    fun configureSupabase(
        projectUrl: String,
        anonKey: String,
    ) {
        supabaseProjectUrl = normalizeProjectUrl(projectUrl)
        supabaseAnonKey = anonKey.trim()
        firebaseFunctionsUrl = ""
    }

    fun configureFirebaseFunctions(functionsBaseUrl: String) {
        firebaseFunctionsUrl = functionsBaseUrl.trim().trimEnd('/')
        supabaseProjectUrl = ""
        supabaseAnonKey = ""
    }

    val baseUrl: String
        get() = when {
            firebaseFunctionsUrl.isNotBlank() -> firebaseFunctionsUrl
            supabaseProjectUrl.isNotBlank() -> "$supabaseProjectUrl/rest/v1"
            else -> LEGACY_BASE_URL
        }

    val isSupabase: Boolean
        get() = supabaseProjectUrl.isNotBlank()

    val isFirebaseFunctions: Boolean
        get() = firebaseFunctionsUrl.isNotBlank()

    val anonKeyOrNull: String?
        get() = supabaseAnonKey.takeIf { it.isNotBlank() }

    private fun normalizeProjectUrl(url: String): String {
        var normalized = url.trim().trimEnd('/')
        if (normalized.endsWith("/rest/v1")) {
            normalized = normalized.removeSuffix("/rest/v1")
        }
        return normalized
    }
}
