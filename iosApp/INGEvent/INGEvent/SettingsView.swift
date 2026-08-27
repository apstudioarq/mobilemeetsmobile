import SwiftUI

struct SettingsView: View {
    @AppStorage(SessionNotificationSettings.reminderMinutesKey)
    private var reminderMinutes = SessionNotificationSettings.defaultReminderMinutes
    @State private var input = ""
    @State private var saved = false
    @StateObject private var authorization = NotificationAuthorizationModel()
    @Environment(\.scenePhase) private var scenePhase

    private var parsedMinutes: Int? {
        guard let value = Int(input), SessionNotificationSettings.validRange.contains(value) else {
            return nil
        }
        return value
    }

    var body: some View {
        VStack(spacing: 0) {
            EventTopBar(section: "Settings")
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    Text("Session reminders")
                        .font(.system(size: 30, weight: .black))
                        .foregroundStyle(Color.vibrantText)
                    Text("Choose when to receive reminders for sessions saved in Favorites.")
                        .font(.subheadline)
                        .foregroundStyle(Color.vibrantMuted)

                    settingsCard {
                        Label("Reminder timing", systemImage: "bell.badge.fill")
                            .font(.headline.weight(.bold))
                            .foregroundStyle(Color.vibrantText)
                        Text("The same interval is used before a session starts and after it ends.")
                            .font(.subheadline)
                            .foregroundStyle(Color.vibrantMuted)

                        TextField("Minutes", text: $input)
                            .keyboardType(.numberPad)
                            .textFieldStyle(.roundedBorder)
                            .onChange(of: input) { _, value in
                                input = String(value.filter(\.isNumber).prefix(4))
                                saved = false
                            }

                        Text(
                            parsedMinutes.map {
                                "Notify \($0) minutes before and \($0) minutes after"
                            } ?? "Enter a value from 1 to 1440 minutes"
                        )
                        .font(.caption)
                        .foregroundStyle(parsedMinutes == nil ? Color.red : Color.vibrantMuted)

                        Button {
                            guard let value = parsedMinutes else { return }
                            reminderMinutes = value
                            saved = true
                        } label: {
                            Label(
                                saved ? "Saved" : "Save reminder time",
                                systemImage: saved ? "checkmark.circle.fill" : "clock.badge.checkmark"
                            )
                            .font(.headline.weight(.bold))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                        }
                        .buttonStyle(.plain)
                        .foregroundStyle(Color.white)
                        .background(parsedMinutes == nil ? Color.vibrantMuted : Color.ingOrange)
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                        .disabled(parsedMinutes == nil)
                    }

                    settingsCard {
                        HStack(alignment: .top, spacing: 12) {
                            Image(systemName: authorization.isAllowed ? "checkmark.circle.fill" : "info.circle")
                                .foregroundStyle(authorization.isAllowed ? Color.ingOrange : Color.vibrantMuted)
                            VStack(alignment: .leading, spacing: 5) {
                                Text("Notifications")
                                    .font(.headline.weight(.bold))
                                    .foregroundStyle(Color.vibrantText)
                                Text(authorization.statusDescription)
                                    .font(.subheadline)
                                    .foregroundStyle(Color.vibrantMuted)
                            }
                        }
                        if !authorization.isAllowed {
                            Button("Allow notifications") {
                                authorization.request()
                            }
                            .font(.headline.weight(.bold))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .foregroundStyle(Color.ingOrange)
                            .background(Color.vibrantWarm)
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                        }
                    }
                }
                .padding(20)
                .padding(.bottom, 70)
            }
            .background(Color.vibrantBackground)
        }
        .onAppear {
            input = String(reminderMinutes)
            authorization.refresh()
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { authorization.refresh() }
        }
    }

    private func settingsCard<Content: View>(
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: 14, content: content)
            .padding(18)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.vibrantSurface)
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .stroke(Color.vibrantBorder)
            )
    }
}
