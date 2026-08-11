import SwiftUI
import shared

struct ContentView: View {
    @State private var selectedTab = 0
    @State private var showSplash = true

    var body: some View {
        Group {
            if showSplash {
                SplashView()
            } else {
                TabView(selection: $selectedTab) {
                    HomeView()
                        .tabItem {
                            Label("HOME", systemImage: selectedTab == 0 ? "house.fill" : "house")
                        }
                        .tag(0)

                    ScheduleView()
                        .tabItem {
                            Label("SCHEDULE", systemImage: selectedTab == 1 ? "calendar.badge.clock" : "calendar")
                        }
                        .tag(1)

                    FavoritesView()
                        .tabItem {
                            Label("FAVORITES", systemImage: selectedTab == 2 ? "heart.fill" : "heart")
                        }
                        .tag(2)
                }
                .tint(.ingOrange)
            }
        }
        .task {
            try? await Task.sleep(nanoseconds: 900_000_000)
            showSplash = false
        }
    }
}

struct SplashView: View {
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        ZStack {
            (colorScheme == .dark ? Color.black : Color.white)
                .ignoresSafeArea()

            Image(colorScheme == .dark ? "SplashLogoDark" : "SplashLogoLight")
                .resizable()
                .scaledToFit()
                .frame(width: 164, height: 164)
        }
    }
}

struct EventTopBar: View {
    let title: String

    var body: some View {
        HStack {
            Text(title)
                .font(.system(size: 22, weight: .black))
                .foregroundStyle(Color.ingOrange)
            Spacer()
        }
        .padding(.horizontal, 24)
        .padding(.vertical, 18)
        .background(Color.white)
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

    static let ingOrange = Color(hex: 0xFF6200)
    static let vibrantBackground = Color(hex: 0xFFF7F4)
    static let vibrantSurface = Color.white
    static let vibrantWarm = Color(hex: 0xFFE8DF)
    static let vibrantBorder = Color(hex: 0xF0C8BC)
    static let vibrantText = Color(hex: 0x2A1A16)
    static let vibrantMuted = Color(hex: 0x745F57)
    static let vibrantSoftMuted = Color(hex: 0x9A8B86)
    static let vibrantBrown = Color(hex: 0x9B3A00)
    static let trackBlue = Color(hex: 0x4285F4)
    static let trackGreen = Color(hex: 0x34A853)
    static let trackYellow = Color(hex: 0xFBBC04)
    static let trackRed = Color(hex: 0xEA4335)
    static let trackPurple = Color(hex: 0xA142F4)
}

extension String: @retroactive Identifiable {
    public var id: String { self }
}
