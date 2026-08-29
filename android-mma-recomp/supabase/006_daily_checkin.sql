-- =============================================================
-- daily_checkins — état de forme quotidien (readiness).
--
-- Alimente la modulation de séance : le questionnaire subjectif module le
-- VOLUME de la séance du jour, jamais la charge. La HRV est optionnelle et
-- saisie à la main (aucun SDK santé intégré à ce stade).
--
-- Chaque item va de 1 (mauvais) à 5 (bon) : le score total se lit toujours
-- dans le même sens, plus haut = plus prêt.
--
-- À exécuter dans Supabase > SQL Editor, après schema.sql.
-- =============================================================

create table if not exists public.daily_checkins (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users (id) on delete cascade,
  date date not null,
  sommeil smallint not null check (sommeil between 1 and 5),
  courbatures smallint not null check (courbatures between 1 and 5),
  fatigue smallint not null check (fatigue between 1 and 5),
  humeur smallint not null check (humeur between 1 and 5),
  stress smallint not null check (stress between 1 and 5),
  -- rMSSD au réveil, en ms. Null tant que l'utilisateur ne mesure pas.
  hrv_rmssd numeric(6, 2),
  -- Suspension à la barre, en secondes — suivi de la poigne dans le temps.
  dead_hang_sec smallint check (dead_hang_sec >= 0),
  created_at timestamptz not null default now(),
  -- Un seul check-in par jour : refaire le test corrige celui du matin
  -- au lieu d'en empiler un second.
  unique (user_id, date)
);

alter table public.daily_checkins enable row level security;

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

-- =============================================================
-- workouts — RPE de séance, pour la charge interne (session-RPE).
-- charge = rpe × duree_min ; l'ACWR en découle (cf. util/TrainingLoad.kt).
-- =============================================================

alter table public.workouts
  add column if not exists rpe smallint check (rpe between 1 and 10);
