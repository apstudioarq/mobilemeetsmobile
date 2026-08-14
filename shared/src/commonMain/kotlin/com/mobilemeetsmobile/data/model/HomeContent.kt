package com.mobilemeetsmobile.data.model

data class HomeContent(
    val title: String = DEFAULT_HOME_TITLE,
    val description: String = DEFAULT_HOME_DESCRIPTION,
    val imageBase64: String = "",
    val imageMimeType: String = "",
    val imageUrl: String = "",
) {
    val welcomeMessage: String
        get() = description

    val heroImageUrl: String
        get() = imageUrl
}

const val DEFAULT_HOME_TITLE = "Mobile Meets Mobile"

const val DEFAULT_HOME_DESCRIPTION =
    "Welcome to Mobile Meets Mobile. Explore the agenda, discover live sessions, and make the event your own."

const val DEFAULT_WELCOME_MESSAGE =
    DEFAULT_HOME_DESCRIPTION
