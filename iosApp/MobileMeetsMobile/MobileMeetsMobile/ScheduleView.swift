import SwiftUI
import shared
import Combine

final class ScheduleViewModelWrapper: ObservableObject {
    let viewModel: ScheduleViewModel
    @Published var state: ScheduleUiState
    private var cancellable: AnyCancellable?

    init() {
        viewModel = KoinInit.shared.getScheduleViewModel()
        state = viewModel.uiState.value as! ScheduleUiState
        cancellable = Timer.publish(every: 0.1, on: .main, in: .common)
            .autoconnect()
            .sink { [weak self] _ in
                guard let self, let next = self.viewModel.uiState.value as? ScheduleUiState else { return }
                self.state = next
            }
    }

    deinit {
        cancellable?.cancel()
        viewModel.onCleared()
    }
}

final class SpeakersViewModelWrapper: ObservableObject {
    let viewModel: SpeakersViewModel
    @Published var state: SpeakersUiState
    private var cancellable: AnyCancellable?

    init() {
        viewModel = KoinInit.shared.getSpeakersViewModel()
        state = viewModel.uiState.value as! SpeakersUiState
        cancellable = Timer.publish(every: 0.1, on: .main, in: .common)
            .autoconnect()
            .sink { [weak self] _ in
                guard let self, let next = self.viewModel.uiState.value as? SpeakersUiState else { return }
                self.state = next
            }
    }
}

struct HomeView: View {
    @StateObject private var schedule = ScheduleViewModelWrapper()
    @StateObject private var speakers = SpeakersViewModelWrapper()
    @State private var selectedSessionId: String?
    var onScheduleTap: () -> Void = {}

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                EventTopBar(section: "Home")
                ScrollView {
                    VStack(alignment: .leading, spacing: 28) {
                        hero
                        SectionHeader(title: "Live Now", action: "View Schedule", onAction: onScheduleTap)
                        ForEach(Array(schedule.state.sessions.prefix(2)), id: \.id) { session in
                            LiveCard(session: session, speakers: speakers.state.speakers) {
                                selectedSessionId = session.id
                            }
                        }
                        SectionHeader(title: "Keynote Speakers")
                        ForEach(Array(speakers.state.speakers.prefix(3)), id: \.id) { speaker in
                            SpeakerRow(speaker: speaker)
                        }
                    }
                    .padding(20)
                    .padding(.bottom, 70)
                }
                .background(Color.vibrantBackground)
            }
            .navigationDestination(item: $selectedSessionId) { sessionId in
                SessionDetailView(sessionId: sessionId)
            }
        }
    }

    private var hero: some View {
        VStack(spacing: 0) {
            VStack(alignment: .leading, spacing: 24) {
                Text("Global Summit 2024")
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(Color.vibrantMuted)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 7)
                    .background(Color.vibrantWarm, in: Capsule())

                Text("Mobile\nMeets\nMobile.")
                    .font(.system(size: 42, weight: .black))
                    .foregroundStyle(Color.vibrantText)

                Text("The convergence of enterprise mobility, next-gen 5G architectures, and the future of connected experiences.")
                    .font(.system(size: 18))
                    .lineSpacing(5)
                    .foregroundStyle(Color.vibrantMuted)

                Button(action: {}) {
                    Label("Join Stream", systemImage: "play.circle")
                        .font(.headline)
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(OrangeButtonStyle())
            }
            .padding(.horizontal, 42)
            .padding(.vertical, 44)

            ZStack {
                Color(hex: 0x30302F)
                Circle()
                    .fill(
                        RadialGradient(
                            colors: [Color(hex: 0xC8FFFF), Color(hex: 0x61DDE4).opacity(0.45), .clear],
                            center: .center,
                            startRadius: 8,
                            endRadius: 110
                        )
                    )
                    .frame(width: 210, height: 210)
                Rectangle()
                    .fill(Color(hex: 0x72E9F1).opacity(0.35))
                    .frame(height: 1)
            }
            .frame(height: 300)
        }
        .background(Color.vibrantSurface)
        .clipShape(RoundedRectangle(cornerRadius: 6))
        .overlay(RoundedRectangle(cornerRadius: 6).stroke(Color.vibrantBorder))
    }
}

struct ScheduleView: View {
    @StateObject private var wrapper = ScheduleViewModelWrapper()
    @State private var selectedSessionId: String?

