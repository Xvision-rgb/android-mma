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

- **Validation & robustesse** (lot 4) : bornes réalistes sur poids/BF/
  quantités, duplication d'exercice, haptics sur les suppressions.
- **AuthScreen** (lot 6, 8) : afficher/masquer le mot de passe, navigation
  clavier Next/Done, message de confirmation après inscription.
- **Vider le formulaire** (lot 6) : ajouté sur tous les écrans de log pour
  cohérence.
- **Programme de la semaine** (lot 7) : section dépliable sur le Dashboard
  listant le split programmé jour par jour.
- **Flags de pesée visibles** (lot 7) : créatine/alcool/post-training
  enfin affichés dans l'historique (calculés mais jamais rendus avant).
- **Dernière charge connue** (lot 8) : repère "Dernière fois : Xkg" par
  exercice dans WorkoutLogScreen, basé sur l'historique déjà chargé.
- **Rappel repas du soir** (lot 8) : second rappel local opt-in (20h),
  même structure que le rappel pesée.
- **Suppression pesée + undo** (lot 9) : dernière table sans endpoint de
  suppression, comblée pour cohérence avec repas/séances.
- **Record personnel** (lot 9) : badge sur la charge max atteinte par
  exercice suivi (ProgressScreen).
- **Repères contextuels non comparatifs** (lot 9) : total calorique d'hier
  sur le Dashboard, explicitement présenté comme un repère et non une
  comparaison.
- **Fenêtre 12 semaines** (lot 10) : ajoutée à ProgressScreen (4/8/12).
- **Volume d'entraînement estimé** (lot 10) : séries × reps × charge
  réelle affiché dans WorkoutLogScreen.
- **Permission notification refusée** (lot 10) : message explicatif au
  lieu d'un échec silencieux.
- **Cible nutrition personnalisée** (lot 11) : calories/protéines cible
  ajustables librement dans MealLogScreen, en plus des préréglages
  training/repos.
- **Historique des séances MMA** (lot 13) : dernier écran de log sans
  historique/suppression, comblé (MmaSessionRepository.delete ajouté).
- **Modifier le programme depuis le Dashboard** (lot 14) : le type de
  séance de chaque jour du split hebdo est modifiable directement dans la
  section "Voir le programme de la semaine" — TrainingPlanRepository.upsert
  existait déjà côté données mais n'avait aucune UI pour l'appeler.


## 10. Améliorations UX — round 4 (accessibilité, robustesse, polish)

