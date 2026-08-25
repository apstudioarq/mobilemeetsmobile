package com.ingevent.presentation.speakers

import com.ingevent.data.model.Speaker
import com.ingevent.domain.usecase.GetSpeakersUseCase
import com.ingevent.data.remote.redactedMessage
import com.ingevent.presentation.StateObservation
import com.ingevent.presentation.observeIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SpeakersUiState(
    val speakers: List<Speaker> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

class SpeakersViewModel(
    private val getSpeakers: GetSpeakersUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow(SpeakersUiState())
    val uiState: StateFlow<SpeakersUiState> = _uiState.asStateFlow()

    init {
        loadSpeakers()
    }

    fun observeState(onStateChanged: (SpeakersUiState) -> Unit): StateObservation {
        return uiState.observeIn(scope, onStateChanged)
    }

    fun loadSpeakers() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        scope.launch {
            try {
                getSpeakers().collect { speakers ->
                    _uiState.update {
                        it.copy(speakers = speakers, isLoading = false)
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.redactedMessage("Unable to load speakers."),
                    )
                }
            }
        }
    }

    fun onCleared() {
        scope.cancel()
    }
}
