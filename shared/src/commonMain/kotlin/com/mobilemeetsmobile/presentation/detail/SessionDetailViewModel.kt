package com.mobilemeetsmobile.presentation.detail

import com.mobilemeetsmobile.data.model.Session
import com.mobilemeetsmobile.data.model.Speaker
import com.mobilemeetsmobile.domain.usecase.GetSessionDetailUseCase
import com.mobilemeetsmobile.domain.usecase.GetSpeakersUseCase
import com.mobilemeetsmobile.domain.usecase.ToggleBookmarkUseCase
import com.mobilemeetsmobile.domain.usecase.SubmitFeedbackUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionDetailUiState(
    val session: Session? = null,
    val speakers: List<Speaker> = emptyList(),
    val relatedSessions: List<Session> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val feedbackRating: Int = 0,
    val feedbackComment: String = "",
    val isSubmittingFeedback: Boolean = false,
    val feedbackSubmitted: Boolean = false,
    val feedbackError: String? = null,
)

class SessionDetailViewModel(
    private val getSessionDetail: GetSessionDetailUseCase,
    private val getSpeakers: GetSpeakersUseCase,
    private val toggleBookmark: ToggleBookmarkUseCase,
    private val submitFeedbackUseCase: SubmitFeedbackUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow(SessionDetailUiState())
    val uiState: StateFlow<SessionDetailUiState> = _uiState.asStateFlow()

    fun loadSession(sessionId: String) {
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                feedbackRating = 0,
                feedbackComment = "",
                isSubmittingFeedback = false,
                feedbackSubmitted = false,
                feedbackError = null,
            )
        }
        scope.launch {
            try {
                val session = getSessionDetail(sessionId)
                val allSpeakers = getSpeakers().first()
                val sessionSpeakers = allSpeakers.filter { it.id in session.speakerIds }

                _uiState.update {
                    it.copy(
                        session = session,
                        speakers = sessionSpeakers,
                        isLoading = false,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message)
                }
            }
        }
    }

    fun onBookmarkToggle() {
        val session = _uiState.value.session ?: return
        toggleBookmark(session.id)
        _uiState.update {
            it.copy(session = session.copy(isBookmarked = !session.isBookmarked))
        }
    }

    fun onRatingSelected(rating: Int) {
        if (rating !in 1..5 || !_uiState.value.canEditFeedback) return
        _uiState.update {
            it.copy(
                feedbackRating = rating,
                feedbackSubmitted = false,
                feedbackError = null,
            )
        }
    }

    fun onFeedbackCommentChanged(comment: String) {
        if (!_uiState.value.canEditFeedback) return
        _uiState.update {
            it.copy(
                feedbackComment = comment.take(MAX_FEEDBACK_COMMENT_LENGTH),
                feedbackSubmitted = false,
                feedbackError = null,
            )
        }
    }

    fun submitFeedback() {
        val state = _uiState.value
        val session = state.session ?: return
        if (state.feedbackRating !in 1..5 || state.isSubmittingFeedback) return

        _uiState.update {
            it.copy(
                isSubmittingFeedback = true,
                feedbackSubmitted = false,
                feedbackError = null,
            )
        }
        scope.launch {
            try {
                submitFeedbackUseCase(
                    session.id,
                    session.title,
                    state.feedbackRating,
                    state.feedbackComment,
                )
                _uiState.update {
                    it.copy(
                        isSubmittingFeedback = false,
                        feedbackSubmitted = true,
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmittingFeedback = false,
                        feedbackError = error.message ?: "Unable to submit feedback.",
                    )
                }
            }
        }
    }

    private val SessionDetailUiState.canEditFeedback: Boolean
        get() = !isSubmittingFeedback && !feedbackSubmitted

    private companion object {
        const val MAX_FEEDBACK_COMMENT_LENGTH = 2_000
    }
}