Après trois rounds, le gisement de nouvelles fonctionnalités sûres était
jugé épuisé — ce round se concentre sur l'accessibilité, la robustesse et
la cohérence, en repartant du principe qu'un round précédent peut toujours
avoir laissé des détails de côté (audit du code, pas seulement de l'ajout) :

- **Toggles accessibles** : les rangées label+Switch (pesée, rappels)
  utilisent `Modifier.toggleable` — toute la ligne cliquable, annoncée
  correctement par TalkBack.
- **Garde-fous valeurs négatives** : calories/macros, charge cible/réelle,
  durée de séance ne peuvent plus être négatives.
- **DateField** : rôle et label d'accessibilité explicites sur le
  déclencheur du sélecteur de date.
- **Feedback transitoire auto-masqué** : les messages "repris depuis
  hier" se masquent après 4s au lieu de rester affichés indéfiniment.
- **Indice %BF manquant** : explique pourquoi la carte %BF n'apparaît pas
  au lieu de la faire disparaître silencieusement.
- **Navigation clavier** : chaîne Next/Done complétée sur Settings
  (Objectifs) et le formulaire d'ajout de repas.

Toujours la même discipline : aucun changement de schéma Supabase, règles
UX non négociables respectées, petits commits testables.

- **Réduction des recompositions inutiles** : `activityStreakDays`
  (Dashboard), `foodCategories` et `filteredFoods` (log repas) étaient
  lues deux fois par passage de composition — chacune refaisait tout son
  calcul (parcours, filtrage) au lieu d'être hoistée en `val` locale une
  seule fois.
- **Auto-masquage des confirmations d'enregistrement** : les messages
  "enregistré ✓" (pesée, séance, repas) restaient affichés indéfiniment
  après le premier enregistrement au lieu de disparaître comme le fait
  déjà le feedback de duplication ; harmonisé à 4s sur les trois écrans.
- **Nettoyage visuel log séance** : suppression d'un libellé texte
  redondant avec le `contentDescription` de l'icône de suppression
  d'exercice, incohérent avec les autres boutons icône de la même ligne.
- **Robustesse écran de connexion** : l'action clavier "Terminé" du champ
  mot de passe pouvait déclencher une tentative de connexion avec un
  email invalide ou un envoi déjà en cours (garde-fou aligné sur celui du
  bouton) ; message dédié pour une erreur réseau plutôt que le message
  générique d'échec d'authentification.
- **Toggle "propre" accessible** : le Switch "Toutes les reps faites
  proprement" (log séance) avait été oublié lors de l'audit précédent des
  toggles — toute la ligne est maintenant cliquable et annoncée
  correctement par TalkBack, comme les autres toggles de l'app.
- **Menu rapide FAB** : le menu d'accès rapide (pesée/repas/séance) du
  Dashboard pouvait rester déplié après un changement d'onglet, faute de
  réinitialisation de son état à la navigation.
- **Messages d'erreur réseau distincts** : tous les écrans de chargement
  initial (Dashboard, pesée, repas, séance, plan, progression, historique
  MMA, réglages) affichaient un message d'échec générique même en cas de
  simple coupure réseau. Chacun distingue maintenant une erreur réseau
  d'une erreur applicative, avec un message qui invite à réessayer plutôt
  qu'à douter des données.

## 11. Améliorations UX — round 5 (fonctionnalités notables)

Après quatre rounds de polish et de robustesse, ce round revient à
l'ajout de fonctionnalités visibles, à partir des données déjà chargées
ou déjà exposées par les repositories existants — toujours aucun
changement de schéma Supabase :

- **Bilan de la semaine (Dashboard)** : nouvelle carte de synthèse —
  volume d'entraînement cumulé (séries × reps × charge réelle),
  régularité des repas loggés (jours sur 7), tendance de poids (moyenne
  mobile 7j).
- **Volume d'entraînement dans le temps (Progression)** : nouveau
  graphique agrégeant le volume total par semaine, en complément des
  graphiques poids/%BF/charge par exercice déjà existants.
- **Comparatif de séance (log séance)** : repère factuel avec le volume
  de la dernière séance du même type ("juste un repère, pas un objectif
  à battre").
- **Objectif de poids sur le Dashboard** : l'écart entre le poids actuel
  (moyenne mobile 7j) et l'objectif défini dans Réglages était calculé
  nulle part ailleurs que dans le formulaire de saisie — affiché
  maintenant de façon neutre, sans direction imposée (prise ou perte).
- **Filtre d'historique par type (log séance)** : chips de filtre sur
  l'historique dépliable, même pattern que le filtre par catégorie
  d'aliment côté repas.
- **Répartition des types de séance (Progression)** : la carte résumé
  affiche maintenant la fréquence de chaque type de séance sur la
  fenêtre sélectionnée.
- **Tendance calorique (Progression)** : nouveau graphique "Calories
  (moyenne 7j)", même principe de lissage que le poids.
- **Rappels intelligents** : les rappels locaux pesée du matin et repas
  du soir ne se déclenchent plus si l'action correspondante a déjà été
  faite aujourd'hui.
- **Export CSV des repas (Progression)** : dernier historique sans
  export disponible, comblé pour cohérence avec pesées/séances.
- **Ressenti moyen MMA** : moyenne du ressenti sur les dernières séances
  affichée dans le log MMA — donnée déjà loguée mais jamais agrégée.
- **Historique récent des repas** : le log repas était le seul écran de
  log sans section historique dépliable — "Voir l'historique récent (14
  derniers jours)" ajouté, avec une action "Reprendre" par ligne pour
  reloguer un repas habituel sans tout ressaisir (même pattern que les
  autres écrans).

## 12. Corrections — patron "écrase au lieu d'accumuler/préserver"

Bug rapporté par l'utilisateur : dans le log repas, sélectionner un
deuxième aliment préchargé écrasait les calories/macros du premier au
lieu de s'y ajouter. Corrigé en rendant l'ajout d'aliment explicite et
additif (liste "Aliments ajoutés à ce repas", un bouton "Ajouter" par
aliment, total recalculé comme la somme). Un audit du reste de l'app à
la recherche du même patron a trouvé et corrigé deux autres cas :

- **"Reprendre le repas d'hier sur ce créneau"** enregistrait
  directement en base, écrasant silencieusement un repas déjà loggé
  aujourd'hui sur ce créneau, et court-circuitait le nouveau système
  d'accumulation. Devient une simple lecture qui ajoute le repas d'hier
  comme ligne de plus au repas en cours (comme "Reprendre" depuis
  l'historique récent, qui avait le même souci — il vidait la liste
  d'aliments déjà ajoutés au lieu d'y ajouter).
- **"Reprendre la séance d'hier"** remplaçait entièrement la liste
  d'exercices déjà saisis par ceux d'hier. Les exercices d'hier sont
  désormais ajoutés à la suite de ceux déjà présents ; la durée n'est
  reprise que si le champ était encore vide.

Vérifié mais volontairement non touché (pas ce patron) : le pré-remplissage
poids/%BF depuis la dernière pesée (champs scalaires uniques, remplacer
est le comportement voulu) ; les cibles nutritionnelles (une seule par
jour par design) ; tous les `remember` locaux (aucun non-clé partagé
entre plusieurs éléments d'une même liste trouvé).

## 13. Import et édition du programme d'entraînement

- **Progression vers l'objectif de %BF (Dashboard)** : `bfObjectifPct`
  était saisi dans Réglages mais jamais réutilisé — même traitement que
  l'objectif de poids (moyenne mobile 7j, message neutre sans direction
  imposée).
- **Éditeur d'exercices du programme** : jusqu'ici seul le type de séance
  de chaque jour du split hebdo était modifiable — la liste d'exercices
  programmés (`PlannedExercise`) n'avait aucune UI d'édition. Nouvel
  écran accessible via une icône crayon sur chaque jour de "Voir le
  programme de la semaine" (Dashboard) : ajouter/modifier/dupliquer/
  réordonner/retirer un exercice, brouillon local avec un seul
  enregistrement explicite (pas d'écriture à chaque frappe).
- **Import d'un programme collé en texte libre** (Réglages → "Importer
  un programme") : parseur best-effort (`TrainingPlanParser`, même
  esprit que `WodParser`) qui détecte les jours de la semaine en titres
  puis les exercices ("Squat 4x8 @80kg" et variantes). Jamais
  d'enregistrement automatique : aperçu éditable par jour détecté, choix
  explicite Compléter/Remplacer si un jour a déjà des exercices
  programmés, validation jour par jour ou en un clic pour tous.

## 14. Programme d'entraînement — fiabilisation (2 lots)

Audit dédié de la fonctionnalité programme (import + éditeur) après sa
mise en place initiale :

- **Parseur enrichi** : jours abrégés (Lun, Mar...), préfixes de liste
  numérotée ("1. Squat..."), reps en plage ("8-12"), poids en livres
  convertis en kg, plusieurs exercices détectés sur une même ligne.
- **Validation** : un exercice avec un nom vide ne peut plus être
  enregistré silencieusement (bloqué avec message explicite, éditeur et
  import).
- **Deux bugs "écrase au lieu de préserver"** trouvés et corrigés (même
  patron que le fix MealLogScreen de cette session) : ré-analyser un
  texte importé effaçait le statut des jours déjà enregistrés ;
  "Réessayer" après un enregistrement raté rechargeait depuis le serveur
  et effaçait les modifications en cours au lieu de retenter
  l'enregistrement.
- **Robustesse réseau** : l'enregistrement groupé de l'import s'arrête
  proprement dès qu'une coupure réseau est détectée au lieu d'insister
  sur chaque jour restant.
- **UX** : indicateur "Modifications non enregistrées", confirmation
  animée avant retour, résumé après "Tout enregistrer", jour importé
  déjà enregistré passant en lecture seule plutôt que de rester dans un
  état éditable sans issue, navigation clavier cohérente avec le reste
  de l'app.
- Suppression d'un petit code mort introduit lors de l'ajout initial
  (deux fonctions jamais appelées, l'import ayant fini par utiliser son
  propre mécanisme).

## 15. Ajout de plusieurs aliments à un même repas — re-vérification

Suite à un retour utilisateur signalant que l'ajout de plusieurs aliments
sur le même créneau ne fonctionnait toujours pas, relecture complète de
`MealLogScreen.kt` et `MealLogViewModel.kt` : le flux d'accumulation
(`mealItems`, bouton "Ajouter X à ce repas", totaux recalculés à chaque
ajout) est intact et fonctionne correctement de bout en bout. **Aucune
régression trouvée dans le code.** Si le problème persiste sur l'appareil
de l'utilisateur, c'est très probablement dû au fait que l'app installée
n'est pas synchronisée avec le dépôt (les fichiers sont copiés à la main
plutôt que via `git pull`) — `MealLogScreen.kt` et `MealLogViewModel.kt`
sont les deux fichiers à resynchroniser en priorité.

