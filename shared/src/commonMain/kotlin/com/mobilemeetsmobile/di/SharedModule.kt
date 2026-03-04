package com.mobilemeetsmobile.di

import com.mobilemeetsmobile.data.local.LocalDataSource
import com.mobilemeetsmobile.data.remote.MobileMeetsMobileApi
import com.mobilemeetsmobile.data.remote.HttpClientFactory
import com.mobilemeetsmobile.data.repository.SessionRepository
import com.mobilemeetsmobile.data.repository.SpeakerRepository
import com.mobilemeetsmobile.domain.usecase.*
import com.mobilemeetsmobile.presentation.detail.SessionDetailViewModel
import com.mobilemeetsmobile.presentation.schedule.ScheduleViewModel
import com.mobilemeetsmobile.presentation.speakers.SpeakersViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

val sharedModule = module {
    // Network
    single { HttpClientFactory.create() }
    single { MobileMeetsMobileApi(get()) }

    // Local
    single { LocalDataSource(get()) }

    // Repositories
    single { SessionRepository(get(), get()) }
    single { SpeakerRepository(get(), get()) }

    // Use Cases
    factory { GetScheduleUseCase(get()) }
    factory { GetSessionDetailUseCase(get()) }
    factory { SearchSessionsUseCase(get()) }
    factory { ToggleBookmarkUseCase(get()) }
    factory { GetBookmarksUseCase(get()) }
    factory { GetSpeakersUseCase(get()) }

    // ViewModels
    factory { ScheduleViewModel(get(), get(), get(), get()) }
    factory { SessionDetailViewModel(get(), get(), get()) }
    factory { SpeakersViewModel(get()) }
}

// Platform-specific modules will provide DatabaseDriverFactory
expect fun platformModule(): Module
