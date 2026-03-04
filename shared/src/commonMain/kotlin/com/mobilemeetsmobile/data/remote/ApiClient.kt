package com.mobilemeetsmobile.data.remote

import com.mobilemeetsmobile.data.model.BookmarkRequest
import com.mobilemeetsmobile.data.model.BookmarkResponse
import com.mobilemeetsmobile.data.remote.dto.SessionDto
import com.mobilemeetsmobile.data.remote.dto.SessionsResponse
import com.mobilemeetsmobile.data.remote.dto.SpeakerDto
import com.mobilemeetsmobile.data.remote.dto.SpeakersResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class MobileMeetsMobileApi(private val client: HttpClient) {

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
        val payload = client.get("$baseUrl/speakers") {
            if (BackendConfig.isSupabase) {
                parameter("order", "name.asc")
            }
        }.bodyAsText()
        return decodeSpeakerList(payload)
    }

    suspend fun getSpeakerById(id: String): SpeakerDto {
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
}
