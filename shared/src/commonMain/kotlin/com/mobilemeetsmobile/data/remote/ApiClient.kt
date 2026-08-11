package com.mobilemeetsmobile.data.remote

import com.mobilemeetsmobile.data.model.BookmarkRequest
import com.mobilemeetsmobile.data.model.BookmarkResponse
import com.mobilemeetsmobile.data.remote.auth.FirebaseIdTokenProvider
import com.mobilemeetsmobile.data.remote.dto.SessionDto
import com.mobilemeetsmobile.data.remote.dto.SessionsResponse
import com.mobilemeetsmobile.data.remote.dto.SpeakerDto
import com.mobilemeetsmobile.data.remote.dto.SpeakersResponse
import com.mobilemeetsmobile.data.remote.dto.FirebaseConferenceDto
import com.mobilemeetsmobile.data.remote.dto.FirebaseRatingDto
import com.mobilemeetsmobile.data.remote.dto.FirebaseRatingSubmissionDto
import com.mobilemeetsmobile.data.remote.dto.toSessionDtos
import com.mobilemeetsmobile.data.remote.dto.toSpeakerDtos
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.datetime.Clock

class MobileMeetsMobileApi(
    private val client: HttpClient,
    private val firebaseIdTokenProvider: FirebaseIdTokenProvider,
) {

    private val baseUrl: String
        get() = BackendConfig.baseUrl
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // ── Sessions ────────────────────────────────────────────

    suspend fun getSessions(
        day: Int? = null,
        track: String? = null,
    ): List<SessionDto> {
        if (BackendConfig.isFirebaseRealtimeDatabase) {
            return getFirebaseSessions()
                .filter { session -> day == null || session.day == day }
                .filter { session -> track == null || session.track == track }
        }

        val payload = client.get("$baseUrl/sessions") {
            if (BackendConfig.isSupabase) {
                day?.let { parameter("day", "eq.$it") }
                track?.let { parameter("track", "eq.$it") }
                parameter("order", "start_time.asc")
            } else {
                day?.let { parameter("day", it) }
                track?.let { parameter("track", it) }
            }
        }.bodyAsText()
        return decodeSessionList(payload)
    }

    suspend fun getSessionById(id: String): SessionDto {
        if (BackendConfig.isFirebaseRealtimeDatabase) {
            return getFirebaseSessions().firstOrNull { it.id == id }
                ?: error("Session not found: $id")
        }

        val payload = if (BackendConfig.isSupabase) {
            client.get("$baseUrl/sessions") {
                parameter("id", "eq.$id")
                parameter("limit", 1)
            }.bodyAsText()
        } else {
            client.get("$baseUrl/sessions/$id").bodyAsText()
        }
        return decodeSingleSession(payload, id)
    }

    suspend fun searchSessions(query: String): List<SessionDto> {
        if (BackendConfig.isFirebaseRealtimeDatabase) {
            return getFirebaseSessions().filter { session ->
                session.title.contains(query, ignoreCase = true) ||
                    session.description.contains(query, ignoreCase = true)
            }
        }

        val payload = if (BackendConfig.isSupabase) {
            client.get("$baseUrl/sessions") {
                parameter("or", "title.ilike.*$query*,description.ilike.*$query*")
                parameter("order", "start_time.asc")
            }.bodyAsText()
        } else {
            client.get("$baseUrl/sessions/search") {
                parameter("q", query)
            }.bodyAsText()
        }
        return decodeSessionList(payload)
    }

    // ── Speakers ────────────────────────────────────────────

    suspend fun getSpeakers(): List<SpeakerDto> {
        if (BackendConfig.isFirebaseRealtimeDatabase) {
            return getFirebaseConferences().toSpeakerDtos(BackendConfig.selectedConferenceId)
        }

        val payload = client.get("$baseUrl/speakers") {
            if (BackendConfig.isSupabase) {
                parameter("order", "name.asc")
            }
        }.bodyAsText()
        return decodeSpeakerList(payload)
    }

    suspend fun getSpeakerById(id: String): SpeakerDto {
        if (BackendConfig.isFirebaseRealtimeDatabase) {
            return getSpeakers().firstOrNull { it.id == id }
                ?: error("Speaker not found: $id")
        }

        val payload = if (BackendConfig.isSupabase) {
            client.get("$baseUrl/speakers") {
                parameter("id", "eq.$id")
                parameter("limit", 1)
            }.bodyAsText()
        } else {
            client.get("$baseUrl/speakers/$id").bodyAsText()
        }
        return decodeSingleSpeaker(payload, id)
    }

    // ── Bookmarks ───────────────────────────────────────────

    suspend fun getBookmarks(userId: String): List<String> {
        return client.get("$baseUrl/users/$userId/bookmarks").body()
    }

    suspend fun toggleBookmark(
        sessionId: String,
        userId: String,
    ): BookmarkResponse {
        return client.post("$baseUrl/users/$userId/bookmarks") {
            contentType(ContentType.Application.Json)
            setBody(BookmarkRequest(sessionId, userId))
        }.body()
    }

    suspend fun removeBookmark(
        sessionId: String,
        userId: String,
    ) {
        client.delete("$baseUrl/users/$userId/bookmarks/$sessionId")
    }

    suspend fun getRatings(): List<FirebaseRatingDto> {
        val payload = firebaseGet("ratings")
        return json.decodeFromString<Map<String, FirebaseRatingDto>>(payload)
            .values
            .toList()
    }

    suspend fun submitFeedback(
        sessionId: String,
        sessionTitle: String,
        rating: Int,
        comment: String,
    ) {
        require(BackendConfig.isFirebaseRealtimeDatabase) {
            "Feedback submission requires Firebase Realtime Database."
        }
        require(rating in 1..5) { "Rating must be between 1 and 5." }

        val conference = getFirebaseConferences().values.firstOrNull { candidate ->
            candidate.rooms.any { room ->
                room.presentations.any { presentation -> presentation.id == sessionId }
            }
        }
        firebasePost(
            path = "ratings",
            body = FirebaseRatingSubmissionDto(
                audience = conference?.audience.orEmpty(),
                country = conference?.organizingCountry.orEmpty(),
                date = Clock.System.now().toString(),
                event = conference?.eventType.orEmpty(),
                name = conference?.title.orEmpty(),
                rating = rating,
                sessionId = sessionId,
                sessionTitle = sessionTitle,
                comment = comment.trim(),
            ),
        )
    }

    private suspend fun getFirebaseSessions(): List<SessionDto> {
        return getFirebaseConferences().toSessionDtos(BackendConfig.selectedConferenceId)
    }

    private suspend fun getFirebaseConferences(): Map<String, FirebaseConferenceDto> {
        val payload = firebaseGet("test/conferences")
        return json.decodeFromString<Map<String, FirebaseConferenceDto>>(payload)
            .mapValues { (key, conference) ->
                conference.copy(id = conference.id.ifBlank { key })
            }
    }

    private fun decodeSessionList(payload: String): List<SessionDto> {
        return runCatching {
            json.decodeFromString<SessionsResponse>(payload).sessions
        }.recoverCatching {
            json.decodeFromString<List<SessionDto>>(payload)
        }.getOrThrow()
    }

    private fun decodeSpeakerList(payload: String): List<SpeakerDto> {
        return runCatching {
            json.decodeFromString<SpeakersResponse>(payload).speakers
        }.recoverCatching {
            json.decodeFromString<List<SpeakerDto>>(payload)
        }.getOrThrow()
    }

    private fun decodeSingleSession(payload: String, id: String): SessionDto {
        return runCatching {
            json.decodeFromString<SessionDto>(payload)
        }.recoverCatching {
            decodeSessionList(payload).firstOrNull()
                ?: error("Session not found: $id")
        }.getOrThrow()
    }

    private fun decodeSingleSpeaker(payload: String, id: String): SpeakerDto {
        return runCatching {
            json.decodeFromString<SpeakerDto>(payload)
        }.recoverCatching {
            decodeSpeakerList(payload).firstOrNull()
                ?: error("Speaker not found: $id")
        }.getOrThrow()
    }

    private fun firebaseJsonUrl(path: String): String {
        return "$baseUrl/${path.trim('/')}.json"
    }

    private suspend fun firebaseGet(path: String): String {
        val firstResponse = authenticatedFirebaseGet(
            path = path,
            forceTokenRefresh = false,
        )
        if (firstResponse.status != HttpStatusCode.Unauthorized) {
            return firstResponse.firebaseBodyOrThrow(path)
        }

        val retryResponse = authenticatedFirebaseGet(
            path = path,
            forceTokenRefresh = true,
        )
        return retryResponse.firebaseBodyOrThrow(path)
    }

    private suspend fun firebasePost(path: String, body: FirebaseRatingSubmissionDto) {
        val firstResponse = authenticatedFirebasePost(
            path = path,
            body = body,
            forceTokenRefresh = false,
        )
        if (firstResponse.status != HttpStatusCode.Unauthorized) {
            firstResponse.firebaseBodyOrThrow(path, operation = "write")
            return
        }

        authenticatedFirebasePost(
            path = path,
            body = body,
            forceTokenRefresh = true,
        ).firebaseBodyOrThrow(path, operation = "write")
    }

    private suspend fun authenticatedFirebaseGet(
        path: String,
        forceTokenRefresh: Boolean,
    ): HttpResponse {
        val idToken = firebaseIdTokenProvider.getIdToken(forceTokenRefresh)
        return client.get(firebaseJsonUrl(path)) {
            parameter("auth", idToken)
        }
    }

    private suspend fun authenticatedFirebasePost(
        path: String,
        body: FirebaseRatingSubmissionDto,
        forceTokenRefresh: Boolean,
    ): HttpResponse {
        val idToken = firebaseIdTokenProvider.getIdToken(forceTokenRefresh)
        return client.post(firebaseJsonUrl(path)) {
            parameter("auth", idToken)
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    private suspend fun HttpResponse.firebaseBodyOrThrow(
        path: String,
        operation: String = "read",
    ): String {
        val payload = bodyAsText()
        if (!status.isSuccess()) {
            val firebaseMessage = runCatching {
                json.decodeFromString<FirebaseDatabaseError>(payload).error
            }.getOrNull()
            throw FirebaseRealtimeDatabaseException(
                "Firebase $operation failed for /$path (${status.value}): " +
                    (firebaseMessage ?: status.description),
            )
        }
        return payload
    }
}

@kotlinx.serialization.Serializable
private data class FirebaseDatabaseError(
    val error: String,
)

class FirebaseRealtimeDatabaseException(message: String) : IllegalStateException(message)
