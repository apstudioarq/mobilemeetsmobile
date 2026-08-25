package com.ing.event.notifications

import android.content.Context

class NotificationPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    var reminderMinutes: Int
        get() = preferences.getInt(KEY_REMINDER_MINUTES, DEFAULT_REMINDER_MINUTES)
        set(value) {
            preferences.edit()
                .putInt(KEY_REMINDER_MINUTES, value.coerceIn(MIN_REMINDER_MINUTES, MAX_REMINDER_MINUTES))
                .apply()
        }

    companion object {
        const val DEFAULT_REMINDER_MINUTES = 5
        const val MIN_REMINDER_MINUTES = 1
        const val MAX_REMINDER_MINUTES = 1_440
        private const val PREFERENCES_NAME = "session_notification_settings"
        private const val KEY_REMINDER_MINUTES = "reminder_minutes"
    }
}
