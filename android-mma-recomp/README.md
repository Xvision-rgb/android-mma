# Recomp & MMA — app Android

Portage Android (Kotlin + Jetpack Compose) de l'app de suivi entraînement /
nutrition / poids en recomposition corporelle et préparation MMA. Même
backend, même schéma Supabase, même logique métier que la version iOS
(`ios-mma-recomp/`, conservée pour référence) — seule la couche UI change.

## Pourquoi Android change la donne (par rapport à iOS)

- **Android Studio tourne nativement sur Windows/Linux** — pas besoin de Mac,
  contrairement à Xcode.
- **Aucun compte payant récurrent requis** pour installer l'app sur ton
  propre téléphone : gratuit, illimité dans le temps.
- Pour des mises à jour "propres" façon store (optionnel, voir §4), le compte
  Google Play Console coûte **25$, une seule fois** (à vie), contre 99$/an
  pour Apple.

## 1. Setup Supabase

Identique à la version iOS : exécute `../ios-mma-recomp/supabase/schema.sql`
dans le SQL Editor de ton projet Supabase (schéma et RLS déjà écrits, rien à
changer côté base). Récupère ensuite l'URL du projet et la clé `anon` dans
**Project Settings > API**.

## 2. Setup du projet Android

1. Installe [Android Studio](https://developer.android.com/studio) (Windows
   ou Linux, gratuit).
2. Ouvre le dossier `android-mma-recomp/` dans Android Studio — il détecte
   `settings.gradle.kts` et propose de synchroniser Gradle automatiquement.
3. Configure tes identifiants Supabase :
   ```bash
   cp app/src/main/java/com/example/mmarecomp/SupabaseConfig.kt.example \
      app/src/main/java/com/example/mmarecomp/SupabaseConfig.kt
   ```
   Édite `SupabaseConfig.kt` avec ton URL de projet et ta clé `anon`. Ce
   fichier est gitignored — ne commit jamais de vraies clés.
4. Avant le premier build, remplace `com.example.mmarecomp` par un
   identifiant que tu possèdes dans `app/build.gradle.kts`
   (`applicationId`) — nécessaire seulement si tu comptes un jour publier
   sur le Play Store, sinon la valeur par défaut suffit pour un usage perso.

> Le SDK `supabase-kt` évoluant vite, si un appel (`postgrest.from().select()...`)
> ne compile pas tel quel avec la version résolue, ajuste la signature : la
> logique métier (repositories/viewmodels/écrans) n'a pas besoin de changer.

## 3. Installer l'app sur ton téléphone (gratuit, sans Mac)

1. Sur ton Android : **Réglages > À propos du téléphone**, tape 7 fois sur
   "Numéro de build" pour activer le mode développeur, puis **Réglages >
   Options pour développeurs > Débogage USB** (activer).
2. Branche le téléphone en USB (ou configure le débogage Wi-Fi si tu
   préfères sans câble).
3. Dans Android Studio, sélectionne ton appareil dans la liste en haut, puis
   clique sur ▶️ **Run**. L'app se compile et s'installe directement — pas de
   limite de 7 jours, pas de compte à créer.

## 4. Mises à jour — deux façons de faire

**Option simple (gratuite, recommandée pour démarrer) :** à chaque fois que
le code change, rebranche le téléphone et reclique sur Run dans Android
Studio. Ça écrase l'ancienne version par la nouvelle en quelques secondes.

