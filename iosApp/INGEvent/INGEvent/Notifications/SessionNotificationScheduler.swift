import Foundation
import UserNotifications

@MainActor
protocol SessionNotificationCenter {
    func authorizationStatus() async -> UNAuthorizationStatus
    func requestAuthorization() async throws -> Bool
    func pendingRequests() async -> [UNNotificationRequest]
    func add(_ request: UNNotificationRequest) async throws
    func removePending(identifiers: [String])
    func removeDeliveredSessionNotifications() async
    func registerCategory()
}

@MainActor
final class SystemSessionNotificationCenter: SessionNotificationCenter {
    private let center = UNUserNotificationCenter.current()

    func authorizationStatus() async -> UNAuthorizationStatus {
        await center.notificationSettings().authorizationStatus
    }

    func requestAuthorization() async throws -> Bool {
        try await center.requestAuthorization(options: [.alert, .badge, .sound])
    }

    func pendingRequests() async -> [UNNotificationRequest] {
        await center.pendingNotificationRequests()
    }

    func add(_ request: UNNotificationRequest) async throws {
        try await center.add(request)
    }

    func removePending(identifiers: [String]) {
        center.removePendingNotificationRequests(withIdentifiers: identifiers)
    }

    func removeDeliveredSessionNotifications() async {
        let identifiers = await center.deliveredNotifications().map(\.request.identifier)
            .filter { $0.hasPrefix(SessionNotificationSettings.identifierPrefix) }
        center.removeDeliveredNotifications(withIdentifiers: identifiers)
    }

    func registerCategory() {
        center.setNotificationCategories([UNNotificationCategory(
            identifier: SessionNotificationSettings.categoryIdentifier,
            actions: [], intentIdentifiers: [], options: [.customDismissAction]
        )])
    }
}

@MainActor
final class SessionNotificationScheduler {
    static let shared = SessionNotificationScheduler(center: SystemSessionNotificationCenter())

    private let center: SessionNotificationCenter
    private let defaults: UserDefaults
    private let now: () -> Date
    private let timeZone: () -> TimeZone
    private let reportError: (String) -> Void
    private var snapshot: Snapshot?
    private var revision = 0
    private var worker: Task<Void, Never>?
    private var submittedOccurrences: [String: String]

    init(
        center: SessionNotificationCenter, defaults: UserDefaults = .standard,
        now: @escaping () -> Date = Date.init,
        timeZone: @escaping () -> TimeZone = { .current },
        reportError: @escaping (String) -> Void = { print($0) }
    ) {
        self.center = center
        self.defaults = defaults
        self.now = now
        self.timeZone = timeZone
        self.reportError = reportError
        submittedOccurrences = defaults.dictionary(
            forKey: SessionNotificationSettings.submittedOccurrencesKey
        ) as? [String: String] ?? [:]
    }

    func registerCategory() { center.registerCategory() }

    func requestAuthorization() async -> Bool {
        do {
            let granted = try await center.requestAuthorization()
            if granted { refresh() }
            return granted
        } catch {
            reportError("Unable to request notification permission: \(error.localizedDescription)")
            return false
        }
    }

    func reschedule(sessions: [ReminderSession], reminderMinutes: Int, isDataReady: Bool) {
        // An initial empty state is not the same as the user removing every favorite.
        guard isDataReady else { return }
        snapshot = Snapshot(sessions: sessions, reminderMinutes: reminderMinutes)
        refresh()
    }

    // Called after permissions, foreground entry, clock changes and time-zone changes.
    func refresh() {
        guard snapshot != nil else { return }
        revision += 1
        guard worker == nil else { return }
        worker = Task { [weak self] in
            guard let self else { return }
            // Serialize replacements. If input changes during an await, finish with the newest snapshot.
            while let snapshot = self.snapshot {
                let revision = self.revision
                await self.reconcile(snapshot, revision: revision)
                if revision == self.revision { break }
            }
            self.worker = nil
        }
    }

    func cancelAll() {
        snapshot = Snapshot(sessions: [], reminderMinutes: 5, clearDelivered: true)
        refresh()
    }

    func waitUntilIdle() async { await worker?.value }

    private func reconcile(_ snapshot: Snapshot, revision: Int) async {
        let savedSessions = snapshot.sessions.filter(\.isBookmarked)
        if !savedSessions.isEmpty {
            var status = await center.authorizationStatus()
            guard revision == self.revision else { return }
            if status == .notDetermined {
                do {
                    guard try await center.requestAuthorization() else { return }
                    status = await center.authorizationStatus()
                } catch {
                    reportError("Unable to request notification permission: \(error.localizedDescription)")
                    return
                }
            }
            guard revision == self.revision else { return }
            var isAllowed = status == .authorized || status == .provisional
            #if os(iOS)
            isAllowed = isAllowed || status == .ephemeral
            #endif
            guard isAllowed else { return }
        }

        let pending = await center.pendingRequests()
        guard revision == self.revision else { return }
        let pendingById = Dictionary(pending.map { ($0.identifier, $0) }, uniquingKeysWith: { _, last in last })
        let unrelatedCount = pending.filter { !isSessionReminder($0.identifier) }.count
        let plans = SessionReminderPlan.make(
            sessions: savedSessions, reminderMinutes: snapshot.reminderMinutes,
            now: now(), timeZone: timeZone()
        ).filter { plan in
            // Keep a successful catch-up from being sent again after it was delivered/dismissed.
            !(plan.isImmediate && submittedOccurrences[plan.identifier] == plan.occurrence &&
              pendingById[plan.identifier] == nil)
        }.prefix(max(0, 64 - unrelatedCount))

        let desiredIds = Set(plans.map(\.identifier))
        let obsolete = pending.map(\.identifier).filter { isSessionReminder($0) && !desiredIds.contains($0) }
        center.removePending(identifiers: obsolete)

        let savedIds = Set(savedSessions.flatMap { session in
            ["before", "after"].map { "\(SessionNotificationSettings.identifierPrefix)\($0).\(session.id)" }
        })
        submittedOccurrences = submittedOccurrences.filter { savedIds.contains($0.key) }

        for plan in plans {
            guard revision == self.revision else { return }
            let request = plan.request
            if let existing = pendingById[plan.identifier], sameRequest(existing, request) {
                submittedOccurrences[plan.identifier] = plan.occurrence
                continue
            }
            do {
                // Adding the same identifier replaces it without deleting unrelated valid requests first.
                try await center.add(request)
                submittedOccurrences[plan.identifier] = plan.occurrence
                persistOccurrences()
            } catch {
                reportError("Unable to schedule session reminder \(plan.identifier): \(error.localizedDescription)")
            }
        }
        persistOccurrences()
        if snapshot.clearDelivered { await center.removeDeliveredSessionNotifications() }
    }

    private func persistOccurrences() {
        defaults.set(submittedOccurrences, forKey: SessionNotificationSettings.submittedOccurrencesKey)
    }

    private func isSessionReminder(_ identifier: String) -> Bool {
        identifier.hasPrefix(SessionNotificationSettings.identifierPrefix)
    }

    private func sameRequest(_ lhs: UNNotificationRequest, _ rhs: UNNotificationRequest) -> Bool {
        let sameTrigger = lhs.trigger == nil && rhs.trigger == nil ||
            (lhs.trigger?.isEqual(rhs.trigger) ?? false)
        return sameTrigger && lhs.content.isEqual(rhs.content)
    }

    private struct Snapshot {
        let sessions: [ReminderSession]
        let reminderMinutes: Int
        var clearDelivered = false
    }
}
