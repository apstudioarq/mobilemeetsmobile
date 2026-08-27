package com.ingevent.presentation.home

import com.ingevent.data.model.Session
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

data class HomeSessionSelection(
    val sessions: List<Session>,
    val isLive: Boolean,
    val nextChangeAt: Instant?,
)

/**
 * Selects every session that is active at [now]. When nothing is live, all
 * sessions in the next start-time slot are returned instead.
 *
 * Firebase sessions currently use local ISO timestamps, while other backends
 * can return timestamps with an offset. Both forms are supported here.
 */
fun selectHomeSessions(
    sessions: List<Session>,
    now: Instant,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): HomeSessionSelection {
    val timedSessions = sessions.mapNotNull { session ->
        val start = session.startTime.toInstantOrNull(timeZone) ?: return@mapNotNull null
        val end = session.endTime.toInstantOrNull(timeZone) ?: return@mapNotNull null
        if (end <= start) return@mapNotNull null
        TimedSession(session = session, start = start, end = end)
    }

    val liveSessions = timedSessions
        .filter { timed -> timed.start <= now && now < timed.end }
        .sortedWith(compareBy<TimedSession> { it.start }.thenBy { it.session.room })
        .map(TimedSession::session)

    if (liveSessions.isNotEmpty()) {
        val nextChangeAt = timedSessions
            .asSequence()
            .flatMap { sequenceOf(it.start, it.end) }
            .filter { it > now }
            .minOrNull()
        return HomeSessionSelection(
            sessions = liveSessions,
            isLive = true,
            nextChangeAt = nextChangeAt,
        )
    }

    val nextStart = timedSessions
        .asSequence()
        .map(TimedSession::start)
        .filter { it > now }
        .minOrNull()

    val nextSessions = nextStart?.let { start ->
        timedSessions
            .filter { it.start == start }
            .sortedBy { it.session.room }
            .map(TimedSession::session)
    }.orEmpty()

    return HomeSessionSelection(
        sessions = nextSessions,
        isLive = false,
        nextChangeAt = nextStart,
    )
}

private data class TimedSession(
    val session: Session,
    val start: Instant,
    val end: Instant,
)

private fun String.toInstantOrNull(timeZone: TimeZone): Instant? {
    return runCatching { Instant.parse(this) }.getOrNull()
        ?: runCatching { LocalDateTime.parse(this).toInstant(timeZone) }.getOrNull()
}
