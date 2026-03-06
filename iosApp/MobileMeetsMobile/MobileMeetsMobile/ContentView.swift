import SwiftUI
import shared

struct ContentView: View {
    @State private var selectedTab = 0

    var body: some View {
        TabView(selection: $selectedTab) {
            ScheduleView()
                .tabItem {
                    Image(systemName: selectedTab == 0 ? "calendar.circle.fill" : "calendar.circle")
                    Text("Schedule")
                }
                .tag(0)

            SpeakersView()
                .tabItem {
                    Image(systemName: selectedTab == 1 ? "person.2.fill" : "person.2")
                    Text("Speakers")
                }
                .tag(1)
        }
        .accentColor(Color(hex: 0x4285F4))
    }
}

// MARK: - Color Extension
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

    static let googleBlue = Color(hex: 0x4285F4)
    static let googleRed = Color(hex: 0xEA4335)
    static let googleYellow = Color(hex: 0xFBBC04)
    static let googleGreen = Color(hex: 0x34A853)
    static let darkBg = Color(hex: 0x0D0D0D)
    static let darkSurface = Color(hex: 0x1A1A1A)
    static let darkSurfaceVariant = Color(hex: 0x242424)
}
