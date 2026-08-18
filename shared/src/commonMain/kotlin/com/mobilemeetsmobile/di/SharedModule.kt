package com.mobilemeetsmobile.di

import com.mobilemeetsmobile.data.local.LocalDataSource
import com.mobilemeetsmobile.data.remote.MobileMeetsMobileApi
import com.mobilemeetsmobile.data.remote.HttpClientFactory
import com.mobilemeetsmobile.data.remote.auth.FirebaseAuthClient
import com.mobilemeetsmobile.data.remote.auth.FirebaseAuthSessionStore
import com.mobilemeetsmobile.data.remote.auth.FirebaseIdTokenProvider
import com.mobilemeetsmobile.data.repository.HomeContentRepository
import com.mobilemeetsmobile.data.repository.MapContentRepository
import com.mobilemeetsmobile.data.repository.ConnectionStateRepository
import com.mobilemeetsmobile.data.repository.SessionRepository
import com.mobilemeetsmobile.data.repository.SpeakerRepository
import com.mobilemeetsmobile.data.repository.RatingRepository
import com.mobilemeetsmobile.domain.usecase.*
import com.mobilemeetsmobile.presentation.detail.SessionDetailViewModel
import com.mobilemeetsmobile.presentation.schedule.ScheduleViewModel
import com.mobilemeetsmobile.presentation.speakers.SpeakersViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

val sharedModule = module {
    // Network
    single { HttpClientFactory.create() }

    // Local
    single { LocalDataSource(get()) }
    single<FirebaseAuthSessionStore> { get<LocalDataSource>() }

    // Firebase authentication
    single<FirebaseIdTokenProvider> { FirebaseAuthClient(get(), get()) }
    single { MobileMeetsMobileApi(get(), get()) }

    // Repositories
    single { ConnectionStateRepository() }
    single { HomeContentRepository(get(), get()) }
    single { MapContentRepository(get()) }
    single { SessionRepository(get(), get(), get()) }
    single { SpeakerRepository(get(), get()) }
    single { RatingRepository(get()) }

    // Use Cases
    factory { GetScheduleUseCase(get()) }
    factory { GetAllSessionsUseCase(get()) }
    factory { GetSessionDetailUseCase(get()) }
    factory { SearchSessionsUseCase(get()) }
    factory { ToggleBookmarkUseCase(get()) }
    factory { GetBookmarksUseCase(get()) }
    factory { GetHomeContentUseCase(get()) }
    factory { GetMapContentUseCase(get()) }
    factory { GetSpeakersUseCase(get()) }
    factory { SubmitFeedbackUseCase(get()) }

    // ViewModels
    factory { ScheduleViewModel(get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { SessionDetailViewModel(get(), get(), get(), get()) }
    factory { SpeakersViewModel(get()) }
}

// Platform-specific modules will provide DatabaseDriverFactory
expect fun platformModule(): Module
