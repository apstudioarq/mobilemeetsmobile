package com.mobilemeetsmobile.data.repository

import com.mobilemeetsmobile.data.local.LocalDataSource
import com.mobilemeetsmobile.data.model.HomeContent
import com.mobilemeetsmobile.data.remote.MobileMeetsMobileApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart

class HomeContentRepository(
    private val api: MobileMeetsMobileApi,
    private val local: LocalDataSource,
) {
    fun getHomeContent(): Flow<HomeContent> {
        return local.getHomeContent().onStart {
            try {
                local.saveHomeContent(api.getHomeContent())
            } catch (e: Exception) {
                println("Network error fetching home content: ${e.message}")
            }
        }
    }
}
