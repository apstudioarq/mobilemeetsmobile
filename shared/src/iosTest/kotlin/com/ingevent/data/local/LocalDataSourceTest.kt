package com.ingevent.data.local

import com.ingevent.data.remote.dto.SessionDto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalDataSourceTest {
    @Test
    fun sessionMappingDoesNotBorrowANestedDatabaseConnection() = runBlocking {
        val local = LocalDataSource(DatabaseDriverFactory())
        val session = SessionDto(
            id = "ios-database-pool-regression",
            title = "Database pool regression",
            description = "",
            startTime = "2026-08-19T09:00:00",
            endTime = "2026-08-19T10:00:00",
            duration = "60 min",
            room = "Test room",
            day = 97,
            track = "GENERIC",
            type = "SESSION",
            level = "INTERMEDIATE",
            speakerIds = emptyList(),
            capacity = 0,
            registered = 0,
        )

        local.replaceSessions(listOf(session))

        val sessions = withTimeout(5_000) {
            local.getSessionsByDay(97).first()
        }
        assertEquals(listOf(session.id), sessions.map { it.id })

        if (local.isBookmarked(session.id)) {
            local.toggleBookmark(session.id)
        }
        local.toggleBookmark(session.id)
        val bookmarked = withTimeout(5_000) {
            local.getSessionById(session.id)
        }
        assertTrue(bookmarked?.isBookmarked == true)
    }
}
