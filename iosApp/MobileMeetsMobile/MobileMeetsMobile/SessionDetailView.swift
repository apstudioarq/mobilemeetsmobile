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
        VStack(alignment: .leading, spacing: 16) {
            Text("Reserve your seat")
                .font(.title3.weight(.black))
                .foregroundStyle(Color.vibrantText)
            Text("Space is limited. Add to favorites to sync with your schedule.")
                .foregroundStyle(Color.vibrantMuted)
            Button {
                wrapper.viewModel.onBookmarkToggle()
            } label: {
                Label(session.isBookmarked ? "Saved" : "Add to Favorites", systemImage: "heart.fill")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(OrangeButtonStyle())

            Button(action: {}) {
                Label("Share Session", systemImage: "square.and.arrow.up")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .foregroundStyle(Color.vibrantText)
                    .background(Color.vibrantSurface)
                    .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.vibrantBorder))
            }
        }
        .padding(24)
        .background(Color.vibrantSurface)
        .clipShape(RoundedRectangle(cornerRadius: 6))
        .overlay(RoundedRectangle(cornerRadius: 6).stroke(Color.vibrantBorder))
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
                    ForEach(0..<4, id: \.self) { _ in Image(systemName: "star.fill").foregroundStyle(Color.ingOrange) }
                    Image(systemName: "star").foregroundStyle(Color.vibrantBorder)
                }
                Text("Additional comments (optional)")
                    .font(.headline)
                    .foregroundStyle(Color.vibrantText)
                TextEditor(text: .constant("What did you like or what could be improved?"))
                    .foregroundStyle(Color.vibrantMuted)
                    .frame(height: 86)
                    .padding(4)
                    .background(Color.vibrantWarm)
                    .overlay(RoundedRectangle(cornerRadius: 4).stroke(Color.vibrantBorder))
                Button(action: {}) {
                    Text("Submit Feedback")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .foregroundStyle(Color.white)
                        .background(Color.vibrantBrown)
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                }
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
                Avatar(name: speaker.name, size: 58)
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
