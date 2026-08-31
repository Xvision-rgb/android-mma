-- =============================================================
-- 009 — daily_checkins : default auth.uid() + policies idempotentes
--
-- Corrige l'échec d'upsert quand NewDailyCheckIn n'envoie pas user_id
-- (aligné sur meals / weigh_ins / workouts du schéma de base).
-- À exécuter dans Supabase > SQL Editor, après 006_daily_checkin.sql.
-- =============================================================

-- Si la table n'existe pas encore, créer le socle (idempotent avec 006).
create table if not exists public.daily_checkins (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null default auth.uid() references auth.users (id) on delete cascade,
  date date not null,
  sommeil smallint not null check (sommeil between 1 and 5),
  courbatures smallint not null check (courbatures between 1 and 5),
  fatigue smallint not null check (fatigue between 1 and 5),
  humeur smallint not null check (humeur between 1 and 5),
  stress smallint not null check (stress between 1 and 5),
  hrv_rmssd numeric(6, 2),
  dead_hang_sec smallint check (dead_hang_sec >= 0),
  created_at timestamptz not null default now(),
  unique (user_id, date)
);

-- Table déjà créée par 006 : ajouter le défaut manquant.
alter table public.daily_checkins
  alter column user_id set default auth.uid();

alter table public.daily_checkins enable row level security;

drop policy if exists "daily_checkins_select_own" on public.daily_checkins;
drop policy if exists "daily_checkins_insert_own" on public.daily_checkins;
drop policy if exists "daily_checkins_update_own" on public.daily_checkins;
drop policy if exists "daily_checkins_delete_own" on public.daily_checkins;

create policy "daily_checkins_select_own" on public.daily_checkins
  for select to authenticated using (auth.uid() = user_id);

create policy "daily_checkins_insert_own" on public.daily_checkins
  for insert to authenticated with check (auth.uid() = user_id);

create policy "daily_checkins_update_own" on public.daily_checkins
  for update to authenticated using (auth.uid() = user_id)
  with check (auth.uid() = user_id);

create policy "daily_checkins_delete_own" on public.daily_checkins
  for delete to authenticated using (auth.uid() = user_id);

create index if not exists daily_checkins_user_date_idx
  on public.daily_checkins (user_id, date desc);
