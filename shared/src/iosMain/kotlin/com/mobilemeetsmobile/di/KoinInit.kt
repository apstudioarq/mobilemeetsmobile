package com.mobilemeetsmobile.di

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin

object KoinInit {
    fun init(): KoinApplication {
        return startKoin {
            modules(sharedModule, platformModule())
        }
    }
}
