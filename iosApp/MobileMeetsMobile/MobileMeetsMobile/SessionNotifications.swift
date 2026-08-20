import Foundation
import Combine
import UIKit
import UserNotifications
import shared

enum SessionNotificationSettings {
    static let reminderMinutesKey = "sessionReminderMinutes"
    static let defaultReminderMinutes = 5
    static let validRange = 1...1_440
    static let categoryIdentifier = "SAVED_SESSION_REMINDER"
    static let sessionIdKey = "sessionId"
    static let identifierPrefix = "saved-session."
}

@MainActor
final class NotificationRouter: ObservableObject {
    static let shared = NotificationRouter()
    @Published var pendingSessionId: String?

    private init() {}
}

final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        let center = UNUserNotificationCenter.current()
        center.delegate = self
        SessionNotificationScheduler.shared.registerCategory()
        return true
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .list, .sound])
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        if let sessionId = response.notification.request.content.userInfo[
            SessionNotificationSettings.sessionIdKey
        ] as? String {
            Task { @MainActor in
                NotificationRouter.shared.pendingSessionId = sessionId
            }
        }
        completionHandler()
    }
}

final class SessionNotificationScheduler {
    static let shared = SessionNotificationScheduler()
    private let center = UNUserNotificationCenter.current()

    private init() {}

    func registerCategory() {
        let category = UNNotificationCategory(
            identifier: SessionNotificationSettings.categoryIdentifier,
            actions: [],
            intentIdentifiers: [],
            options: [.customDismissAction]
        )
        center.setNotificationCategories([category])
    }

    func requestAuthorization(completion: ((Bool) -> Void)? = nil) {
        center.requestAuthorization(options: [.alert, .badge, .sound]) { granted, _ in
            DispatchQueue.main.async { completion?(granted) }
        }
    }

    func reschedule(sessions: [Session], reminderMinutes: Int) {
        let minutes = min(
            max(reminderMinutes, SessionNotificationSettings.validRange.lowerBound),
            SessionNotificationSettings.validRange.upperBound
        )
        if !sessions.contains(where: \.isBookmarked) {
            removePendingSessionNotifications()
            return
        }
        center.getNotificationSettings { [weak self] settings in
            guard let self else { return }
            if settings.authorizationStatus == .notDetermined {
                self.requestAuthorization { granted in
                    if granted {
                        self.replacePendingRequests(sessions: sessions, reminderMinutes: minutes)
                    }
                }
            } else if settings.authorizationStatus == .authorized ||
                        settings.authorizationStatus == .provisional ||
                        settings.authorizationStatus == .ephemeral {
                self.replacePendingRequests(sessions: sessions, reminderMinutes: minutes)
            }
        }
    }

    func cancelAll() {
        center.removeAllPendingNotificationRequests()
        center.removeAllDeliveredNotifications()
    }

    private func removePendingSessionNotifications() {
        center.getPendingNotificationRequests { [weak self] requests in
            guard let self else { return }
            let identifiers = requests
                .map(\.identifier)
                .filter { $0.hasPrefix(SessionNotificationSettings.identifierPrefix) }
            self.center.removePendingNotificationRequests(withIdentifiers: identifiers)
        }
    }

    private func replacePendingRequests(sessions: [Session], reminderMinutes: Int) {
        center.getPendingNotificationRequests { [weak self] requests in
            guard let self else { return }
            let currentIdentifiers = requests
                .map(\.identifier)
                .filter { $0.hasPrefix(SessionNotificationSettings.identifierPrefix) }
            self.center.removePendingNotificationRequests(withIdentifiers: currentIdentifiers)

            let candidates = sessions
                .filter(\.isBookmarked)
                .flatMap { self.candidates(for: $0, reminderMinutes: reminderMinutes) }
                .filter { $0.date.timeIntervalSinceNow > 1 }
                .sorted { $0.date < $1.date }
                .prefix(64)

            for candidate in candidates {
                let trigger = UNTimeIntervalNotificationTrigger(
                    timeInterval: candidate.date.timeIntervalSinceNow,
                    repeats: false
                )
                let request = UNNotificationRequest(
                    identifier: candidate.identifier,
                    content: candidate.content,
                    trigger: trigger
                )
                self.center.add(request)
            }
        }
    }

