package com.ingevent.presentation.home

import com.ingevent.data.model.Level
import com.ingevent.data.model.Session
import com.ingevent.data.model.SessionType
import com.ingevent.data.model.Track
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeSessionSelectionTest {
    private val utc = TimeZone.UTC

    @Test
    fun returnsEverySessionThatIsCurrentlyLive() {
        val selection = selectHomeSessions(
            sessions = listOf(
                session("finished", "2026-08-18T08:00:00Z", "2026-08-18T09:00:00Z"),
                session("live-a", "2026-08-18T09:00:00Z", "2026-08-18T11:00:00Z"),
                session("live-b", "2026-08-18T10:00:00Z", "2026-08-18T10:30:00Z"),
                session("future", "2026-08-18T12:00:00Z", "2026-08-18T13:00:00Z"),
            ),
            now = Instant.parse("2026-08-18T10:15:00Z"),
            timeZone = utc,
        )

        assertTrue(selection.isLive)
        assertEquals(listOf("live-a", "live-b"), selection.sessions.map(Session::id))
        assertEquals(Instant.parse("2026-08-18T10:30:00Z"), selection.nextChangeAt)
    }

    @Test
    fun returnsAllSessionsInTheNextSlotWhenNothingIsLive() {
        val selection = selectHomeSessions(
            sessions = listOf(
                session("next-a", "2026-08-18T11:00:00Z", "2026-08-18T12:00:00Z"),
                session("later", "2026-08-18T13:00:00Z", "2026-08-18T14:00:00Z"),
                session("next-b", "2026-08-18T11:00:00Z", "2026-08-18T11:30:00Z"),
            ),
            now = Instant.parse("2026-08-18T10:15:00Z"),
            timeZone = utc,
        )

        assertFalse(selection.isLive)
        assertEquals(listOf("next-a", "next-b"), selection.sessions.map(Session::id))
        assertEquals(Instant.parse("2026-08-18T11:00:00Z"), selection.nextChangeAt)
    }

    @Test
    fun treatsTheEndAsExclusiveAndSupportsFirebaseLocalTimestamps() {
        val selection = selectHomeSessions(
            sessions = listOf(
                session("ending", "2026-08-18T09:00:00", "2026-08-18T10:00:00"),
                session("starting", "2026-08-18T10:00:00", "2026-08-18T11:00:00"),
            ),
            now = Instant.parse("2026-08-18T10:00:00Z"),
            timeZone = utc,
        )

        assertTrue(selection.isLive)
        assertEquals(listOf("starting"), selection.sessions.map(Session::id))
    }

    @Test
    fun returnsAnEmptyUpcomingStateAfterTheEvent() {
        val selection = selectHomeSessions(
            sessions = listOf(
                session("finished", "2026-08-18T08:00:00Z", "2026-08-18T09:00:00Z"),
            ),
            now = Instant.parse("2026-08-18T10:00:00Z"),
            timeZone = utc,
        )

        assertFalse(selection.isLive)
        assertTrue(selection.sessions.isEmpty())
        assertEquals(null, selection.nextChangeAt)
    }

    private fun session(id: String, start: String, end: String) = Session(
        id = id,
        title = id,
        description = "",
        startTime = start,
        endTime = end,
        duration = "60 min",
        room = id,
        day = 1,
        track = Track.GENERIC,
        type = SessionType.SESSION,
        level = Level.INTERMEDIATE,
        speakerIds = listOf("speaker-$id"),
        capacity = 0,
        registered = 0,
    )
}
