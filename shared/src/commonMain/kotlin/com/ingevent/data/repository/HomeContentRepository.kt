package com.ingevent.data.repository

import com.ingevent.data.local.LocalDataSource
import com.ingevent.data.model.HomeContent
import com.ingevent.data.remote.INGEventApi
import com.ingevent.data.remote.redactedMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart

class HomeContentRepository(
    private val api: INGEventApi,
    private val local: LocalDataSource,
) {
    fun getHomeContent(): Flow<HomeContent> {
        return local.getHomeContent().onStart {
            try {
                local.saveHomeContent(api.getHomeContent())
            } catch (e: Exception) {
                println("Network error fetching home content: ${e.redactedMessage("Unknown error")}")
            }
        }
    }
}
