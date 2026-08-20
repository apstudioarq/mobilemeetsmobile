package com.mobilemeetsmobile.presentation.application

import com.mobilemeetsmobile.data.repository.ApplicationStatusRepository
import com.mobilemeetsmobile.presentation.StateObservation
import com.mobilemeetsmobile.presentation.observeIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val APPLICATION_LOCK_MESSAGE =
    "Thank you for participating. This application was created specifically for the event and is no longer active. You may now uninstall the app from your device."

data class ApplicationStatusUiState(
    val isChecking: Boolean = true,
    val isLocked: Boolean = false,
    val message: String = APPLICATION_LOCK_MESSAGE,
)

class ApplicationStatusViewModel(
    private val repository: ApplicationStatusRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var refreshJob: Job? = null

    private val _uiState = MutableStateFlow(ApplicationStatusUiState())
    val uiState: StateFlow<ApplicationStatusUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun observeState(onStateChanged: (ApplicationStatusUiState) -> Unit): StateObservation {
        return uiState.observeIn(scope, onStateChanged)
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = scope.launch {
            val isLocked = repository.isApplicationLocked()
            _uiState.update { it.copy(isChecking = false, isLocked = isLocked) }
        }
    }

    fun onCleared() {
        scope.cancel()
    }
}
