import SwiftUI
import UIKit
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
                    VStack(alignment: .leading, spacing: 18) {
                        hero
                        SectionHeader(title: "Live Now", action: "View Schedule", onAction: onScheduleTap)
                        ForEach(Array(schedule.state.sessions.prefix(2)), id: \.id) { session in
                            LiveCard(session: session, speakers: speakers.state.speakers) {
                                selectedSessionId = session.id
                            }
                        }
                        SectionHeader(title: "Keynote Speakers")
                        ForEach(Array(speakers.state.speakers.prefix(3)), id: \.id) { speaker in
                            NavigationLink {
                                SpeakerProfileView(speakerId: speaker.id)
                            } label: {
                                SpeakerRow(speaker: speaker)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(16)
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
        VStack(alignment: .leading, spacing: 0) {
            VStack(alignment: .leading, spacing: 8) {
                Text("MOBILE MEETS MOBILE")
                    .font(.caption.weight(.bold))
                    .tracking(1.4)
                    .foregroundStyle(Color.white.opacity(0.84))
                Text(schedule.state.homeContent.title)
                    .font(.system(size: 32, weight: .semibold))
                    .lineLimit(2)
                    .foregroundStyle(Color.white)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(20)
            .background(Color.ingOrange)

            HeroImage(content: schedule.state.homeContent)

            HStack(spacing: 12) {
                Rectangle()
                    .fill(Color.ingPurple)
                    .frame(width: 4, height: 44)
                Text(schedule.state.homeContent.description_)
                    .font(.subheadline)
                    .lineSpacing(3)
                    .lineLimit(3)
                    .foregroundStyle(Color.vibrantMuted)
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
        }
        .background(Color.vibrantSurface)
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.vibrantBorder.opacity(0.55)))
    }
}

private struct HeroImage: View {
    let content: HomeContent

    var body: some View {
        ZStack {
            Color(hex: 0x30302F)
            if let uiImage = decodedImage {
                Image(uiImage: uiImage)
                    .resizable()
                    .scaledToFill()
            } else if let url = URL(string: content.imageUrl), !content.imageUrl.isEmpty {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFill()
                    default:
                        fallback
                    }
                }
            } else {
                fallback
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: 138)
        .clipped()
    }

    private var decodedImage: UIImage? {
        let cleanBase64 = content.imageBase64
            .components(separatedBy: "base64,")
            .last?
            .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        guard !cleanBase64.isEmpty, let data = Data(base64Encoded: cleanBase64) else {
            return nil
        }
        return UIImage(data: data)
    }

    private var fallback: some View {
        Image("SplashLogo")
            .resizable()
            .scaledToFit()
            .frame(width: 76, height: 76)
            .clipShape(RoundedRectangle(cornerRadius: 18))
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
                    VStack(alignment: .leading, spacing: 12) {
                        if let error = wrapper.state.error, wrapper.state.sessions.isEmpty {
                            FirebaseLoadError(message: error) {
                                wrapper.viewModel.loadDay(day: wrapper.state.selectedDay)
                            }
                        }

                        if wrapper.state.days.count > 1 {
                            dayTabs
                        }

                        CategoryFilter(
                            tracks: scheduleCategoryTracks,
                            selectedTrack: wrapper.state.selectedTrack,
                            onTrackSelected: { track in
                                wrapper.viewModel.selectTrack(track: track)
                            }
                        )

                        if !wrapper.state.isLoading && wrapper.state.sessions.isEmpty && wrapper.state.error == nil {
                            Text("No sessions match this category.")
                                .font(.body)
                                .foregroundStyle(Color.vibrantMuted)
                        }

                        ForEach(wrapper.state.timeSlots.sorted { $0.key < $1.key }, id: \.key) { time, sessions in
                            Text(displayClock(time))
                                .font(.headline.weight(.bold))
                                .foregroundStyle(Color.vibrantMuted)
                                .padding(.top, 6)

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
        GeometryReader { proxy in
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
                        .frame(width: 96, height: 3)
                            }
                            .foregroundStyle(wrapper.state.selectedDay == day.dayNumber ? Color.ingOrange : Color.vibrantMuted)
                        }
                    }
                }
                .frame(minWidth: proxy.size.width, alignment: .center)
            }
        }
        .frame(height: 48)
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
            VStack(alignment: .leading, spacing: 0) {
                Rectangle()
                    .fill(Color.ingOrange)
                    .frame(height: 4)
                VStack(alignment: .leading, spacing: 11) {
                HStack(alignment: .top) {
                    HStack(spacing: 6) {
                        Badge(text: session.type.displayName, color: .ingOrange)
                        Badge(text: session.track.displayName, color: trackColor(session.track))
                    }
                    Spacer()
                    Button(action: onBookmark) {
                        Image(systemName: (session.isBookmarked || session.tags.contains(where: { $0.caseInsensitiveCompare("Saved") == .orderedSame })) ? "star.fill" : "star")
                            .foregroundStyle(session.isBookmarked ? Color.ingOrange : Color.vibrantMuted)
                    }
                }
                Text(session.title)
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(Color.vibrantText)
                    .multilineTextAlignment(.leading)
                Text(session.description)
                    .font(.body)
                    .lineLimit(2)
                    .foregroundStyle(Color.vibrantMuted)
                Rectangle()
                    .fill(Color.vibrantBorder)
                    .frame(height: 1)
                HStack(spacing: 9) {
                    Image(systemName: "clock")
                        .foregroundStyle(Color.ingOrange)
                    Text(displayTimeRange(session))
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(Color.vibrantText)
                    Text("•").foregroundStyle(Color.vibrantSoftMuted)
                    Text(session.room)
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(Color.vibrantMuted)
                        .lineLimit(1)
                    Spacer()
                    Image(systemName: "chevron.right")
                        .foregroundStyle(Color.ingPurple)
                }
                }
                .padding(18)
            }
            .background(Color.vibrantSurface)
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .shadow(color: Color.black.opacity(0.12), radius: 2, y: 2)
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
            HStack(spacing: 0) {
                Rectangle()
                    .fill(Color.ingOrange)
                    .frame(width: 5)
                VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Text("● LIVE")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(Color.ingOrange)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 5)
                        .background(Color.ingOrange.opacity(0.10), in: RoundedRectangle(cornerRadius: 4))
                    Spacer()
                    Text(session.room)
                        .font(.caption.weight(.bold))
                        .foregroundStyle(Color.vibrantSoftMuted)
                }
                Text(session.title)
                    .font(.title3.weight(.semibold))
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
                .padding(18)
            }
            .background(Color.vibrantSurface)
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .shadow(color: Color.black.opacity(0.12), radius: 2, y: 2)
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
            SpeakerAvatar(speaker: speaker, size: 58)
            VStack(alignment: .leading, spacing: 3) {
                Text(speaker.name)
                    .font(.headline.weight(.bold))
                    .foregroundStyle(Color.vibrantText)
                Text(speaker.role)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Color.vibrantMuted)
                    .lineLimit(1)
            }
            Spacer()
            Image(systemName: "chevron.right")
                .font(.headline.weight(.bold))
                .foregroundStyle(Color.ingPurple)
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 12)
        .background(Color.vibrantSurface)
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .shadow(color: Color.black.opacity(0.08), radius: 1, y: 1)
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
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(Color.vibrantText)
                Spacer()
                if let action {
                    Button(action: { onAction?() }) {
                        Text(action)
                            .font(.headline.weight(.bold))
                            .foregroundStyle(Color.ingPurple)
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
            .background(color.opacity(0.10), in: RoundedRectangle(cornerRadius: 4))
    }
}