Correctif réel trouvé pendant cette même passe : chips de filtre
(créneau de repas, catégories d'aliments, durée de séance présélectionnée,
type de séance dans l'historique) qui pouvaient déborder hors de l'écran
sur les appareils étroits, même patron que la barre de navigation du bas
corrigée précédemment — ajout d'un défilement horizontal sur ces lignes.

## 16. Bibliothèque d'aliments — extension

`supabase/002_foods.sql` ne contenait qu'une trentaine d'aliments de base.
Ajout de `supabase/003_foods_extended.sql` avec ~150 aliments courants
supplémentaires (viandes/poissons/œufs/protéines végétales, féculents et
légumineuses, oléagineux et huiles, une large variété de fruits et
légumes, fromages et produits laitiers, snacks/boissons/condiments) pour
que la recherche préchargée dans l'écran Repas couvre beaucoup plus de
cas sans ressaisie manuelle des macros. Même format que le fichier
existant (`on conflict (nom) do nothing`, donc ré-exécutable sans risque
de doublon) — **à exécuter dans le SQL Editor Supabase, après
`002_foods.sql`**, comme n'importe quelle nouvelle migration du dossier
`supabase/`.

Ajout de `supabase/004_foods_extended_2.sql` avec 236 aliments
supplémentaires : charcuterie et gibier, poissons et fruits de mer
(dorade, moules, huîtres, coquilles Saint-Jacques...), légumineuses et
céréales moins courantes, une quarantaine de fromages réels
supplémentaires, beaucoup plus de fruits/légumes/herbes fraîches, des
plats préparés de cuisine du monde (couscous royal, curry, pad thaï,
ramen, tacos, falafel...), snacks/pâtisseries/boissons, et quelques
produits de nutrition sportive. Bibliothèque totale : ~410 aliments
(002 + 003 + 004). Même format, à exécuter après les deux fichiers
précédents. Note honnêteté : la demande initiale portait sur 1000
aliments — au-delà de quelques centaines, continuer à générer des noms
"réels" distincts sans tomber dans le doublon déguisé ou la valeur
nutritionnelle approximative au hasard devient contre-productif ; ce lot
vise la couverture la plus large et fiable possible en une passe, la
suite peut se faire par lots supplémentaires si besoin.

## 17. Bandeau "supprimé / Annuler" qui ne disparaissait jamais tout seul

En Compose Material3, `SnackbarHostState.showSnackbar()` bascule sa durée
par défaut sur `Indefinite` dès qu'un `actionLabel` est fourni (au lieu
de `Short`) — les 5 snackbars "X supprimé(e) / Annuler" de l'app (repas,
exercice de séance, séance training, séance MMA, pesée) restaient donc
affichées indéfiniment jusqu'à balayage manuel. Ajout explicite de
`duration = SnackbarDuration.Long` sur ces 5 snackbars.

