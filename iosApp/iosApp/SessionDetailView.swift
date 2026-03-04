import SwiftUI
import shared

struct SessionDetailView: View {
    let sessionId: String
    @Environment(\.dismiss) private var dismiss

    // In production, get from Koin
    @State private var session: Session?
    @State private var isLoading = true

    var body: some View {
        NavigationStack {
            ScrollView {
                if isLoading {
                    ProgressView()
                        .tint(.googleBlue)
                        .padding(.top, 48)
                } else if let session = session {
                    VStack(alignment: .leading, spacing: 24) {
                        // Badges
                        HStack(spacing: 10) {
                            Text(session.type.displayName)
                                .font(.caption)
                                .fontWeight(.semibold)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 6)
                                .background(
                                    session.type == .keynote
                                        ? AnyShapeStyle(LinearGradient(colors: [.googleBlue, Color(hex: 0xA142F4)], startPoint: .leading, endPoint: .trailing))
                                        : AnyShapeStyle(.white.opacity(0.1))
                                )
                                .cornerRadius(8)
                                .foregroundColor(.white)

                            Text(session.track.displayName)
                                .font(.caption)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 6)
                                .background(.white.opacity(0.06))
                                .cornerRadius(8)
                                .foregroundColor(.white.opacity(0.7))

                            Text(session.level.displayName)
                                .font(.caption)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 6)
                                .background(.white.opacity(0.04))
                                .cornerRadius(8)
                                .foregroundColor(.white.opacity(0.5))
                        }

                        // Title
                        Text(session.title)
                            .font(.largeTitle)
                            .fontWeight(.bold)
                            .foregroundColor(.white)

                        // Description
                        Text(session.description)
                            .font(.body)
                            .foregroundColor(.white.opacity(0.6))
                            .lineSpacing(4)

                        // Meta info
                        VStack(spacing: 8) {
                            MetaRow(icon: "calendar", label: "Date", value: "Day \(session.day) · \(session.startTime)")
                            MetaRow(icon: "clock", label: "Duration", value: session.duration)
                            MetaRow(icon: "mappin", label: "Location", value: session.room)
                            MetaRow(icon: "chart.bar", label: "Level", value: session.level.displayName)
                        }

                        // Capacity
                        VStack(alignment: .leading, spacing: 6) {
                            GeometryReader { geo in
                                ZStack(alignment: .leading) {
                                    RoundedRectangle(cornerRadius: 2)
                                        .fill(.white.opacity(0.06))
                                        .frame(height: 4)
                                    RoundedRectangle(cornerRadius: 2)
                                        .fill(
                                            session.registered >= session.capacity
                                                ? LinearGradient(colors: [.googleRed, Color(hex: 0xFF6B6B)], startPoint: .leading, endPoint: .trailing)
                                                : LinearGradient(colors: [.googleBlue, .googleBlue.opacity(0.5)], startPoint: .leading, endPoint: .trailing)
                                        )
                                        .frame(width: geo.size.width * CGFloat(session.registered) / CGFloat(max(session.capacity, 1)), height: 4)
                                }
                            }
                            .frame(height: 4)

                            Text(session.registered >= session.capacity
                                 ? "FULL"
                                 : "\(session.capacity - session.registered) spots left")
                                .font(.caption2)
                                .foregroundColor(.white.opacity(0.35))
                        }

                        // Action buttons
                        HStack(spacing: 12) {
                            Button(action: {}) {
                                Label("Save to Schedule", systemImage: "star")
                                    .font(.callout)
                                    .fontWeight(.semibold)
                            }
                            .padding(.horizontal, 20)
                            .padding(.vertical, 12)
                            .background(.white.opacity(0.1))
                            .cornerRadius(12)
                            .foregroundColor(.white)

                            Button(action: {}) {
                                Text("Reserve Seat")
                                    .font(.callout)
                                    .fontWeight(.semibold)
                            }
                            .padding(.horizontal, 20)
                            .padding(.vertical, 12)
                            .background(.googleBlue)
                            .cornerRadius(12)
                            .foregroundColor(.white)
                        }
                    }
                    .padding()
                }
            }
            .background(Color.darkBg)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: { dismiss() }) {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(.white.opacity(0.5))
                    }
                }
            }
        }
        .onAppear {
            loadSession()
        }
    }

    private func loadSession() {
        // In production, use the shared SessionDetailViewModel
        // For now, simulate loading
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            isLoading = false
            // session = viewModel.getSession(sessionId)
        }
    }
}

struct MetaRow: View {
    let icon: String
    let label: String
    let value: String

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: icon)
                .font(.callout)
                .frame(width: 20)
            VStack(alignment: .leading, spacing: 2) {
                Text(label)
                    .font(.caption2)
                    .foregroundColor(.white.opacity(0.5))
                Text(value)
                    .font(.callout)
                    .fontWeight(.medium)
                    .foregroundColor(.white)
            }
            Spacer()
        }
        .padding(12)
        .background(.white.opacity(0.04))
        .cornerRadius(12)
    }
}
