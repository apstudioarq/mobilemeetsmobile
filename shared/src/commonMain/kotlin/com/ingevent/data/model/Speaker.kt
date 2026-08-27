package com.ingevent.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Speaker(
    val id: String,
    val name: String,
    val role: String,
    val company: String,
    val bio: String,
    val photoUrl: String,
    val photoBase64: String = "",
    val photoMimeType: String = "",
    val socialLinks: Map<String, String> = emptyMap(),
) {
    val photoImageSource: String
        get() {
            val cleanBase64 = photoBase64.trim()
            return when {
                cleanBase64.startsWith("data:") -> cleanBase64
                cleanBase64.isNotBlank() -> "data:${photoMimeType.ifBlank { "image/*" }};base64,$cleanBase64"
                else -> photoUrl
            }
        }
}
