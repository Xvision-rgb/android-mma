-- =============================================================
-- profiles — ajoute le mode d'objectif calorique (Bulk / Recomposition /
-- Coupe), persisté depuis le nouvel écran "Objectif calorique".
-- À exécuter dans Supabase > SQL Editor.
-- =============================================================

alter table public.profiles
  add column if not exists objectif_calorie_mode text not null default 'recomposition'
  check (objectif_calorie_mode in ('bulk', 'recomposition', 'coupe'));
