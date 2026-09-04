# iOS session reminders

These are local iOS notifications, not Firebase push messages. Firebase supplies
the session dates; the device clock determines when each reminder is due.
Only bookmarked sessions are scheduled, after both sessions and bookmarks have
loaded. An initial loading state must not erase previously scheduled requests.

- Before: session start minus the configured reminder minutes (default: 5).
- Feedback: session end plus the same configured minutes.
- Bookmarking within the before-reminder window sends an immediate
  "Starting soon" notification, once per reminder occurrence. There is no
  before-reminder after the session has started.
- Opening the app after a feedback reminder's due time does not replay old feedback.

Future requests use nonrepeating calendar triggers with absolute UTC components.
Timestamps with an offset preserve that instant. Legacy timestamps without an
offset are interpreted in the device's current time zone, like the schedule UI.
The scheduler reconciles on saved-session/settings changes, foreground entry,
significant clock changes, time-zone changes and permission approval. It preserves
unchanged requests and notifications owned by other features. Scheduling errors
are logged as `Unable to schedule session reminder ...` and retried at the next
reconciliation.

## Automated checks

From the repository root:

```sh
swift test --package-path iosApp --scratch-path /tmp/ingevent-notifications-swift-tests
./gradlew :shared:testDebugUnitTest
```

The Swift package compiles the production planner and scheduler against a fake
notification center and an injected clock. It does not ask for permissions, send
real notifications, or read device data. The Kotlin test covers initial readiness.
These checks do not verify banner delivery by iOS on a physical device.

## Physical-device validation

1. Build and install the updated app. Allow notifications, banners and sounds in
   iOS Settings. Disable Focus and scheduled notification summary for this test.
2. Keep automatic date/time enabled. Use a test session with a future start and end
   in a test conference; do not change production event dates for this check.
3. In the app, set reminder minutes to 1 and bookmark the session at least two
   minutes before its start. Wait for the schedule/favorites to load before leaving.
4. Verify the start reminder at `start - 1 minute` and feedback at `end + 1 minute`.
   Repeat with separate future test sessions while the app is foregrounded,
   backgrounded and terminated. Tap a notification to check session navigation.
5. With another future session, deny permission first, then enable it in iOS
   Settings and return to the app. Confirm its future reminders arrive.
6. Bookmark a new session less than one minute before its start. Expect one
   immediate "Starting soon" notification; foregrounding again must not repeat it.
7. For a manual clock-change check, schedule a future session first, then change
   the clock to *before* its reminder time and return to the app. Verify delivery
   when that time is reached. Jumping past a due time is not equivalent to waiting
   through it; old feedback is deliberately not replayed. Restore automatic time
   after the test. Reusing an already delivered occurrence is not a fresh test.

No live delivery on a physical device is asserted by the automated checks.
