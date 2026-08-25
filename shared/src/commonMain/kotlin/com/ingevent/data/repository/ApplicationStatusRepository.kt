package com.ingevent.data.repository

import com.ingevent.data.local.LocalDataSource
import com.ingevent.data.remote.INGEventApi
import kotlinx.coroutines.CancellationException

class ApplicationStatusRepository(
    private val api: INGEventApi,
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
