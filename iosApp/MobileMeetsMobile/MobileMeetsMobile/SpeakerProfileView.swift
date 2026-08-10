import SwiftUI
import shared

struct SpeakerProfileView: View {
    let speakerId: String
    @Environment(\.dismiss) private var dismiss
    @StateObject private var speakers = SpeakersViewModelWrapper()
    @StateObject private var schedule = ScheduleViewModelWrapper()
    @State private var selectedSessionId: String?

    private var speaker: Speaker? {
        speakers.state.speakers.first { $0.id == speakerId }
    }

    private var featuredSession: Session? {
        schedule.state.sessions.first { $0.speakerIds.contains(speakerId) }
    }

    var body: some View {
        VStack(spacing: 0) {
            topBar
            Rectangle()
                .fill(Color.vibrantBorder)
                .frame(height: 1)

            if let speaker {
                ScrollView {
                    VStack(alignment: .leading, spacing: 26) {
                        EventLogoView()

                        VStack(alignment: .leading, spacing: 4) {
                            Text(speaker.name)
                                .font(.system(size: 30, weight: .black))
                                .foregroundStyle(Color.vibrantText)
                            Text("\(speaker.role), \(speaker.company)")
                                .font(.caption.weight(.bold))
                                .foregroundStyle(Color.ingOrange)
                        }

                        socialRow

                        Text(speaker.bio)
                            .font(.system(size: 13))
                            .lineSpacing(4)
                            .foregroundStyle(Color.vibrantText)

                        Rectangle()
                            .fill(Color.vibrantBorder)
                            .frame(height: 1)
                            .padding(.top, 10)

                        featuredSessionSection
                    }
                    .padding(.horizontal, 14)
                    .padding(.top, 44)
                    .padding(.bottom, 42)
                }
                .background(Color.vibrantBackground)
            } else {
                ProgressView()
                    .tint(.ingOrange)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(Color.vibrantBackground)
            }
        }
        .navigationBarBackButtonHidden(true)
        .navigationDestination(item: $selectedSessionId) { sessionId in
            SessionDetailView(sessionId: sessionId)
        }
    }

    private var topBar: some View {
        HStack(spacing: 10) {
            Button(action: { dismiss() }) {
                Image(systemName: "chevron.left")
            }
            .foregroundStyle(Color.vibrantBrown)

            Text("MOBILE MEETS MOBILE")
                .font(.system(size: 9, weight: .bold))
                .tracking(1.5)
                .foregroundStyle(Color.vibrantBrown)

            Spacer()

            Button(action: { dismiss() }) {
                Image(systemName: "xmark")
            }
            .foregroundStyle(Color.vibrantText)
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 12)
        .background(Color.vibrantSurface)
    }

    private var socialRow: some View {
        HStack(spacing: 10) {
            Text("∞")
            Text("●")
            Text("<>")
        }
        .font(.system(size: 10, weight: .bold))
        .foregroundStyle(Color.vibrantBrown)
        .padding(.top, 28)
    }

    private var featuredSessionSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("FEATURED SESSION")
                .font(.system(size: 11, weight: .medium))
                .tracking(1.8)
                .foregroundStyle(Color.vibrantMuted)

            if let featuredSession {
                HStack(spacing: 6) {
                    Image(systemName: "clock")
                        .font(.caption)
                    Text("\(displayShortDate(featuredSession.startTime)), \(displayClock(featuredSession.startTime)) • \(featuredSession.room)")
                }
                .font(.caption.weight(.bold))
                .foregroundStyle(Color.vibrantBrown)

                Text(featuredSession.title)
                    .font(.system(size: 13))
                    .foregroundStyle(Color.vibrantText)

                Text(featuredSession.description)
                    .font(.system(size: 12))
                    .lineSpacing(3)
                    .foregroundStyle(Color.vibrantText)

                Button {
                    selectedSessionId = featuredSession.id
                } label: {
                    Text("View session details")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(Color.vibrantBrown)
                        .overlay(alignment: .bottom) {
                            Rectangle()
                                .fill(Color.vibrantBrown)
                                .frame(height: 1)
                                .offset(y: 2)
                        }
                }
            }
        }
    }
}

struct EventLogoView: View {
    var body: some View {
        VStack(spacing: 0) {
            HStack(alignment: .bottom, spacing: 0) {
                Text("m")
                    .font(.system(size: 26, weight: .black))
                    .foregroundStyle(Color.ingOrange)
                Text("m")
                    .font(.system(size: 26, weight: .black))
                    .foregroundStyle(Color(hex: 0xFFB000))
                Text("26")
                    .font(.system(size: 9, weight: .black))
                    .foregroundStyle(Color.vibrantText)
            }
            Text("mobile meets mobile")
                .font(.system(size: 5, weight: .bold))
                .foregroundStyle(Color.vibrantBrown)
        }
        .frame(width: 52, height: 52)
        .background(Color.vibrantSurface)
        .clipShape(RoundedRectangle(cornerRadius: 5))
        .overlay(RoundedRectangle(cornerRadius: 5).stroke(Color.vibrantBrown))
    }
}

func displayShortDate(_ iso: String) -> String {
    let date = String(iso.prefix(10))
    let pieces = date.split(separator: "-")
    guard pieces.count == 3, let month = Int(pieces[1]), let day = Int(pieces[2]) else {
        return date
    }
    let monthName = [
        1: "Jan", 2: "Feb", 3: "Mar", 4: "Apr", 5: "May", 6: "Jun",
        7: "Jul", 8: "Aug", 9: "Sep", 10: "Oct", 11: "Nov", 12: "Dec"
    ][month] ?? ""
    return "\(monthName) \(day)"
}
