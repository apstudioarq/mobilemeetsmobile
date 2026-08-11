package com.mobilemeetsmobile.data.remote

object BackendConfig {
    private const val LEGACY_BASE_URL = "https://api.mobilemeetsmobile-clone.dev/v1"
    private const val DEFAULT_FIREBASE_REALTIME_DATABASE_URL =
        "https://ingtechrating-default-rtdb.europe-west1.firebasedatabase.app"

    private var supabaseProjectUrl: String = ""

    private var supabaseAnonKey: String = ""

    private var firebaseFunctionsUrl: String = ""

    private var firebaseRealtimeDatabaseUrl: String = DEFAULT_FIREBASE_REALTIME_DATABASE_URL

    private var firebaseConferenceId: String = ""

    private var firebaseApiKey: String = ""

    fun configureSupabase(
        projectUrl: String,
        anonKey: String,
    ) {
        supabaseProjectUrl = normalizeProjectUrl(projectUrl)
        supabaseAnonKey = anonKey.trim()
        firebaseFunctionsUrl = ""
        firebaseRealtimeDatabaseUrl = ""
        firebaseConferenceId = ""
        firebaseApiKey = ""
    }

    fun configureFirebaseFunctions(functionsBaseUrl: String) {
        firebaseFunctionsUrl = functionsBaseUrl.trim().trimEnd('/')
        supabaseProjectUrl = ""
        supabaseAnonKey = ""
        firebaseRealtimeDatabaseUrl = ""
        firebaseConferenceId = ""
        firebaseApiKey = ""
    }

    fun configureFirebaseRealtimeDatabase(
        databaseUrl: String = DEFAULT_FIREBASE_REALTIME_DATABASE_URL,
        conferenceId: String = "",
        apiKey: String = "",
    ) {
        firebaseRealtimeDatabaseUrl = normalizeRealtimeDatabaseUrl(databaseUrl)
        firebaseConferenceId = conferenceId.trim()
        firebaseApiKey = apiKey.trim()
        firebaseFunctionsUrl = ""
        supabaseProjectUrl = ""
        supabaseAnonKey = ""
    }

    val baseUrl: String
        get() = when {
            firebaseRealtimeDatabaseUrl.isNotBlank() -> firebaseRealtimeDatabaseUrl
            firebaseFunctionsUrl.isNotBlank() -> firebaseFunctionsUrl
            supabaseProjectUrl.isNotBlank() -> "$supabaseProjectUrl/rest/v1"
            else -> LEGACY_BASE_URL
        }

    val isSupabase: Boolean
        get() = supabaseProjectUrl.isNotBlank()

    val isFirebaseFunctions: Boolean
        get() = firebaseFunctionsUrl.isNotBlank()

    val isFirebaseRealtimeDatabase: Boolean
        get() = firebaseRealtimeDatabaseUrl.isNotBlank()

    val selectedConferenceId: String?
        get() = firebaseConferenceId.takeIf { it.isNotBlank() }

    val firebaseApiKeyOrNull: String?
        get() = firebaseApiKey.takeIf { it.isNotBlank() }

    val anonKeyOrNull: String?
        get() = supabaseAnonKey.takeIf { it.isNotBlank() }

    private fun normalizeProjectUrl(url: String): String {
        var normalized = url.trim().trimEnd('/')
        if (normalized.endsWith("/rest/v1")) {
            normalized = normalized.removeSuffix("/rest/v1")
        }
        return normalized
    }

    private fun normalizeRealtimeDatabaseUrl(url: String): String {
        return url.trim().trimEnd('/').removeSuffix(".json")
    }
}
