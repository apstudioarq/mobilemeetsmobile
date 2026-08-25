package com.ingevent.data.repository

import com.ingevent.data.local.LocalDataSource
import com.ingevent.data.model.Speaker
import com.ingevent.data.remote.BackendConfig
import com.ingevent.data.remote.INGEventApi
import com.ingevent.data.remote.redactedMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart

class SpeakerRepository(
    private val api: INGEventApi,
    private val local: LocalDataSource,
) {
    fun getAllSpeakers(): Flow<List<Speaker>> {
        return local.getAllSpeakers().onStart {
            if (!BackendConfig.isFirebaseRealtimeDatabase) {
                local.seedDesignData()
            }
            try {
                val remote = api.getSpeakers()
                if (BackendConfig.isFirebaseRealtimeDatabase) {
                    local.replaceSpeakers(remote)
                } else {
                    local.insertSpeakers(remote)
                }
            } catch (e: Exception) {
                println("Network error fetching speakers: ${e.redactedMessage("Unknown error")}")
                if (BackendConfig.isFirebaseRealtimeDatabase && !local.hasCachedSpeakers()) {
                    throw e
                }
            }
        }
    }

    suspend fun getSpeakerById(id: String): Speaker {
        return try {
            val remote = api.getSpeakerById(id)
            remote.toDomain()
        } catch (e: Exception) {
            if (!BackendConfig.isFirebaseRealtimeDatabase) {
                local.seedDesignData()
            }
            local.getSpeakerById(id) ?: throw e
        }
    }
}
