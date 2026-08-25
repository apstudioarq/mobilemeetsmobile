package com.ingevent.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ConnectionState(
    val isOffline: Boolean = false,
    val requiresConnection: Boolean = false,
)

class ConnectionStateRepository {
    private val _state = MutableStateFlow(ConnectionState())
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    fun reportOnline() {
        _state.value = ConnectionState()
    }

    fun reportOffline(hasCachedData: Boolean) {
        _state.value = ConnectionState(
            isOffline = true,
            requiresConnection = !hasCachedData,
        )
    }
}
