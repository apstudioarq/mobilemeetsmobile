package com.ingevent.data.model

data class HomeContent(
    val title: String = DEFAULT_HOME_TITLE,
    val description: String = DEFAULT_HOME_DESCRIPTION,
    val imageBase64: String = "",
    val imageMimeType: String = "",
    val imageUrl: String = "",
)

const val DEFAULT_HOME_TITLE = "ING Event"

const val DEFAULT_HOME_DESCRIPTION =
    "Welcome to ING Event. Explore the agenda, discover live sessions, and make the event your own."
