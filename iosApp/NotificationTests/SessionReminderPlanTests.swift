import XCTest
import UserNotifications
@testable import SessionNotifications

final class SessionReminderPlanTests: XCTestCase {
    private let utc = TimeZone(secondsFromGMT: 0)!
    private let now = instant("2026-09-29T08:00:00Z")

    func testFutureRemindersUseAbsoluteCalendarDates() throws {
        let plans = SessionReminderPlan.make(sessions: [testSession()], reminderMinutes: 5, now: now)
        XCTAssertEqual(plans.count, 2)
        let before = try XCTUnwrap(plans.first)
        let after = try XCTUnwrap(plans.last)
        XCTAssertEqual(before.scheduledDate, instant("2026-09-29T08:05:00Z"))
        XCTAssertEqual(after.scheduledDate, instant("2026-09-29T09:05:00Z"))
        for plan in plans {
            let trigger = try XCTUnwrap(plan.request.trigger as? UNCalendarNotificationTrigger)
            XCTAssertFalse(trigger.repeats)
            XCTAssertEqual(trigger.dateComponents.timeZone, utc)
            XCTAssertEqual(trigger.dateComponents.year, 2026)
            XCTAssertEqual(trigger.dateComponents.month, 9)
            XCTAssertEqual(trigger.dateComponents.day, 29)
            XCTAssertEqual(trigger.dateComponents.date, plan.scheduledDate)
        }
        XCTAssertEqual(after.content.userInfo[SessionNotificationSettings.sessionIdKey] as? String, "talk")
    }

    func testInsideReminderWindowUsesImmediateDeliveryInsteadOfDroppingIt() throws {
        let plan = try XCTUnwrap(SessionReminderPlan.make(
            sessions: [testSession()], reminderMinutes: 5,
            now: instant("2026-09-29T08:08:00Z")
        ).first)
        XCTAssertTrue(plan.isImmediate)
        XCTAssertNil(plan.request.trigger)
        XCTAssertTrue(plan.content.title.hasPrefix("Starting soon"))
    }

    func testExactReminderBoundaryIsImmediate() throws {
        let plan = try XCTUnwrap(SessionReminderPlan.make(
            sessions: [testSession()], reminderMinutes: 5,
            now: instant("2026-09-29T08:05:00Z")
        ).first)
        XCTAssertTrue(plan.isImmediate)
    }

    func testPreReminderIsNotSentOnceSessionHasStarted() {
        let plans = SessionReminderPlan.make(
            sessions: [testSession()], reminderMinutes: 5,
            now: instant("2026-09-29T08:10:00Z")
        )
        XCTAssertEqual(plans.map(\.identifier), ["saved-session.after.talk"])
    }

    func testExactFeedbackBoundaryIsImmediateButOldFeedbackIsNotReplayed() {
        let plans = SessionReminderPlan.make(
            sessions: [testSession()], reminderMinutes: 5,
            now: instant("2026-09-29T09:05:00Z")
        )
        XCTAssertEqual(plans.count, 1)
        XCTAssertTrue(plans[0].isImmediate)
        XCTAssertTrue(SessionReminderPlan.make(
            sessions: [testSession()], reminderMinutes: 5,
            now: instant("2026-09-29T09:06:00Z")
        ).isEmpty)
    }

    func testInvalidDatesAndUnsavedSessionsProduceNoPlans() {
        let invalid = testSession(start: "invalid", end: "invalid")
        XCTAssertTrue(SessionReminderPlan.make(
            sessions: [invalid, testSession(saved: false)], reminderMinutes: 5, now: now
        ).isEmpty)
    }

    func testLocalFirebaseAndExplicitOffsetDatesReferToSameInstant() {
        let madrid = TimeZone(identifier: "Europe/Madrid")!
        let expected = instant("2026-09-29T08:10:00Z")
        for timestamp in ["2026-09-29T10:10", "2026-09-29T10:10:00", "2026-09-29T10:10:00.000", "2026-09-29T10:10:00+02:00"] {
            XCTAssertEqual(SessionReminderPlan.parseDate(timestamp, timeZone: madrid), expected)
        }
        XCTAssertEqual(
            SessionReminderPlan.parseDate("2026-09-29T08:10:00.000Z", timeZone: madrid), expected
        )
    }

    func testFractionalDatesAreNotRoundedIntoThePast() throws {
        let plan = try XCTUnwrap(SessionReminderPlan.make(
            sessions: [testSession(start: "2026-09-29T08:05:00.900Z")],
            reminderMinutes: 5, now: now.addingTimeInterval(0.5)
        ).first)
        let trigger = try XCTUnwrap(plan.request.trigger as? UNCalendarNotificationTrigger)
        XCTAssertEqual(trigger.dateComponents.date, now.addingTimeInterval(1))
    }
}

func instant(_ value: String) -> Date {
    ISO8601DateFormatter().date(from: value)!
}

func testSession(
    id: String = "talk", start: String = "2026-09-29T08:10:00Z",
    end: String = "2026-09-29T09:00:00Z", saved: Bool = true
) -> ReminderSession {
    ReminderSession(id: id, title: "Test talk", room: "Room A", startTime: start, endTime: end, isBookmarked: saved)
}