**Option "store" (25$ une fois, plus confortable au quotidien) :** si tu veux
recevoir tes mises à jour comme une vraie app (notification "Mise à jour
disponible" dans le Play Store, sans rebrancher de câble) :
1. Crée un compte [Google Play Console](https://play.google.com/console)
   (25$, paiement unique, à vie).
2. Crée l'app et une piste de **test interne** (gratuite, illimitée,
   réservée aux emails que tu invites — toi-même).
3. Génère un app bundle signé (`./gradlew bundleRelease` avec un keystore, ou
   via Android Studio : **Build > Generate Signed App Bundle**) et
   uploade-le sur la piste de test interne.
4. Installe l'app **Google Play Store** sur ton téléphone, accepte
   l'invitation de testeur, et l'app apparaît comme installable/mettable à
   jour normalement.

Si tu veux automatiser cette étape 4 avec une pipeline CI (comme la
TestFlight qu'on avait commencée côté iOS), c'est possible et beaucoup plus
simple qu'avec Apple — dis-le-moi et je la mets en place.

## 5. Structure du projet

```
android-mma-recomp/
├── app/src/main/java/com/example/mmarecomp/
│   ├── MainActivity.kt, MMARecompApp.kt
│   ├── model/        # data classes @Serializable ↔ tables Postgres
│   ├── data/          # repositories Supabase (1 par table)
│   ├── util/           # moyenne mobile, détection plateau, calcul cible, parsing WOD
│   ├── viewmodel/       # ViewModels (state Compose), 1 par écran
│   └── ui/
│       ├── theme/        # couleurs/typo (même identité que la maquette)
│       ├── nav/            # navigation par onglets
│       ├── auth/, dashboard/, workout/, nutrition/, weighin/, progress/, settings/
│       └── components/    # graphique de tendance, barres cible/réel, bandeau doux
└── app/src/main/res/       # icône adaptative, thème, strings
```

Mêmes règles UX que la version iOS : `WeightTrendChart` n'affiche que la
moyenne mobile 7 jours (jamais le poids brut), `PlateauDetector` transforme
un plateau + performances en hausse en message positif, et
`TargetVsActualBar` ne culpabilise jamais sur un déficit calorique.

## 6. Limitations connues de ce scaffold

- Les ViewModels d'écran sont instanciés directement (pas via
  `ViewModelProvider.Factory`) pour rester simple : une rotation d'écran
  réinitialise le formulaire en cours. À corriger si ça gêne à l'usage.
- Pas de compilation possible dans cet environnement (pas d'Android SDK/Gradle
  ici) — le code est écrit avec soin mais pas vérifié par le compilateur.
- `LoggedExercise` (exercices d'une séance) n'a pas d'identifiant stable côté
  client : la liste `itemsIndexed` de `WorkoutLogScreen` recompose donc par
  index plutôt que par clé stable. Ajouter un id générerait un champ qui
  n'existe pas dans le schéma Postgres (`workouts.exercices` est un JSON de
  `LoggedExercise` tel quel) — à traiter avec une migration de schéma si ça
  devient gênant.

## 7. Améliorations UX (itérations récentes)

Ajouts successifs à l'expérience utilisateur, tous sans changer le schéma
Supabase ni la logique métier :

- **États vides & erreurs** : écrans repas/séances/progression avec des
  états vides encourageants (`EmptyState`), bannière d'erreur réseau avec
  bouton "Réessayer" (`ErrorBanner`), gestion du chargement/erreur sur le
  Dashboard.
- **Accessibilité** : `contentDescription`/semantics TalkBack sur
  `TargetVsActualBar` et `WeightTrendChart` (le graphique décrit une
  tendance, jamais une valeur brute), touch targets ≥ 48dp sur la liste
  d'aliments préchargés.
- **Formulaires** : validation en temps réel (poids, email), clavier
  adapté, auto-focus, bouton pour effacer une recherche.
- **Petits ajouts utiles** : mode sombre manuel (Système/Clair/Sombre),
  export CSV de l'historique des pesées, rappel local doux pour la pesée du
  matin (opt-in, 7h30), suppression de repas avec "Annuler" (undo),
  recherche/filtre des repas par créneau avec en-têtes collants, FAB
  d'accès rapide (Pesée/Repas/Séance) sur le Dashboard.
- **Feedback & cohérence visuelle** : transitions animées entre onglets,
  retour haptique à la sauvegarde, confirmations de succès animées,
  formatage numérique cohérent (`Formatting.oneDecimal`), tokens
  d'espacement partagés (`Dimens`).

Tout respecte les règles UX non négociables existantes : moyenne mobile 7
jours uniquement pour le poids, messages toujours positifs sur les
plateaux, jamais de ton culpabilisant sur les écarts caloriques.

## 8. Améliorations UX — round 2

Deuxième lot de 30 améliorations, complémentaire au premier (§7) :

- **Robustesse réseau** : `ErrorBanner` + "Réessayer" branché sur les cinq
  écrans de log (pesée, séance, repas, progression, réglages) —
  `WorkoutLogViewModel.loadPlan` remontait auparavant ses erreurs en
  silence.
- **DateField enrichi** : labels relatifs ("Aujourd'hui"/"Hier"), raccourci
  "Aujourd'hui", et date future bloquée par défaut (log rétroactif
  uniquement, `maxDate` reste surchargeable).
- **Répétition intelligente** : reprendre le repas d'hier sur un créneau,
  reprendre les valeurs de la dernière pesée, répartition indicative
  calories/protéines par créneau affichée dans le formulaire repas.
- **Séances** : exercices réordonnables (flèches), suppression avec undo,
  chips de durée rapide, compteur d'exercices et de séries dans l'en-tête,
  pastille de couleur par type de séance, texte d'aide sous "reps propres".
- **Confiance & repères doux** : confirmation avant déconnexion, repère
  "Dernière pesée : il y a X jours" (jamais le poids brut), badge neutre
  "✓ atteint" sur `TargetVsActualBar` (jamais de mention négative),
  message d'accueil contextualisé et compteur de repas loggés aujourd'hui
  sur le Dashboard.
- **Clavier & perf** : navigation clavier cohérente (Next/Done + fermeture)
  sur les derniers champs numériques, liste de repas filtrée mémoïsée
  (`remember`) pour éviter un refiltre à chaque recomposition.
- **Contraste états désactivés** : vérifié plutôt que modifié — les
  `Button`/`OutlinedButton` Material3 appliquent déjà une opacité réduite
  conforme aux specs d'accessibilité sur leur état `disabled`, pas de
  changement nécessaire.

Même discipline que le round 1 : aucune modification du schéma Supabase ni
de la logique métier, règles UX non négociables toujours respectées.

## 9. Améliorations UX — round 3 (en continu)

Amélioration continue par petits lots, fusionnés au fur et à mesure :

- **Écran MMA complété** (lot 1) : champ Date manquant ajouté (la séance
  MMA était toujours loguée à la date du jour), ErrorBanner + retry,
  feedback de succès animé + haptique, tous les mouvements détectés
  affichés, accessibilité sur l'échelle de ressenti.
- **Dashboard** (lot 1) : carte "série" — jours consécutifs avec au moins
  une activité loguée, jamais de message négatif si la série est courte.
- **WeighInScreen** (lot 2) : historique complet dépliable, indicateur de
  tendance texte (hausse/baisse/stable, jamais de valeur brute).
- **ProgressScreen** (lot 2) : carte résumé (séances/pesées loguées sur la
  fenêtre sélectionnée).
- **MealLogScreen** (lot 2, 3) : totaux glucides/lipides du jour affichés,
  filtre par catégorie d'aliment en plus de la recherche par nom.
- **Settings** (lot 2) : section "À propos" avec le numéro de version.
- **WorkoutLogScreen** (lot 3) : historique des séances dépliable avec
  suppression + undo, bouton "Reprendre la séance d'hier", bouton "Vider
  le formulaire".

Toujours la même discipline : aucun changement de schéma Supabase, règles
UX non négociables respectées, petits commits testables.
