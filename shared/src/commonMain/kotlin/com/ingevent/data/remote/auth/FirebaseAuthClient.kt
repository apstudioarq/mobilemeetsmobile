package com.ingevent.data.remote.auth

import com.ingevent.data.remote.BackendConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class FirebaseAuthSession(
    val idToken: String,
    val refreshToken: String,
    val expiresAtEpochSeconds: Long,
)

interface FirebaseAuthSessionStore {
    fun getSession(): FirebaseAuthSession?
    fun saveSession(session: FirebaseAuthSession)
    fun clearSession()
}

interface FirebaseIdTokenProvider {
    suspend fun getIdToken(forceRefresh: Boolean = false): String
}

class FirebaseAuthClient(
    private val client: HttpClient,
    private val sessionStore: FirebaseAuthSessionStore,
    private val nowEpochSeconds: () -> Long = { Clock.System.now().epochSeconds },
) : FirebaseIdTokenProvider {
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getIdToken(forceRefresh: Boolean): String = mutex.withLock {
        val apiKey = BackendConfig.firebaseApiKeyOrNull
            ?: throw FirebaseAuthenticationException(
                "Firebase Authentication is not configured. Set FIREBASE_API_KEY for this app.",
            )
        val currentSession = sessionStore.getSession()

        if (!forceRefresh && currentSession?.isUsable(nowEpochSeconds()) == true) {
            return@withLock currentSession.idToken
        }

        if (currentSession != null) {
            try {
                return@withLock refreshSession(apiKey, currentSession.refreshToken).also {
                    sessionStore.saveSession(it)
                }.idToken
            } catch (error: FirebaseAuthenticationException) {
                if (!error.requiresNewAnonymousSession) throw error
                sessionStore.clearSession()
            }
        }

        createAnonymousSession(apiKey).also(sessionStore::saveSession).idToken
    }

    private suspend fun createAnonymousSession(apiKey: String): FirebaseAuthSession {
        val response = client.post(
            "https://identitytoolkit.googleapis.com/v1/accounts:signUp",
        ) {
            parameter("key", apiKey)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(AnonymousSignInRequest()))
        }
        val body = response.decodeOrThrow<AnonymousSignInResponse>("anonymous sign-in")
        return FirebaseAuthSession(
            idToken = body.idToken,
            refreshToken = body.refreshToken,
            expiresAtEpochSeconds = expiresAt(body.expiresIn),
        )
    }

    private suspend fun refreshSession(apiKey: String, refreshToken: String): FirebaseAuthSession {
        val response = client.post("https://securetoken.googleapis.com/v1/token") {
            parameter("key", apiKey)
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("grant_type", "refresh_token")
                        append("refresh_token", refreshToken)
                    },
                ),
            )
        }
        val body = response.decodeOrThrow<RefreshTokenResponse>("token refresh")
        return FirebaseAuthSession(
            idToken = body.idToken,
            refreshToken = body.refreshToken,
            expiresAtEpochSeconds = expiresAt(body.expiresIn),
        )
    }

    private fun expiresAt(expiresIn: String): Long {
        return nowEpochSeconds() + (expiresIn.toLongOrNull() ?: DEFAULT_TOKEN_LIFETIME_SECONDS)
    }

    private suspend inline fun <reified T> HttpResponse.decodeOrThrow(operation: String): T {
        val payload = bodyAsText()
        if (!status.isSuccess()) {
            val code = runCatching {
                json.decodeFromString<FirebaseAuthErrorEnvelope>(payload).error.message
            }.getOrNull()
            throw FirebaseAuthenticationException(
                message = "Firebase $operation failed (${status.value}): ${code ?: status.description}",
                status = status,
                code = code,
            )
        }
        return json.decodeFromString(payload)
    }

    private fun FirebaseAuthSession.isUsable(now: Long): Boolean {
        return idToken.isNotBlank() && expiresAtEpochSeconds - TOKEN_EXPIRY_MARGIN_SECONDS > now
    }

    private companion object {
        const val DEFAULT_TOKEN_LIFETIME_SECONDS = 3_600L
        const val TOKEN_EXPIRY_MARGIN_SECONDS = 60L
    }
}

class FirebaseAuthenticationException(
    message: String,
    val status: HttpStatusCode? = null,
    val code: String? = null,
) : IllegalStateException(message) {
    val requiresNewAnonymousSession: Boolean
        get() = code in setOf(
            "INVALID_REFRESH_TOKEN",
            "TOKEN_EXPIRED",
            "USER_DISABLED",
            "USER_NOT_FOUND",
        )
}

@Serializable
private data class AnonymousSignInRequest(
    val returnSecureToken: Boolean = true,
)

@Serializable
private data class AnonymousSignInResponse(
    val idToken: String,
    val refreshToken: String,
    val expiresIn: String,
)

@Serializable
private data class RefreshTokenResponse(
    @SerialName("id_token") val idToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: String,
)

@Serializable
private data class FirebaseAuthErrorEnvelope(
    val error: FirebaseAuthError,
)

@Serializable
private data class FirebaseAuthError(
    val message: String,
)
