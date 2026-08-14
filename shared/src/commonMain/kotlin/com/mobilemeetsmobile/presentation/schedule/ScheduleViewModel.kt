package com.mobilemeetsmobile.presentation.schedule

import com.mobilemeetsmobile.data.model.ConferenceDay
import com.mobilemeetsmobile.data.model.HomeContent
import com.mobilemeetsmobile.data.model.Session
import com.mobilemeetsmobile.data.model.Track
import com.mobilemeetsmobile.domain.usecase.GetAllSessionsUseCase
import com.mobilemeetsmobile.domain.usecase.GetBookmarksUseCase
import com.mobilemeetsmobile.domain.usecase.GetHomeContentUseCase
import com.mobilemeetsmobile.domain.usecase.GetScheduleUseCase
import com.mobilemeetsmobile.domain.usecase.GetSpeakersUseCase
import com.mobilemeetsmobile.domain.usecase.ToggleBookmarkUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class ScheduleUiState(
    val selectedDay: Int = 1,
    val selectedTrack: Track? = null,
    val sessions: List<Session> = emptyList(),
    val allSessions: List<Session> = emptyList(),
    val timeSlots: Map<String, List<Session>> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val bookmarkedIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val showBookmarksOnly: Boolean = false,
    val days: List<ConferenceDay> = listOf(ConferenceDay(1, "Day 1", "TBD")),
    val homeContent: HomeContent = HomeContent(),
)

