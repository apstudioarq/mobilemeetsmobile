package com.mobilemeetsmobile.android.notifications

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import com.mobilemeetsmobile.android.R
import com.mobilemeetsmobile.android.ui.MainActivity

class SessionNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getStringExtra(SessionNotificationScheduler.EXTRA_SESSION_ID) ?: return
        val sessionTitle = intent.getStringExtra(SessionNotificationScheduler.EXTRA_SESSION_TITLE).orEmpty()
        val room = intent.getStringExtra(SessionNotificationScheduler.EXTRA_ROOM).orEmpty()
        val minutes = intent.getIntExtra(
            SessionNotificationScheduler.EXTRA_REMINDER_MINUTES,
            NotificationPreferences.DEFAULT_REMINDER_MINUTES,
        )
        val type = intent.getStringExtra(SessionNotificationScheduler.EXTRA_REMINDER_TYPE)
            ?.let { runCatching { SessionNotificationScheduler.ReminderType.valueOf(it) }.getOrNull() }
            ?: return

        val (title, body) = when (type) {
            SessionNotificationScheduler.ReminderType.BEFORE -> {
                "Starts in $minutes min · $sessionTitle" to listOfNotNull(
                    room.takeIf(String::isNotBlank),
                    "Tap to view session details",
                ).joinToString(" • ")
            }
            SessionNotificationScheduler.ReminderType.AFTER -> {
                "How was $sessionTitle?" to "Rate the session and share your comments."
            }
        }

        val detailIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(SessionNotificationScheduler.EXTRA_SESSION_ID, sessionId)
        }
        val detailPendingIntent = PendingIntent.getActivity(
            context,
            "detail|$sessionId|${type.name}".hashCode(),
            detailIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = Notification.Builder(context, SessionNotificationScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.splash_logo))
            .setColor(0xFFFF6200.toInt())
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setContentIntent(detailPendingIntent)
            .setCategory(Notification.CATEGORY_EVENT)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify("$sessionId|${type.name}".hashCode(), notification)
    }
}