    private var scheduleCategoryTracks: [Track] {
        allCategoryTracks.filter { track in
            wrapper.state.allSessions.contains { session in
                session.day == wrapper.state.selectedDay && session.track == track
            }
        }
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                EventTopBar(section: "Schedule")
                ScrollView {
                    VStack(alignment: .leading, spacing: 24) {
                        Text("Schedule")
                            .font(.system(size: 30, weight: .black))
                            .foregroundStyle(Color.vibrantText)
                            .padding(.top, 18)
                        Text("The premier gathering for mobile innovators.")
                            .font(.subheadline)
                            .foregroundStyle(Color.vibrantMuted)
                            .padding(.bottom, 12)

                        if let error = wrapper.state.error, wrapper.state.sessions.isEmpty {
                            FirebaseLoadError(message: error) {
                                wrapper.viewModel.loadDay(day: wrapper.state.selectedDay)
                            }
                        }

                        dayTabs

                        CategoryFilter(
                            tracks: scheduleCategoryTracks,
                            selectedTrack: wrapper.state.selectedTrack,
                            onTrackSelected: { track in
                                wrapper.viewModel.selectTrack(track: track)
                            }
                        )
                        .padding(.bottom, 10)

                        if !wrapper.state.isLoading && wrapper.state.sessions.isEmpty && wrapper.state.error == nil {
                            Text("No sessions match this category.")
                                .font(.body)
                                .foregroundStyle(Color.vibrantMuted)
                        }

                        ForEach(wrapper.state.timeSlots.sorted { $0.key < $1.key }, id: \.key) { time, sessions in
                            Text(displayClock(time))
                                .font(.headline.weight(.bold))
                                .foregroundStyle(Color.vibrantMuted)
                                .padding(.top, 28)

                            ForEach(sessions, id: \.id) { session in
                                VibrantSessionCard(session: session) {
                                    selectedSessionId = session.id
                                } onBookmark: {
                                    wrapper.viewModel.onBookmarkToggle(sessionId: session.id)
                                }
                            }
                        }
                    }
                    .padding(.horizontal, 24)
                    .padding(.bottom, 90)
                }
                .background(Color.vibrantBackground)
            }
            .navigationDestination(item: $selectedSessionId) { sessionId in
                SessionDetailView(sessionId: sessionId)
            }
        }
    }

    private var dayTabs: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 10) {
                ForEach(wrapper.state.days, id: \.dayNumber) { day in
                    Button {
                        wrapper.viewModel.loadDay(day: day.dayNumber)
                    } label: {
                        VStack(spacing: 12) {
                            Text(day.date)
                                .font(.headline.weight(.bold))
                            Rectangle()
                                .frame(width: 96, height: 2)
                        }
                        .foregroundStyle(wrapper.state.selectedDay == day.dayNumber ? Color.ingOrange : Color.vibrantMuted)
                    }
                }
            }
        }
    }
}

private struct FirebaseLoadError: View {
    let message: String
    let retry: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(message)
                .font(.callout)
                .foregroundStyle(Color.vibrantMuted)
            Button("Retry", action: retry)
                .buttonStyle(.borderedProminent)
                .tint(Color.ingOrange)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

struct FavoritesView: View {
    @StateObject private var wrapper = ScheduleViewModelWrapper()
    @State private var selectedSessionId: String?

    var savedSessions: [Session] {
        Array(wrapper.state.allSessions.filter { session in
            session.isBookmarked ||
            session.tags.contains(where: { $0.caseInsensitiveCompare("Saved") == .orderedSame })
        })
    }

    var categoryTracks: [Track] {
        allCategoryTracks.filter { track in
            savedSessions.contains { $0.track == track }
        }
    }

    var filteredSavedSessions: [Session] {
        savedSessions.filter { session in
            wrapper.state.selectedTrack == nil || session.track == wrapper.state.selectedTrack
        }
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                EventTopBar(section: "Favorites")
                ScrollView {
                    VStack(alignment: .leading, spacing: 24) {
                        Text("Saved Sessions")
                            .font(.system(size: 30, weight: .black))
                            .foregroundStyle(Color.vibrantText)
                            .padding(.top, 18)
                        Text("Your personalized schedule. These are the talks and workshops you've marked as high priority.")
                            .font(.subheadline)
                            .lineSpacing(3)
                            .foregroundStyle(Color.vibrantMuted)
                            .padding(.bottom, 10)

                        CategoryFilter(
                            tracks: categoryTracks,
                            selectedTrack: wrapper.state.selectedTrack,
                            onTrackSelected: { track in
                                wrapper.viewModel.selectTrack(track: track)
                            }
                        )
                        .padding(.bottom, 8)

                        if filteredSavedSessions.isEmpty {
                            Text("No saved sessions match this category.")
                                .font(.body)
                                .foregroundStyle(Color.vibrantMuted)
                        }

                        ForEach(filteredSavedSessions, id: \.id) { session in
                            VibrantSessionCard(session: session) {
                                selectedSessionId = session.id
                            } onBookmark: {
                                wrapper.viewModel.onBookmarkToggle(sessionId: session.id)
                            }
                        }
                    }
                    .padding(.horizontal, 24)
                    .padding(.bottom, 90)
                }
                .background(Color.vibrantBackground)
            }
            .navigationDestination(item: $selectedSessionId) { sessionId in
                SessionDetailView(sessionId: sessionId)
            }
        }
    }
}

