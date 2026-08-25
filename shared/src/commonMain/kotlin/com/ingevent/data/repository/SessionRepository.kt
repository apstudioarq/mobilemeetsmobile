package com.ingevent.data.repository

import com.ingevent.data.local.LocalDataSource
import com.ingevent.data.model.Session
import com.ingevent.data.model.Track
import com.ingevent.data.remote.BackendConfig
import com.ingevent.data.remote.INGEventApi
import com.ingevent.data.remote.redactedMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart

class SessionRepository(
    private val api: INGEventApi,
    private val local: LocalDataSource,
    private val connectionState: ConnectionStateRepository,
) {
    fun getAllSessions(): Flow<List<Session>> {
        return local.getAllSessions().onStart {
            if (!BackendConfig.isFirebaseRealtimeDatabase) {
                local.seedDesignData()
            }
            try {
                val remoteSessions = api.getSessions()
                if (BackendConfig.isFirebaseRealtimeDatabase) {
                    local.replaceSessions(remoteSessions)
                } else {
                    local.insertSessions(remoteSessions)
                }
                connectionState.reportOnline()
            } catch (e: Exception) {
                println("Network error fetching all sessions: ${e.redactedMessage("Unknown error")}")
                val hasCachedSessions = local.hasCachedSessions()
                connectionState.reportOffline(hasCachedSessions)
                if (BackendConfig.isFirebaseRealtimeDatabase && !hasCachedSessions) {
                    throw e
                }
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
            if (!BackendConfig.isFirebaseRealtimeDatabase) {
                local.seedDesignData()
            }
            // Trigger network fetch in the background
            try {
                if (BackendConfig.isFirebaseRealtimeDatabase) {
                    local.replaceSessions(api.getSessions())
                } else {
                    val remoteSessions = api.getSessions(day, track?.name)
                    local.insertSessions(remoteSessions)
                }
                connectionState.reportOnline()
            } catch (e: Exception) {
                // Network failed, rely on cache
                println("Network error: ${e.redactedMessage("Unknown error")}")
                val hasCachedSessions = local.hasCachedSessions()
                connectionState.reportOffline(hasCachedSessions)
                if (BackendConfig.isFirebaseRealtimeDatabase && !hasCachedSessions) {
                    throw e
                }
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
            if (!BackendConfig.isFirebaseRealtimeDatabase) {
                local.seedDesignData()
            }
            local.getSessionById(id) ?: throw e
        }
    }

    fun toggleBookmark(sessionId: String) {
        local.toggleBookmark(sessionId)
    }

    fun getBookmarkedSessionIds(): Flow<List<String>> {
        return local.getAllBookmarkIds()
    }
}
