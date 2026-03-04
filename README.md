# 📱 Mobile Meets Mobile Clone — Kotlin Multiplatform (KMM)

Un clon completo de la app de Mobile Meets Mobile construido con **Kotlin Multiplatform Mobile**, compartiendo lógica de negocio entre Android (Jetpack Compose) e iOS (SwiftUI).

## 🏗️ Arquitectura

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

## 📂 Estructura del Proyecto

| Módulo | Contenido |
|--------|-----------|
| `shared/commonMain` | Modelos, API client, repositories, use cases, ViewModels, DI |
| `shared/androidMain` | SQLDelight Android driver, plataforma DI |
| `shared/iosMain` | SQLDelight iOS driver, plataforma DI, Koin init |
| `androidApp` | UI con Jetpack Compose, navegación, tema Material 3 |
| `iosApp` | UI con SwiftUI, vistas nativas iOS |

## 🔧 Stack Tecnológico

### Shared (Kotlin Multiplatform)
- **Ktor** — HTTP client multiplataforma
- **kotlinx.serialization** — JSON serialization
- **kotlinx.datetime** — Fechas multiplataforma
- **SQLDelight** — Base de datos local (offline cache)
- **Koin** — Inyección de dependencias
- **Coroutines + Flow** — Programación asíncrona reactiva

### Android
- **Jetpack Compose** — UI declarativa
- **Material 3** — Design system
- **Navigation Compose** — Navegación
- **Coil** — Carga de imágenes

### iOS
- **SwiftUI** — UI nativa
- **Combine** — Observación de estado desde KMM

## 🚀 Cómo ejecutar

### Android
```bash
./gradlew :androidApp:installDebug
```

### iOS
```bash
cd iosApp
pod install   # si usas CocoaPods
open iosApp.xcworkspace
# Build & Run en Xcode
```

## 🖥️ Opciones de Backend

### Opción 1: Ktor Server (Recomendada — Full Kotlin)
```kotlin
// Comparte modelos con shared/
// Deploy: Google Cloud Run / Railway / Fly.io
// DB: PostgreSQL + Exposed ORM
```
**Ventaja**: 100% Kotlin, modelos compartidos entre cliente y servidor.

### Opción 2: Supabase (Rápido)
```sql
-- API REST auto-generada desde PostgreSQL
-- Auth, Real-time, Storage incluidos
-- Free tier generoso
```
**Ventaja**: Backend en minutos, PostgreSQL real.

### Opción 3: Firebase
```
-- Firestore + Cloud Functions
-- Auth nativo + FCM push notifications
-- Real-time sync
```
**Ventaja**: Integración nativa con Android, escalado automático.

## 📋 Features

- [x] Schedule por días con timeline visual
- [x] Filtro por tracks (AI/ML, Android, Web, Cloud, Firebase, Flutter, Design)
- [x] Vista detalle de sesión con speakers y metadata
- [x] Sistema de bookmarks (offline-first con SQLDelight)
- [x] Búsqueda de sesiones
- [x] Vista de speakers
- [x] Barra de capacidad por sesión
- [x] Bottom navigation (Android) / Tab view (iOS)
- [x] Tema oscuro Mobile Meets Mobile branded
- [x] Offline-first: cache local → sync con servidor

## 📋 Por implementar

- [ ] Push notifications (recordatorio antes de sesión)
- [ ] Mapa del venue con Google Maps SDK
- [ ] Livestream integrado
- [ ] Check-in vía QR
- [ ] Feedback post-sesión
- [ ] Exportar agenda a Google Calendar
- [ ] Login con Google Sign-In
- [ ] Compose Multiplatform (compartir UI entre Android e iOS)
