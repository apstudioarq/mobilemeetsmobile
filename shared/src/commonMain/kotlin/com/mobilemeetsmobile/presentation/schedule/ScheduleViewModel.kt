package com.mobilemeetsmobile.presentation.schedule

import com.mobilemeetsmobile.data.model.ConferenceDay
import com.mobilemeetsmobile.data.model.Session
import com.mobilemeetsmobile.data.model.Track
import com.mobilemeetsmobile.domain.usecase.GetBookmarksUseCase
import com.mobilemeetsmobile.domain.usecase.GetScheduleUseCase
import com.mobilemeetsmobile.domain.usecase.SearchSessionsUseCase
import com.mobilemeetsmobile.domain.usecase.ToggleBookmarkUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ScheduleUiState(
    val selectedDay: Int = 1,
    val selectedTrack: Track? = null,
    val sessions: List<Session> = emptyList(),
    val timeSlots: Map<String, List<Session>> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val bookmarkedIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val showBookmarksOnly: Boolean = false,
    val days: List<ConferenceDay> = listOf(
        ConferenceDay(1, "Day 1", "May 14"),
        ConferenceDay(2, "Day 2", "May 15"),
        ConferenceDay(3, "Day 3", "May 16"),
    ),
)

class ScheduleViewModel(
    private val getSchedule: GetScheduleUseCase,
    private val searchSessions: SearchSessionsUseCase,
    private val toggleBookmark: ToggleBookmarkUseCase,
    private val getBookmarks: GetBookmarksUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        loadDay(1)
        observeBookmarks()
    }

    fun loadDay(day: Int) {
        _uiState.update { it.copy(selectedDay = day, isLoading = true, error = null) }
        scope.launch {
            try {
                getSchedule(day, _uiState.value.selectedTrack)
                    .collect { sessions ->
                        val filtered = applyFilters(sessions)
                        _uiState.update {
                            it.copy(
                                sessions = filtered,
                                timeSlots = filtered.groupBy { s -> s.startTime },
                                isLoading = false,
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message)
                }
            }
        }
    }

    fun selectTrack(track: Track?) {
        _uiState.update { it.copy(selectedTrack = track) }
        loadDay(_uiState.value.selectedDay)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.isBlank()) {
            loadDay(_uiState.value.selectedDay)
            return
        }
        scope.launch {
            searchSessions(query).collect { results ->
                val dayFiltered = results.filter { it.day == _uiState.value.selectedDay }
                _uiState.update {
                    it.copy(
                        sessions = dayFiltered,
                        timeSlots = dayFiltered.groupBy { s -> s.startTime },
                    )
                }
            }
        }
    }

    fun onBookmarkToggle(sessionId: String) {
        toggleBookmark(sessionId)
        // Update local state immediately
        _uiState.update { state ->
            val newBookmarks = state.bookmarkedIds.toMutableSet()
            if (newBookmarks.contains(sessionId)) {
                newBookmarks.remove(sessionId)
            } else {
                newBookmarks.add(sessionId)
            }
            val updatedSessions = state.sessions.map { s ->
                if (s.id == sessionId) s.copy(isBookmarked = !s.isBookmarked) else s
            }
            state.copy(
                bookmarkedIds = newBookmarks,
                sessions = updatedSessions,
                timeSlots = updatedSessions.groupBy { s -> s.startTime },
            )
        }
    }

    fun toggleShowBookmarksOnly() {
        _uiState.update { it.copy(showBookmarksOnly = !it.showBookmarksOnly) }
        loadDay(_uiState.value.selectedDay)
    }

    private fun observeBookmarks() {
        scope.launch {
            getBookmarks().collect { ids ->
                _uiState.update { it.copy(bookmarkedIds = ids.toSet()) }
            }
        }
    }

    private fun applyFilters(sessions: List<Session>): List<Session> {
        val state = _uiState.value
        return sessions.filter { session ->
            val matchesBookmark = !state.showBookmarksOnly || state.bookmarkedIds.contains(session.id)
            val matchesSearch = state.searchQuery.isBlank() ||
                session.title.contains(state.searchQuery, ignoreCase = true) ||
                session.description.contains(state.searchQuery, ignoreCase = true)
            matchesBookmark && matchesSearch
        }
    }

    fun onCleared() {
        // Cancel scope if needed
    }
}
