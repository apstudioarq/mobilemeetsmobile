package com.mobilemeetsmobile.data.model

data class HomeContent(
    val welcomeMessage: String = DEFAULT_WELCOME_MESSAGE,
    val heroImageUrl: String = "",
)

const val DEFAULT_WELCOME_MESSAGE =
    "Welcome to Mobile Meets Mobile. Explore the agenda, discover live sessions, and make the event your own."
