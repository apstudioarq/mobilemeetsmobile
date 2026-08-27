package com.ingevent.data.repository

import com.ingevent.data.remote.INGEventApi

class RatingRepository(
    private val api: INGEventApi,
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
