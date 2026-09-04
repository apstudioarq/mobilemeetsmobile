import SwiftUI
import UIKit
import shared
import Combine

@MainActor
final class ApplicationStatusViewModelWrapper: ObservableObject {
    let viewModel: ApplicationStatusViewModel
    @Published var state: ApplicationStatusUiState
    private var observation: StateObservation?

    init() {
        viewModel = KoinInit.shared.getApplicationStatusViewModel()
        state = viewModel.uiState.value as! ApplicationStatusUiState
        observation = viewModel.observeState { [weak self] next in
            self?.state = next
        }
    }

    func refresh() {
        viewModel.refresh()
    }

    deinit {
        observation?.cancel()
        viewModel.onCleared()
    }
}

struct ContentView: View {
    @State private var showSplash = true
    @StateObject private var applicationStatus = ApplicationStatusViewModelWrapper()
    @ObservedObject private var notificationRouter = NotificationRouter.shared
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        Group {
            if showSplash || applicationStatus.state.isChecking {
                SplashView()
            } else if applicationStatus.state.isLocked {
                ApplicationLockedView(message: applicationStatus.state.message)
            } else {
                MainContentView()
            }
        }
        .task {
            try? await Task.sleep(nanoseconds: 900_000_000)
            showSplash = false
            while !Task.isCancelled {
                try? await Task.sleep(nanoseconds: 30_000_000_000)
                if !Task.isCancelled {
                    applicationStatus.refresh()
                }
            }
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active {
                applicationStatus.refresh()
            }
        }
        .onChange(of: applicationStatus.state.isLocked, initial: true) { _, isLocked in
            if isLocked {
                SessionNotificationScheduler.shared.cancelAll()
                notificationRouter.pendingSessionId = nil
            }
        }
        .onChange(of: notificationRouter.pendingSessionId) { _, _ in
            if applicationStatus.state.isLocked {
                notificationRouter.pendingSessionId = nil
            }
        }
    }
}

private struct MainContentView: View {
    @State private var selectedTab = 0
    @StateObject private var notificationSchedule = ScheduleViewModelWrapper()
    @StateObject private var homeSpeakers = SpeakersViewModelWrapper()
    @ObservedObject private var notificationRouter = NotificationRouter.shared
    @Environment(\.scenePhase) private var scenePhase
    @AppStorage(SessionNotificationSettings.reminderMinutesKey)
    private var reminderMinutes = SessionNotificationSettings.defaultReminderMinutes

    private var notificationScheduleSignature: String {
        let savedSessions = notificationSchedule.state.allSessions
            .filter(\.isBookmarked)
            .map { "\($0.id)|\($0.startTime)|\($0.endTime)|\($0.title)|\($0.room)" }
            .sorted()
            .joined(separator: ";")
        return "\(notificationSchedule.state.isNotificationScheduleReady):\(reminderMinutes):\(savedSessions)"
    }

    var body: some View {
        TabView(selection: $selectedTab) {
            HomeView(
                schedule: notificationSchedule,
                speakers: homeSpeakers,
                onScheduleTap: { selectedTab = 1 }
            )
                .tabItem {
                    Label("Home", systemImage: selectedTab == 0 ? "house.fill" : "house")
                }
                .tag(0)

            ScheduleView()
                .tabItem {
                    Label("Schedule", systemImage: selectedTab == 1 ? "calendar.badge.clock" : "calendar")
                }
                .tag(1)

            MapView()
                .tabItem {
                    Label("Map", systemImage: selectedTab == 2 ? "map.fill" : "map")
                }
                .tag(2)

            FavoritesView()
                .tabItem {
                    Label("Favorites", systemImage: selectedTab == 3 ? "heart.fill" : "heart")
                }
                .tag(3)

            SettingsView()
                .tabItem {
                    Label("Settings", systemImage: selectedTab == 4 ? "gearshape.fill" : "gearshape")
                }
                .tag(4)
        }
        .tint(.ingOrange)
        .onChange(of: notificationScheduleSignature, initial: true) { _, _ in
            rescheduleNotifications()
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { rescheduleNotifications() }
        }
        .onReceive(NotificationCenter.default.publisher(for: .NSSystemTimeZoneDidChange)) { _ in
            rescheduleNotifications()
        }
        .fullScreenCover(item: $notificationRouter.pendingSessionId) { sessionId in
            SessionDetailView(sessionId: sessionId)
        }
    }

