package com.mobilemeetsmobile.di

import com.mobilemeetsmobile.data.local.DatabaseDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single { DatabaseDriverFactory(get()) }
}
