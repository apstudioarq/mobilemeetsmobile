import XCTest
import UserNotifications
@testable import SessionNotifications

final class SessionNotificationSchedulerTests: XCTestCase {
    @MainActor
    func testInitialLoadingDoesNotDeletePendingNotifications() async {
        let fixture = Fixture()
        fixture.center.pending = [fixture.existingReminder()]
        fixture.scheduler.reschedule(sessions: [], reminderMinutes: 5, isDataReady: false)
        await fixture.scheduler.waitUntilIdle()
        XCTAssertEqual(fixture.center.pending.count, 1)
        XCTAssertTrue(fixture.center.removed.isEmpty)
    }

    @MainActor
    func testLoadedEmptyFavoritesOnlyRemoveSessionReminders() async {
        let fixture = Fixture()
        fixture.center.pending = [fixture.existingReminder(), fixture.unrelatedRequest()]
        fixture.scheduler.reschedule(sessions: [], reminderMinutes: 5, isDataReady: true)
        await fixture.scheduler.waitUntilIdle()
        XCTAssertEqual(fixture.center.pending.map(\.identifier), ["other-feature"])
    }

    @MainActor
    func testPermissionGrantedInSettingsIsPickedUpOnForegroundRefresh() async {
        let fixture = Fixture()
        fixture.center.status = .denied
        fixture.submit()
        await fixture.scheduler.waitUntilIdle()
        XCTAssertTrue(fixture.center.added.isEmpty)
        fixture.center.status = .authorized
        fixture.scheduler.refresh()
        await fixture.scheduler.waitUntilIdle()
        XCTAssertEqual(fixture.center.pending.count, 2)
    }

    @MainActor
    func testExplicitPermissionRequestReschedulesLatestFavorites() async {
        let fixture = Fixture()
        fixture.center.status = .denied
        fixture.submit()
        await fixture.scheduler.waitUntilIdle()
        let granted = await fixture.scheduler.requestAuthorization()
        XCTAssertTrue(granted)
        await fixture.scheduler.waitUntilIdle()
        XCTAssertEqual(fixture.center.pending.count, 2)
    }

    @MainActor
    func testFirstPermissionPromptSchedulesAndDenialDoesNot() async {
        let fixture = Fixture()
        fixture.center.status = .notDetermined
        fixture.center.grantPermission = false
        fixture.submit()
        await fixture.scheduler.waitUntilIdle()
        XCTAssertEqual(fixture.center.permissionRequests, 1)
        XCTAssertTrue(fixture.center.added.isEmpty)
        fixture.center.status = .notDetermined
        fixture.center.grantPermission = true
        fixture.scheduler.refresh()
        await fixture.scheduler.waitUntilIdle()
        XCTAssertEqual(fixture.center.pending.count, 2)
    }

    @MainActor
    func testClockJumpReplacesOverduePendingBeforeReminderWithImmediateDelivery() async {
        let fixture = Fixture()
        fixture.submit()
        await fixture.scheduler.waitUntilIdle()
        fixture.clock.now = instant("2026-09-29T08:08:00Z")
        fixture.scheduler.refresh()
        await fixture.scheduler.waitUntilIdle()
        let before = fixture.center.pending.first { $0.identifier.contains(".before.") }
        XCTAssertNotNil(before)
        XCTAssertNil(before?.trigger)
        XCTAssertNotNil(fixture.center.pending.first { $0.identifier.contains(".after.") }?.trigger as? UNCalendarNotificationTrigger)
    }

    @MainActor
    func testDeliveredCatchUpIsNotRepeatedEvenAfterSchedulerIsRecreated() async {
        let fixture = Fixture()
        fixture.clock.now = instant("2026-09-29T08:08:00Z")
        fixture.submit()
        await fixture.scheduler.waitUntilIdle()
        fixture.center.pending.removeAll { $0.identifier.contains(".before.") }
        fixture.scheduler.refresh()
        await fixture.scheduler.waitUntilIdle()
        let nextScheduler = fixture.makeScheduler()
        nextScheduler.reschedule(sessions: [testSession()], reminderMinutes: 5, isDataReady: true)
        await nextScheduler.waitUntilIdle()
        XCTAssertEqual(fixture.center.added.filter { $0.identifier.contains(".before.") }.count, 1)
    }