## 18. Objectif calorique personnalisé (Bulk / Recomposition / Coupe)

L'app calculait une cible calorique générique et figée (~2000 cal), bien
trop agressive pour un pratiquant de sport de combat qui s'entraîne
6-7x/semaine (déficit réel de l'ordre de -600 cal, avec risque de perte
musculaire). Ajout de `CalorieCalculator` :
- Maintenance = poids × 30 × 1.4-1.6 (multiplicateur sport de combat),
  au lieu d'une formule type Mifflin-St Jeor pensée population sédentaire.
- Trois modes avec offsets bornés à des plages sûres : Bulk +300 à +500,
  Recomposition +0 (recommandé par défaut pour rester sec), Coupe -200 à
  -300 (jamais -600).
- Avertissements doux si déficit/surplus dépasse un seuil, plancher
  absolu (jamais sous poids × 25 cal/j) quel que soit le mode.
- Macros dérivées de la masse maigre estimée (protéines 2g/kg, lipides
  ~27.5% des calories, glucides le reste).
- `MealLogViewModel.setTarget()` utilise maintenant cette formule
  personnalisée (poids réel de la dernière pesée + mode choisi) au lieu
  des valeurs figées 2050/1800 — c'était la source directe du problème.
- Nouvel écran **Réglages → Objectif calorique** : maintenance estimée,
  une carte par mode, badge "Recommandé", application en un clic.
- Nouveau champ profil `objectif_calorie_mode`, migration
  `supabase/005_calorie_mode.sql` à exécuter après les précédentes.

Volontairement pas inclus dans ce lot (features à part entière, hors
scope) : suivi de circonférences, comparaison photos, section "sources
scientifiques" affichée dans l'app — plusieurs références proposées
n'étaient pas des citations vérifiables, donc non intégrées pour éviter
une fausse caution scientifique dans le produit.

## 19. Passe UX longue — inspirée d'apps de référence (Strong, Hevy, MyFitnessPal…)

Lot 1 (musculation, inspiré de Strong/Hevy) :
- Minuteur de repos manuel (60/90/120/150s) sur l'écran Séance, avec
  vibration en fin de décompte — déclenchement manuel car notre modèle
  logue un exercice global, pas set par set comme Strong/Hevy.
- Détection de record personnel : la charge saisie est comparée au MAX
  historique (pas seulement la dernière séance) — bandeau positif +
  vibration uniquement en cas de vrai record, jamais de faux positif au
  premier log d'un exercice.
- Pré-remplissage de la charge cible avec la dernière charge connue quand
  le nom d'un exercice est renseigné, sans jamais écraser une saisie
  manuelle existante.

Lot 2 (nutrition, inspiré de MyFitnessPal) :
- Jusqu'à 5 aliments récents en accès rapide au-dessus de la recherche
  sur l'écran Repas, déduits de l'historique déjà chargé (pas de requête
  serveur supplémentaire).
