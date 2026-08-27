import SwiftUI
import shared
import Combine

final class SessionDetailViewModelWrapper: ObservableObject {
    let viewModel: SessionDetailViewModel
    @Published var state: SessionDetailUiState
    private var observation: StateObservation?

    init(sessionId: String) {
        viewModel = KoinInit.shared.getSessionDetailViewModel()
        state = viewModel.uiState.value as! SessionDetailUiState
        observation = viewModel.observeState { [weak self] next in
            self?.state = next
        }
        viewModel.loadSession(sessionId: sessionId)
    }

    deinit {
        observation?.cancel()
        viewModel.onCleared()
    }
}

struct SessionDetailView: View {
    let sessionId: String
    @Environment(\.dismiss) private var dismiss
    @StateObject private var wrapper: SessionDetailViewModelWrapper
    @State private var selectedSpeakerId: String?

    init(sessionId: String) {
        self.sessionId = sessionId
        _wrapper = StateObject(wrappedValue: SessionDetailViewModelWrapper(sessionId: sessionId))
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Button(action: { dismiss() }) {
                    Image(systemName: "xmark")
                }
                .font(.headline.weight(.bold))
                .foregroundStyle(Color.ingOrange)
                Spacer()
            }
            .padding(.horizontal, 18)
            .padding(.vertical, 12)
            .background(Color.vibrantSurface)

            Rectangle()
                .fill(Color.vibrantBorder)
                .frame(height: 1)

