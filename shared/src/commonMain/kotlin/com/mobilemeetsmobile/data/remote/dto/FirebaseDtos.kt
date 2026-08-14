@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.mobilemeetsmobile.data.remote.dto

import com.mobilemeetsmobile.data.model.DEFAULT_WELCOME_MESSAGE
import com.mobilemeetsmobile.data.model.HomeContent
import com.mobilemeetsmobile.data.model.Level
import com.mobilemeetsmobile.data.model.SessionType
import com.mobilemeetsmobile.data.model.Track
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class FirebaseConferenceDto(
    val id: String = "",
    val title: String = "",
    val audience: String = "",
    val eventType: String = "",
    val organizingCountry: String = "",
    @JsonNames("welcome_message")
    val welcomeMessage: String = "",
    @JsonNames("hero_image_url", "welcome_image_url")
    val heroImageUrl: String = "",
    val rooms: List<FirebaseRoomDto> = emptyList(),
    val startDate: Long = 0,
)

@Serializable
data class FirebaseRoomDto(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val presentations: List<FirebasePresentationDto> = emptyList(),
)

@Serializable
data class FirebasePresentationDto(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val durationMinutes: Int = 0,
    val presenters: List<String> = emptyList(),
    @JsonNames("presenterProfiles", "presenter_profiles")
    val speakers: List<FirebaseSpeakerDto> = emptyList(),
    val startDate: Long = 0,
    val tags: List<String> = emptyList(),
    val technology: String = "",
    val type: String = "",
)

@Serializable
data class FirebaseSpeakerDto(
    val name: String = "",
    val role: String = "",
    val company: String = "",
    val bio: String = "",
    @JsonNames("photo_url")
    val photoUrl: String = "",
    @JsonNames("photo_base64")
    val photoBase64: String = "",
    @JsonNames("photo_mime_type")
    val photoMimeType: String = "",
    @JsonNames("social_links")
    val socialLinks: Map<String, String> = emptyMap(),
)

@Serializable
data class FirebaseRatingDto(
    val audience: String = "",
    val country: String = "",
    val date: String = "",
    val event: String = "",
    val name: String = "",
    val rating: Int = 0,
)

@Serializable
data class FirebaseRatingSubmissionDto(
    val audience: String,
    val country: String,
    val date: String,
    val event: String,
    val name: String,
    val rating: Int,
    val sessionId: String,
    val sessionTitle: String,
    val comment: String,
)

fun Map<String, FirebaseConferenceDto>.toSessionDtos(
    selectedConferenceId: String? = null,
): List<SessionDto> {
    val conferences = selectedConferences(selectedConferenceId)

    val distinctDates = conferences
        .flatMap { conference ->
            conference.rooms.flatMap { room ->
                room.presentations.mapNotNull { presentation ->
                    presentation.startDate.takeIf { it > 0 }?.let(::dateKey)
                }
            }
        }
        .distinct()
        .sorted()

    return conferences
        .flatMap { conference ->
            conference.rooms.flatMap { room ->
                room.presentations.map { presentation ->
                    presentation.toSessionDto(
                        conference = conference,
                        room = room,
                        day = distinctDates.indexOf(dateKey(presentation.startDate)).let { index ->
                            if (index >= 0) index + 1 else 1
                        },
                    )
                }
            }
        }
        .sortedWith(compareBy<SessionDto> { it.day }.thenBy { it.startTime })
}

fun Map<String, FirebaseConferenceDto>.toSpeakerDtos(
    selectedConferenceId: String? = null,
): List<SpeakerDto> {
    return selectedConferences(selectedConferenceId)
        .flatMap { conference ->
            conference.rooms.flatMap { room ->
                room.presentations.flatMap { it.cleanSpeakers() }
            }
        }
        .groupBy { speakerIdFor(it.name) }
        .map { (speakerId, speakers) ->
            speakerId to speakers.maxByOrNull { it.completenessScore() }!!
        }
        .sortedBy { (_, speaker) -> speaker.name }
        .map { (speakerId, speaker) ->
            SpeakerDto(
                id = speakerId,
                name = speaker.name,
                role = speaker.role.ifBlank { "Presenter" },
                company = speaker.company,
                bio = speaker.bio,
                photoUrl = speaker.photoUrl,
                photoBase64 = speaker.photoBase64,
                photoMimeType = speaker.photoMimeType,
                socialLinks = speaker.socialLinks,
            )
        }
}

