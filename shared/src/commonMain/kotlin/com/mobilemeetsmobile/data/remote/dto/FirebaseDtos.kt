package com.mobilemeetsmobile.data.remote.dto

import com.mobilemeetsmobile.data.model.Level
import com.mobilemeetsmobile.data.model.SessionType
import com.mobilemeetsmobile.data.model.Track
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class FirebaseConferenceDto(
    val id: String = "",
    val title: String = "",
    val audience: String = "",
    val eventType: String = "",
    val organizingCountry: String = "",
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
    val startDate: Long = 0,
    val tags: List<String> = emptyList(),
    val technology: String = "",
    val type: String = "",
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
                room.presentations.flatMap { it.cleanPresenters() }
            }
        }
        .distinctBy { speakerIdFor(it) }
        .sortedBy { it }
        .map { presenter ->
            SpeakerDto(
                id = speakerIdFor(presenter),
                name = presenter,
                role = "Presenter",
                company = "",
                bio = "",
                photoUrl = "",
                socialLinks = emptyMap(),
            )
        }
}

private fun Map<String, FirebaseConferenceDto>.selectedConferences(
    selectedConferenceId: String?,
): List<FirebaseConferenceDto> {
    if (selectedConferenceId != null) {
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
    val presenterIds = cleanPresenters().map(::speakerIdFor)
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
