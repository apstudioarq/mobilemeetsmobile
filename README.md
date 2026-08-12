# 📱 Mobile Meets Mobile Clone - Kotlin Multiplatform (KMM)

A full clone of the Mobile Meets Mobile app built with **Kotlin Multiplatform Mobile**, sharing business logic between Android (Jetpack Compose) and iOS (SwiftUI).

## 🏗️ Architecture

```
┌──────────────────────────────────────────────────┐
│                    shared/                        │
│  ┌──────────┐  ┌──────────┐  ┌────────────────┐  │
│  │  Models   │  │   Data   │  │  Presentation  │  │
│  │ Session   │  │ Ktor API │  │  ViewModels    │  │
│  │ Speaker   │  │ SQLDelight│  │  UiStates      │  │
│  │ Track     │  │ Repos    │  │                │  │
│  └──────────┘  └──────────┘  └────────────────┘  │
│  ┌──────────┐  ┌──────────┐                       │
│  │  Domain   │  │    DI    │                       │
│  │ UseCases  │  │   Koin   │                       │
│  └──────────┘  └──────────┘                       │
└───────────────────┬──────────────────┬───────────┘
                    │                  │
          ┌─────────▼────────┐ ┌───────▼──────────┐
          │   androidApp/    │ │    iosApp/        │
          │ Jetpack Compose  │ │    SwiftUI        │
          │ Material 3       │ │    Native iOS UI  │
          │ Navigation       │ │    TabView        │
          └──────────────────┘ └──────────────────┘
```

## 📂 Project Structure

| Module | Content |
|--------|---------|
| `shared/commonMain` | Models, API client, repositories, use cases, ViewModels, DI |
| `shared/androidMain` | SQLDelight Android driver, platform DI |
| `shared/iosMain` | SQLDelight iOS driver, platform DI, Koin init |
| `androidApp` | Jetpack Compose UI, navigation, Material 3 theme |
| `iosApp` | SwiftUI UI, native iOS views |

## 🔧 Tech Stack

### Shared (Kotlin Multiplatform)
- **Ktor** - Multiplatform HTTP client
- **kotlinx.serialization** - JSON serialization
- **kotlinx.datetime** - Multiplatform dates
- **SQLDelight** - Local database (offline cache)
- **Koin** - Dependency injection
- **Coroutines + Flow** - Reactive async programming

### Android
- **Jetpack Compose** - Declarative UI
- **Material 3** - Design system
- **Navigation Compose** - Navigation
- **Coil** - Image loading

### iOS
- **SwiftUI** - Native iOS UI
- **Combine** - State observation from KMM

## 🚀 How to Run

### Android
```bash
./gradlew :androidApp:installDebug
```

### iOS
```bash
./gradlew :shared:prepareSharedSpm
# Add local package iosApp/SharedSPM in Xcode and run
```

#### iOS detailed setup

1. Install prerequisites:
   - Xcode 15+
2. Generate the KMM XCFramework wrapped for SPM:
   - Run `./gradlew :shared:prepareSharedSpm`
   - This generates `iosApp/SharedSPM/shared.xcframework`
3. Open your iOS app project in Xcode and add the local package:
   - `File` -> `Add Package Dependencies...`
   - `Add Local...`
   - Select folder `iosApp/SharedSPM`
   - Add product `SharedKMM` to your app target
4. Ensure SQLite linker flag is present in your app target:
   - `Build Settings` -> `Other Linker Flags` includes `-lsqlite3`
5. Configure Firebase for iOS app startup:
   - Place `GoogleService-Info.plist` in `iosApp/MobileMeetsMobile/MobileMeetsMobile`.
   - Ensure its `BUNDLE_ID` matches the target bundle identifier (`com.mobilemeetsmobile.ios`).
   - The app reads `DATABASE_URL` and `API_KEY` from this file automatically.
   - Optionally define `FIREBASE_CONFERENCE_ID` as a user-defined Xcode build setting.
   - Legacy Supabase fallback:
   - `SUPABASE_URL` = `https://<your-project>.supabase.co`
   - `SUPABASE_ANON_KEY` = `<your-anon-key>`
6. Open and run:
   - Select an iOS Simulator (for example, iPhone 16 Pro)
   - Press `Cmd + R`

#### Troubleshooting