fun Map<String, FirebaseConferenceDto>.toHomeContent(
    selectedConferenceId: String? = null,
): HomeContent {
    val conference = selectedConferences(selectedConferenceId).firstOrNull()
    return HomeContent(
        welcomeMessage = conference
            ?.welcomeMessage
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?: DEFAULT_WELCOME_MESSAGE,
        heroImageUrl = conference
            ?.heroImageUrl
            ?.trim()
            .orEmpty(),
    )
}

private fun Map<String, FirebaseConferenceDto>.selectedConferences(
    selectedConferenceId: String?,
): List<FirebaseConferenceDto> {
    if (!selectedConferenceId.isNullOrBlank()) {
        return values.filter { it.id == selectedConferenceId }
    }

    return values
        .filter { it.title.contains("Mobile Meets Mobile", ignoreCase = true) }
        .ifEmpty { values.toList() }
}

private fun FirebasePresentationDto.toSessionDto(
    conference: FirebaseConferenceDto,
    room: FirebaseRoomDto,
    day: Int,
): SessionDto {
    val duration = durationMinutes.coerceAtLeast(0)
    val start = startDate.takeIf { it > 0 } ?: conference.startDate
    val end = start + duration.toLong() * 60
    val presenterIds = cleanSpeakers().map { speakerIdFor(it.name) }
    val cleanTags = tags.mapNotNull { it.trim().takeIf(String::isNotBlank) }

    return SessionDto(
        id = id.ifBlank { "${conference.id}-${room.id}-$start-${title.hashCode()}" },
        title = title.ifBlank { "Untitled presentation" },
        description = description,
        startTime = isoString(start),
        endTime = isoString(end),
        duration = "$duration min",
        room = room.name.ifBlank { room.description },
        day = day,
        track = technology.toTrack().name,
        type = type.toSessionType().name,
        level = Level.INTERMEDIATE.name,
        speakerIds = presenterIds,
        capacity = 0,
        registered = 0,
        tags = cleanTags,
        livestreamUrl = null,
        slidesUrl = null,
    )
}

private fun FirebasePresentationDto.cleanPresenters(): List<String> {
    return presenters.mapNotNull { it.trim().takeIf(String::isNotBlank) }
}

private fun FirebasePresentationDto.cleanSpeakers(): List<FirebaseSpeakerDto> {
    return speakers
        .mapNotNull { speaker ->
            val name = speaker.name.trim().takeIf(String::isNotBlank)
            name?.let { speaker.copy(name = it) }
        }
        .ifEmpty {
            cleanPresenters().map { presenter -> FirebaseSpeakerDto(name = presenter, role = "Presenter") }
        }
}

private fun FirebaseSpeakerDto.completenessScore(): Int {
    return listOf(
        photoBase64,
        photoUrl,
        role,
        company,
        bio,
    ).count { it.isNotBlank() }
}

private fun String.toTrack(): Track {
    val normalized = trim().lowercase()
    return when {
        normalized.contains("android") -> Track.ANDROID
        normalized == "ios" || normalized.contains("iphone") || normalized.contains("swift") -> Track.IOS
        normalized.contains("firebase") -> Track.FIREBASE
        normalized.contains("flutter") -> Track.FLUTTER
        normalized.contains("web") -> Track.WEB
        normalized.contains("cloud") -> Track.CLOUD
        normalized.contains("design") -> Track.DESIGN
        normalized.contains("ai") || normalized.contains("ml") -> Track.AI_ML
        else -> Track.GENERIC
    }
}

private fun String.toSessionType(): SessionType {
    val normalized = trim().lowercase()
    return when {
        normalized.contains("keynote") -> SessionType.KEYNOTE
        normalized.contains("workshop") -> SessionType.WORKSHOP
        normalized.contains("codelab") -> SessionType.CODELAB
        normalized.contains("office") -> SessionType.OFFICE_HOURS
        else -> SessionType.SESSION
    }
}

private fun speakerIdFor(name: String): String {
    return name.trim()
        .lowercase()
        .map { char ->
            when {
                char.isLetterOrDigit() -> char
                else -> '-'
            }
        }
        .joinToString("")
        .replace(Regex("-+"), "-")
        .trim('-')
        .ifBlank { "unknown-presenter" }
}

private fun dateKey(epochSeconds: Long): String {
    return localDateTime(epochSeconds).date.toString()
}

private fun isoString(epochSeconds: Long): String {
    return localDateTime(epochSeconds).toString()
}

private fun localDateTime(epochSeconds: Long) =
    Instant.fromEpochSeconds(epochSeconds)
        .toLocalDateTime(TimeZone.currentSystemDefault())
