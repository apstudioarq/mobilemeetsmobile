package com.mobilemeetsmobile.data.model

data class MapContent(
    val imageBase64: String = "",
    val imageMimeType: String = "",
    val imageUrl: String = "",
) {
    val hasImage: Boolean
        get() = imageBase64.isNotBlank() || imageUrl.isNotBlank()
}
