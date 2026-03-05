import SwiftUI
import shared
import Combine

// MARK: - Observable wrapper for KMM ViewModel
class ScheduleViewModelWrapper: ObservableObject {
    let viewModel: ScheduleViewModel

    @Published var state: ScheduleUiState

    private var cancellable: AnyCancellable?

    init() {
        // Get from Koin
        let koin = KoinInit.shared.koin
        self.viewModel = koin.get(objCClass: ScheduleViewModel.self) as! ScheduleViewModel
        self.state = viewModel.uiState.value as! ScheduleUiState

        // Observe state changes via polling (or use SKIE/KMP-NativeCoroutines for proper Flow collection)
        cancellable = Timer.publish(every: 0.1, on: .main, in: .common)
            .autoconnect()
            .sink { [weak self] _ in
                guard let self = self else { return }
                if let newState = self.viewModel.uiState.value as? ScheduleUiState {
                    self.state = newState
                }
            }
    }

    deinit {
        cancellable?.cancel()
        viewModel.onCleared()
    }
}

// MARK: - Schedule View
struct ScheduleView: View {
    @StateObject private var wrapper = ScheduleViewModelWrapper()
    @State private var selectedSessionId: String?

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    // Day tabs
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 12) {
                            ForEach(wrapper.state.days, id: \.dayNumber) { day in
                                DayTabView(
                                    label: day.label,
                                    date: day.date,
                                    isSelected: wrapper.state.selectedDay == day.dayNumber,
                                    action: {
                                        wrapper.viewModel.loadDay(day: day.dayNumber)
                                    }
                                )
                            }
                        }
                        .padding(.horizontal)
                    }

                    // Track filter
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            TrackChipView(
                                label: "All",
                                isSelected: wrapper.state.selectedTrack == nil,
                                color: .googleBlue
                            ) {
                                wrapper.viewModel.selectTrack(track: nil)
                            }

                            ForEach(Track.entries, id: \.self) { track in
                                TrackChipView(
                                    label: track.displayName,
                                    isSelected: wrapper.state.selectedTrack == track,
                                    color: trackColor(track)
                                ) {
                                    wrapper.viewModel.selectTrack(track: track)
                                }
                            }
                        }
                        .padding(.horizontal)
                    }

                    // Sessions by time slot
                    if wrapper.state.isLoading {
                        HStack {
                            Spacer()
                            ProgressView()
                                .tint(.googleBlue)
                            Spacer()
                        }
                        .padding(.top, 48)
                    } else if wrapper.state.timeSlots.isEmpty {
                        VStack(spacing: 12) {
                            Text("📭").font(.system(size: 48))
                            Text("No sessions match your filters")
                                .foregroundColor(.white.opacity(0.5))
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.top, 48)
                    } else {
                        let sortedSlots = wrapper.state.timeSlots.sorted { $0.key < $1.key }
                        ForEach(sortedSlots, id: \.key) { time, sessions in
                            VStack(alignment: .leading, spacing: 8) {
                                // Time header
                                HStack(spacing: 12) {
                                    Circle()
                                        .fill(
                                            LinearGradient(
                                                colors: [.googleBlue, .googleGreen],
                                                startPoint: .topLeading,
                                                endPoint: .bottomTrailing
                                            )
                                        )
                                        .frame(width: 10, height: 10)

                                    Text(time)
                                        .font(.caption)
                                        .fontWeight(.semibold)
                                        .foregroundColor(.white.opacity(0.4))

                                    Rectangle()
                                        .fill(.white.opacity(0.06))
                                        .frame(height: 1)
                                }
                                .padding(.horizontal)

                                // Session cards
                                ForEach(sessions, id: \.id) { session in
                                    SessionCardView(
                                        session: session,
                                        onTap: { selectedSessionId = session.id },
                                        onBookmark: {
                                            wrapper.viewModel.onBookmarkToggle(sessionId: session.id)
                                        }
                                    )
                                    .padding(.horizontal)
                                    .padding(.leading, 22)
                                }
                            }
                        }
                    }
                }
                .padding(.bottom, 80)
            }
            .background(Color.darkBg)
            .navigationTitle("")
            .toolbar {
                ToolbarItem(placement: .principal) {
                    EventHubTitle()
                }
            }
            .sheet(item: $selectedSessionId) { sessionId in
                SessionDetailView(sessionId: sessionId)
            }
        }
    }

    func trackColor(_ track: Track) -> Color {
        switch track {
        case .aiMl: return .googleBlue
        case .android: return .googleGreen
        case .web: return .googleYellow
        case .cloud: return .googleRed
        case .firebase: return Color(hex: 0xFF6D00)
        case .flutter: return Color(hex: 0x42A5F5)
        case .design: return Color(hex: 0xA142F4)
        default: return .googleBlue
        }
    }
}

// MARK: - Subviews
struct EventHubTitle: View {
    var body: some View {
        Text("Event Hub")
            .foregroundColor(.white)
            .fontWeight(.bold)
            .font(.title3)
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .background(.white.opacity(0.08))
            .cornerRadius(10)
    }
}

struct DayTabView: View {
    let label: String
    let date: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 2) {
                Text(date)
                    .font(.caption2)
                    .opacity(0.7)
                Text(label)
                    .font(.callout)
                    .fontWeight(isSelected ? .semibold : .regular)
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 12)
            .background(
                isSelected
                    ? AnyShapeStyle(LinearGradient(colors: [.googleBlue, .googleGreen], startPoint: .topLeading, endPoint: .bottomTrailing))
                    : AnyShapeStyle(.white.opacity(0.05))
            )
            .cornerRadius(16)
            .foregroundColor(isSelected ? .white : .white.opacity(0.5))
        }
    }
}

struct TrackChipView: View {
    let label: String
    let isSelected: Bool
    let color: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.caption)
                .fontWeight(isSelected ? .semibold : .regular)
                .padding(.horizontal, 16)
                .padding(.vertical, 7)
                .background(isSelected ? color : .white.opacity(0.06))
                .cornerRadius(20)
                .foregroundColor(isSelected ? .white : .white.opacity(0.6))
        }
    }
}

struct SessionCardView: View {
    let session: Session
    let onTap: () -> Void
    let onBookmark: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 10) {
                HStack {
                    Text(session.type.displayName)
                        .font(.caption2)
                        .fontWeight(.semibold)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(.white.opacity(0.1))
                        .cornerRadius(8)

                    Text(session.track.displayName)
                        .font(.caption2)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(.white.opacity(0.06))
                        .cornerRadius(8)

                    Spacer()

                    Button(action: onBookmark) {
                        Image(systemName: session.isBookmarked ? "star.fill" : "star")
                            .foregroundColor(session.isBookmarked ? .googleYellow : .white.opacity(0.25))
                    }
                }

                Text(session.title)
                    .font(.headline)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
                    .multilineTextAlignment(.leading)

                Text(session.description)
                    .font(.caption)
                    .foregroundColor(.white.opacity(0.45))
                    .lineLimit(2)

                HStack {
                    Label(session.duration, systemImage: "clock")
                    Label(session.room, systemImage: "mappin")
                }
                .font(.caption2)
                .foregroundColor(.white.opacity(0.35))
            }
            .padding(16)
            .background(Color.darkSurfaceVariant)
            .cornerRadius(16)
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Extensions
extension String: @retroactive Identifiable {
    public var id: String { self }
}
