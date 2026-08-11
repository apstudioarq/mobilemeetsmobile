package com.mobilemeetsmobile.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Session(
    val id: String,
    val title: String,
    val description: String,
    val startTime: String,  // ISO 8601
    val endTime: String,
    val duration: String,
    val room: String,
    val day: Int,
    val track: Track,
    val type: SessionType,
    val level: Level,
    val speakerIds: List<String>,
    val capacity: Int,
    val registered: Int,
    val tags: List<String> = emptyList(),
    val livestreamUrl: String? = null,
    val slidesUrl: String? = null,
    val isBookmarked: Boolean = false,
)

@Serializable
enum class SessionType(val displayName: String) {
    KEYNOTE("Keynote"),
    SESSION("Session"),
    WORKSHOP("Workshop"),
    CODELAB("Codelab"),
    OFFICE_HOURS("Office Hours");
}

@Serializable
enum class Level(val displayName: String) {
    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced");
}

@Serializable
enum class Track(
    val displayName: String,
    val colorHex: Long,
    val bgColorHex: Long,
) {
    AI_ML("AI/ML", 0xFF4285F4, 0xFFE8F0FE),
    ANDROID("Android", 0xFF34A853, 0xFFE6F4EA),
    IOS("iOS", 0xFF111111, 0xFFEFEFEF),
    GENERIC("Generic", 0xFF5F6368, 0xFFF1F3F4),
    WEB("Web", 0xFFFBBC04, 0xFFFEF7E0),
    CLOUD("Cloud", 0xFFEA4335, 0xFFFCE8E6),
    FIREBASE("Firebase", 0xFFFF6D00, 0xFFFFF3E0),
    FLUTTER("Flutter", 0xFF42A5F5, 0xFFE8F5E9),
    DESIGN("Design", 0xFFA142F4, 0xFFF3E8FD);
}
