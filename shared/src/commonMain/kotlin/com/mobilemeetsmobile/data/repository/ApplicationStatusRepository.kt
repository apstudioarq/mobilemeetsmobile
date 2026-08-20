package com.mobilemeetsmobile.data.repository

import com.mobilemeetsmobile.data.local.LocalDataSource
import com.mobilemeetsmobile.data.remote.MobileMeetsMobileApi
import kotlinx.coroutines.CancellationException

class ApplicationStatusRepository(
    private val api: MobileMeetsMobileApi,
    private val localDataSource: LocalDataSource,
) {
    suspend fun isApplicationLocked(): Boolean {
        val cachedValue = localDataSource.getApplicationLocked()
        return try {
            api.getApplicationLocked().also(localDataSource::saveApplicationLocked)
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            cachedValue ?: false
        }
    }
}
