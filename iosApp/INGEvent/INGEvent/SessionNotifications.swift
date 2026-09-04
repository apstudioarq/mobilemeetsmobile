import Foundation
import Combine
import UIKit
import UserNotifications
import shared

extension Session {
    var reminderSession: ReminderSession {
        ReminderSession(
            id: id, title: title, room: room,
            startTime: startTime, endTime: endTime, isBookmarked: isBookmarked
        )
    }
}

@MainActor
final class NotificationRouter: ObservableObject {
    static let shared = NotificationRouter()
    @Published var pendingSessionId: String?

    private init() {}
}

@MainActor
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

    func applicationSignificantTimeChange(_ application: UIApplication) {
        SessionNotificationScheduler.shared.refresh()
    }

    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .list, .sound])
    }

    nonisolated func userNotificationCenter(
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

@MainActor
final class NotificationAuthorizationModel: ObservableObject {
    @Published private(set) var isAllowed = false
    @Published private(set) var statusDescription = "Checking notification access…"

    func refresh() {
        Task {
            let settings = await UNUserNotificationCenter.current().notificationSettings()
            switch settings.authorizationStatus {
            case .authorized, .provisional, .ephemeral:
                isAllowed = true
                statusDescription = "Allowed. Session reminders can appear on this device."
            case .denied:
                isAllowed = false
                statusDescription = "Notifications are disabled in iOS Settings."
            case .notDetermined:
                isAllowed = false
                statusDescription = "Permission is required to show session reminders."
            @unknown default:
                isAllowed = false
                statusDescription = "Notification access is unavailable."
            }
        }
    }

    func request() {
        Task {
            let settings = await UNUserNotificationCenter.current().notificationSettings()
            if settings.authorizationStatus == .denied {
                guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
                await UIApplication.shared.open(url)
            } else {
                _ = await SessionNotificationScheduler.shared.requestAuthorization()
                refresh()
            }
        }
    }
}
