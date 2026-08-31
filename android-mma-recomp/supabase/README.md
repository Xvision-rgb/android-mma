# Schéma Supabase — notes Android

Le schéma de base (`meals`, `workouts`, `weigh_ins`, `profiles`, RLS, etc.)
vit historiquement dans `ios-mma-recomp/supabase/schema.sql`. Ce dépôt Android
ne le re-vendor pas intégralement : exécute d'abord ce fichier iOS, puis les
migrations `002`–`010` ici.

## Convention `user_id`

Les payloads Kotlin `New*` (repas, pesées, séances, check-ins) **n'envoient pas**
`user_id`. Postgres doit le remplir via :

```sql
user_id uuid not null default auth.uid() references auth.users (id) on delete cascade
```

Sans ce `DEFAULT`, l'insert échoue (NOT NULL / RLS) alors que le WiFi est OK.

La migration `009_daily_checkin_user_id_default.sql` aligne `daily_checkins`
sur cette convention.

La migration `010_training_plan_creneau.sql` ajoute le créneau `matin`/`soir`
sur `training_plan` (deux séances possibles le même jour). Les unités
(reps / secondes / minutes / mètres) sont dans le JSONB `exercices`.

## Ordre d'exécution

1. `schema.sql` (iOS / base)
2. `002`–`004` seeds foods
3. `005`–`010` migrations Android

## Hors-ligne (app)

Outbox Room pour : repas, séances salle, pesées, check-ins, séances MMA.
Pas d'outbox pour : plan d'entraînement, cibles nutrition, profil.
Seules les erreurs **réseau** partent en file ; une table manquante remonte
un message schéma (pas une fausse synchro).
