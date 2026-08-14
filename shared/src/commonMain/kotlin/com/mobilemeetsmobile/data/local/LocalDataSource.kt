package com.mobilemeetsmobile.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.mobilemeetsmobile.data.model.*
import com.mobilemeetsmobile.data.seed.DesignSeedData
import com.mobilemeetsmobile.data.remote.dto.SessionDto
import com.mobilemeetsmobile.data.remote.dto.SpeakerDto
import com.mobilemeetsmobile.data.remote.auth.FirebaseAuthSession
import com.mobilemeetsmobile.data.remote.auth.FirebaseAuthSessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LocalDataSource(driverFactory: DatabaseDriverFactory) : FirebaseAuthSessionStore {

    private val database = MobileMeetsMobileDatabase(driverFactory.createDriver())
    private val sessionQueries = database.mobileMeetsMobileDatabaseQueries
    private val json = Json { ignoreUnknownKeys = true }

    // ── Sessions ────────────────────────────────────────────

    fun getAllSessions(): Flow<List<Session>> {
        return sessionQueries.getAllSessions(::mapSession)
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    fun getSessionsByDay(day: Int): Flow<List<Session>> {
        return sessionQueries.getSessionsByDay(day.toLong(), ::mapSession)
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    fun getSessionsByDayAndTrack(day: Int, track: Track): Flow<List<Session>> {
        return sessionQueries.getSessionsByDayAndTrack(day.toLong(), track.name, ::mapSession)
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    fun getSessionById(id: String): Session? {
        return sessionQueries.getSessionById(id, ::mapSession).executeAsOneOrNull()
    }

    fun hasCachedSessions(): Boolean {
        return sessionQueries.getAllSessions().executeAsList().isNotEmpty()
    }

    fun searchSessions(query: String): Flow<List<Session>> {
        return sessionQueries.searchSessions(query, query, ::mapSession)
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    fun insertSessions(sessions: List<SessionDto>) {
        val now = kotlinx.datetime.Clock.System.now().epochSeconds
        database.transaction {
            sessions.forEach { dto ->
                sessionQueries.insertSession(
                    id = dto.id,
                    title = dto.title,
                    description = dto.description,
                    startTime = dto.startTime,
                    endTime = dto.endTime,
                    duration = dto.duration,
                    room = dto.room,
                    day = dto.day.toLong(),
                    track = dto.track,
                    type = dto.type,
                    level = dto.level,
                    speakerIds = json.encodeToString(dto.speakerIds),
                    capacity = dto.capacity.toLong(),
                    registered = dto.registered.toLong(),
                    tags = json.encodeToString(dto.tags),
                    livestreamUrl = dto.livestreamUrl,
                    slidesUrl = dto.slidesUrl,
                    updatedAt = now,
                )
            }
        }
    }

    fun replaceSessions(sessions: List<SessionDto>) {
        val now = kotlinx.datetime.Clock.System.now().epochSeconds
        database.transaction {
            sessionQueries.deleteAllSessions()
            sessions.forEach { dto ->
                sessionQueries.insertSession(
                    id = dto.id,
                    title = dto.title,
                    description = dto.description,
                    startTime = dto.startTime,
                    endTime = dto.endTime,
                    duration = dto.duration,
                    room = dto.room,
                    day = dto.day.toLong(),
                    track = dto.track,
                    type = dto.type,
                    level = dto.level,
                    speakerIds = json.encodeToString(dto.speakerIds),
                    capacity = dto.capacity.toLong(),
                    registered = dto.registered.toLong(),
                    tags = json.encodeToString(dto.tags),
                    livestreamUrl = dto.livestreamUrl,
                    slidesUrl = dto.slidesUrl,
                    updatedAt = now,
                )
            }
        }
    }

    // ── Speakers ────────────────────────────────────────────

    fun getAllSpeakers(): Flow<List<Speaker>> {
        return sessionQueries.getAllSpeakers(::mapSpeaker)
            .asFlow()
            .mapToList(Dispatchers.IO)
    }

    fun insertSpeakers(speakers: List<SpeakerDto>) {
        database.transaction {
            speakers.forEach { dto ->
                sessionQueries.insertSpeaker(
                    id = dto.id,
                    name = dto.name,
                    role = dto.role,
                    company = dto.company,
                    bio = dto.bio,
                    photoUrl = dto.photoUrl,
                    photoBase64 = dto.photoBase64,
                    photoMimeType = dto.photoMimeType,
                    socialLinks = json.encodeToString(dto.socialLinks),
                )
            }
        }
    }

    fun replaceSpeakers(speakers: List<SpeakerDto>) {
        database.transaction {
            sessionQueries.deleteAllSpeakers()
            speakers.forEach { dto ->
                sessionQueries.insertSpeaker(
                    id = dto.id,
                    name = dto.name,
                    role = dto.role,
                    company = dto.company,
                    bio = dto.bio,
                    photoUrl = dto.photoUrl,
                    photoBase64 = dto.photoBase64,
                    photoMimeType = dto.photoMimeType,
                    socialLinks = json.encodeToString(dto.socialLinks),
                )
            }
        }
    }

    fun getSpeakerById(id: String): Speaker? {
        return sessionQueries.getSpeakerById(id, ::mapSpeaker).executeAsOneOrNull()
    }

    fun hasCachedSpeakers(): Boolean {
        return sessionQueries.getAllSpeakers().executeAsList().isNotEmpty()
    }

    fun seedDesignData() {
        insertSpeakers(DesignSeedData.speakers)
        insertSessions(DesignSeedData.sessions)
    }

    // ── Home content ────────────────────────────────────────

    fun getHomeContent(): Flow<HomeContent> {
        return sessionQueries.getHomeContent { welcomeMessage, heroImageUrl ->
            HomeContent(
                welcomeMessage = welcomeMessage,
                heroImageUrl = heroImageUrl,
            )
        }
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { it.firstOrNull() ?: HomeContent() }
    }

    fun saveHomeContent(content: HomeContent) {
        val now = kotlinx.datetime.Clock.System.now().epochSeconds
        sessionQueries.saveHomeContent(
            welcomeMessage = content.welcomeMessage,
            heroImageUrl = content.heroImageUrl,
            updatedAt = now,
        )
    }

    // ── Bookmarks ───────────────────────────────────────────

    fun getAllBookmarkIds(): Flow<List<String>> {
        return sessionQueries.getAllBookmarks()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { it.map { id -> id } }
    }

    fun isBookmarked(sessionId: String): Boolean {
        return sessionQueries.isBookmarked(sessionId).executeAsOne() > 0
    }

    fun toggleBookmark(sessionId: String) {
        if (isBookmarked(sessionId)) {
            sessionQueries.deleteBookmark(sessionId)
        } else {
            val now = kotlinx.datetime.Clock.System.now().epochSeconds
            sessionQueries.insertBookmark(sessionId, now)
        }
    }

    // ── Firebase authentication ─────────────────────────────

    override fun getSession(): FirebaseAuthSession? {
        return sessionQueries.getFirebaseAuthSession { idToken, refreshToken, expiresAt ->
            FirebaseAuthSession(
                idToken = idToken,
                refreshToken = refreshToken,
                expiresAtEpochSeconds = expiresAt,
            )
        }.executeAsOneOrNull()
    }

    override fun saveSession(session: FirebaseAuthSession) {
        sessionQueries.saveFirebaseAuthSession(
            idToken = session.idToken,
            refreshToken = session.refreshToken,
            expiresAt = session.expiresAtEpochSeconds,
        )
    }

    override fun clearSession() {
        sessionQueries.deleteFirebaseAuthSession()
    }

    // ── Mappers ─────────────────────────────────────────────

    private fun mapSession(
        id: String,
        title: String,
        description: String,
        startTime: String,
        endTime: String,
        duration: String,
        room: String,
        day: Long,
        track: String,
        type: String,
        level: String,
        speakerIds: String,
        capacity: Long,
        registered: Long,
        tags: String,
        livestreamUrl: String?,
        slidesUrl: String?,
        updatedAt: Long,
    ): Session {
        val speakerIdsList: List<String> = try {
            json.decodeFromString(speakerIds)
        } catch (_: Exception) { emptyList() }

        val tagsList: List<String> = try {
            json.decodeFromString(tags)
        } catch (_: Exception) { emptyList() }

        return Session(
            id = id,
            title = title,
            description = description,
            startTime = startTime,
            endTime = endTime,
            duration = duration,
            room = room,
            day = day.toInt(),
            track = Track.valueOf(track),
            type = SessionType.valueOf(type),
            level = Level.valueOf(level),
            speakerIds = speakerIdsList,
            capacity = capacity.toInt(),
            registered = registered.toInt(),
            tags = tagsList,
            livestreamUrl = livestreamUrl,
            slidesUrl = slidesUrl,
            isBookmarked = isBookmarked(id),
        )
    }

    private fun mapSpeaker(
        id: String,
        name: String,
        role: String,
        company: String,
        bio: String,
        photoUrl: String,
        photoBase64: String,
        photoMimeType: String,
        socialLinks: String,
    ): Speaker {
        val links: Map<String, String> = try {
            json.decodeFromString(socialLinks)
        } catch (_: Exception) { emptyMap() }

        return Speaker(
            id = id,
            name = name,
            role = role,
            company = company,
            bio = bio,
            photoUrl = photoUrl,
            photoBase64 = photoBase64,
            photoMimeType = photoMimeType,
            socialLinks = links,
        )
    }
}