struct VibrantSessionCard: View {
    let session: Session
    let onTap: () -> Void
    let onBookmark: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 13) {
                HStack(alignment: .top) {
                    HStack(spacing: 6) {
                        Badge(text: session.type.displayName, color: .ingOrange)
                        Badge(text: session.track.displayName, color: trackColor(session.track))
                    }
                    Spacer()
                    Button(action: onBookmark) {
                        Image(systemName: (session.isBookmarked || session.tags.contains(where: { $0.caseInsensitiveCompare("Saved") == .orderedSame })) ? "star.fill" : "star")
                            .foregroundStyle(Color.vibrantBrown)
                    }
                }
                Text(session.title)
                    .font(.title2.weight(.black))
                    .foregroundStyle(Color.vibrantText)
                    .multilineTextAlignment(.leading)
                Text(session.description)
                    .font(.body)
                    .lineLimit(3)
                    .foregroundStyle(Color.vibrantMuted)
                Rectangle()
                    .fill(Color.vibrantBorder)
                    .frame(height: 1)
                Text(displayTimeRange(session))
                    .font(.headline.weight(.bold))
                    .foregroundStyle(Color.vibrantBrown)
                Text(session.room.uppercased())
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Color.vibrantMuted)
            }
            .padding(24)
            .background(Color.vibrantSurface)
            .clipShape(RoundedRectangle(cornerRadius: 6))
            .overlay(RoundedRectangle(cornerRadius: 6).stroke(Color.vibrantBorder))
        }
        .buttonStyle(.plain)
    }
}

struct LiveCard: View {
    let session: Session
    let speakers: [Speaker]
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 14) {
                HStack {
                    Text("● LIVE")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(Color(hex: 0xEA4335))
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .background(Color(hex: 0xFFD8D8), in: Capsule())
                    Spacer()
                    Text(session.room)
                        .font(.caption.weight(.bold))
                        .foregroundStyle(Color.vibrantSoftMuted)
                }
                Text(session.title)
                    .font(.title2.weight(.black))
                    .foregroundStyle(Color.vibrantText)
                    .multilineTextAlignment(.leading)
                Text(session.description)
                    .font(.body)
                    .lineLimit(2)
                    .foregroundStyle(Color.vibrantMuted)
                Text(speakerNames)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Color.vibrantMuted)
            }
            .padding(22)
            .background(Color.vibrantSurface)
            .clipShape(RoundedRectangle(cornerRadius: 6))
            .overlay(RoundedRectangle(cornerRadius: 6).stroke(Color.vibrantBorder))
        }
        .buttonStyle(.plain)
    }

    private var speakerNames: String {
        speakers.filter { session.speakerIds.contains($0.id) }.map(\.name).joined(separator: ", ")
    }
}

struct SpeakerRow: View {
    let speaker: Speaker

    var body: some View {
        HStack(spacing: 18) {
            Avatar(name: speaker.name, size: 58)
            VStack(alignment: .leading, spacing: 3) {
                Text(speaker.name)
                    .font(.headline.weight(.bold))
                    .foregroundStyle(Color.vibrantText)
                Text("\(speaker.role), \(speaker.company)")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Color.vibrantBrown)
            }
            Spacer()
        }
        .padding(.horizontal, 24)
        .padding(.vertical, 20)
        .background(Color.vibrantSurface)
        .clipShape(RoundedRectangle(cornerRadius: 6))
        .overlay(RoundedRectangle(cornerRadius: 6).stroke(Color.vibrantBorder))
    }
}

struct SectionHeader: View {
    let title: String
    var action: String?
    var onAction: (() -> Void)?

