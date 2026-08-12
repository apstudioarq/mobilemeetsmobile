# Conference Admin Panel

Static Firebase Hosting panel to manage conference data stored in Firebase Realtime Database.

## Run Locally

From the repository root:

```bash
firebase emulators:start --only hosting
```

Open:

- http://localhost:5000

## Firebase Setup

1. Keep the Firebase project on the Spark plan.
2. Enable Realtime Database.
3. Enable Firebase Authentication with Email/Password.
4. Create an admin user.
5. Add the admin UID to Realtime Database:

```json
{
  "admins": {
    "YOUR_ADMIN_UID": true
  }
}
```

6. Deploy Hosting and Realtime Database rules:

```bash
firebase deploy --only hosting,database
```

The panel stores Firebase connection details in browser `localStorage`, but it does not store the admin password.
When served by Firebase Hosting, the Firebase client config is loaded automatically from `/__/firebase/init.json`, so the login form only asks for the Firebase Authentication email and password.

## Data Paths

The panel edits:

- `/test/conferences/{conferenceId}`

The ratings tab reads:

- `/ratings/{ratingId}`

The mobile app already reads `/test/conferences` and transforms each room presentation into app sessions and speakers.

## Conference Shape

```json
{
  "id": "mmm-2026",
  "title": "Mobile Meets Mobile 2026",
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
