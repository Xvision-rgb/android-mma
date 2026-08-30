-- =============================================================
-- workouts / training_plan — ajoute le type "course".
--
-- Les sorties de course étaient jusqu'ici loguées en "mma_wod" faute de
-- mieux. Cela corrompait deux comptages à la fois : le nombre de sorties
-- (gonflé par les séances de combat) et le nombre de séances MMA (gonflé
-- par les sorties). La détection d'interférence course/force et la charge
-- interne lisaient donc des données fausses.
--
-- Ces tables stockent le type en text avec une contrainte CHECK : il faut
-- la remplacer, une contrainte n'étant pas extensible en place.
--
-- À exécuter dans Supabase > SQL Editor, après 007_nutrition_target_macros.sql.
-- =============================================================

alter table public.workouts drop constraint if exists workouts_type_check;

alter table public.workouts
  add constraint workouts_type_check check (type in (
    'jambes_force', 'torse_force', 'jambes_hypertrophie',
    'torse_hypertrophie', 'hiit', 'mma_wod', 'course'
  ));

alter table public.training_plan drop constraint if exists training_plan_type_check;

alter table public.training_plan
  add constraint training_plan_type_check check (type in (
    'jambes_force', 'torse_force', 'jambes_hypertrophie',
    'torse_hypertrophie', 'hiit', 'mma_wod', 'course', 'repos'
  ));

-- Aucune migration de données automatique : distinguer rétroactivement une
-- sortie de course d'une séance MMA dans l'historique existant demanderait
-- de deviner. Les anciennes lignes restent en 'mma_wod' ; à corriger à la
-- main si besoin.
