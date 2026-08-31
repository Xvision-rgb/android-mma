-- =============================================================
-- training_plan — deux créneaux possibles par jour (matin / soir).
--
-- Cas d'usage : salle le matin + maison le soir, sans écraser l'un
-- par l'autre. L'unité de prescription (reps / secondes / minutes /
-- mètres) vit dans le JSONB `exercices`, pas en colonne SQL.
--
-- À exécuter dans Supabase > SQL Editor, après 009_daily_checkin_user_id_default.sql.
-- =============================================================

alter table public.training_plan
  add column if not exists creneau text not null default 'matin';

alter table public.training_plan drop constraint if exists training_plan_creneau_check;

alter table public.training_plan
  add constraint training_plan_creneau_check check (creneau in ('matin', 'soir'));

-- Ancienne unicité (user_id, jour_semaine, phase) — noms Postgres courants.
alter table public.training_plan
  drop constraint if exists training_plan_user_id_jour_semaine_phase_key;

alter table public.training_plan
  drop constraint if exists training_plan_user_jour_phase_key;

drop index if exists training_plan_user_id_jour_semaine_phase_key;
drop index if exists training_plan_user_jour_phase_key;

alter table public.training_plan
  drop constraint if exists training_plan_user_id_jour_semaine_phase_creneau_key;

alter table public.training_plan
  add constraint training_plan_user_id_jour_semaine_phase_creneau_key
  unique (user_id, jour_semaine, phase, creneau);

comment on column public.training_plan.creneau is
  'Créneau du jour : matin (défaut) ou soir — permet deux séances programmées le même jour.';
