# Conference Admin Panel

Static Firebase Hosting panel to manage conference data stored in Firebase Realtime Database.

## Run Locally

From the repository root:

```bash
firebase emulators:start --only hosting,database,auth
```

Open:

- http://localhost:5002

When running on `localhost`, the panel automatically connects Firebase Auth to `127.0.0.1:9099` and Realtime Database to `127.0.0.1:9000`.
If `/__/firebase/init.json` is empty locally, the panel falls back to a local emulator config for the `ingtechrating` Firebase project. That fallback is only used on `localhost`.

For a fully local test:

1. Open the Emulator UI at http://localhost:4000.
2. Go to Authentication and create/sign in with a Google test user.
3. Copy that local user's UID.
4. Go to Realtime Database and create:

```json
{
  "admins": {
    "LOCAL_AUTH_UID": true
  }
}
```

5. Open http://localhost:5002 and use Google Sign-In.

## Firebase Setup

1. Keep the Firebase project on the Spark plan.
2. Enable Realtime Database.
3. Enable Firebase Authentication with the Google provider.
4. Create a Firebase Web App in Project settings if the project does not have one.
5. Link that Web App to the Firebase Hosting site. This is required for `/__/firebase/init.json`.
6. Sign in once with the Google account that will administer the panel.
7. Add the admin UID to Realtime Database:

```json
{
  "admins": {
    "YOUR_ADMIN_UID": true
  }
}
```

8. Deploy Hosting and Realtime Database rules:

```bash
firebase deploy --only hosting,database
```

When served by Firebase Hosting, the Firebase client config is loaded automatically from `/__/firebase/init.json`, so the login form only shows Google Sign-In.

## Global application lock

The **App Lock** tab manages the boolean value at `test/config/appLocked` in Firebase Realtime Database. When it is `true`, Android and iOS hide all event content and only display the event-ended message. A missing value is treated as `false`.

Deploy the Realtime Database rules together with the panel so the apps can read this non-sensitive flag before loading event data:

```bash
firebase deploy --only hosting,database
```

## Data Paths

The panel edits:

- `/test/home`
- `/test/map`
- `/test/conferences/{conferenceId}`

The ratings tab reads:

- `/ratings/{ratingId}`

The mobile app reads `/test/home` for the Home hero, `/test/map` for the event map, and `/test/conferences` for sessions and speakers. The Map tab accepts image files up to 5 MB and stores the image as base64 in Realtime Database.

## Conference Shape

```json
{
  "id": "mmm-2026",
  "title": "ING Event 2026",
  "audience": "Developers",
  "eventType": "Conference",
  "organizingCountry": "ES",
  "startDate": 1781942400,
  "rooms": [
    {
      "id": "main-stage",
      "name": "Main Stage",
      "description": "Auditorium",
      "presentations": [
        {
          "id": "session-opening",
          "title": "Opening Keynote",
          "description": "Welcome session.",
          "durationMinutes": 45,
          "presenters": ["Jane Doe"],
          "startDate": 1781946000,
          "tags": ["android", "kmm"],
          "technology": "Android",
          "type": "Keynote"
        }
      ]
    }
  ]
}
```
