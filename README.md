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
cd iosApp
pod install   # if using CocoaPods
open iosApp.xcworkspace
# Build & Run in Xcode
```

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
