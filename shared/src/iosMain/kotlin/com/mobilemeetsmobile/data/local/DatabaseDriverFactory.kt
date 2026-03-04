package com.mobilemeetsmobile.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = MobileMeetsMobileDatabase.Schema,
            name = "mobilemeetsmobile.db"
        )
    }
}
