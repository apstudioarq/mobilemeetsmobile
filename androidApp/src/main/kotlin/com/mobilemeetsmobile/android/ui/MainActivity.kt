package com.mobilemeetsmobile.android.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.mobilemeetsmobile.android.notifications.SessionNotificationScheduler
import com.mobilemeetsmobile.android.ui.navigation.AppNavigation
import com.mobilemeetsmobile.android.ui.lock.ApplicationLockedScreen
import com.mobilemeetsmobile.android.ui.theme.MobileMeetsMobileTheme
import com.mobilemeetsmobile.android.ui.splash.SplashScreen
import com.mobilemeetsmobile.presentation.application.ApplicationStatusViewModel
import kotlinx.coroutines.delay
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val applicationStatusViewModel: ApplicationStatusViewModel by inject()
    private val sessionIdToOpen = mutableStateOf<String?>(null)
    private val notificationsAllowed = mutableStateOf(false)
    private val exactAlarmsAllowed = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionIdToOpen.value = intent.notificationSessionId()
        updatePermissionStates()
        enableEdgeToEdge()
        setContent {
            MobileMeetsMobileTheme {
                var showSplash by remember { mutableStateOf(true) }
                val applicationStatus by applicationStatusViewModel.uiState.collectAsState()
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { granted -> notificationsAllowed.value = granted }
                LaunchedEffect(Unit) {
                    delay(900)
                    showSplash = false
                    while (true) {
                        delay(APPLICATION_STATUS_REFRESH_MILLIS)
                        applicationStatusViewModel.refresh()
                    }
                }
                LaunchedEffect(applicationStatus.isLocked) {
                    if (applicationStatus.isLocked) {
                        SessionNotificationScheduler(applicationContext).cancelAll()
                        sessionIdToOpen.value = null
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background,
                ) {
                    if (showSplash || applicationStatus.isChecking) {
                        SplashScreen()
                    } else if (applicationStatus.isLocked) {
                        ApplicationLockedScreen(applicationStatus.message)
                    } else {
                        AppNavigation(
                            sessionIdToOpen = sessionIdToOpen.value,
                            onSessionIdConsumed = { sessionIdToOpen.value = null },
                            notificationsAllowed = notificationsAllowed.value,
                            exactAlarmsAllowed = exactAlarmsAllowed.value,
                            onRequestNotificationPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            onRequestExactAlarmPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    startActivity(
                                        Intent(
                                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                            Uri.parse("package:$packageName"),
                                        ),
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sessionIdToOpen.value = if (applicationStatusViewModel.uiState.value.isLocked) {
            null
        } else {
            intent.notificationSessionId()
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStates()
        applicationStatusViewModel.refresh()
    }

    private fun updatePermissionStates() {
        notificationsAllowed.value = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        exactAlarmsAllowed.value = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    private fun Intent.notificationSessionId(): String? =
        getStringExtra(SessionNotificationScheduler.EXTRA_SESSION_ID)

    private companion object {
        const val APPLICATION_STATUS_REFRESH_MILLIS = 30_000L
    }
}
