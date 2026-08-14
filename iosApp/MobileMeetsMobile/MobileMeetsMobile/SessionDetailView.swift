import SwiftUI
import shared
import Combine

final class SessionDetailViewModelWrapper: ObservableObject {
    let viewModel: SessionDetailViewModel
    @Published var state: SessionDetailUiState
    private var cancellable: AnyCancellable?

    init(sessionId: String) {
        viewModel = KoinInit.shared.getSessionDetailViewModel()
        state = viewModel.uiState.value as! SessionDetailUiState
        viewModel.loadSession(sessionId: sessionId)
        cancellable = Timer.publish(every: 0.1, on: .main, in: .common)
            .autoconnect()
            .sink { [weak self] _ in
                guard let self, let next = self.viewModel.uiState.value as? SessionDetailUiState else { return }
                self.state = next
            }
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
            HStack(spacing: 6) {
                Button(action: { dismiss() }) {
                    Image(systemName: "chevron.left")
                    Text("Back to Schedule")
                }
                .font(.subheadline.weight(.bold))
                .foregroundStyle(Color.vibrantMuted)
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
                    VStack(alignment: .leading, spacing: 28) {
                        heroImage
                        VStack(alignment: .leading, spacing: 12) {
                            HStack(spacing: 8) {
                                Badge(text: session.type.displayName, color: .ingOrange)
                                Badge(text: session.track.displayName, color: trackColor(session.track))
                            }
                            Text(session.title)
                                .font(.system(size: 38, weight: .black))
                                .foregroundStyle(Color.vibrantText)
                            MetaLine(systemImage: "calendar", text: "October 24, 2024 • \(displayTimeRange(session))")
                            MetaLine(systemImage: "mappin", text: session.room)
                        }

                        Rectangle()
                            .fill(Color.vibrantBorder)
                            .frame(height: 1)

                        VStack(alignment: .leading, spacing: 18) {
                            Text("About this session")
                                .font(.title.weight(.black))
                                .foregroundStyle(Color.vibrantText)
                            Text(session.description)
                                .font(.body)
                                .lineSpacing(5)
                                .foregroundStyle(Color.vibrantMuted)
                            Text("Attendees will learn practical frameworks for implementing rigid grid philosophies, balancing density with whitespace, and replacing heavy shadows with subtle tonal elevation strategies.")
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
                    .padding(22)
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
        ZStack {
            Color(hex: 0x061015)
            Rectangle()
                .fill(
                    LinearGradient(
                        colors: [.clear, .ingOrange, Color(hex: 0x18DDF2), .clear],
                        startPoint: .leading,
                        endPoint: .trailing
                    )
                )
                .frame(height: 16)
            Circle()
                .fill(
                    RadialGradient(
                        colors: [Color(hex: 0xA9FCFF), Color(hex: 0x1FB9E0).opacity(0.35), .clear],
                        center: .center,
                        startRadius: 8,
                        endRadius: 72
                    )
                )
                .frame(width: 130, height: 130)
        }
        .frame(height: 180)
        .clipShape(RoundedRectangle(cornerRadius: 6))
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
                    .foregroundStyle(Color.white)
                    .background(isBookmarked ? Color.vibrantText : Color.ingOrange)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
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
        VStack(alignment: .leading, spacing: 18) {
            HStack(spacing: 16) {
                SpeakerAvatar(speaker: speaker, size: 58)
                VStack(alignment: .leading) {
                    Text(speaker.name)
                        .font(.headline.weight(.bold))
                        .foregroundStyle(Color.vibrantText)
                    Text("\(speaker.role), \(speaker.company)")
                        .font(.caption)
                        .foregroundStyle(Color.vibrantMuted)
                }
            }
            Text(speaker.bio)
                .foregroundStyle(Color.vibrantMuted)
            Button(action: onProfileTap) {
                Text("View full profile ->")
                    .font(.headline.weight(.bold))
                    .foregroundStyle(Color.ingOrange)
            }
        }
        .padding(24)
        .background(Color.vibrantSurface)
        .clipShape(RoundedRectangle(cornerRadius: 6))
        .overlay(RoundedRectangle(cornerRadius: 6).stroke(Color.vibrantBorder))
    }
}