    @MainActor
    func testUnchangedRefreshKeepsPendingRemindersInPlace() async {
        let fixture = Fixture()
        fixture.submit()
        await fixture.scheduler.waitUntilIdle()
        fixture.scheduler.refresh()
        await fixture.scheduler.waitUntilIdle()
        XCTAssertEqual(fixture.center.added.count, 2)
        XCTAssertTrue(fixture.center.removed.isEmpty)
    }

    @MainActor
    func testDeliveredCalendarReminderIsNotSentAgainInsideBeforeWindow() async {
        let fixture = Fixture()
        fixture.submit()
        await fixture.scheduler.waitUntilIdle()
        fixture.center.pending.removeAll { $0.identifier.contains(".before.") }
        fixture.clock.now = instant("2026-09-29T08:08:00Z")
        fixture.scheduler.refresh()
        await fixture.scheduler.waitUntilIdle()
        XCTAssertEqual(fixture.center.added.count, 2)
        XCTAssertEqual(fixture.center.pending.map(\.identifier), ["saved-session.after.talk"])
    }

    @MainActor
    func testUpdatedSessionTimesReplaceBothRequests() async {
        let fixture = Fixture()
        fixture.submit()
        await fixture.scheduler.waitUntilIdle()
        fixture.scheduler.reschedule(
            sessions: [testSession(start: "2026-09-29T08:30:00Z", end: "2026-09-29T09:30:00Z")],
            reminderMinutes: 5, isDataReady: true
        )
        await fixture.scheduler.waitUntilIdle()
        XCTAssertEqual(fixture.center.pending.count, 2)
        XCTAssertEqual(fixture.center.pending.compactMap {
            ($0.trigger as? UNCalendarNotificationTrigger)?.dateComponents.date
        }, [instant("2026-09-29T08:25:00Z"), instant("2026-09-29T09:35:00Z")])
    }

    @MainActor
    func testReminderPreferenceChangeUpdatesBeforeAndFeedbackDates() async {
        let fixture = Fixture()
        fixture.submit()
        await fixture.scheduler.waitUntilIdle()
        fixture.scheduler.reschedule(sessions: [testSession()], reminderMinutes: 1, isDataReady: true)
        await fixture.scheduler.waitUntilIdle()
        XCTAssertEqual(fixture.center.pending.compactMap {
            ($0.trigger as? UNCalendarNotificationTrigger)?.dateComponents.date
        }, [instant("2026-09-29T08:09:00Z"), instant("2026-09-29T09:01:00Z")])
    }

    @MainActor
    func testTimeZoneChangeReinterpretsLocalDatesButKeepsExplicitInstants() async {
        let fixture = Fixture()
        let sessions = [testSession(), testSession(
            id: "local", start: "2026-09-29T10:10:00", end: "2026-09-29T11:00:00"
        )]
        fixture.scheduler.reschedule(sessions: sessions, reminderMinutes: 5, isDataReady: true)
        await fixture.scheduler.waitUntilIdle()
        fixture.clock.timeZone = TimeZone(identifier: "Europe/Madrid")!
        fixture.scheduler.refresh()
        await fixture.scheduler.waitUntilIdle()
        let beforeDates = fixture.center.pending.filter { $0.identifier.contains(".before.") }.compactMap {
            ($0.trigger as? UNCalendarNotificationTrigger)?.dateComponents.date
        }
        XCTAssertEqual(beforeDates, [instant("2026-09-29T08:05:00Z"), instant("2026-09-29T08:05:00Z")])
        XCTAssertEqual(fixture.center.added.filter { $0.identifier.hasSuffix(".talk") }.count, 2)
        XCTAssertEqual(fixture.center.added.filter { $0.identifier.hasSuffix(".local") }.count, 4)
    }

    @MainActor
    func testAStalePermissionCallbackCannotRecreateRemovedFavorites() async {
        let fixture = Fixture()
        fixture.center.beforeStatus = {
            fixture.scheduler.reschedule(sessions: [], reminderMinutes: 5, isDataReady: true)
        }
        fixture.submit()
        await fixture.scheduler.waitUntilIdle()
        XCTAssertTrue(fixture.center.pending.isEmpty)
        XCTAssertTrue(fixture.center.added.isEmpty)
    }

