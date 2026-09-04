package com.ingevent.presentation.schedule

import com.ingevent.data.model.ConferenceDay
import com.ingevent.data.model.HomeContent
import com.ingevent.data.model.MapContent
import com.ingevent.data.model.Session
import com.ingevent.data.model.Track
import com.ingevent.data.repository.ConnectionStateRepository
import com.ingevent.data.remote.redactedMessage
import com.ingevent.domain.usecase.GetAllSessionsUseCase
import com.ingevent.domain.usecase.GetBookmarksUseCase
import com.ingevent.domain.usecase.GetHomeContentUseCase
import com.ingevent.domain.usecase.GetMapContentUseCase
import com.ingevent.domain.usecase.GetScheduleUseCase
import com.ingevent.domain.usecase.GetSpeakersUseCase
import com.ingevent.domain.usecase.ToggleBookmarkUseCase
import com.ingevent.presentation.StateObservation
import com.ingevent.presentation.home.selectHomeSessions
import com.ingevent.presentation.observeIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class ScheduleUiState(
    val selectedDay: Int = 1,
    val selectedTrack: Track? = null,
    val sessions: List<Session> = emptyList(),
    val allSessions: List<Session> = emptyList(),
    val hasLoadedAllSessions: Boolean = false,
    val hasLoadedBookmarks: Boolean = false,
    val homeSessions: List<Session> = emptyList(),
    val homeSessionsAreLive: Boolean = false,
    val timeSlots: Map<String, List<Session>> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val bookmarkedIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val showBookmarksOnly: Boolean = false,
    val days: List<ConferenceDay> = listOf(ConferenceDay(1, "Day 1", "TBD")),
    val homeContent: HomeContent = HomeContent(),
    val mapContent: MapContent = MapContent(),
    val isMapLoading: Boolean = false,
    val mapError: String? = null,
    val isOffline: Boolean = false,
    val requiresConnection: Boolean = false,
) {
    val isNotificationScheduleReady: Boolean
        get() = hasLoadedAllSessions && hasLoadedBookmarks
}

