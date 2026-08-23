-- =============================================================
-- MMA Recomp — Supabase schema
-- Suivi entraînement / nutrition / poids, mono-utilisateur au
-- départ, avec Row Level Security prête pour du multi-utilisateur.
-- =============================================================

create extension if not exists pgcrypto;

-- =============================================================
-- profiles
-- =============================================================
create table public.profiles (
  id uuid primary key references auth.users (id) on delete cascade,
  poids_objectif_kg numeric(5, 2) not null default 84,
  bf_objectif_pct numeric(4, 1) not null default 10,
  -- "phase" permet d'étendre le suivi (métriques MMA spécifiques) sans
  -- refondre le schéma une fois le curriculum MMA lancé.
  phase text not null default 'ete' check (phase in ('ete', 'curriculum_mma')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.profiles enable row level security;

create policy "profiles_select_own" on public.profiles
  for select using (id = auth.uid());
create policy "profiles_insert_own" on public.profiles
  for insert with check (id = auth.uid());
create policy "profiles_update_own" on public.profiles
  for update using (id = auth.uid());

-- Crée automatiquement la ligne de profil au signup.
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer set search_path = public
as $$
begin
  insert into public.profiles (id) values (new.id)
  on conflict (id) do nothing;
  return new;
end;
$$;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- =============================================================
-- training_plan — le split hebdo programmé (jour → séance → exercices)
-- Séparé de "workouts" qui est le log réel des séances effectuées.
-- =============================================================
create table public.training_plan (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users (id) on delete cascade default auth.uid(),
  jour_semaine smallint not null check (jour_semaine between 1 and 7), -- 1 = lundi ... 7 = dimanche
  type text not null check (type in (
    'jambes_force', 'torse_force', 'jambes_hypertrophie',
    'torse_hypertrophie', 'hiit', 'mma_wod', 'repos'
  )),
  -- exercices: [{ "nom": "Squat", "series": 5, "reps": 3, "charge_cible_kg": 100 }, ...]
  exercices jsonb not null default '[]'::jsonb,
  phase text not null default 'ete' check (phase in ('ete', 'curriculum_mma')),
  notes text,
  actif boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (user_id, jour_semaine, phase)
);

alter table public.training_plan enable row level security;

create policy "training_plan_all_own" on public.training_plan
  for all using (user_id = auth.uid()) with check (user_id = auth.uid());

-- =============================================================
-- workouts — log réel des séances (force/hypertrophie/HIIT)
-- =============================================================
create table public.workouts (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users (id) on delete cascade default auth.uid(),
  date date not null default current_date,
  type text not null check (type in (
    'jambes_force', 'torse_force', 'jambes_hypertrophie',
    'torse_hypertrophie', 'hiit', 'mma_wod'
  )),
  -- exercices: [{ "nom", "series", "reps", "charge_cible_kg",
  --                "charge_reelle_kg", "reps_reelles", "propre": bool }]
  exercices jsonb not null default '[]'::jsonb,
  duree_min integer,
  notes text,
  created_at timestamptz not null default now()
);

alter table public.workouts enable row level security;

create policy "workouts_all_own" on public.workouts
  for all using (user_id = auth.uid()) with check (user_id = auth.uid());

create index workouts_user_date_idx on public.workouts (user_id, date desc);

-- =============================================================
-- mma_sessions — WOD du coach + ressenti + notes technique
-- =============================================================
create table public.mma_sessions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users (id) on delete cascade default auth.uid(),
  date date not null default current_date,
  wod_content text not null default '', -- texte du coach, collé depuis WhatsApp
  rounds_sets text,
  ressenti smallint check (ressenti between 1 and 5),
  notes_technique text,
  created_at timestamptz not null default now()
);

alter table public.mma_sessions enable row level security;

create policy "mma_sessions_all_own" on public.mma_sessions
  for all using (user_id = auth.uid()) with check (user_id = auth.uid());

create index mma_sessions_user_date_idx on public.mma_sessions (user_id, date desc);

-- =============================================================
-- weigh_ins — pesée matin à jeun (fiable) vs soir (jamais mélangées)
-- =============================================================
create table public.weigh_ins (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users (id) on delete cascade default auth.uid(),
  date date not null default current_date,
  heure time not null default current_time,
  type text not null check (type in ('matin_jeun', 'soir')),
  poids_kg numeric(5, 2) not null,
  bf_pct numeric(4, 1),
  -- contexte: { "creatine_recente": bool, "alcool_recent": bool, "post_training": bool }
  contexte jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  unique (user_id, date, type)
);

alter table public.weigh_ins enable row level security;

create policy "weigh_ins_all_own" on public.weigh_ins
  for all using (user_id = auth.uid()) with check (user_id = auth.uid());

create index weigh_ins_user_date_idx on public.weigh_ins (user_id, date desc);

-- =============================================================
-- meals — 1 à 4 créneaux/jour (matin / post-training / après-midi / soir)
-- =============================================================
create table public.meals (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users (id) on delete cascade default auth.uid(),
  date date not null default current_date,
  repas smallint not null check (repas between 1 and 4),
  -- 1 = matin, 2 = post-training, 3 = après-midi, 4 = soir
  calories integer not null check (calories >= 0),
  proteines_g numeric(5, 1) not null default 0,
  glucides_g numeric(5, 1) not null default 0,
  lipides_g numeric(5, 1) not null default 0,
  description text,
  created_at timestamptz not null default now(),
  unique (user_id, date, repas)
);

alter table public.meals enable row level security;

create policy "meals_all_own" on public.meals
  for all using (user_id = auth.uid()) with check (user_id = auth.uid());

create index meals_user_date_idx on public.meals (user_id, date desc);

-- =============================================================
-- nutrition_targets — cible calorique/protéique du jour (calorie cycling)
-- =============================================================
create table public.nutrition_targets (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users (id) on delete cascade default auth.uid(),
  date date not null default current_date,
  type_jour text not null check (type_jour in ('training', 'repos')),
  calories_cible integer not null,
  proteines_cible_g numeric(5, 1) not null,
  created_at timestamptz not null default now(),
  unique (user_id, date)
);

alter table public.nutrition_targets enable row level security;

create policy "nutrition_targets_all_own" on public.nutrition_targets
  for all using (user_id = auth.uid()) with check (user_id = auth.uid());

-- =============================================================
-- updated_at triggers
-- =============================================================
create or replace function public.set_updated_at()
returns trigger language plpgsql as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

create trigger profiles_set_updated_at
  before update on public.profiles
  for each row execute function public.set_updated_at();

create trigger training_plan_set_updated_at
  before update on public.training_plan
  for each row execute function public.set_updated_at();

-- =============================================================
-- Seed optionnel — split hebdo par défaut.
-- À exécuter une fois connecté (remplacer :user_id par l'UID auth,
-- visible dans Supabase > Authentication > Users) :
-- =============================================================
-- insert into public.training_plan (user_id, jour_semaine, type, exercices) values
-- (:user_id, 1, 'jambes_force', '[
--   {"nom": "Squat", "series": 5, "reps": 3, "charge_cible_kg": null},
--   {"nom": "Jump squat", "series": 5, "reps": 3, "charge_cible_kg": null},
--   {"nom": "Fentes", "series": 3, "reps": 6, "charge_cible_kg": null}
-- ]'::jsonb),
-- (:user_id, 2, 'torse_force', '[
--   {"nom": "Bench press", "series": 5, "reps": 3, "charge_cible_kg": null},
--   {"nom": "Tirage barre", "series": 5, "reps": 5, "charge_cible_kg": null},
--   {"nom": "Dips lestés", "series": 3, "reps": 6, "charge_cible_kg": null}
-- ]'::jsonb),
-- (:user_id, 3, 'repos', '[]'::jsonb),
-- (:user_id, 4, 'jambes_hypertrophie', '[
--   {"nom": "Leg press", "series": 4, "reps": 10, "charge_cible_kg": null},
--   {"nom": "Hack squat", "series": 3, "reps": 10, "charge_cible_kg": null},
--   {"nom": "Box jump", "series": 4, "reps": 5, "charge_cible_kg": null},
--   {"nom": "Mollets", "series": 3, "reps": 12, "charge_cible_kg": null}
-- ]'::jsonb),
-- (:user_id, 5, 'torse_hypertrophie', '[
--   {"nom": "Développé haltères", "series": 4, "reps": 10, "charge_cible_kg": null},
--   {"nom": "Tractions", "series": 4, "reps": 8, "charge_cible_kg": null},
--   {"nom": "Lancers med-ball", "series": 4, "reps": 5, "charge_cible_kg": null},
--   {"nom": "Dips", "series": 3, "reps": 10, "charge_cible_kg": null}
-- ]'::jsonb),
-- (:user_id, 6, 'mma_wod', '[]'::jsonb),
-- (:user_id, 7, 'repos', '[]'::jsonb);
