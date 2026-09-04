import Foundation
import UserNotifications

enum SessionNotificationSettings {
    static let reminderMinutesKey = "sessionReminderMinutes"
    static let defaultReminderMinutes = 5
    static let validRange = 1...1_440
    static let categoryIdentifier = "SAVED_SESSION_REMINDER"
    nonisolated static let sessionIdKey = "sessionId"
    static let occurrenceKey = "reminderOccurrence"
    static let identifierPrefix = "saved-session."
    static let submittedOccurrencesKey = "submittedSessionReminderOccurrences"
}

struct ReminderSession: Equatable {
    let id: String
    let title: String
    let room: String
    let startTime: String
    let endTime: String
    let isBookmarked: Bool
}

struct SessionReminderPlan {
    let identifier: String
    let occurrence: String
    let scheduledDate: Date
    let isImmediate: Bool
    let content: UNMutableNotificationContent

    var request: UNNotificationRequest {
        // Absolute UTC components preserve the instant across time-zone/DST changes.
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(secondsFromGMT: 0)!
        var components = calendar.dateComponents(
            [.year, .month, .day, .hour, .minute, .second],
            from: Date(timeIntervalSince1970: ceil(scheduledDate.timeIntervalSince1970))
        )
        components.calendar = calendar
        components.timeZone = calendar.timeZone
        let trigger = isImmediate ? nil : UNCalendarNotificationTrigger(
            dateMatching: components, repeats: false
        )
        return UNNotificationRequest(identifier: identifier, content: content, trigger: trigger)
    }

    static func make(
        sessions: [ReminderSession],
        reminderMinutes: Int,
        now: Date,
        timeZone: TimeZone = .current
    ) -> [SessionReminderPlan] {
        let minutes = min(max(reminderMinutes, 1), SessionNotificationSettings.validRange.upperBound)
        let offset = TimeInterval(minutes * 60)
        var plans: [SessionReminderPlan] = []
        for session in sessions where session.isBookmarked {
            if let start = parseDate(session.startTime, timeZone: timeZone), start > now {
                let scheduled = start.addingTimeInterval(-offset)
                plans.append(makePlan(
                    session: session, kind: "before", date: scheduled,
                    isImmediate: scheduled <= now,
                    title: scheduled <= now
                        ? "Starting soon · \(session.title)"
                        : "Starts in \(minutes) min · \(session.title)",
                    subtitle: session.room, body: "Tap to view session details."
                ))
            }
            if let end = parseDate(session.endTime, timeZone: timeZone) {
                let scheduled = end.addingTimeInterval(offset)
                // Do not replay feedback from old sessions when opening the app later.
                if scheduled >= now {
                    plans.append(makePlan(
                        session: session, kind: "after", date: scheduled,
                        isImmediate: scheduled == now,
                        title: "How was \(session.title)?", subtitle: "Your feedback matters",
                        body: "Rate the session and share your comments."
                    ))
                }
            }
        }
        return plans.sorted {
            if $0.scheduledDate != $1.scheduledDate { return $0.scheduledDate < $1.scheduledDate }
            return $0.identifier < $1.identifier
        }
    }

    static func parseDate(_ value: String, timeZone: TimeZone) -> Date? {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let date = formatter.date(from: value) { return date }
        formatter.formatOptions = [.withInternetDateTime]
        if let date = formatter.date(from: value) { return date }

        // Older Firebase/cache entries contain local ISO dates without an offset.
        let local = DateFormatter()
        local.locale = Locale(identifier: "en_US_POSIX")
        local.calendar = Calendar(identifier: .gregorian)
        local.timeZone = timeZone
        local.isLenient = false
        for format in ["yyyy-MM-dd'T'HH:mm:ss.SSS", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm"] {
            local.dateFormat = format
            if let date = local.date(from: value) { return date }
        }
        return nil
    }

    private static func makePlan(
        session: ReminderSession, kind: String, date: Date, isImmediate: Bool,
        title: String, subtitle: String, body: String
    ) -> SessionReminderPlan {
        let identifier = "\(SessionNotificationSettings.identifierPrefix)\(kind).\(session.id)"
        let occurrence = "\(identifier)|\(date.timeIntervalSince1970)"
        let content = UNMutableNotificationContent()
        content.title = title
        content.subtitle = subtitle
        content.body = body
        content.sound = .default
        content.categoryIdentifier = SessionNotificationSettings.categoryIdentifier
        content.threadIdentifier = "saved-sessions"
        content.userInfo = [
            SessionNotificationSettings.sessionIdKey: session.id,
            SessionNotificationSettings.occurrenceKey: occurrence
        ]
        return SessionReminderPlan(
            identifier: identifier, occurrence: occurrence, scheduledDate: date,
            isImmediate: isImmediate, content: content
        )
    }
}
