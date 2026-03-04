@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.mobilemeetsmobile.data.remote.dto

import com.mobilemeetsmobile.data.model.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class SessionDto(
    val id: String,
    val title: String,
    val description: String,
    @JsonNames("start_time")
    val startTime: String,
    @JsonNames("end_time")
    val endTime: String,
    val duration: String,
    val room: String,
    val day: Int,
    val track: String,
    val type: String,
    val level: String,
    @JsonNames("speaker_ids")
    val speakerIds: List<String>,
    val capacity: Int,
    val registered: Int,
    val tags: List<String> = emptyList(),
    @JsonNames("livestream_url")
    val livestreamUrl: String? = null,
    @JsonNames("slides_url")
    val slidesUrl: String? = null,
) {
    fun toDomain(isBookmarked: Boolean = false): Session = Session(
        id = id,
        title = title,
        description = description,
        startTime = startTime,
        endTime = endTime,
        duration = duration,
        room = room,
        day = day,
        track = Track.valueOf(track),
        type = SessionType.valueOf(type),
        level = Level.valueOf(level),
        speakerIds = speakerIds,
        capacity = capacity,
        registered = registered,
        tags = tags,
        livestreamUrl = livestreamUrl,
        slidesUrl = slidesUrl,
        isBookmarked = isBookmarked,
    )
}

@Serializable
data class SpeakerDto(
    val id: String,
    val name: String,
    val role: String,
    val company: String,
    val bio: String,
    @JsonNames("photo_url")
    val photoUrl: String,
    @JsonNames("social_links")
    val socialLinks: Map<String, String> = emptyMap(),
) {
    fun toDomain(): Speaker = Speaker(
        id = id,
        name = name,
        role = role,
        company = company,
        bio = bio,
        photoUrl = photoUrl,
        socialLinks = socialLinks,
    )
}

@Serializable
data class SessionsResponse(
    val sessions: List<SessionDto>,
)

@Serializable
data class SpeakersResponse(
    val speakers: List<SpeakerDto>,
)
