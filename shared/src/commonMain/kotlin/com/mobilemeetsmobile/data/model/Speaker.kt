package com.mobilemeetsmobile.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Speaker(
    val id: String,
    val name: String,
    val role: String,
    val company: String,
    val bio: String,
    val photoUrl: String,
    val socialLinks: Map<String, String> = emptyMap(),
)
