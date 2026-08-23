# Recomp & MMA — app iOS

App iOS native (SwiftUI, iOS 17+) avec backend Supabase pour centraliser
suivi d'entraînement, nutrition et poids pendant une phase de recomposition
corporelle / préparation MMA — avec une UX pensée pour ne jamais transformer
la pesée quotidienne en source d'anxiété.

Ce dossier est un module autonome à l'intérieur de ce repo : il ne dépend de
rien d'autre ici et peut être déplacé/extrait tel quel dans son propre repo
si besoin.

## Stack

- SwiftUI, iOS 17+
- Supabase (Auth email, Postgres, Row Level Security)
- [supabase-swift](https://github.com/supabase/supabase-swift) (SPM)
- Swift Charts (natif) pour les graphiques

## 1. Setup Supabase

1. Crée un projet sur [supabase.com](https://supabase.com).
2. Dans **SQL Editor**, exécute `supabase/schema.sql` en entier. Il crée les
   tables, active RLS avec des policies "chacun ne voit/modifie que ses
   propres lignes" (`auth.uid()`), et un trigger qui crée automatiquement une
   ligne `profiles` à l'inscription.
3. Dans **Authentication > Providers**, laisse Email activé (c'est le seul
   utilisé ici). Désactive la confirmation d'email si tu veux tester vite en
   dev (Authentication > Settings).
4. Crée ton compte utilisateur (soit depuis l'app une fois lancée, soit
   directement dans Authentication > Users), récupère son UID, puis
   décommente et adapte le bloc `insert into training_plan` en bas de
   `schema.sql` pour pré-remplir ton split hebdo (sinon tu peux le
   programmer depuis l'app en écrivant directement dans `training_plan`).
5. Dans **Project Settings > API**, note l'URL du projet et la clé `anon`.

## 2. Setup du projet Xcode

Le projet Xcode (`.xcodeproj`) n'est pas committé — il est généré à partir de
`project.yml` avec [XcodeGen](https://github.com/yonaskolb/XcodeGen), pour
garder le repo propre et éviter les conflits de `.pbxproj`.

```bash
brew install xcodegen
cd ios-mma-recomp
xcodegen generate
open MMARecomp.xcodeproj
```

Xcode résout la dépendance SPM `supabase-swift` automatiquement au premier
build (relance "File > Packages > Resolve Package Versions" si besoin).

Configure ensuite tes identifiants Supabase :

```bash
cp MMARecomp/App/Secrets.swift.example MMARecomp/App/Secrets.swift
```

Édite `Secrets.swift` avec ton URL de projet et ta clé `anon`. Ce fichier est
gitignored — ne commit jamais de vraies clés.

> Le SDK `supabase-swift` évoluant vite, si un appel (`.from().select()...`)
> ne compile pas tel quel avec ta version du package, ajuste la signature :
> la logique métier (services/viewmodels/vues) n'a pas besoin de changer.

## 3. Structure du projet

```
ios-mma-recomp/
├── supabase/schema.sql          # schéma complet + RLS + seed du split
├── project.yml                  # spec XcodeGen
└── MMARecomp/
    ├── App/                     # entrée SwiftUI + config Supabase
    ├── Models/                  # structs Codable ↔ tables Postgres
    ├── Services/                # appels Supabase (1 service par table)
    ├── ViewModels/               # @MainActor ObservableObject, 1 par écran
    ├── Utilities/                # moyenne mobile, détection plateau,
    │                             # calcul cible nutrition, parsing WOD
    └── Views/
        ├── Root/                 # Auth + navigation par onglets
        ├── Dashboard/             # vue semaine
        ├── Workout/               # log séance + WOD MMA
        ├── Nutrition/             # log repas
        ├── WeighIn/               # log pesée + tendance
        ├── Progress/              # vue progression 4-8 semaines
        ├── Settings/              # objectifs, phase, déconnexion
        └── Components/            # graphique tendance, barres cible/réel...
```

Chaque table Postgres a : un modèle de lecture (`Workout`), un modèle
d'écriture (`NewWorkout`) et un service dédié (`WorkoutService`). Les
ViewModels ne parlent jamais directement au `SupabaseClient`.

## 4. Écrans du MVP

- **Dashboard** — séances faites/prévues cette semaine, tendance poids
  (moyenne 7j uniquement), moyenne calorique 7j, cible du jour.
- **Log séance** — préremplit les exercices depuis le split programmé du
  jour (`training_plan`), permet d'ajuster charges/reps réelles, et propose
  automatiquement "+2.5kg la prochaine fois" quand un exercice est coché
  "toutes les reps faites proprement". Bouton dédié pour coller le WOD MMA
  quand le type de séance est "MMA".
- **Log repas** — jusqu'à 4 créneaux/jour (matin / post-training /
  après-midi / soir), cible du jour calculée selon jour training/repos
  (calorie cycling), répartition indicative mais jamais bloquante.
- **Log pesée** — distingue strictement matin à jeun (utilisée pour la
  tendance) et soir (affichée à part) ; flags contextuels (créatine, alcool,
  post-training) ; graphique en moyenne mobile 7 jours uniquement.
- **Progression** — comparaison 4-8 semaines : poids, %BF, charges par
  exercice.

## 5. Principes UX implémentés

- **Jamais de poids brut comme donnée principale** : `WeightTrendChart`
  n'affiche que la moyenne mobile 7 jours (`Utilities/MovingAverage.swift`),
  partout dans l'app (Dashboard, Log pesée, Progression).
- **Pas de culpabilisation nutrition** : `TargetVsActualBar` ne vire jamais
  au rouge ; un déficit reste un simple "il reste X kcal", jamais une alerte.
  `MealLogViewModel.softUnderTargetAlert` ne se déclenche que sur 3 jours
  consécutifs nettement sous la cible — jamais sur un jour isolé.
- **Détection de plateau positive** : `PlateauDetector` transforme un poids
  stable ±0.5kg sur 2+ semaines en message "recomposition en cours" dès que
  les performances progressent, plutôt qu'en alerte de stagnation.
- **Aucune notification push** n'est configurée pour inciter à se peser plus
  souvent — volontairement absent de ce scaffold.
- **Champ `phase`** sur `profiles` et `training_plan` prépare l'ajout de
  métriques MMA spécifiques (explosivité, cardio) après le passage au
  curriculum MMA, sans migration de schéma.

## 6. Prochaines étapes suggérées

- Écran dédié à l'édition du split hebdo (`training_plan`) depuis l'app —
  pour l'instant le seed SQL et un upsert basique (`TrainingPlanService`)
  suffisent pour le MVP, mais une UI de configuration serait utile.
- Historique / édition des séances et repas passés (suppression déjà
  câblée côté service, pas encore d'UI).
- Widgets iOS (tendance poids, cible du jour) une fois le MVP validé.
- Métriques MMA spécifiques une fois `phase == curriculum_mma`.