    private func candidates(for session: Session, reminderMinutes: Int) -> [Candidate] {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        let fallbackFormatter = ISO8601DateFormatter()
        fallbackFormatter.formatOptions = [.withInternetDateTime]
        let localFormatter = DateFormatter()
        localFormatter.locale = Locale(identifier: "en_US_POSIX")
        localFormatter.calendar = Calendar(identifier: .gregorian)
        localFormatter.timeZone = .current
        localFormatter.isLenient = false
        func date(from value: String) -> Date? {
            if let instant = formatter.date(from: value) ?? fallbackFormatter.date(from: value) {
                return instant
            }
            for format in ["yyyy-MM-dd'T'HH:mm:ss.SSS", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm"] {
                localFormatter.dateFormat = format
                if let localDate = localFormatter.date(from: value) {
                    return localDate
                }
            }
            return nil
        }

        let offset = TimeInterval(reminderMinutes * 60)
        var result: [Candidate] = []

        if let startDate = date(from: session.startTime) {
            let now = Date()
            let configuredDate = startDate.addingTimeInterval(-offset)
            let deliveryDate = configuredDate <= now && now < startDate
                ? now.addingTimeInterval(1)
                : configuredDate
            let content = baseContent(for: session)
            content.title = "Starts in \(reminderMinutes) min · \(session.title)"
            content.subtitle = session.room
            content.body = "Tap to view session details."
            result.append(
                Candidate(
                    identifier: identifier(sessionId: session.id, kind: "before"),
                    date: deliveryDate,
                    content: content
                )
            )
        }

        if let endDate = date(from: session.endTime) {
            let content = baseContent(for: session)
            content.title = "How was \(session.title)?"
            content.subtitle = "Your feedback matters"
            content.body = "Rate the session and share your comments."
            result.append(
                Candidate(
                    identifier: identifier(sessionId: session.id, kind: "after"),
                    date: endDate.addingTimeInterval(offset),
                    content: content
                )
            )
        }
        return result
    }

    private func baseContent(for session: Session) -> UNMutableNotificationContent {
        let content = UNMutableNotificationContent()
        content.sound = .default
        content.categoryIdentifier = SessionNotificationSettings.categoryIdentifier
        content.threadIdentifier = "saved-sessions"
        content.userInfo = [SessionNotificationSettings.sessionIdKey: session.id]
        return content
    }

    private func identifier(sessionId: String, kind: String) -> String {
        "\(SessionNotificationSettings.identifierPrefix)\(kind).\(sessionId)"
    }

    private struct Candidate {
        let identifier: String
        let date: Date
        let content: UNMutableNotificationContent
    }
}

@MainActor
final class NotificationAuthorizationModel: ObservableObject {
    @Published private(set) var isAllowed = false
    @Published private(set) var statusDescription = "Checking notification access…"

    func refresh() {
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            Task { @MainActor in
                switch settings.authorizationStatus {
                case .authorized, .provisional, .ephemeral:
                    self.isAllowed = true
                    self.statusDescription = "Allowed. Session reminders can appear on this device."
                case .denied:
                    self.isAllowed = false
                    self.statusDescription = "Notifications are disabled in iOS Settings."
                case .notDetermined:
                    self.isAllowed = false
                    self.statusDescription = "Permission is required to show session reminders."
                @unknown default:
                    self.isAllowed = false
                    self.statusDescription = "Notification access is unavailable."
                }
            }
        }
    }

    func request() {
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            if settings.authorizationStatus == .denied {
                DispatchQueue.main.async {
                    guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
                    UIApplication.shared.open(url)
                }
            } else {
                let authorizationModel = self
                SessionNotificationScheduler.shared.requestAuthorization { _ in
                    Task { @MainActor in
                        authorizationModel.refresh()
                    }
                }
            }
        }
    }
}
