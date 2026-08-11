package com.mobilemeetsmobile.android.di

import android.app.Application
import com.mobilemeetsmobile.android.BuildConfig
import com.mobilemeetsmobile.data.remote.BackendConfig
import com.mobilemeetsmobile.di.platformModule
import com.mobilemeetsmobile.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MobileMeetsMobileApp : Application() {
    override fun onCreate() {
        super.onCreate()
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
            androidContext(this@MobileMeetsMobileApp)
            modules(sharedModule, platformModule())
        }
    }
}
