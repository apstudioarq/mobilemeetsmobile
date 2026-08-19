package com.mobilemeetsmobile.data.remote

import io.ktor.client.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object HttpClientFactory {
    fun create(): HttpClient = HttpClient {
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 30_000
            socketTimeoutMillis = 30_000
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = false
                encodeDefaults = true
            })
        }
        defaultRequest {
            BackendConfig.anonKeyOrNull?.let { anonKey ->
                header("apikey", anonKey)
                header(HttpHeaders.Authorization, "Bearer $anonKey")
            }
        }
        install(Logging) {
            level = LogLevel.INFO
            sanitizeHeader { headerName ->
                headerName.equals(HttpHeaders.Authorization, ignoreCase = true) ||
                    headerName.equals("apikey", ignoreCase = true)
            }
            logger = object : Logger {
                override fun log(message: String) {
                    println("HTTP: ${message.redactFirebaseParameters()}")
                }
            }
        }
    }
}

internal fun String.redactFirebaseParameters(): String {
    return replace(Regex("([?&](?:auth|key)=)[^&\\s,\\]]+"), "$1<redacted>")
}

internal fun Throwable.redactedMessage(fallback: String): String {
    return message?.redactFirebaseParameters() ?: fallback
}
