package com.mobilemeetsmobile.data.remote

object BackendConfig {
    private const val LEGACY_BASE_URL = "https://api.mobilemeetsmobile-clone.dev/v1"

    private var supabaseProjectUrl: String = ""

    private var supabaseAnonKey: String = ""

    fun configureSupabase(
        projectUrl: String,
        anonKey: String,
    ) {
        supabaseProjectUrl = normalizeProjectUrl(projectUrl)
        supabaseAnonKey = anonKey.trim()
    }

    val baseUrl: String
        get() = if (supabaseProjectUrl.isBlank()) LEGACY_BASE_URL else "$supabaseProjectUrl/rest/v1"

    val isSupabase: Boolean
        get() = supabaseProjectUrl.isNotBlank()

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
