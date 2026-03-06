import SwiftUI
import shared

struct SpeakersView: View {
    // In production, wrap SpeakersViewModel from shared module
    @State private var speakers: [Speaker] = []

    let columns = [
        GridItem(.adaptive(minimum: 160), spacing: 16)
    ]

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVGrid(columns: columns, spacing: 16) {
                    ForEach(speakers, id: \.id) { speaker in
                        SpeakerCardView(speaker: speaker)
                    }
                }
                .padding()
            }
            .background(Color.darkBg)
            .navigationTitle("Speakers")
            .navigationBarTitleDisplayMode(.large)
            .toolbarColorScheme(.dark, for: .navigationBar)
        }
    }
}

struct SpeakerCardView: View {
    let speaker: Speaker

    private var initials: String {
        speaker.name
            .split(separator: " ")
            .compactMap { $0.first.map(String.init) }
            .joined()
    }

    private var avatarColor: Color {
        let colors: [Color] = [Color.googleBlue, Color.googleRed, Color.googleGreen, Color.googleYellow, Color(hex: 0xA142F4)]
        let hash = abs(speaker.name.hashValue)
        return colors[hash % colors.count]
    }

    var body: some View {
        VStack(spacing: 16) {
            // Avatar
            ZStack {
                Circle()
                    .fill(
                        LinearGradient(
                            colors: [avatarColor, avatarColor.opacity(0.6)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    )
                    .frame(width: 64, height: 64)

                Text(initials)
                    .font(.title3)
                    .fontWeight(.bold)
                    .foregroundColor(.white)
            }

            VStack(spacing: 4) {
                Text(speaker.name)
                    .font(.callout)
                    .fontWeight(.semibold)
                    .foregroundColor(.white)
                    .multilineTextAlignment(.center)

                Text(speaker.role)
                    .font(.caption)
                    .foregroundColor(.white.opacity(0.5))
                    .multilineTextAlignment(.center)
                    .lineLimit(2)
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity)
        .background(Color.darkSurfaceVariant)
        .cornerRadius(20)
    }
}
