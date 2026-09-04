package com.ingevent.presentation.schedule

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationScheduleReadinessTest {
    @Test
    fun waitsForBothSessionsAndBookmarksBeforeReconcilingNotifications() {
        assertFalse(ScheduleUiState().isNotificationScheduleReady)
        assertFalse(ScheduleUiState(hasLoadedAllSessions = true).isNotificationScheduleReady)
        assertFalse(ScheduleUiState(hasLoadedBookmarks = true).isNotificationScheduleReady)
        assertTrue(
            ScheduleUiState(hasLoadedAllSessions = true, hasLoadedBookmarks = true)
                .isNotificationScheduleReady,
        )
    }
}