- If you get module errors for `shared`, rebuild the KMM framework first:
```bash
./gradlew :shared:prepareSharedSpm
```
- This repository currently contains iOS source files but does not include a committed `.xcodeproj` or `.xcworkspace`.
  To launch on iOS, restore the project file or create an Xcode iOS app target and add local package `iosApp/SharedSPM`.

## 🖥️ Backend Options

### Option 1: Ktor Server (Recommended - Full Kotlin)
```kotlin
// Share models with shared/
// Deploy: Google Cloud Run / Railway / Fly.io
// DB: PostgreSQL + Exposed ORM
```
**Advantage**: 100% Kotlin, shared models between client and server.

### Option 2: Supabase (Fast)
```sql
-- Auto-generated REST API from PostgreSQL
-- Auth, Realtime, Storage included
-- Generous free tier
```
**Advantage**: Backend in minutes, real PostgreSQL.

### Option 3: Firebase  
```
-- Realtime Database REST API
-- Native Auth + FCM push notifications
-- Realtime sync
```
**Advantage**: Native Android integration, automatic scaling.

The app now reads conference data from Firebase Realtime Database by default using `FIREBASE_DATABASE_URL`. When no `FIREBASE_CONFERENCE_ID` is set, it picks the conference whose title contains `Mobile Meets Mobile`; set `FIREBASE_CONFERENCE_ID` to force a specific conference.

### Firebase anonymous authentication

Realtime Database requests are authenticated automatically without showing a login screen. The shared client creates one anonymous Firebase session per installation, persists its refresh token locally, refreshes the ID token before expiry, and retries one database request after a `401`.

For Android, place the Firebase configuration at `androidApp/google-services.json`. The
build reads the Realtime Database URL and the API key from the client whose package is
`com.mobilemeetsmobile.android`.

You can override those values, or set an optional conference filter, in the untracked
`local.properties` file:

```properties
FIREBASE_DATABASE_URL=https://ingtechrating-default-rtdb.europe-west1.firebasedatabase.app
FIREBASE_API_KEY=<Firebase Web API key>
FIREBASE_CONFERENCE_ID=A0C691FD-E111-4436-8B51-2EF97C14E548
```

For iOS, place the Firebase configuration at
`iosApp/MobileMeetsMobile/MobileMeetsMobile/GoogleService-Info.plist`. You can override
`FIREBASE_DATABASE_URL` or `FIREBASE_API_KEY`, and optionally define
`FIREBASE_CONFERENCE_ID`, as user-defined Xcode build settings.

Enable the Anonymous provider in Firebase Authentication and deploy the Realtime Database rules from `firebase/database.rules.json`.
The mobile app can read conferences and submit anonymous ratings; the web admin panel can write conferences and read rating statistics only when the signed-in UID exists under `/admins/{uid}: true`.

Do not place a service-account JSON, private key, database secret, or shared email/password in either app.

## 📋 Features

- [x] Day-based schedule with visual timeline
- [x] Event days dynamically computed from remote data
- [x] Track filters (AI/ML, Android, iOS, Generic, Web, Cloud, Firebase, Flutter, Design)
- [x] Session detail screen with speakers and metadata
- [x] Bookmark system (offline-first with SQLDelight)
- [x] Session search
- [x] Speakers screen
- [x] Session capacity bar
- [x] Bottom navigation (Android) / Tab view (iOS)
- [x] Branded dark theme
- [x] Offline-first flow: local cache -> server sync
- [x] Web admin panel for Firebase conferences and ratings (`admin-panel/`)

## 🧰 Web Admin Panel

The project includes an admin panel in `admin-panel/` to create/edit/delete Firebase Realtime Database conferences and inspect session rating statistics.

### Run

```bash
firebase emulators:start --only hosting,database,auth
```

Open: `http://localhost:5000`

To deploy the panel to Firebase Hosting with Realtime Database rules:

```bash
firebase deploy --only hosting,database
```

Enable the Google provider in Firebase Authentication, link the Firebase Web App to the Hosting site, sign in with the admin Google account, and add its UID under `/admins/{uid}: true` before using the hosted panel.

## 📋 Backlog

- [ ] Push notifications (session reminders)
- [ ] Venue map with Google Maps SDK
- [ ] Built-in livestream
- [ ] QR check-in
- [ ] Post-session feedback
- [ ] Export schedule to Google Calendar
- [ ] Login with Google Sign-In
- [ ] Compose Multiplatform (shared UI between Android and iOS)
