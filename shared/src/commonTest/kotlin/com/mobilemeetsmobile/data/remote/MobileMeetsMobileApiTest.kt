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
    fun loadsPublicApplicationLockFlag() = runBlocking {
        BackendConfig.configureFirebaseRealtimeDatabase(
            databaseUrl = "https://example.firebaseio.com",
            apiKey = "test-api-key",
        )
        val engine = MockEngine { request ->
            assertEquals("/test/config/appLocked.json", request.url.encodedPath)
            assertEquals(null, request.url.parameters["auth"])
            respond(
                content = "true",
                status = HttpStatusCode.OK,
                headers = jsonHeaders,
            )
        }
        val api = createApi(engine)

        assertEquals(true, api.getApplicationLocked())
    }

    @Test
    fun missingApplicationLockFlagDefaultsToUnlocked() = runBlocking {
        BackendConfig.configureFirebaseRealtimeDatabase(
            databaseUrl = "https://example.firebaseio.com",
            apiKey = "test-api-key",
        )
        val engine = MockEngine {
            respond(
                content = "null",
                status = HttpStatusCode.OK,
                headers = jsonHeaders,
            )
        }
        val api = createApi(engine)

        assertEquals(false, api.getApplicationLocked())
    }

    @Test
    fun loadsMapContentFromDedicatedFirebaseNode() = runBlocking {
        BackendConfig.configureFirebaseRealtimeDatabase(
            databaseUrl = "https://example.firebaseio.com",
            apiKey = "test-api-key",
        )
        val engine = MockEngine { request ->
            assertEquals("id-token", request.url.parameters["auth"])
            assertEquals("/test/map.json", request.url.encodedPath)
            respond(
                content = """
                    {
                      "imageBase64": "map-image-data",
                      "imageMimeType": "image/png"
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = jsonHeaders,
            )
        }
        val api = createApi(engine)

        val content = api.getMapContent()

        assertEquals("map-image-data", content.imageBase64)
        assertEquals("image/png", content.imageMimeType)
    }

    @Test
    fun loadsHomeContentFromDedicatedFirebaseNode() = runBlocking {
        BackendConfig.configureFirebaseRealtimeDatabase(
            databaseUrl = "https://example.firebaseio.com",
            apiKey = "test-api-key",
        )
        val requestedPaths = mutableListOf<String>()
        val engine = MockEngine { request ->
            assertEquals("id-token", request.url.parameters["auth"])
            requestedPaths += request.url.encodedPath
            when (request.url.encodedPath) {
                "/test/home.json" -> respond(
                    content = """
                        {
                          "title": "Custom Home",
                          "description": "Loaded from admin",
                          "image": "base64-image",
                          "imageMimeType": "image/png"
                        }
                    """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders,
                )

                else -> error("Unexpected request: ${request.url}")
            }
        }
        val api = createApi(engine)

        val content = api.getHomeContent()

        assertEquals(listOf("/test/home.json"), requestedPaths)
        assertEquals("Custom Home", content.title)
        assertEquals("Loaded from admin", content.description)
        assertEquals("base64-image", content.imageBase64)
        assertEquals("image/png", content.imageMimeType)
    }

    @Test
    fun fallsBackToConferenceHomeContentWhenDedicatedNodeIsEmpty() = runBlocking {
        BackendConfig.configureFirebaseRealtimeDatabase(
            databaseUrl = "https://example.firebaseio.com",
            apiKey = "test-api-key",
        )
        val engine = MockEngine { request ->
            assertEquals("id-token", request.url.parameters["auth"])
            when (request.url.encodedPath) {
                "/test/home.json" -> respond(
                    content = "null",
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders,
                )

                "/test/conferences.json" -> respond(
                    content = conferencePayload,
                    status = HttpStatusCode.OK,
                    headers = jsonHeaders,
                )

                else -> error("Unexpected request: ${request.url}")
            }
        }
        val api = createApi(engine)

        val content = api.getHomeContent()

        assertEquals("Mobile Meets Mobile '26", content.title)
    }

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
        val api = createApi(engine)

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
        fun createApi(engine: MockEngine): MobileMeetsMobileApi {
            val client = HttpClient(engine) {
                install(ContentNegotiation) {
                    json(Json { encodeDefaults = true })
                }
            }
            return MobileMeetsMobileApi(
                client = client,
                firebaseIdTokenProvider = object : FirebaseIdTokenProvider {
                    override suspend fun getIdToken(forceRefresh: Boolean): String = "id-token"
                },
            )
        }

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
