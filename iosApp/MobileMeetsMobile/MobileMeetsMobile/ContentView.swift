import SwiftUI
import UIKit
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
                    HomeView(onScheduleTap: { selectedTab = 1 })
                        .tabItem {
                            Label("Home", systemImage: selectedTab == 0 ? "house.fill" : "house")
                        }
                        .tag(0)

                    ScheduleView()
                        .tabItem {
                            Label("Schedule", systemImage: selectedTab == 1 ? "calendar.badge.clock" : "calendar")
                        }
                        .tag(1)

                    FavoritesView()
                        .tabItem {
                            Label("Favorites", systemImage: selectedTab == 2 ? "heart.fill" : "heart")
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

    var body: some View {
        VStack(spacing: 0) {
            Color.clear
                .frame(height: topSafeAreaInset)

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

            Rectangle()
                .fill(Color.vibrantBorder.opacity(0.65))
                .frame(height: 1)
        }
        .background(Color.vibrantSurface)
        .ignoresSafeArea(edges: .top)
    }

    private var topSafeAreaInset: CGFloat {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first { $0.isKeyWindow }?
            .safeAreaInsets.top ?? 0
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