    @MainActor
    func testCancelDuringAdditionFinishesWithoutAnySessionReminders() async {
        let fixture = Fixture()
        fixture.center.afterAdd = { fixture.scheduler.cancelAll() }
        fixture.submit()
        await fixture.scheduler.waitUntilIdle()
        XCTAssertTrue(fixture.center.pending.isEmpty)
        XCTAssertEqual(fixture.center.deliveredClears, 1)
    }

    @MainActor
    func testAddErrorsAreReportedAndRetriedWithoutDeletingUnrelatedRequests() async {
        let fixture = Fixture()
        fixture.center.pending = [fixture.unrelatedRequest()]
        fixture.center.failAdd = true
        fixture.submit()
        await fixture.scheduler.waitUntilIdle()
        XCTAssertEqual(fixture.log.messages.count, 2)
        XCTAssertEqual(fixture.center.pending.map(\.identifier), ["other-feature"])
        fixture.center.failAdd = false
        fixture.scheduler.refresh()
        await fixture.scheduler.waitUntilIdle()
        XCTAssertEqual(fixture.center.pending.count, 3)
    }

    @MainActor
    func testCapacityIncludesRequestsOwnedByOtherFeatures() async {
        let fixture = Fixture()
        fixture.center.pending = [fixture.unrelatedRequest()]
        fixture.scheduler.reschedule(
            sessions: (1...40).map { testSession(id: "talk-\($0)") },
            reminderMinutes: 5, isDataReady: true
        )
        await fixture.scheduler.waitUntilIdle()
        XCTAssertEqual(fixture.center.pending.count, 64)
        XCTAssertTrue(fixture.center.pending.contains { $0.identifier == "other-feature" })
    }
}

@MainActor
private final class Fixture {
    let center = FakeNotificationCenter()
    let defaults: UserDefaults
    let suiteName = "SessionNotificationsTests.\(UUID().uuidString)"
    let clock = TestClock()
    let log = ErrorLog()
    lazy var scheduler = makeScheduler()

    init() { defaults = UserDefaults(suiteName: suiteName)! }
    deinit { defaults.removePersistentDomain(forName: suiteName) }

    func makeScheduler() -> SessionNotificationScheduler {
        SessionNotificationScheduler(
            center: center, defaults: defaults, now: { [clock] in clock.now },
            timeZone: { [clock] in clock.timeZone },
            reportError: { [log] in log.messages.append($0) }
        )
    }

    func submit() {
        scheduler.reschedule(sessions: [testSession()], reminderMinutes: 5, isDataReady: true)
    }

    func existingReminder() -> UNNotificationRequest {
        SessionReminderPlan.make(sessions: [testSession()], reminderMinutes: 5, now: clock.now)[0].request
    }

    func unrelatedRequest() -> UNNotificationRequest {
        UNNotificationRequest(identifier: "other-feature", content: UNMutableNotificationContent(), trigger: nil)
    }
}

private final class TestClock {
    var now = instant("2026-09-29T08:00:00Z")
    var timeZone = TimeZone(secondsFromGMT: 0)!
}
private final class ErrorLog { var messages: [String] = [] }

@MainActor
private final class FakeNotificationCenter: SessionNotificationCenter {
    var status = UNAuthorizationStatus.authorized
    var grantPermission = true
    var permissionRequests = 0
    var pending: [UNNotificationRequest] = []
    var added: [UNNotificationRequest] = []
    var removed: [String] = []
    var deliveredClears = 0
    var failAdd = false
    var beforeStatus: (() -> Void)?
    var afterAdd: (() -> Void)?

    func authorizationStatus() async -> UNAuthorizationStatus {
        let callback = beforeStatus
        beforeStatus = nil
        callback?()
        return status
    }

    func requestAuthorization() async throws -> Bool {
        permissionRequests += 1
        status = grantPermission ? .authorized : .denied
        return grantPermission
    }

    func pendingRequests() async -> [UNNotificationRequest] { pending }

    func add(_ request: UNNotificationRequest) async throws {
        if failAdd { throw NSError(domain: "TestSchedulingError", code: 1) }
        added.append(request)
        pending.removeAll { $0.identifier == request.identifier }
        pending.append(request)
        let callback = afterAdd
        afterAdd = nil
        callback?()
    }

    func removePending(identifiers: [String]) {
        removed.append(contentsOf: identifiers)
        pending.removeAll { identifiers.contains($0.identifier) }
    }

    func removeDeliveredSessionNotifications() async { deliveredClears += 1 }
    func registerCategory() {}
}
