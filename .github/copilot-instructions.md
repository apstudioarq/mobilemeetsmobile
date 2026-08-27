# Copilot Instructions for ING Event KMP

## Architecture Overview

This is a **Kotlin Multiplatform (KMP)** project sharing business logic between Android and iOS. The architecture follows clean architecture patterns with clear separation:

```
shared/ (Kotlin Multiplatform)
├── commonMain/        # Core business logic (models, API, DB, DI)
│   ├── data/          # Repositories, DTOs, API clients, local DB
│   ├── domain/        # Use cases (business logic)
│   └── presentation/  # ViewModels with StateFlow for reactive UI
├── androidMain/       # Android-specific (Ktor Android driver, SQLDelight Android)
└── iosMain/          # iOS-specific (Ktor Darwin driver, SQLDelight iOS)

androidApp/           # Android UI
├── Jetpack Compose   # Declarative UI
├── Material 3        # Design system
└── Navigation Compose

iosApp/               # iOS UI
└── SwiftUI           # Native iOS UI
```

### Key Architectural Patterns

1. **Offline-First Data Flow**: Local SQLite cache syncs with remote (Firebase/Supabase)
   - Repositories fetch from local first, fallback to remote
   - Firebase auth is transparent with cached tokens (anonymous)
   - `LocalDataSource` manages SQLDelight queries

2. **Dependency Injection** via Koin:
   - `SharedModule` in `shared/src/commonMain/kotlin/com/ingevent/di/SharedModule.kt`
   - Single instances: HTTP client, repositories, API client
   - Factory instances: use cases (stateless) and ViewModels (stateful)
   - Platform-specific modules via `expect/actual` pattern

3. **ViewModel + StateFlow Pattern**:
   - All ViewModels inherit state management logic
   - Use cases are injected, not instantiated
   - State stored in `UiState` data classes (e.g., `ScheduleUiState`)
   - Updates via `StateFlow.update { copy(...) }`

4. **Repository Pattern**:
   - `SessionRepository`, `SpeakerRepository`, `RatingRepository`
   - Each repository coordinates local cache + remote API
   - Handle offline scenarios gracefully

## Build & Test Commands

### Android

```bash
# Build debug APK
./gradlew :androidApp:assembleDebug

# Install and run on device/emulator
./gradlew :androidApp:installDebug

# Run linting
./gradlew :androidApp:lint

# Run unit tests
./gradlew test
```

### iOS

```bash
# Generate KMM XCFramework (wrapped for Swift Package Manager)
./gradlew :shared:prepareSharedSpm

# Build and test iOS targets
./gradlew iosSimulatorArm64Test

# Compile iOS framework for all targets
./gradlew :shared:assembleXCFramework
```

### Shared Module

```bash
# Build shared library for all targets
./gradlew :shared:build

# Run all tests (all platforms)
./gradlew allTests

# Compile specific iOS target
./gradlew :shared:compileKotlinIosArm64
```

## Key Conventions & Patterns

### Code Organization

- **Models** (`data/model/`): `Session`, `Speaker`, `ConferenceDay`, `Track` — define domain objects
- **DTOs** (`data/remote/dto/`): Request/response shapes — use `@Serializable` from `kotlinx.serialization`
- **Repositories** (`data/repository/`): Coordinate local + remote sources
- **Use Cases** (`domain/usecase/`): Single-responsibility functions (e.g., `GetScheduleUseCase`, `ToggleBookmarkUseCase`)
- **ViewModels** (`presentation/`): Manage screen state with `UiState` data class + `StateFlow`

### Database (SQLDelight)

- Queries defined in `.sq` files in `src/commonMain/resources`
- Generated code goes to `shared/build/generated/sqldelight/`
- Platform drivers automatically selected (Android/iOS via `expect/actual`)
- Access via `INGEventDatabase` singleton (from `LocalDataSource`)

### Serialization

All DTOs use `@Serializable` from `kotlinx.serialization`:
```kotlin
@Serializable
data class SessionDto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String
)
```

### Firebase Configuration

- **Android**: Reads from `androidApp/google-services.json` or `local.properties`
- **iOS**: Reads from `iosApp/INGEvent/INGEvent/GoogleService-Info.plist` or Xcode build settings
- Override via build config or environment variables: `FIREBASE_DATABASE_URL`, `FIREBASE_API_KEY`, `FIREBASE_CONFERENCE_ID`

### Gradle Conventions

- Version constraints in `libs.versions.toml` (root `gradle/libs.versions.toml`)
- Kotlin DSL for all build files (`.kts`)
- Targets: `androidTarget`, `iosX64`, `iosArm64`, `iosSimulatorArm64`
- BuildConfig fields for runtime configuration (API URLs, keys)

### State Management (Android/iOS Binding)

- **Android**: ViewModels exposed via Koin, observed directly in Compose
- **iOS**: ViewModels wrapped in `ObservableObject` for SwiftUI `@ObservedObject`
- **Shared**: All state mutations in `commonMain` for consistency

