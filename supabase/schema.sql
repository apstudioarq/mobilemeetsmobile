-- Base schema for ING Event style event app.
-- Run this once in Supabase SQL editor for a new project.

create extension if not exists pgcrypto;

create table if not exists public.speakers (
  id text primary key,
  name text not null,
  role text not null,
  company text not null,
  bio text not null,
  photo_url text not null,
  photo_base64 text not null default '',
  photo_mime_type text not null default '',
  social_links jsonb not null default '{}'::jsonb
);

create table if not exists public.sessions (
  id text primary key,
  title text not null,
  description text not null,
  start_time timestamptz not null,
  end_time timestamptz not null,
  duration text not null,
  room text not null,
  day int not null,
  track text not null,
  type text not null,
  level text not null,
  speaker_ids text[] not null default '{}',
  capacity int not null default 0,
  registered int not null default 0,
  tags text[] not null default '{}',
  livestream_url text,
  slides_url text,
  updated_at timestamptz not null default now(),
  constraint sessions_registered_lte_capacity check (registered <= capacity)
);

create index if not exists sessions_day_start_time_idx
  on public.sessions(day, start_time);

create index if not exists sessions_track_idx
  on public.sessions(track);

create table if not exists public.user_bookmarks (
  user_id uuid not null,
  session_id text not null references public.sessions(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (user_id, session_id)
);

alter table public.speakers enable row level security;
alter table public.sessions enable row level security;
alter table public.user_bookmarks enable row level security;

-- Read-only public data for app clients.
drop policy if exists "public read speakers" on public.speakers;
create policy "public read speakers"
on public.speakers
for select
using (true);

drop policy if exists "public read sessions" on public.sessions;
create policy "public read sessions"
on public.sessions
for select
using (true);

-- Bookmark data should be per authenticated user.
drop policy if exists "bookmark read own" on public.user_bookmarks;
create policy "bookmark read own"
on public.user_bookmarks
for select
using (auth.uid() = user_id);

drop policy if exists "bookmark write own" on public.user_bookmarks;
create policy "bookmark write own"
on public.user_bookmarks
for all
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

-- NOTE:
-- Admin writes for speakers/sessions can be done with service_role key
-- from the local admin panel, or with explicit admin RLS policies.
