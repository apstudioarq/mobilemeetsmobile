// swift-tools-version: 5.9
import PackageDescription

// Tests the same notification sources included by the iOS app, using a fake
// notification center. No device permissions or real notifications are needed.
let package = Package(
    name: "SessionNotifications",
    platforms: [.macOS(.v12), .iOS(.v15)],
    targets: [
        .target(name: "SessionNotifications", path: "INGEvent/INGEvent/Notifications"),
        .testTarget(
            name: "SessionNotificationsTests", dependencies: ["SessionNotifications"],
            path: "NotificationTests"
        )
    ]
)