### Naming Conventions

- Screen composables: `*Screen.kt` (e.g., `ScheduleScreen`, `SessionDetailScreen`)
- Components: `Components.kt` (shared helpers)
- ViewModels: `*ViewModel.kt` (e.g., `ScheduleViewModel`)
- Use cases: `*UseCase.kt` (e.g., `GetScheduleUseCase`)
- Repository classes: `*Repository.kt`
- State data classes: `*UiState` (e.g., `ScheduleUiState`)

### Tracks & Filters

Sessions are categorized by track enum:
- `AI_ML`, `ANDROID`, `WEB`, `CLOUD`, `FIREBASE`, `FLUTTER`, `DESIGN`, `IOS`, `GENERIC`

Session types: `KEYNOTE`, `SESSION`, `WORKSHOP`, `CODELAB`, `OFFICE_HOURS`

## Backend Integration

The app supports three backends (configurable at build time):

1. **Firebase Realtime Database** (default, `FIREBASE_DATABASE_URL`)
   - Anonymous auth with token refresh
   - REST API via `INGEventApi`

2. **Supabase PostgreSQL** (fallback, `SUPABASE_URL` + `SUPABASE_ANON_KEY`)
   - Auto-generated REST API
   - JWT token auth

3. **Ktor Server** (custom, `FIREBASE_FUNCTIONS_URL`)
   - Share models via shared module
   - Deploy to Cloud Run, Railway, Fly.io

Active backend chosen at runtime; see `BackendConfig.kt`.

## Important Nuances

### Expect/Actual Pattern

Platform-specific code uses Kotlin's `expect/actual`:
- `shared/src/commonMain/`: `expect` declarations
- `shared/src/androidMain/`, `shared/src/iosMain/`: `actual` implementations
- Example: `DatabaseDriverFactory`, `PlatformModule`

### Coroutine Scopes in ViewModels

ViewModels create a `CoroutineScope` with `SupervisorJob()` to manage async work:
```kotlin
class ScheduleViewModel(...) {
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    
    fun loadData() {
        scope.launch {
            // Fetch and update state
        }
    }
}
```

Cancellation cleanup must be handled manually in iOS (no ViewModel lifecycle).

### Offline-First Behavior

1. Call `LocalDataSource.getX()` first (SQLDelight query)
2. If empty or stale, call API via `INGEventApi`
3. On success, write to local DB and emit updated state
4. On failure, emit cached data or error

### Bookmarks (Offline Feature)

- Stored locally in SQLDelight
- Toggled via `ToggleBookmarkUseCase`
- Synced to Firebase `ratings` table for persistence
- Works fully offline

## Admin Panel & Data Setup

Located in `admin-panel/`:
- Run: `cd admin-panel && python3 -m http.server 8080`
- Manage speakers and sessions in Supabase
- Bootstrap schema: `supabase/schema.sql` and `supabase/seed.sql`

## iOS Setup Gotchas

1. **SPM Integration**: After running `./gradlew :shared:prepareSharedSpm`, add `iosApp/SharedSPM` as a local package in Xcode
2. **Linker Flag**: Ensure `Other Linker Flags` includes `-lsqlite3`
3. **Bundle ID**: Firebase `GoogleService-Info.plist` must match target bundle ID (`com.ing.event`)
4. **Build Settings**: Override `FIREBASE_DATABASE_URL` or `FIREBASE_API_KEY` as user-defined settings if needed

## Common Tasks

### Adding a New Use Case
1. Create `NewUseCase.kt` in `shared/src/commonMain/kotlin/com/ingevent/domain/usecase/`
2. Inject dependencies in constructor
3. Implement `operator fun invoke()` or a named function
4. Register in `SharedModule.kt` as `factory`

### Adding a New Screen (Android)
1. Create `NewScreen.kt` in `androidApp/src/main/kotlin/com/ing/event/ui/`
2. Create corresponding `NewViewModel` in `shared/src/commonMain/kotlin/com/ingevent/presentation/new/`
3. Add route to `AppNavigation.kt`
4. Register ViewModel in `SharedModule`

### Adding a Database Table
1. Create `.sq` file in `shared/src/commonMain/resources/com/ingevent/data/local/`
2. Run `./gradlew :shared:generateDebugDatabaseSchema` (or build task)
3. Use generated types in `LocalDataSource`

### Debugging JSON Serialization
- Check DTOs use `@Serializable` and `@SerialName` for field mapping
- Use `httpClientLogging` in `HttpClientFactory` for request/response logging
- Firebase responses are wrapped; check `FirebaseDtos.kt` for nesting

## Useful Gradle Commands

```bash
# Clean build
./gradlew clean

# Build all targets
./gradlew build

# Lint and fix
./gradlew lintFix

# View project dependencies
./gradlew dependencies

# Check Kotlin version and plugins
./gradlew buildEnvironment
```
