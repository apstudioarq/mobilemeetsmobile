package com.mobilemeetsmobile.android.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mobilemeetsmobile.android.notifications.NotificationPreferences
import com.mobilemeetsmobile.android.ui.theme.IngOrange
import com.mobilemeetsmobile.android.ui.theme.VibrantBackground
import com.mobilemeetsmobile.android.ui.theme.VibrantBorder
import com.mobilemeetsmobile.android.ui.theme.VibrantMuted
import com.mobilemeetsmobile.android.ui.theme.VibrantSurface
import com.mobilemeetsmobile.android.ui.theme.VibrantSurfaceWarm
import com.mobilemeetsmobile.android.ui.theme.VibrantText

@Composable
fun SettingsScreen(
    reminderMinutes: Int,
    notificationsAllowed: Boolean,
    exactAlarmsAllowed: Boolean,
    onSaveReminderMinutes: (Int) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestExactAlarmPermission: () -> Unit,
) {
    var input by remember { mutableStateOf(reminderMinutes.toString()) }
    var saved by remember { mutableStateOf(false) }
    val parsed = input.toIntOrNull()
    val valid = parsed != null && parsed in
        NotificationPreferences.MIN_REMINDER_MINUTES..NotificationPreferences.MAX_REMINDER_MINUTES

    LaunchedEffect(reminderMinutes) {
        input = reminderMinutes.toString()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VibrantBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = "Session reminders",
            color = VibrantText,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = "Choose when to receive reminders for sessions saved in Favorites.",
            color = VibrantMuted,
            style = MaterialTheme.typography.bodyMedium,
        )

        SettingsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Notifications, contentDescription = null, tint = IngOrange)
                Text(
                    text = "Reminder timing",
                    modifier = Modifier.padding(start = 10.dp),
                    color = VibrantText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = "The same interval is used before a session starts and after it ends.",
                color = VibrantMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = input,
                onValueChange = {
                    input = it.filter(Char::isDigit).take(4)
                    saved = false
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Minutes") },
                supportingText = {
                    Text(
                        if (valid) {
                            "Notify $parsed minutes before and $parsed minutes after"
                        } else {
                            "Enter a value from 1 to 1440 minutes"
                        },
                    )
                },
                isError = input.isNotEmpty() && !valid,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            Button(
                onClick = {
                    onSaveReminderMinutes(parsed ?: return@Button)
                    saved = true
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = valid,
                colors = ButtonDefaults.buttonColors(containerColor = IngOrange),
            ) {
                if (saved) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (saved) "Saved" else "Save reminder time", fontWeight = FontWeight.Bold)
            }
        }

        PermissionCard(
            title = "Notifications",
            description = if (notificationsAllowed) {
                "Allowed. Session reminders can appear on this device."
            } else {
                "Permission is required to show session reminders."
            },
            allowed = notificationsAllowed,
            buttonLabel = "Allow notifications",
            onClick = onRequestNotificationPermission,
        )

        PermissionCard(
            title = "Precise delivery",
            description = if (exactAlarmsAllowed) {
                "Allowed. Android can deliver reminders at the selected time."
            } else {
                "Without this access Android may delay reminders to save battery."
            },
            allowed = exactAlarmsAllowed,
            buttonLabel = "Allow precise reminders",
            onClick = onRequestExactAlarmPermission,
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = VibrantSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, VibrantBorder),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    allowed: Boolean,
    buttonLabel: String,
    onClick: () -> Unit,
) {
    SettingsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (allowed) Icons.Filled.CheckCircle else Icons.Outlined.Info,
                contentDescription = null,
                tint = if (allowed) IngOrange else VibrantMuted,
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(title, color = VibrantText, fontWeight = FontWeight.Bold)
                Text(description, color = VibrantMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (!allowed) {
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VibrantSurfaceWarm,
                    contentColor = IngOrange,
                ),
            ) {
                Text(buttonLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}