class ScheduleViewModel(
    private val getSchedule: GetScheduleUseCase,
    private val getAllSessions: GetAllSessionsUseCase,
    private val toggleBookmark: ToggleBookmarkUseCase,
    private val getBookmarks: GetBookmarksUseCase,
    private val getHomeContent: GetHomeContentUseCase,
    private val getSpeakers: GetSpeakersUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var loadDayJob: Job? = null
    private var speakerNamesById: Map<String, String> = emptyMap()

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        loadDay(_uiState.value.selectedDay)
        observeAvailableDays()
        observeBookmarks()
        observeHomeContent()
        observeSpeakers()
    }

    fun loadDay(day: Int) {
        _uiState.update { it.copy(selectedDay = day, isLoading = true, error = null) }
        loadDayJob?.cancel()
        loadDayJob = scope.launch {
            try {
                getSchedule(day, _uiState.value.selectedTrack).collect { sessions ->
                    val filtered = applyFilters(applyBookmarkState(sessions))
                    _uiState.update {
                        it.copy(
                            sessions = filtered,
                            timeSlots = filtered.groupBy { session -> session.startTime },
                            isLoading = false,
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(isLoading = false, error = e.message) }
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

        refilterCurrentDayFromAllSessions()
    }

    fun onBookmarkToggle(sessionId: String) {
        toggleBookmark(sessionId)
        _uiState.update { state ->
            val newBookmarks = state.bookmarkedIds.toMutableSet()
            if (newBookmarks.contains(sessionId)) {
                newBookmarks.remove(sessionId)
            } else {
                newBookmarks.add(sessionId)
            }

            val updatedSessions = state.sessions.map { session ->
                if (session.id == sessionId) {
                    session.copy(isBookmarked = !session.isBookmarked)
                } else {
                    session
                }
            }
            val updatedAllSessions = state.allSessions.map { session ->
                if (session.id == sessionId) {
                    session.copy(isBookmarked = !session.isBookmarked)
                } else {
                    session
                }
            }

            val refiltered = applyFilters(updatedSessions, newBookmarks)
            state.copy(
                bookmarkedIds = newBookmarks,
                sessions = refiltered,
                allSessions = updatedAllSessions,
                timeSlots = refiltered.groupBy { session -> session.startTime },
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
                val bookmarkIds = ids.toSet()
                _uiState.update {
                    it.copy(
                        bookmarkedIds = bookmarkIds,
                        sessions = applyBookmarkState(it.sessions, bookmarkIds),
                        allSessions = applyBookmarkState(it.allSessions, bookmarkIds),
                    )
                }
            }
        }
    }

    private fun observeHomeContent() {
        scope.launch {
            getHomeContent().collect { content ->
                _uiState.update { it.copy(homeContent = content) }
            }
        }
    }

    private fun observeSpeakers() {
        scope.launch {
            getSpeakers().collect { speakers ->
                speakerNamesById = speakers.associate { speaker -> speaker.id to speaker.name }
                if (_uiState.value.searchQuery.isNotBlank()) {
                    refilterCurrentDayFromAllSessions()
                }
            }
        }
    }

    private fun observeAvailableDays() {
        scope.launch {
            try {
                getAllSessions().collect { sessions ->
                    val allSessions = applyBookmarkState(sessions)
                    val derivedDays = buildConferenceDays(allSessions)
                    if (derivedDays.isEmpty()) return@collect

                    val current = _uiState.value
                    val selectedDay = if (derivedDays.any { it.dayNumber == current.selectedDay }) {
                        current.selectedDay
                    } else {
                        derivedDays.first().dayNumber
                    }

                    val shouldReload = selectedDay != current.selectedDay || derivedDays != current.days

                    _uiState.update {
                        it.copy(
                            allSessions = allSessions,
                            days = derivedDays,
                            selectedDay = selectedDay,
                        )
                    }

                    if (shouldReload) {
                        loadDay(selectedDay)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { current ->
                    if (current.sessions.isEmpty()) {
                        current.copy(isLoading = false, error = e.message)
                    } else {
                        current
                    }
                }
            }
        }
    }

    private fun buildConferenceDays(sessions: List<Session>): List<ConferenceDay> {
        if (sessions.isEmpty()) return emptyList()

        val firstStartByDay = sessions
            .groupBy { it.day }
            .mapValues { (_, daySessions) ->
                daySessions.minByOrNull { it.startTime }?.startTime
            }

        return firstStartByDay
            .keys
            .sorted()
            .map { dayNumber ->
                val startTime = firstStartByDay[dayNumber]
                ConferenceDay(
                    dayNumber = dayNumber,
                    label = "Day $dayNumber",
                    date = formatDayLabel(startTime),
                )
            }
    }

    private fun formatDayLabel(startTime: String?): String {
        if (startTime.isNullOrBlank()) return "TBD"
        return runCatching {
            val localDate = Instant.parse(startTime)
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .date
            "${monthShort(localDate.monthNumber)} ${localDate.dayOfMonth}"
        }.getOrElse {
            startTime.take(10)
        }
    }

    private fun monthShort(monthNumber: Int): String {
        return when (monthNumber) {
            1 -> "Jan"
            2 -> "Feb"
            3 -> "Mar"
            4 -> "Apr"
            5 -> "May"
            6 -> "Jun"
            7 -> "Jul"
            8 -> "Aug"
            9 -> "Sep"
            10 -> "Oct"
            11 -> "Nov"
            12 -> "Dec"
            else -> ""
        }
    }

    private fun applyFilters(
        sessions: List<Session>,
        bookmarkIds: Set<String> = _uiState.value.bookmarkedIds,
    ): List<Session> {
        val state = _uiState.value
        return sessions.filter { session ->
            val matchesBookmark = !state.showBookmarksOnly || bookmarkIds.contains(session.id)
            val matchesTrack = state.selectedTrack == null || session.track == state.selectedTrack
            val matchesSearch = state.searchQuery.isBlank() || sessionMatchesSearch(session, state.searchQuery)
            matchesBookmark && matchesTrack && matchesSearch
        }
    }

    private fun sessionMatchesSearch(session: Session, query: String): Boolean {
        return session.title.contains(query, ignoreCase = true) ||
            session.description.contains(query, ignoreCase = true) ||
            session.speakerIds.any { speakerId ->
                speakerId.contains(query, ignoreCase = true) ||
                    speakerNamesById[speakerId]?.contains(query, ignoreCase = true) == true
            }
    }

    private fun refilterCurrentDayFromAllSessions() {
        val state = _uiState.value
        val allDaySessions = state.allSessions.filter { session -> session.day == state.selectedDay }
        val daySessions = allDaySessions.ifEmpty { state.sessions }
        val filtered = applyFilters(applyBookmarkState(daySessions))
        _uiState.update {
            it.copy(
                sessions = filtered,
                timeSlots = filtered.groupBy { session -> session.startTime },
            )
        }
    }

    private fun applyBookmarkState(
        sessions: List<Session>,
        bookmarkIds: Set<String> = _uiState.value.bookmarkedIds,
    ): List<Session> {
        return sessions.map { session ->
            session.copy(isBookmarked = session.id in bookmarkIds)
        }
    }

    fun onCleared() {
        loadDayJob?.cancel()
    }
}
