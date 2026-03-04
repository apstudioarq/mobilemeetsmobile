package com.mobilemeetsmobile.domain.usecase

import com.mobilemeetsmobile.data.model.Session
import com.mobilemeetsmobile.data.model.Speaker
import com.mobilemeetsmobile.data.model.Track
import com.mobilemeetsmobile.data.repository.SessionRepository
import com.mobilemeetsmobile.data.repository.SpeakerRepository
import kotlinx.coroutines.flow.Flow

class GetScheduleUseCase(private val repository: SessionRepository) {
    operator fun invoke(day: Int, track: Track? = null): Flow<List<Session>> {
        return repository.getSessionsByDay(day, track)
    }
}

class GetSessionDetailUseCase(private val repository: SessionRepository) {
    suspend operator fun invoke(sessionId: String): Session {
        return repository.getSessionById(sessionId)
    }
}

class SearchSessionsUseCase(private val repository: SessionRepository) {
    operator fun invoke(query: String): Flow<List<Session>> {
        return repository.searchSessions(query)
    }
}

class ToggleBookmarkUseCase(private val repository: SessionRepository) {
    operator fun invoke(sessionId: String) {
        repository.toggleBookmark(sessionId)
    }
}

class GetBookmarksUseCase(private val repository: SessionRepository) {
    operator fun invoke(): Flow<List<String>> {
        return repository.getBookmarkedSessionIds()
    }
}

class GetSpeakersUseCase(private val repository: SpeakerRepository) {
    operator fun invoke(): Flow<List<Speaker>> {
        return repository.getAllSpeakers()
    }
}
