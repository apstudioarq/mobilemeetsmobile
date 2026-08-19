package com.mobilemeetsmobile.android.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mobilemeetsmobile.data.model.Session
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class SessionNotificationScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val scheduleStore = context.getSharedPreferences(SCHEDULE_STORE, Context.MODE_PRIVATE)

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Saved session reminders",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Reminders before and after your saved conference sessions"
            enableVibration(true)
            setShowBadge(true)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun reschedule(sessions: List<Session>, bookmarkedIds: Set<String>, reminderMinutes: Int) {
        synchronized(SCHEDULING_LOCK) {
            cancelScheduledAlarms()
            val minutes = reminderMinutes.coerceIn(
                NotificationPreferences.MIN_REMINDER_MINUTES,
                NotificationPreferences.MAX_REMINDER_MINUTES,
            )
            val now = System.currentTimeMillis()
            val newTokens = mutableSetOf<String>()

            sessions
                .filter { it.id in bookmarkedIds }
                .forEach { session ->
                    scheduleIfFuture(session, ReminderType.BEFORE, minutes, now)?.let(newTokens::add)
                    scheduleIfFuture(session, ReminderType.AFTER, minutes, now)?.let(newTokens::add)
                }

            scheduleStore.edit().putStringSet(KEY_SCHEDULED_TOKENS, newTokens).commit()
        }
    }

    private fun scheduleIfFuture(
        session: Session,
        type: ReminderType,
        reminderMinutes: Int,
        now: Long,
    ): String? {
        val rawSessionTime = if (type == ReminderType.BEFORE) session.startTime else session.endTime
        val sessionTime = parseSessionTime(rawSessionTime) ?: return null
        val offset = reminderMinutes * 60_000L
        var triggerAt = if (type == ReminderType.BEFORE) sessionTime - offset else sessionTime + offset
        // TIME_SET may be received a few milliseconds after the configured reminder instant.
        // Keep a pre-session reminder alive while the session itself has not started yet.
        if (type == ReminderType.BEFORE && triggerAt <= now && now < sessionTime) {
            triggerAt = now + IMMEDIATE_DELIVERY_DELAY_MILLIS
        }
        if (triggerAt <= now) return null

        val token = token(session.id, type)
        val pendingIntent = alarmPendingIntent(
            token = token,
            sessionId = session.id,
            sessionTitle = session.title,
            room = session.room,
            reminderMinutes = reminderMinutes,
            type = type,
            flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return null

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            // The reminder still works when exact-alarm access has not been granted, but Android may defer it.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
        return token
    }

    private fun cancelScheduledAlarms() {
        scheduleStore.getStringSet(KEY_SCHEDULED_TOKENS, emptySet()).orEmpty().forEach { token ->
            val parts = token.split(TOKEN_SEPARATOR, limit = 2)
            if (parts.size != 2) return@forEach
            val type = ReminderType.entries.firstOrNull { it.name == parts[0] } ?: return@forEach
            alarmManager.cancel(
                alarmPendingIntent(
                    token = token,
                    sessionId = parts[1],
                    sessionTitle = "",
                    room = "",
                    reminderMinutes = NotificationPreferences.DEFAULT_REMINDER_MINUTES,
                    type = type,
                    flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
                ) ?: return@forEach,
            )
        }
        scheduleStore.edit().remove(KEY_SCHEDULED_TOKENS).apply()
    }

    private fun alarmPendingIntent(
        token: String,
        sessionId: String,
        sessionTitle: String,
        room: String,
        reminderMinutes: Int,
        type: ReminderType,
        flags: Int,
    ): PendingIntent? {
        val intent = Intent(context, SessionNotificationReceiver::class.java).apply {
            action = "$REMINDER_ACTION.$token"
            putExtra(EXTRA_SESSION_ID, sessionId)
            putExtra(EXTRA_SESSION_TITLE, sessionTitle)
            putExtra(EXTRA_ROOM, room)
            putExtra(EXTRA_REMINDER_MINUTES, reminderMinutes)
            putExtra(EXTRA_REMINDER_TYPE, type.name)
        }
        return PendingIntent.getBroadcast(context, token.hashCode(), intent, flags)
    }

    private fun token(sessionId: String, type: ReminderType) = "${type.name}$TOKEN_SEPARATOR$sessionId"

    internal fun parseSessionTime(value: String): Long? {
        return runCatching { Instant.parse(value).toEpochMilli() }
            .getOrElse {
                runCatching {
                    LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                }.getOrNull()
            }
    }

    enum class ReminderType { BEFORE, AFTER }

    companion object {
        const val CHANNEL_ID = "saved_session_reminders"
        const val EXTRA_SESSION_ID = "notification_session_id"
        const val EXTRA_SESSION_TITLE = "notification_session_title"
        const val EXTRA_ROOM = "notification_session_room"
        const val EXTRA_REMINDER_MINUTES = "notification_reminder_minutes"
        const val EXTRA_REMINDER_TYPE = "notification_reminder_type"

        private const val SCHEDULE_STORE = "scheduled_session_notifications"
        private const val KEY_SCHEDULED_TOKENS = "scheduled_tokens"
        private const val REMINDER_ACTION = "com.mobilemeetsmobile.android.SESSION_REMINDER"
        private const val TOKEN_SEPARATOR = "|"
        private const val IMMEDIATE_DELIVERY_DELAY_MILLIS = 750L
        private val SCHEDULING_LOCK = Any()
    }
}