    var body: some View {
        VStack(spacing: 14) {
            HStack(alignment: .bottom) {
                Text(title)
                    .font(.title.weight(.black))
                    .foregroundStyle(Color.vibrantText)
                Spacer()
                if let action {
                    Button(action: { onAction?() }) {
                        Text(action)
                            .font(.headline.weight(.bold))
                            .foregroundStyle(Color.vibrantBrown)
                    }
                    .buttonStyle(.plain)
                }
            }
            Rectangle()
                .fill(Color.vibrantBorder)
                .frame(height: 1)
        }
    }
}

private let allCategoryTracks: [Track] = [
    .aiMl,
    .android,
    .ios,
    .generic,
    .web,
    .cloud,
    .firebase,
    .flutter,
    .design,
]

struct CategoryFilter: View {
    let tracks: [Track]
    let selectedTrack: Track?
    let onTrackSelected: (Track?) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Category")
                .font(.headline.weight(.bold))
                .foregroundStyle(Color.vibrantMuted)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    CategoryChip(
                        label: "All",
                        isSelected: selectedTrack == nil,
                        onTap: { onTrackSelected(nil) }
                    )
                    ForEach(tracks, id: \.self) { track in
                        CategoryChip(
                            label: track.displayName,
                            isSelected: selectedTrack == track,
                            onTap: { onTrackSelected(track) }
                        )
                    }
                }
            }
        }
    }
}

private struct CategoryChip: View {
    let label: String
    let isSelected: Bool
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            Text(label)
                .font(.headline.weight(.bold))
                .foregroundStyle(isSelected ? Color.vibrantBackground : Color.vibrantText)
                .padding(.horizontal, 16)
                .padding(.vertical, 9)
                .background(isSelected ? Color.ingOrange : Color.vibrantSurface)
                .clipShape(Capsule())
                .overlay(
                    Capsule()
                        .stroke(isSelected ? Color.ingOrange : Color.vibrantBorder)
                )
        }
        .buttonStyle(.plain)
    }
}

struct Badge: View {
    let text: String
    let color: Color

    var body: some View {
        Text(text)
            .font(.caption.weight(.bold))
            .foregroundStyle(color)
            .padding(.horizontal, 10)
            .padding(.vertical, 5)
            .background(Color.vibrantWarm, in: Capsule())
    }
}

struct Avatar: View {
    let name: String
    let size: CGFloat

    var body: some View {
        RoundedRectangle(cornerRadius: 10)
            .fill(
                RadialGradient(
                    colors: [avatarColor(name), Color(hex: 0x111111)],
                    center: .center,
                    startRadius: 4,
                    endRadius: size
                )
            )
            .frame(width: size, height: size)
            .overlay(
                Text(initials(name))
                    .font(.headline.weight(.black))
                    .foregroundStyle(Color.white)
            )
    }
}

struct OrangeButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .padding(.vertical, 13)
            .background(Color.ingOrange.opacity(configuration.isPressed ? 0.75 : 1))
            .foregroundStyle(Color.white)
            .clipShape(RoundedRectangle(cornerRadius: 8))
    }
}

func displayClock(_ iso: String) -> String {
    let time = String(iso.split(separator: "T").last?.prefix(5) ?? "")
    let parts = time.split(separator: ":")
    guard let hour = Int(parts.first ?? "") else { return iso }
    let minute = parts.count > 1 ? String(parts[1]) : "00"
    let suffix = hour >= 12 ? "PM" : "AM"
    let hour12 = hour == 0 ? 12 : (hour > 12 ? hour - 12 : hour)
    return String(format: "%02d:%@ %@", hour12, minute, suffix)
}

func displayTimeRange(_ session: Session) -> String {
    "\(displayClock(session.startTime)) - \(displayClock(session.endTime))"
}

func initials(_ name: String) -> String {
    name.split(separator: " ").compactMap { $0.first }.map(String.init).joined()
}

func avatarColor(_ seed: String) -> Color {
    let colors: [Color] = [.ingOrange, .trackPurple, .trackGreen, .trackBlue, .vibrantBrown]
    return colors[abs(seed.hashValue) % colors.count]
}

func trackColor(_ track: Track) -> Color {
    switch track {
    case .aiMl: return .trackBlue
    case .android: return .trackGreen
    case .web: return .trackYellow
    case .cloud: return .trackRed
    case .firebase: return .ingOrange
    case .flutter: return Color(hex: 0x42A5F5)
    case .design: return .trackPurple
    default: return .ingOrange
    }
}
