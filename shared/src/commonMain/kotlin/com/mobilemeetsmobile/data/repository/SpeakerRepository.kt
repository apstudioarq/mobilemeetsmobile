package com.mobilemeetsmobile.data.repository

import com.mobilemeetsmobile.data.local.LocalDataSource
import com.mobilemeetsmobile.data.model.Speaker
import com.mobilemeetsmobile.data.remote.MobileMeetsMobileApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart

class SpeakerRepository(
    private val api: MobileMeetsMobileApi,
    private val local: LocalDataSource,
) {
    fun getAllSpeakers(): Flow<List<Speaker>> {
        return local.getAllSpeakers().onStart {
            try {
                val remote = api.getSpeakers()
                local.insertSpeakers(remote)
            } catch (e: Exception) {
                println("Network error fetching speakers: ${e.message}")
            }
        }
    }

    suspend fun getSpeakerById(id: String): Speaker {
        return api.getSpeakerById(id).toDomain()
    }
}
