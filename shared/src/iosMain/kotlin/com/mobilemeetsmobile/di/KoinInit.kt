package com.mobilemeetsmobile.di

import com.mobilemeetsmobile.presentation.detail.SessionDetailViewModel
import com.mobilemeetsmobile.presentation.schedule.ScheduleViewModel
import com.mobilemeetsmobile.presentation.speakers.SpeakersViewModel
import com.mobilemeetsmobile.presentation.application.ApplicationStatusViewModel
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module

object KoinInit {
    private var koinApp: KoinApplication? = null

    fun start(): KoinApplication {
        if (koinApp == null) {
            koinApp = startKoin {
                modules(sharedModule, platformModule())
            }
        }
        return koinApp!!
    }

    // Backward compatible alias for older Swift calls that map `init` to `doInit`.
    fun init(): KoinApplication = start()

    fun getScheduleViewModel(): ScheduleViewModel = start().koin.get()

    fun getSpeakersViewModel(): SpeakersViewModel = start().koin.get()

    fun getSessionDetailViewModel(): SessionDetailViewModel = start().koin.get()

    fun getApplicationStatusViewModel(): ApplicationStatusViewModel = start().koin.get()

fun modules(): List<Module> = listOf(sharedModule, platformModule())
}
