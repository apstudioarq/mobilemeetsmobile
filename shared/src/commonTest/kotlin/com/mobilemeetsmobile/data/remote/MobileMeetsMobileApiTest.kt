package com.mobilemeetsmobile.data.remote

import com.mobilemeetsmobile.data.remote.auth.FirebaseIdTokenProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.content.OutgoingContent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class MobileMeetsMobileApiTest {
    @Test
    fun submitsAuthenticatedSessionFeedbackUsingFirebasePush() = runBlocking {
        BackendConfig.configureFirebaseRealtimeDatabase(
            databaseUrl = "https://example.firebaseio.com",
            apiKey = "test-api-key",
        )
        var submittedBody = ""
        val engine = MockEngine { request ->
            assertEquals("id-token", request.url.parameters["auth"])
            when (request.url.encodedPath) {
                "/test/conferences.json" -> respond(
                    content = conferencePayload,
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders,
                )

                "/ratings.json" -> {
                    assertEquals(HttpMethod.Post, request.method)
                    submittedBody = (request.body as OutgoingContent.ByteArrayContent)
                        .bytes()
                        .decodeToString()
                    respond(
                        content = """{"name":"-new-rating"}""",
                        status = HttpStatusCode.OK,
                        headers = jsonHeaders,
                    )
                }

                else -> error("Unexpected request: ${request.url}")
            }
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { encodeDefaults = true })
            }
        }
        val api = MobileMeetsMobileApi(
            client = client,
            firebaseIdTokenProvider = object : FirebaseIdTokenProvider {
                override suspend fun getIdToken(forceRefresh: Boolean): String = "id-token"
            },
        )

        api.submitFeedback(
            sessionId = "session-1",
            sessionTitle = "Kotlin Everywhere",
            rating = 5,
            comment = "  Excellent session  ",
        )

        val submitted = Json.parseToJsonElement(submittedBody).jsonObject
        assertEquals("Tech BE", submitted.getValue("audience").jsonPrimitive.content)
        assertEquals("Belgium", submitted.getValue("country").jsonPrimitive.content)
        assertEquals("Townhall", submitted.getValue("event").jsonPrimitive.content)
        assertEquals("Mobile Meets Mobile '26", submitted.getValue("name").jsonPrimitive.content)
        assertEquals("session-1", submitted.getValue("sessionId").jsonPrimitive.content)
        assertEquals("Kotlin Everywhere", submitted.getValue("sessionTitle").jsonPrimitive.content)
        assertEquals("Excellent session", submitted.getValue("comment").jsonPrimitive.content)
        assertEquals("5", submitted.getValue("rating").jsonPrimitive.content)
    }

    private companion object {
        val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        val conferencePayload = """
            {
              "conference-1": {
                "id": "conference-1",
                "title": "Mobile Meets Mobile '26",
                "audience": "Tech BE",
                "eventType": "Townhall",
                "organizingCountry": "Belgium",
                "rooms": [
                  {
                    "id": "room-1",
                    "name": "Main room",
                    "presentations": [
                      {
                        "id": "session-1",
                        "title": "Kotlin Everywhere"
                      }
                    ]
                  }
                ]
              }
            }
        """.trimIndent()
    }
}