class ScheduleViewModel(
    private val getSchedule: GetScheduleUseCase,
    private val getAllSessions: GetAllSessionsUseCase,
    private val toggleBookmark: ToggleBookmarkUseCase,
    private val getBookmarks: GetBookmarksUseCase,
    private val getHomeContent: GetHomeContentUseCase,
    private val getMapContent: GetMapContentUseCase,
    private val getSpeakers: GetSpeakersUseCase,
    private val connectionState: ConnectionStateRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var loadDayJob: Job? = null
    private var availableDaysJob: Job? = null
    private var homeContentJob: Job? = null
    private var mapContentJob: Job? = null
    private var speakersJob: Job? = null
    private var homeClockJob: Job? = null
    private var speakerNamesById: Map<String, String> = emptyMap()

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        loadDay(_uiState.value.selectedDay)
        observeAvailableDays()
        observeBookmarks()
        observeHomeContent()
        observeSpeakers()
        observeConnectionState()
        observeHomeClock()
    }

    fun observeState(onStateChanged: (ScheduleUiState) -> Unit): StateObservation {
        return uiState.observeIn(scope, onStateChanged)
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
                _uiState.update { it.copy(isLoading = false, error = e.redactedMessage("Unable to load sessions.")) }
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

    fun retryConnection() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        loadDay(_uiState.value.selectedDay)
        observeAvailableDays()
        observeHomeContent()
        observeSpeakers()
    }

    private fun observeBookmarks() {
        scope.launch {
            getBookmarks().collect { ids ->
                val bookmarkIds = ids.toSet()
                _uiState.update {
                    it.copy(
                        bookmarkedIds = bookmarkIds,
                        hasLoadedBookmarks = true,
                        sessions = applyBookmarkState(it.sessions, bookmarkIds),
                        allSessions = applyBookmarkState(it.allSessions, bookmarkIds),
                    )
                }
            }
        }
    }

    private fun observeHomeContent() {
        homeContentJob?.cancel()
        homeContentJob = scope.launch {
            try {
                getHomeContent().collect { content ->
                    _uiState.update { it.copy(homeContent = content) }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                println("Unable to load home content: ${e.redactedMessage("Unknown error")}")
            }
        }
    }

    fun loadMapContent() {
        mapContentJob?.cancel()
        _uiState.update { it.copy(isMapLoading = true, mapError = null) }
        mapContentJob = scope.launch {
            try {
                getMapContent().collect { content ->
                    _uiState.update {
                        it.copy(mapContent = content, isMapLoading = false, mapError = null)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update {
                    it.copy(isMapLoading = false, mapError = e.redactedMessage("Unable to load the map."))
                }
            }
        }
    }

    private fun observeSpeakers() {
        speakersJob?.cancel()
        speakersJob = scope.launch {
            try {
                getSpeakers().collect { speakers ->
                    speakerNamesById = speakers.associate { speaker -> speaker.id to speaker.name }
                    if (_uiState.value.searchQuery.isNotBlank()) {
                        refilterCurrentDayFromAllSessions()
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                println("Unable to load speakers: ${e.redactedMessage("Unknown error")}")
            }
        }
    }

    private fun observeAvailableDays() {
        availableDaysJob?.cancel()
        availableDaysJob = scope.launch {
            try {
                getAllSessions().collect { sessions ->
                    val allSessions = applyBookmarkState(sessions)
                    val homeSelection = selectHomeSessions(
                        sessions = allSessions,
                        now = Clock.System.now(),
                    )
                    val derivedDays = buildConferenceDays(allSessions)
                    if (derivedDays.isEmpty()) {
                        _uiState.update {
                            it.copy(
                                allSessions = emptyList(),
                                hasLoadedAllSessions = true,
                                homeSessions = emptyList(),
                                homeSessionsAreLive = false,
                            )
                        }
                        observeHomeClock()
                        return@collect
                    }

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
                            hasLoadedAllSessions = true,
                            homeSessions = homeSelection.sessions,
                            homeSessionsAreLive = homeSelection.isLive,
                            days = derivedDays,
                            selectedDay = selectedDay,
                        )
                    }
                    observeHomeClock()

                    if (shouldReload) {
                        loadDay(selectedDay)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { current ->
                    if (current.sessions.isEmpty()) {
                        current.copy(
                            isLoading = false,
                            error = e.redactedMessage("Unable to load sessions."),
                        )
                    } else {
                        current
                    }
                }
            }
        }
    }

    private fun observeConnectionState() {
        scope.launch {
            connectionState.state.collect { state ->
                _uiState.update {
                    it.copy(
                        isOffline = state.isOffline,
                        requiresConnection = state.requiresConnection,
                    )
                }
            }
        }
    }

    private fun observeHomeClock() {
        homeClockJob?.cancel()
        homeClockJob = scope.launch {
            while (true) {
                val nextChangeAt = refreshHomeSessions()
                val nowMillis = Clock.System.now().toEpochMilliseconds()
                val untilNextChange = nextChangeAt
                    ?.toEpochMilliseconds()
                    ?.minus(nowMillis)
                    ?.plus(HOME_CLOCK_BOUNDARY_GRACE_MILLIS)
                delay(
                    untilNextChange
                        ?.coerceIn(HOME_CLOCK_MIN_DELAY_MILLIS, HOME_CLOCK_SAFETY_REFRESH_MILLIS)
                        ?: HOME_CLOCK_SAFETY_REFRESH_MILLIS,
                )
            }
        }
    }

    private fun refreshHomeSessions(): Instant? {
        var nextChangeAt: Instant? = null
        _uiState.update { state ->
            val selection = selectHomeSessions(
                sessions = state.allSessions,
                now = Clock.System.now(),
            )
            nextChangeAt = selection.nextChangeAt
            if (
                state.homeSessions == selection.sessions &&
                state.homeSessionsAreLive == selection.isLive
            ) {
                state
            } else {
                state.copy(
                    homeSessions = selection.sessions,
                    homeSessionsAreLive = selection.isLive,
                )
            }
        }
        return nextChangeAt
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
        scope.cancel()
        homeClockJob?.cancel()
    }

    private companion object {
        const val HOME_CLOCK_BOUNDARY_GRACE_MILLIS = 100L
        const val HOME_CLOCK_MIN_DELAY_MILLIS = 100L
        const val HOME_CLOCK_SAFETY_REFRESH_MILLIS = 60_000L
    }
}
