package com.ingevent.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals

class ConnectionStateRepositoryTest {
    @Test
    fun cachedDataEnablesOfflineMode() {
        val repository = ConnectionStateRepository()

        repository.reportOffline(hasCachedData = true)

        assertEquals(ConnectionState(isOffline = true), repository.state.value)
    }

    @Test
    fun missingCacheRequiresConnection() {
        val repository = ConnectionStateRepository()

        repository.reportOffline(hasCachedData = false)

        assertEquals(
            ConnectionState(isOffline = true, requiresConnection = true),
            repository.state.value,
        )
    }

    @Test
    fun successfulRefreshReturnsOnline() {
        val repository = ConnectionStateRepository()
        repository.reportOffline(hasCachedData = true)

        repository.reportOnline()

        assertEquals(ConnectionState(), repository.state.value)
    }
}
