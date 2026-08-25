# Firebase Backend

This folder contains the Firebase implementation for ING Event.

## What It Provides

- Cloud Firestore collections:
  - `sessions`
  - `speakers`
  - `users/{uid}/bookmarks`
- Cloud Functions HTTPS API compatible with the existing KMM client:
  - `GET /sessions`
  - `GET /sessions?day=1`
  - `GET /sessions?day=1&track=ANDROID`
  - `GET /sessions/{sessionId}`
  - `GET /sessions/search?q=compose`
  - `GET /speakers`
  - `GET /speakers/{speakerId}`
  - `GET /users/{userId}/bookmarks`
  - `POST /users/{userId}/bookmarks`
  - `DELETE /users/{userId}/bookmarks/{sessionId}`

## One-Time Firebase Setup

1. Install or update the Firebase CLI:

   ```bash
   npm install -g firebase-tools
   firebase login
   ```

2. Create or select a Firebase project in the Firebase Console.

3. Enable Cloud Firestore in production mode.

4. Copy `.firebaserc.example` to `.firebaserc` and replace `your-firebase-project-id`.

5. Install Functions dependencies:

   ```bash
   cd firebase/functions
   npm install
   ```

6. Deploy rules, indexes, and Functions:

   ```bash
   firebase deploy --only firestore,functions
   ```

7. Import the existing Supabase seed data into Firestore:

   ```bash
   export GOOGLE_APPLICATION_CREDENTIALS="/absolute/path/to/service-account.json"
   node firebase/scripts/import-supabase-seed.mjs
   ```

   To validate parsing without writing:

   ```bash
   node firebase/scripts/import-supabase-seed.mjs --dry-run
   ```

## Configure The Apps

After deploy, Firebase will expose the HTTPS function URL. For this repo the base URL should end at the `api` function, for example:

```text
https://europe-west1-your-project-id.cloudfunctions.net/api
```

Android reads it from `local.properties` or the environment:

```properties
FIREBASE_FUNCTIONS_URL=https://europe-west1-your-project-id.cloudfunctions.net/api
```

iOS reads it from `Info.plist`:

```text
FIREBASE_FUNCTIONS_URL = https://europe-west1-your-project-id.cloudfunctions.net/api
```

When `FIREBASE_FUNCTIONS_URL` is present, the app uses Firebase. Otherwise it falls back to Supabase.

## Realtime Database Home Content

The home hero reads editable content from `test/home` in Firebase Realtime Database:

```json
{
  "test": {
      "home": {
        "title": "ING Event",
        "description": "Welcome to ING Event. Explore the agenda and enjoy the event.",
        "imageBase64": "iVBORw0KGgoAAAANSUhEUgAA...",
        "imageMimeType": "image/png",
        "imageUrl": ""
    }
  }
}
```

The image can also be stored as `image` when it contains base64 data.
The app also accepts the legacy conference fields as a fallback: `welcomeMessage`, `welcome_message`, `heroImageUrl`, `hero_image_url`, and `welcome_image_url`.
Remote values are cached locally, so the latest loaded title, description, and image remain available offline.

## Realtime Database Event Map

The event map reads its image from `test/map`:

```json
{
  "test": {
    "map": {
      "imageBase64": "iVBORw0KGgoAAAANSUhEUgAA...",
      "imageMimeType": "image/png",
      "imageUrl": ""
    }
  }
}
```

Admins can upload, replace, or delete this image from the Map tab in the admin panel.

## Local Emulator

Run Firestore and Functions locally:

```bash
firebase emulators:start --only functions,firestore
```

Then point the app to the local function URL, usually:

```properties
FIREBASE_FUNCTIONS_URL=http://127.0.0.1:5001/your-project-id/europe-west1/api
```

For Android emulator, replace `127.0.0.1` with `10.0.2.2`.
