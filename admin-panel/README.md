# Admin Panel (Web)

Web panel to manage `speakers` and `sessions` in Supabase and reuse the KMM app for any event.

## Open the panel

From the repository root:

```bash
cd admin-panel
python3 -m http.server 8080
```

Then open:

- http://localhost:8080

## How to use it

1. Paste `Project URL` (example: `https://xxxxx.supabase.co`).
2. Paste `API Key`.
3. Click `Connect` and then `Load data`.
4. Create/edit/delete speakers and sessions.

## Recommended security setup

- For local admin usage, you can use the `service_role` key.
- Do not deploy this panel publicly with an embedded `service_role` key.
- For a public deployment, use an `anon` key with admin RLS policies.

## Data expected by the app

### `speakers` table

Used fields:
- `id` (text, pk)
- `name` (text)
- `role` (text)
- `company` (text)
- `bio` (text)
- `photo_url` (text)
- `social_links` (jsonb)

### `sessions` table

Used fields:
- `id` (text, pk)
- `title` (text)
- `description` (text)
- `start_time` (timestamptz)
- `end_time` (timestamptz)
- `duration` (text)
- `room` (text)
- `day` (int)
- `track` (text app enum: `AI_ML`, `ANDROID`, `WEB`, `CLOUD`, `FIREBASE`, `FLUTTER`, `DESIGN`)
- `type` (text app enum: `KEYNOTE`, `SESSION`, `WORKSHOP`, `CODELAB`, `OFFICE_HOURS`)
- `level` (text app enum: `BEGINNER`, `INTERMEDIATE`, `ADVANCED`)
- `speaker_ids` (text[])
- `capacity` (int)
- `registered` (int)
- `tags` (text[])
- `livestream_url` (nullable text)
- `slides_url` (nullable text)
- `updated_at` (timestamptz)

Use [`../supabase/schema.sql`](../supabase/schema.sql) to create these tables in a new project.
