package com.ingevent.data.remote.auth

import com.ingevent.data.remote.BackendConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FirebaseAuthClientTest {
    @Test
    fun createsAndPersistsAnonymousSessionWhenThereIsNoSession() = runBlocking {
        configureFirebase()
        val store = InMemorySessionStore()
        val engine = MockEngine { request ->
            assertEquals("identitytoolkit.googleapis.com", request.url.host)
            assertEquals("test-api-key", request.url.parameters["key"])
            respond(
                content = """
                    {
                      "idToken": "new-id-token",
                      "refreshToken": "new-refresh-token",
                      "expiresIn": "3600"
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = jsonHeaders,
            )
        }

        val token = FirebaseAuthClient(
            client = HttpClient(engine),
            sessionStore = store,
            nowEpochSeconds = { 1_000L },
        ).getIdToken()

        assertEquals("new-id-token", token)
        assertEquals("new-refresh-token", store.storedSession?.refreshToken)
        assertEquals(4_600L, store.storedSession?.expiresAtEpochSeconds)
    }

    @Test
    fun reusesValidSessionWithoutNetworkRequest() = runBlocking {
        configureFirebase()
        val store = InMemorySessionStore(
            FirebaseAuthSession(
                idToken = "cached-id-token",
                refreshToken = "cached-refresh-token",
                expiresAtEpochSeconds = 2_000L,
            ),
        )
        val engine = MockEngine { error("No network request was expected") }

        val token = FirebaseAuthClient(
            client = HttpClient(engine),
            sessionStore = store,
            nowEpochSeconds = { 1_000L },
        ).getIdToken()

        assertEquals("cached-id-token", token)
    }

    @Test
    fun refreshesExpiredSession() = runBlocking {
        configureFirebase()
        val store = InMemorySessionStore(
            FirebaseAuthSession(
                idToken = "expired-id-token",
                refreshToken = "existing-refresh-token",
                expiresAtEpochSeconds = 1_000L,
            ),
        )
        val engine = MockEngine { request ->
            assertEquals("securetoken.googleapis.com", request.url.host)
            respond(
                content = """
                    {
                      "id_token": "refreshed-id-token",
                      "refresh_token": "rotated-refresh-token",
                      "expires_in": "3600"
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = jsonHeaders,
            )
        }

        val token = FirebaseAuthClient(
            client = HttpClient(engine),
            sessionStore = store,
            nowEpochSeconds = { 1_000L },
        ).getIdToken()

        assertEquals("refreshed-id-token", token)
        assertNotNull(store.storedSession)
        assertEquals("rotated-refresh-token", store.storedSession?.refreshToken)
    }

    private fun configureFirebase() {
        BackendConfig.configureFirebaseRealtimeDatabase(apiKey = "test-api-key")
    }

    private class InMemorySessionStore(
        var storedSession: FirebaseAuthSession? = null,
    ) : FirebaseAuthSessionStore {
        override fun getSession(): FirebaseAuthSession? = storedSession

        override fun saveSession(session: FirebaseAuthSession) {
            storedSession = session
        }

        override fun clearSession() {
            storedSession = null
        }
    }

    private companion object {
        val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    }
}
