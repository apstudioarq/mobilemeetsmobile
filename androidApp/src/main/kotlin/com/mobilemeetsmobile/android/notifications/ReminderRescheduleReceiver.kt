package com.mobilemeetsmobile.android.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mobilemeetsmobile.data.local.LocalDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ReminderRescheduleReceiver : BroadcastReceiver(), KoinComponent {
    private val localDataSource: LocalDataSource by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val sessions = localDataSource.getAllSessions().first()
                val bookmarks = localDataSource.getAllBookmarkIds().first().toSet()
                SessionNotificationScheduler(context.applicationContext).reschedule(
                    sessions = sessions,
                    bookmarkedIds = bookmarks,
                    reminderMinutes = NotificationPreferences(context).reminderMinutes,
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