    private func rescheduleNotifications() {
        SessionNotificationScheduler.shared.reschedule(
            sessions: notificationSchedule.state.allSessions.map(\.reminderSession),
            reminderMinutes: reminderMinutes,
            isDataReady: notificationSchedule.state.isNotificationScheduleReady
        )
    }
}

private struct ApplicationLockedView: View {
    let message: String

    var body: some View {
        Text(message)
            .font(.body)
            .multilineTextAlignment(.center)
            .foregroundStyle(Color.primary)
            .frame(maxWidth: 520)
            .padding(32)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color(uiColor: .systemBackground).ignoresSafeArea())
    }
}

struct SplashView: View {
    var body: some View {
        ZStack {
            Color(uiColor: .systemBackground)
                .ignoresSafeArea()

            Image("SplashLogo")
                .resizable()
                .scaledToFit()
                .frame(width: 164, height: 164)
        }
    }
}

struct EventTopBar: View {
    let section: String
    var showsOfflineMode = false

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 10) {
                Image("SplashLogo")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 30, height: 30)
                    .clipShape(RoundedRectangle(cornerRadius: 8))

                Text(section)
                    .font(.system(size: 20, weight: .semibold))
                    .lineLimit(1)
                    .foregroundStyle(Color.vibrantText)

                Spacer(minLength: 10)
            }
            .padding(.horizontal, 18)
            .frame(height: 56)

            if showsOfflineMode {
                HStack(spacing: 7) {
                    Image(systemName: "wifi.slash")
                    Text("Offline mode · Showing saved data")
                }
                .font(.caption.weight(.bold))
                .foregroundStyle(Color.ingOrange)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 8)
                .background(Color.ingOrange.opacity(0.14))
            }

            Rectangle()
                .fill(Color.vibrantBorder.opacity(0.65))
                .frame(height: 1)
        }
        .background(Color.vibrantSurface.ignoresSafeArea(edges: .top))
    }
}

extension View {
    func internetConnectionRequiredAlert(
        isPresented: Binding<Bool>,
        retry: @escaping () -> Void
    ) -> some View {
        alert("Internet connection required", isPresented: isPresented) {
            Button("Try again", action: retry)
        } message: {
            Text("No saved event data is available on this device. Connect to the internet and try again.")
        }
    }
}

extension Color {
    init(hex: UInt, alpha: Double = 1.0) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xff) / 255,
            green: Double((hex >> 8) & 0xff) / 255,
            blue: Double(hex & 0xff) / 255,
            opacity: alpha
        )
    }

    static func adaptive(light: UInt, dark: UInt) -> Color {
        Color(
            uiColor: UIColor { traits in
                UIColor(hex: traits.userInterfaceStyle == .dark ? dark : light)
            }
        )
    }

    static let ingOrange = Color(hex: 0xFF6200)
    static let ingPurple = Color.adaptive(light: 0x525199, dark: 0x8E8BDB)
    static let ingMagenta = Color(hex: 0xC00067)
    static let vibrantBackground = Color.adaptive(light: 0xF3F3F3, dark: 0x121214)
    static let vibrantSurface = Color.adaptive(light: 0xFFFFFF, dark: 0x1D1D20)
    static let vibrantWarm = Color.adaptive(light: 0xEDEDEF, dark: 0x29292D)
    static let vibrantBorder = Color.adaptive(light: 0xDADADD, dark: 0x414146)
    static let vibrantText = Color.adaptive(light: 0x202020, dark: 0xF4F4F5)
    static let vibrantMuted = Color.adaptive(light: 0x66666A, dark: 0xB8B8BE)
    static let vibrantSoftMuted = Color.adaptive(light: 0x8E8E93, dark: 0x96969E)
    static let vibrantBrown = Color.ingPurple
    static let trackBlue = Color(hex: 0x4285F4)
    static let trackGreen = Color(hex: 0x34A853)
    static let trackYellow = Color(hex: 0xFBBC04)
    static let trackRed = Color(hex: 0xEA4335)
    static let trackPurple = Color(hex: 0xA142F4)
}

private extension UIColor {
    convenience init(hex: UInt) {
        self.init(
            red: CGFloat((hex >> 16) & 0xff) / 255,
            green: CGFloat((hex >> 8) & 0xff) / 255,
            blue: CGFloat(hex & 0xff) / 255,
            alpha: 1
        )
    }
}

extension String: @retroactive Identifiable {
    public var id: String { self }
}
