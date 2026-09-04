import XCTest

final class StoreCaptureTests: XCTestCase {
    @MainActor
    func testCaptureScreens() throws {
        continueAfterFailure = false
        XCUIDevice.shared.orientation = .portrait
        let app = XCUIApplication(bundleIdentifier: "com.ing.event")
        app.launchArguments = ["-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
        app.launch()
        XCTAssertTrue(app.buttons["Schedule"].firstMatch.waitForExistence(timeout: 30))
        app.buttons["Schedule"].firstMatch.tap()
        XCTAssertTrue(app.buttons["Android"].firstMatch.waitForExistence(timeout: 180))
        XCTAssertTrue(app.staticTexts["Breakfast"].firstMatch.waitForExistence(timeout: 180))
        app.buttons["Android"].firstMatch.tap()
        let title = app.staticTexts["Design and Implementation of Desktop Debug Tools for Android Development"].firstMatch
        XCTAssertTrue(title.waitForExistence(timeout: 30))
        pause(3)
        capture("01-schedule")
        title.tap()
        XCTAssertTrue(app.staticTexts["About this session"].waitForExistence(timeout: 180))
        pause(3)
        capture("03-session-detail")
        for _ in 0..<8 {
            if app.buttons["Save Session"].isHittable || app.buttons["Saved Session"].isHittable { break }
            app.swipeUp()
        }
        if app.buttons["Save Session"].isHittable { app.buttons["Save Session"].tap(); pause(2) }
        let speaker = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "Jakub Biliński")).firstMatch
        for _ in 0..<8 { if speaker.isHittable { break }; app.swipeUp() }
        XCTAssertTrue(speaker.isHittable)
        speaker.tap()
        XCTAssertTrue(app.staticTexts["SESSIONS"].waitForExistence(timeout: 180))
        pause(3)
        capture("04-speaker")
        app.buttons.matching(NSPredicate(format: "identifier == 'xmark' OR label == 'Close'")).firstMatch.tap()
        XCTAssertTrue(app.staticTexts["About this session"].waitForExistence(timeout: 180))
        for _ in 0..<10 { if app.buttons["Submit Feedback"].isHittable { break }; app.swipeUp() }
        pause(3)
        capture("07-feedback")
        app.buttons.matching(NSPredicate(format: "identifier == 'xmark' OR label == 'Close'")).firstMatch.tap()
        app.buttons["Favorites"].firstMatch.tap()
        XCTAssertTrue(title.waitForExistence(timeout: 180))
        pause(3)
        capture("02-favorites")
        app.buttons["Home"].firstMatch.tap()
        pause(3)
        capture("08-home")
        app.buttons["Map"].firstMatch.tap()
        pause(50)
        capture("05-map")
        app.buttons["Settings"].firstMatch.tap()
        pause(2)
        capture("06-settings")
    }

    @MainActor
    func testRefineFavoritesAndFeedback() throws {
        continueAfterFailure = false
        let app = XCUIApplication(bundleIdentifier: "com.ing.event")
        app.launchArguments = ["-AppleLanguages", "(en)", "-AppleLocale", "en_US"]
        app.launch()
        XCTAssertTrue(app.buttons["Favorites"].firstMatch.waitForExistence(timeout: 30))
        app.buttons["Favorites"].firstMatch.tap()
        let title = app.staticTexts["Design and Implementation of Desktop Debug Tools for Android Development"].firstMatch
        XCTAssertTrue(title.waitForExistence(timeout: 180))
        pause(3)
        capture("02-favorites")
        title.tap()
        XCTAssertTrue(app.staticTexts["About this session"].waitForExistence(timeout: 180))
        for _ in 0..<10 { if app.buttons["Submit Feedback"].isHittable { break }; app.swipeUp() }
        app.swipeUp()
        pause(3)
        capture("07-feedback")
    }

    @MainActor
    private func capture(_ name: String) {
        let attachment = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

    private func pause(_ seconds: TimeInterval) {
        RunLoop.current.run(until: Date().addingTimeInterval(seconds))
    }
}
