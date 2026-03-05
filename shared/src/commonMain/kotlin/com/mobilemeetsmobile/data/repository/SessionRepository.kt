package com.mobilemeetsmobile.data.repository

import com.mobilemeetsmobile.data.local.LocalDataSource
import com.mobilemeetsmobile.data.model.Session
import com.mobilemeetsmobile.data.model.Track
import com.mobilemeetsmobile.data.remote.MobileMeetsMobileApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart

class SessionRepository(
    private val api: MobileMeetsMobileApi,
    private val local: LocalDataSource,
) {
    fun getAllSessions(): Flow<List<Session>> {
        return local.getAllSessions().onStart {
            try {
                val remoteSessions = api.getSessions()
                local.insertSessions(remoteSessions)
            } catch (e: Exception) {
                println("Network error fetching all sessions: ${e.message}")
            }
        }
    }

    /**
     * Offline-first schedule loading.
     * 1. Emit cached sessions immediately
     * 2. Fetch from network in background
     * 3. Cache network result and emit updated data
     */
    fun getSessionsByDay(day: Int, track: Track? = null): Flow<List<Session>> {
        return flow {
            // Emit from cache first
            val cachedFlow = if (track != null) {
                local.getSessionsByDayAndTrack(day, track)
            } else {
                local.getSessionsByDay(day)
            }

            // Collect cached immediately
            cachedFlow.collect { cached ->
                emit(cached)
            }
        }.onStart {
            // Trigger network fetch in the background
            try {
                val remoteSessions = api.getSessions(day, track?.name)
                local.insertSessions(remoteSessions)
            } catch (e: Exception) {
                // Network failed, rely on cache
                println("Network error: ${e.message}")
            }
        }
    }

    fun searchSessions(query: String): Flow<List<Session>> {
        return local.searchSessions(query)
    }

    suspend fun getSessionById(id: String): Session {
        return try {
            val remote = api.getSessionById(id)
            local.insertSessions(listOf(remote))
            remote.toDomain(local.isBookmarked(id))
        } catch (e: Exception) {
            // Fallback: query from local
            throw e
        }
    }

    fun toggleBookmark(sessionId: String) {
        local.toggleBookmark(sessionId)
    }

    fun getBookmarkedSessionIds(): Flow<List<String>> {
        return local.getAllBookmarkIds()
    }
}