            if wrapper.state.isLoading || wrapper.state.session == nil {
                ProgressView()
                    .tint(.ingOrange)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(Color.vibrantBackground)
            } else if let session = wrapper.state.session {
                ScrollView {
                    VStack(alignment: .leading, spacing: 22) {
                        heroImage
                        VStack(alignment: .leading, spacing: 12) {
                            HStack(spacing: 8) {
                                Badge(text: session.type.displayName, color: .ingOrange)
                                Badge(text: session.track.displayName, color: trackColor(session.track))
                            }
                            Text(session.title)
                                .font(.system(size: 34, weight: .semibold))
                                .foregroundStyle(Color.vibrantText)
                            MetaLine(systemImage: "calendar", text: "\(displayFullDate(session.startTime)) • \(displayTimeRange(session))")
                            MetaLine(systemImage: "mappin", text: session.room)
                        }

                        Rectangle()
                            .fill(Color.vibrantBorder)
                            .frame(height: 1)

                        VStack(alignment: .leading, spacing: 18) {
                            Text("About this session")
                                .font(.title.weight(.black))
                                .foregroundStyle(Color.vibrantText)
                            Text(session.description_)
                                .font(.body)
                                .lineSpacing(5)
                                .foregroundStyle(Color.vibrantMuted)
                        }

                        reserveCard(session: session)

                        VStack(alignment: .leading, spacing: 16) {
                            Text("Speaker")
                                .font(.title.weight(.black))
                                .foregroundStyle(Color.vibrantText)
                            ForEach(wrapper.state.speakers, id: \.id) { speaker in
                                SpeakerProfileCard(speaker: speaker) {
                                    selectedSpeakerId = speaker.id
                                }
                            }
                        }

                        feedbackCard
                    }
                    .padding(16)
                    .padding(.bottom, 40)
                }
                .background(Color.vibrantBackground)
            }
        }
        .navigationBarBackButtonHidden(true)
        .navigationDestination(item: $selectedSpeakerId) { speakerId in
            SpeakerProfileView(speakerId: speakerId)
        }
    }

    private var heroImage: some View {
        HStack {
            VStack(alignment: .leading, spacing: 8) {
                Text("ING EVENT")
                    .font(.caption.weight(.bold))
                    .tracking(1.4)
                    .foregroundStyle(Color.white.opacity(0.82))
                Text("Ideas in motion.")
                    .font(.title.weight(.semibold))
                    .foregroundStyle(Color.white)
            }
            Spacer()
            Image(systemName: "mic.fill")
                .font(.system(size: 30, weight: .semibold))
                .foregroundStyle(Color.white)
                .frame(width: 72, height: 72)
                .background(Color.ingPurple, in: Circle())
        }
        .padding(22)
        .frame(maxWidth: .infinity)
        .frame(height: 164)
        .background(Color.ingOrange)
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }

    private func reserveCard(session: Session) -> some View {
        let isBookmarked = session.isBookmarked

        return VStack(alignment: .leading, spacing: 18) {
            HStack(alignment: .top, spacing: 14) {
                ZStack {
                    RoundedRectangle(cornerRadius: 8)
                        .fill(Color.ingOrange.opacity(0.12))
                    Image(systemName: "heart.fill")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundStyle(Color.ingOrange)
                }
                .frame(width: 46, height: 46)

                VStack(alignment: .leading, spacing: 6) {
                    Text("Save this session")
                        .font(.title3.weight(.black))
                        .foregroundStyle(Color.vibrantText)
                    Text(
                        isBookmarked
                            ? "This talk is in your Saved Sessions."
                            : "Keep this talk handy and sync it with your schedule."
                    )
                    .foregroundStyle(Color.vibrantMuted)
                }

                Spacer(minLength: 12)

                Text(isBookmarked ? "Saved" : "Not saved")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(isBookmarked ? Color.ingOrange : Color.vibrantMuted)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 7)
                    .background(
                        isBookmarked
                            ? Color.ingOrange.opacity(0.12)
                            : Color.vibrantWarm
                    )
                    .clipShape(Capsule())
                    .overlay(
                        Capsule()
                            .stroke(isBookmarked ? Color.ingOrange.opacity(0.55) : Color.vibrantBorder)
                    )
            }

            Button {
                wrapper.viewModel.onBookmarkToggle()
            } label: {
                Label(isBookmarked ? "Saved Session" : "Save Session", systemImage: "heart.fill")
                    .font(.headline.weight(.bold))
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .foregroundStyle(isBookmarked ? Color.ingOrange : Color.white)
                    .background(isBookmarked ? Color.vibrantWarm : Color.ingOrange)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                    .overlay(
                        RoundedRectangle(cornerRadius: 8)
                            .stroke(
                                isBookmarked ? Color.ingOrange.opacity(0.7) : Color.clear,
                                lineWidth: 1
                            )
                    )
            }
            .buttonStyle(.plain)
        }
        .padding(20)
        .background(
            LinearGradient(
                colors: [
                    Color.vibrantSurface,
                    isBookmarked ? Color.ingOrange.opacity(0.06) : Color.vibrantSurface
                ],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        )
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .overlay(
            RoundedRectangle(cornerRadius: 8)
                .stroke(isBookmarked ? Color.ingOrange.opacity(0.55) : Color.vibrantBorder)
        )
    }

    private var feedbackCard: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Rate this session")
                .font(.title.weight(.black))
                .foregroundStyle(Color.vibrantText)
            VStack(alignment: .leading, spacing: 14) {
                Text("How was your experience with this session?")
                    .foregroundStyle(Color.vibrantMuted)
                HStack(spacing: 2) {
                    ForEach(1...5, id: \.self) { star in
                        Button {
                            wrapper.viewModel.onRatingSelected(rating: Int32(star))
                        } label: {
                            Image(
                                systemName: star <= Int(wrapper.state.feedbackRating)
                                    ? "star.fill"
                                    : "star"
                            )
                            .font(.system(size: 28))
                            .foregroundStyle(
                                star <= Int(wrapper.state.feedbackRating)
                                    ? Color.ingOrange
                                    : Color.vibrantBorder
                            )
                            .frame(width: 42, height: 42)
                        }
                        .buttonStyle(.plain)
                        .disabled(
                            wrapper.state.isSubmittingFeedback || wrapper.state.feedbackSubmitted
                        )
                        .accessibilityLabel("\(star) stars")
                    }
                }
                Text("Additional comments (optional)")
                    .font(.headline)
                    .foregroundStyle(Color.vibrantText)
                ZStack(alignment: .topLeading) {
                    if wrapper.state.feedbackComment.isEmpty {
                        Text("What did you like or what could be improved?")
                            .foregroundStyle(Color.vibrantMuted)
                            .padding(.horizontal, 9)
                            .padding(.vertical, 12)
                            .allowsHitTesting(false)
                    }
                    TextEditor(
                        text: Binding(
                            get: { wrapper.state.feedbackComment },
                            set: { wrapper.viewModel.onFeedbackCommentChanged(comment: $0) }
                        )
                    )
                    .scrollContentBackground(.hidden)
                    .foregroundStyle(Color.vibrantText)
                    .disabled(
                        wrapper.state.isSubmittingFeedback || wrapper.state.feedbackSubmitted
                    )
                }
                .frame(height: 96)
                .padding(4)
                .background(Color.vibrantWarm)
                .overlay(RoundedRectangle(cornerRadius: 4).stroke(Color.vibrantBorder))

                if let error = wrapper.state.feedbackError {
                    Text(error)
                        .font(.footnote)
                        .foregroundStyle(Color.red)
                } else if wrapper.state.feedbackSubmitted {
                    Text("Thanks! Your feedback was submitted.")
                        .font(.subheadline.weight(.bold))
                        .foregroundStyle(Color.green)
                }

                Button(action: { wrapper.viewModel.submitFeedback() }) {
                    Group {
                        if wrapper.state.isSubmittingFeedback {
                            ProgressView()
                                .tint(.white)
                        } else {
                            Text(wrapper.state.feedbackSubmitted ? "Feedback Submitted" : "Submit Feedback")
                                .font(.headline)
                        }
                    }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .foregroundStyle(Color.white)
                        .background(Color.vibrantBrown)
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                }
                .disabled(
                    wrapper.state.feedbackRating == 0 ||
                        wrapper.state.isSubmittingFeedback ||
                        wrapper.state.feedbackSubmitted
                )
                .opacity(wrapper.state.feedbackRating == 0 ? 0.55 : 1)
            }
            .padding(24)
            .background(Color.vibrantSurface)
            .clipShape(RoundedRectangle(cornerRadius: 6))
            .overlay(RoundedRectangle(cornerRadius: 6).stroke(Color.vibrantBorder))
        }
    }
}

