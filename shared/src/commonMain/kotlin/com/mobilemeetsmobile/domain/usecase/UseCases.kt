package com.mobilemeetsmobile.domain.usecase

import com.mobilemeetsmobile.data.model.Session
import com.mobilemeetsmobile.data.model.Speaker
import com.mobilemeetsmobile.data.model.HomeContent
import com.mobilemeetsmobile.data.model.MapContent
import com.mobilemeetsmobile.data.model.Track
import com.mobilemeetsmobile.data.repository.HomeContentRepository
import com.mobilemeetsmobile.data.repository.MapContentRepository
import com.mobilemeetsmobile.data.repository.SessionRepository
import com.mobilemeetsmobile.data.repository.SpeakerRepository
import com.mobilemeetsmobile.data.repository.RatingRepository
import kotlinx.coroutines.flow.Flow

class GetScheduleUseCase(private val repository: SessionRepository) {
    operator fun invoke(day: Int, track: Track? = null): Flow<List<Session>> {
        return repository.getSessionsByDay(day, track)
    }
}

class GetAllSessionsUseCase(private val repository: SessionRepository) {
    operator fun invoke(): Flow<List<Session>> {
        return repository.getAllSessions()
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

class GetHomeContentUseCase(private val repository: HomeContentRepository) {
    operator fun invoke(): Flow<HomeContent> {
        return repository.getHomeContent()
    }
}

class GetMapContentUseCase(private val repository: MapContentRepository) {
    operator fun invoke(): Flow<MapContent> {
        return repository.getMapContent()
    }
}

class GetSpeakersUseCase(private val repository: SpeakerRepository) {
    operator fun invoke(): Flow<List<Speaker>> {
        return repository.getAllSpeakers()
    }
}

class SubmitFeedbackUseCase(private val repository: RatingRepository) {
    suspend operator fun invoke(
        sessionId: String,
        sessionTitle: String,
        rating: Int,
        comment: String,
    ) {
        repository.submitFeedback(sessionId, sessionTitle, rating, comment)
    }
}
