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
        BackendConfig.configureSupabase(
            projectUrl = BuildConfig.SUPABASE_URL,
            anonKey = BuildConfig.SUPABASE_ANON_KEY,
        )
        startKoin {
            androidLogger()
            androidContext(this@MobileMeetsMobileApp)
            modules(sharedModule, platformModule())
        }
    }
}