struct Avatar: View {
    let name: String
    let size: CGFloat

    var body: some View {
        Circle()
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

struct SpeakerAvatar: View {
    let speaker: Speaker
    let size: CGFloat

    var body: some View {
        if let image = imageFromBase64(speaker.photoBase64) {
            Image(uiImage: image)
                .resizable()
                .scaledToFill()
                .frame(width: size, height: size)
                .clipShape(Circle())
        } else if let url = URL(string: speaker.photoUrl), !speaker.photoUrl.isEmpty {
            AsyncImage(url: url) { phase in
                switch phase {
                case .success(let image):
                    image
                        .resizable()
                        .scaledToFill()
                default:
                    Avatar(name: speaker.name, size: size)
                }
            }
            .frame(width: size, height: size)
            .clipShape(Circle())
        } else {
            Avatar(name: speaker.name, size: size)
        }
    }

    private func imageFromBase64(_ base64: String) -> UIImage? {
        let raw = base64
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .components(separatedBy: ",")
            .last ?? ""
        guard !raw.isEmpty, let data = Data(base64Encoded: raw, options: .ignoreUnknownCharacters) else {
            return nil
        }
        return UIImage(data: data)
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
    String(name.split(separator: " ").compactMap { $0.first }.prefix(2)).uppercased()
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
