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
5. Configure backend keys for iOS app startup:
   - Open the iOS target in Xcode and add these entries to `Info.plist`:
   - Firebase option:
   - `FIREBASE_FUNCTIONS_URL` = `https://europe-west1-<project-id>.cloudfunctions.net/api`
   - Supabase fallback:
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
-- Firestore + Cloud Functions
-- Native Auth + FCM push notifications
-- Realtime sync
```
**Advantage**: Native Android integration, automatic scaling.

This repository includes a Firebase implementation in `firebase/`. The app uses Firebase when `FIREBASE_FUNCTIONS_URL` is configured; otherwise it keeps using Supabase. See [`firebase/README.md`](firebase/README.md) for setup, deploy, and Firestore import steps.

## 📋 Features

- [x] Day-based schedule with visual timeline
- [x] Event days dynamically computed from remote data
- [x] Track filters (AI/ML, Android, Web, Cloud, Firebase, Flutter, Design)
- [x] Session detail screen with speakers and metadata
- [x] Bookmark system (offline-first with SQLDelight)
- [x] Session search
- [x] Speakers screen
- [x] Session capacity bar
- [x] Bottom navigation (Android) / Tab view (iOS)
- [x] Branded dark theme
- [x] Offline-first flow: local cache -> server sync
- [x] Web admin panel for speakers/sessions (`admin-panel/`)

## 🧰 Web Admin Panel

The project includes an admin panel in `admin-panel/` to create/edit/delete `speakers` and `sessions` in Supabase.

### Run

```bash
cd admin-panel
python3 -m http.server 8080
```

Open: `http://localhost:8080`

To bootstrap a new backend from scratch, use:

- `supabase/schema.sql`
- `supabase/seed.sql`

## 📋 Backlog

- [ ] Push notifications (session reminders)
- [ ] Venue map with Google Maps SDK
- [ ] Built-in livestream
- [ ] QR check-in
- [ ] Post-session feedback
- [ ] Export schedule to Google Calendar
- [ ] Login with Google Sign-In
- [ ] Compose Multiplatform (shared UI between Android and iOS)
