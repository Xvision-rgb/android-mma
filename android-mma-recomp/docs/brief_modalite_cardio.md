# Brief — Modalités d’exercice Force vs Cardio

## Problème
Aujourd’hui, tout mouvement loggé est traité comme de la **force** (séries × reps × kg).
Marche rapide, marche inclinée, course, vélo, etc. n’ont donc :
- ni UI adaptée (durée / distance / intensité),
- ni impact calorique crédible (dépense = seulement RPE × durée de séance),
- ni évolution propre (pas de courbe km / allure / minutes).

## Objectif produit
Permettre de logger un bloc **cardio** distinct d’un exo musculation, avec impact sur :
1. **Calories / EA** — estimation MET × poids × durée
2. **Charge interne** — fallback si pas de RPE séance ; sinon complément cohérent
3. **Évolution** — historique durée / distance / allure par activité
4. **UX** — presets + bascule Force ↔ Cardio sur chaque exercice

## Modèle (rétrocompatible JSONB)
```kotlin
enum class ExerciseModality { Strength, Cardio }

LoggedExercise(
  …champs force existants…
  modality: ExerciseModality = Strength,  // absent = Strength
  dureeMin: Int? = null,                  // Cardio
  distanceKm: Double? = null,             // Cardio optionnel
  intensite: Int? = null,                 // 1–10, défaut 5
)
```

## Règles métier
- Détection auto du nom (`marche`, `course`, `vélo`, `elliptique`, `rameur`…) → Cardio
- `volumeTotal` / APRE / RIR / zones musculaires : **ignorés** si Cardio
- Modulation readiness : n’allège que les exos Strength
- Dépense kcal séance = `max(RPE×durée séance → kcal, Σ MET cardio)` pour éviter le double-compte grossier
- Charge interne ACWR = `max(RPE×durée, Σ intensité×durée cardio)` si l’un manque
- Progression Progress : séries force inchangées + tendances cardio (min, km, allure)

## UI
- Presets chips : Marche rapide, Marche inclinée, Course, Vélo, Elliptique, Rameur
- `ExerciseRow` : si Cardio → durée / distance / intensité ; sinon séries actuelles
- Toggle Force / Cardio
- Résumé séance : volume kg + minutes cardio + kcal estimées

## Hors scope
- GPS / Health Connect
- Zones FC
- Outbox schéma SQL (JSONB déjà souple)
