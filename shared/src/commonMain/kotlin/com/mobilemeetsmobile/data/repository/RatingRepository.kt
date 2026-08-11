package com.mobilemeetsmobile.data.repository

import com.mobilemeetsmobile.data.remote.MobileMeetsMobileApi

class RatingRepository(
    private val api: MobileMeetsMobileApi,
) {
    suspend fun submitFeedback(
        sessionId: String,
        sessionTitle: String,
        rating: Int,
        comment: String,
    ) {
        api.submitFeedback(
            sessionId = sessionId,
            sessionTitle = sessionTitle,
            rating = rating,
            comment = comment,
        )
    }
}
