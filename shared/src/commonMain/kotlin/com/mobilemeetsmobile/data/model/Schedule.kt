package com.mobilemeetsmobile.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ConferenceDay(
    val dayNumber: Int,
    val label: String,
    val date: String,
)

@Serializable
data class TimeSlot(
    val time: String,
    val sessions: List<Session>,
)

@Serializable
data class Schedule(
    val days: List<ConferenceDay>,
    val timeSlots: List<TimeSlot>,
)

@Serializable
data class BookmarkRequest(
    val sessionId: String,
    val userId: String,
)

@Serializable
data class BookmarkResponse(
    val sessionId: String,
    val isBookmarked: Boolean,
)
