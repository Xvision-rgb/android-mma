-- =============================================================
-- nutrition_targets — glucides et lipides cibles.
--
-- La cible du jour ne portait que les calories et les protéines : la
-- périodisation glucidique (plus de glucides les jours chargés, moins les
-- jours de repos, protéines et lipides constants) n'avait donc nulle part
-- où être stockée.
--
-- Colonnes NULLABLES à dessein : tout l'historique antérieur en est
-- dépourvu, et une valeur par défaut inventée fausserait les récapitulatifs
-- hebdomadaires qui relisent ces lignes.
--
-- À exécuter dans Supabase > SQL Editor, après 006_daily_checkin.sql.
-- =============================================================

alter table public.nutrition_targets
  add column if not exists glucides_cible_g numeric(6, 1) check (glucides_cible_g >= 0);

alter table public.nutrition_targets
  add column if not exists lipides_cible_g numeric(6, 1) check (lipides_cible_g >= 0);
