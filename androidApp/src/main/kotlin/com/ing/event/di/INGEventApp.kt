package com.ing.event.di

import android.app.Application
import com.ing.event.BuildConfig
import com.ing.event.notifications.SessionNotificationScheduler
import com.ingevent.data.remote.BackendConfig
import com.ingevent.di.platformModule
import com.ingevent.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class INGEventApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SessionNotificationScheduler(this).createNotificationChannel()
        if (BuildConfig.FIREBASE_DATABASE_URL.isNotBlank() || BuildConfig.FIREBASE_FUNCTIONS_URL.isBlank()) {
            BackendConfig.configureFirebaseRealtimeDatabase(
                databaseUrl = BuildConfig.FIREBASE_DATABASE_URL.ifBlank {
                    "https://ingtechrating-default-rtdb.europe-west1.firebasedatabase.app"
                },
                conferenceId = BuildConfig.FIREBASE_CONFERENCE_ID,
                apiKey = BuildConfig.FIREBASE_API_KEY,
            )
        } else if (BuildConfig.FIREBASE_FUNCTIONS_URL.isNotBlank()) {
            BackendConfig.configureFirebaseFunctions(
                functionsBaseUrl = BuildConfig.FIREBASE_FUNCTIONS_URL,
            )
        } else {
            BackendConfig.configureSupabase(
                projectUrl = BuildConfig.SUPABASE_URL,
                anonKey = BuildConfig.SUPABASE_ANON_KEY,
            )
        }
        startKoin {
            androidLogger()
            androidContext(this@INGEventApp)
            modules(sharedModule, platformModule())
        }
    }
}