struct MetaLine: View {
    let systemImage: String
    let text: String

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: systemImage)
                .foregroundStyle(Color.vibrantBrown)
                .frame(width: 20)
            Text(text)
                .foregroundStyle(Color.vibrantMuted)
        }
        .font(.body)
    }
}

struct SpeakerProfileCard: View {
    let speaker: Speaker
    let onProfileTap: () -> Void

    var body: some View {
        Button(action: onProfileTap) {
            HStack(spacing: 12) {
                SpeakerAvatar(speaker: speaker, size: 52)
                VStack(alignment: .leading, spacing: 3) {
                    Text(speaker.name)
                        .font(.headline.weight(.bold))
                        .foregroundStyle(Color.vibrantText)
                    Text(speaker.role)
                        .font(.caption)
                        .foregroundStyle(Color.vibrantMuted)
                        .lineLimit(1)
                }
                Spacer(minLength: 8)
                Image(systemName: "chevron.right")
                    .font(.headline.weight(.bold))
                    .foregroundStyle(Color.ingOrange)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .background(Color.vibrantSurface)
            .clipShape(RoundedRectangle(cornerRadius: 6))
            .overlay(RoundedRectangle(cornerRadius: 6).stroke(Color.vibrantBorder))
        }
        .buttonStyle(.plain)
    }
}
