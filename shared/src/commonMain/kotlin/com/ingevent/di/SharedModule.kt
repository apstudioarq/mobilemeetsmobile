package com.ingevent.di

import com.ingevent.data.local.LocalDataSource
import com.ingevent.data.remote.INGEventApi
import com.ingevent.data.remote.HttpClientFactory
import com.ingevent.data.remote.auth.FirebaseAuthClient
import com.ingevent.data.remote.auth.FirebaseAuthSessionStore
import com.ingevent.data.remote.auth.FirebaseIdTokenProvider
import com.ingevent.data.repository.HomeContentRepository
import com.ingevent.data.repository.MapContentRepository
import com.ingevent.data.repository.ConnectionStateRepository
import com.ingevent.data.repository.SessionRepository
import com.ingevent.data.repository.SpeakerRepository
import com.ingevent.data.repository.RatingRepository
import com.ingevent.data.repository.ApplicationStatusRepository
import com.ingevent.domain.usecase.*
import com.ingevent.presentation.detail.SessionDetailViewModel
import com.ingevent.presentation.schedule.ScheduleViewModel
import com.ingevent.presentation.speakers.SpeakersViewModel
import com.ingevent.presentation.application.ApplicationStatusViewModel
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
    single { INGEventApi(get(), get()) }

    // Repositories
    single { ConnectionStateRepository() }
    single { HomeContentRepository(get(), get()) }
    single { MapContentRepository(get()) }
    single { SessionRepository(get(), get(), get()) }
    single { SpeakerRepository(get(), get()) }
    single { RatingRepository(get()) }
    single { ApplicationStatusRepository(get(), get()) }

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
    factory { ApplicationStatusViewModel(get()) }
}

// Platform-specific modules will provide DatabaseDriverFactory
expect fun platformModule(): Module
