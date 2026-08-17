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

    private var speakerSessions: [Session] {
        let all = schedule.state.allSessions.filter { $0.speakerIds.contains(speakerId) }
        let fallback = schedule.state.sessions.filter { $0.speakerIds.contains(speakerId) }
        return (all.isEmpty ? fallback : all).sorted { $0.startTime < $1.startTime }
    }

    var body: some View {
        VStack(spacing: 0) {
            topBar
            Rectangle()
                .fill(Color.vibrantBorder)
                .frame(height: 1)

            if let speaker {
                ScrollView {
                    VStack(alignment: .leading, spacing: 22) {
                        profileHeader(speaker)
                        sessionsSection
                    }
                    .padding(.horizontal, 18)
                    .padding(.top, 24)
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
                Image(systemName: "xmark")
            }
            .foregroundStyle(Color.vibrantText)
            Spacer()
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 12)
        .background(Color.vibrantSurface)
    }

    private func profileHeader(_ speaker: Speaker) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 16) {
                SpeakerAvatar(speaker: speaker, size: 86)
                VStack(alignment: .leading, spacing: 5) {
                    Text(speaker.name)
                        .font(.system(size: 30, weight: .black))
                        .foregroundStyle(Color.vibrantText)
                    let roleLine = [speaker.role, speaker.company]
                        .filter { !$0.isEmpty }
                        .joined(separator: ", ")
                    if !roleLine.isEmpty {
                        Text(roleLine)
                            .font(.caption.weight(.bold))
                            .foregroundStyle(Color.ingOrange)
                    }
                }
            }

            if !speaker.bio.isEmpty {
                Text(speaker.bio)
                    .font(.system(size: 13))
                    .lineSpacing(4)
                    .foregroundStyle(Color.vibrantMuted)
            }
        }
        .padding(18)
        .background(Color.vibrantSurface)
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.vibrantBorder))
    }

    private var sessionsSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("SESSIONS")
                .font(.system(size: 11, weight: .medium))
                .tracking(1.8)
                .foregroundStyle(Color.vibrantMuted)

            if speakerSessions.isEmpty {
                Text("No sessions associated with this speaker.")
                    .font(.callout)
                    .foregroundStyle(Color.vibrantMuted)
            } else {
                ForEach(speakerSessions, id: \.id) { session in
                    Button {
                        selectedSessionId = session.id
                    } label: {
                        HStack(spacing: 12) {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(session.title)
                                    .font(.system(size: 14, weight: .medium))
                                    .lineLimit(1)
                                    .foregroundStyle(Color.vibrantText)
                                Text([displayShortDate(session.startTime), displayClock(session.startTime), session.room]
                                    .filter { !$0.isEmpty }
                                    .joined(separator: " • "))
                                    .font(.caption.weight(.bold))
                                    .foregroundStyle(Color.vibrantBrown)
                            }
                            Spacer(minLength: 8)
                            Image(systemName: "chevron.right")
                                .font(.headline.weight(.bold))
                                .foregroundStyle(Color.ingOrange)
                        }
                        .padding(.horizontal, 14)
                        .padding(.vertical, 12)
                        .background(Color.vibrantSurface)
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.vibrantBorder))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
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
