# BRIEF — Refonte visuelle (Lot 0 + 8 lots)

Repo : `android-mma` — module `android-mma-recomp/`
Stack : Kotlin + Jetpack Compose (Material3), MVVM, Supabase (Postgres + RLS),
SharedPreferences pour le local.
Branche : `claude/ux-visual-redesign-iieorn`. Commit + push à chaque lot.

Sources et références : voir `refonte_visuelle_sources.md`.
Briefs liés : `brief_coach_autoregulation.md` (règles métier), `coach_prompt.md`.

## OBJECTIF DE L'UTILISATEUR — cadre toutes les décisions produit

Force relative et densité musculaire — archétype lutteur. Pratique MMA en
parallèle. L'indicateur directeur est **1RM estimé / poids corporel**.

COROLLAIRE VISUEL, nouveau et non négociable : **ce qui est le plus gros à
l'écran est ce que l'app désigne comme l'objectif.** Aujourd'hui rien n'est
plus gros que rien — donc l'app ne désigne rien. La refonte doit rendre la
force relative visuellement dominante, et le poids brut visuellement mineur.

## RÈGLES EXISTANTES — à préserver dans chaque lot

Reprises de `brief_coach_autoregulation.md`, toujours valides :

1. Le poids ne s'affiche QUE via `MovingAverage.sevenDay()`. Jamais de pesée
   brute présentée comme un résultat.
2. Aucune culpabilisation sur un déficit, un repas ou une séance manquée.
   Alertes via `SoftAlertBanner`, ton neutre.
3. Séries de jours consécutifs positives uniquement — jamais de « série brisée ».
4. Palette Material3 existante uniquement (`ui/theme/Color.kt`). Espacements
   via `Dimens`. Pas de valeurs codées en dur.
5. Français dans toute l'UI. KDoc en français.

> **Note sur la règle 4** : elle est aujourd'hui violée dans le code lui-même
> (voir défaut D8). Le lot 1 la rend applicable ; les lots suivants
> l'appliquent. Elle n'est pas assouplie, elle est enfin outillée.

---

# AUDIT DE L'EXISTANT

Constats vérifiés dans le code au 29 août 2026, avec références de fichiers.
Chaque défaut est repris par au moins un lot.

## D1 — Aucune `TopAppBar` dans toute l'application

Le seul `Scaffold` de l'app est celui de `ui/nav/RootNav.kt:88`. Aucun écran
n'a de barre de titre. Les titres d'écran sont des `Text` posés comme premier
`item` d'une `LazyColumn` — `Text("Log séance")` en `WorkoutLogScreen.kt:119`,
`Text("Progression")` dans `ProgressScreen.kt`. **Ils défilent et disparaissent
dès le premier scroll** : passé quelques centimètres, l'écran n'est plus
identifiable.

## D2 — Quatre écrans empilés sans aucun retour visible — *bug de navigation*

`plan_edit/{jourSemaine}`, `workout/mma`, `plan_import` et `calorie_goal` sont
des destinations poussées sur la back stack. Aucune des quatre ne contient
`ArrowBack` (vérifié : 0 occurrence dans `TrainingPlanEditScreen.kt`,
`MmaSessionScreen.kt`, `ImportTrainingPlanScreen.kt`, `CalorieGoalScreen.kt`).
Elles n'affichent pas non plus la `NavigationBar`… qui reste pourtant montée
par le `Scaffold` parent. **Le seul retour est le geste système.** Sur
`CalorieGoalScreen` et `ImportTrainingPlanScreen`, atteintes depuis Réglages,
c'est une impasse visuelle.

C'est le défaut le plus grave de l'audit : les trois autres écrans ont au moins
un `onSaved` qui dépile, `ImportTrainingPlanScreen` et `CalorieGoalScreen` n'ont
rien du tout.

## D3 — Six onglets dans la barre de navigation

`RootNav.kt:73-80`. Material 3 en recommande 3 à 5. La conséquence est déjà
visible dans le code : le cinquième onglet est libellé `"Progr."`
(`RootNav.kt:78`) — une abréviation tronquée pour tenir dans la largeur, avec
`TextOverflow.Ellipsis` en filet de sécurité. Le libellé a été sacrifié plutôt
que la structure.

## D4 — Le dashboard empile 14 blocs de poids visuel identique

`DashboardScreen.kt` : salutation, `StreakBadge`, `RecoveryReadinessCard`,
`RelativeStrengthCard`, `VolumeDistributionCard`, bannières de conflits,
`WearablesCard`, `NextWorkoutCard`, `AskClaudeCard`, `ErrorBanner`, carte série
d'activité, « Cette semaine », « Séances », « Tendance poids », « Nutrition ».
Tous rendus dans le même conteneur, à la même largeur, avec le même rayon de
16dp et le même espacement de 16dp.

Aucun de ces blocs n'est plus important qu'un autre **visuellement**, alors que
`RelativeStrengthCard` porte l'indicateur directeur du projet et que
`WearablesCard` affiche des données factices (voir D6). L'ordre du code
place d'ailleurs la force relative en 4ᵉ position, derrière un badge de streak.

## D5 — La même donnée affichée deux fois, deux streaks concurrents

- `avgCaloriesLast7Days` est rendu dans la carte « Cette semaine »
  (`DashboardScreen.kt:198`) **et** dans la carte « Nutrition »
  (`DashboardScreen.kt:342`). Même valeur, deux formulations, deux endroits.
- Deux compteurs de série coexistent sur le même écran :
  `StreakBadge(currentStreak, bestStreak)` (`DashboardScreen.kt:103`, alimenté
  par `streakManager`) et la carte `activityStreakDays`
  (`DashboardScreen.kt:165-184`, calcul distinct dans le ViewModel).
  Deux nombres de « jours d'affilée » qui peuvent diverger, sans que rien
  n'explique lequel fait foi.

## D6 — De l'UI factice et un bouton mort en production sur l'écran d'accueil

- `WearablesCard` affiche des constantes écrites en dur — `"7h 30m"` de sommeil
  et `"52 bpm"` de fréquence cardiaque — sous un libellé `"Mock data (demo):"`
  (`components/WearablesCard.kt:148-160`).
- `AskClaudeCard` reçoit un `mockSummary` construit en dur, incluant un
  `"Charge max: 80kg"` qui ne vient d'aucune donnée (`DashboardScreen.kt:158`).
- `NextWorkoutCard` expose un `Button` pleine largeur « Lancer la séance »
  câblé sur `onStartClick = {}` (`DashboardScreen.kt:152`) : **le bouton le plus
  proéminent du dashboard ne fait rien.**

Ces trois éléments viennent des lots « engagement » (commits `39612ad` à
`cfc5588`, explicitement « Mock Components »). Ils n'ont jamais été retirés.

## D7 — Une échelle typographique définie au tiers

`ui/theme/Type.kt` ne déclare que 6 des 15 styles Material 3 : `displayLarge`,
`titleLarge`, `titleMedium`, `bodyLarge`, `bodyMedium`, `labelSmall`. Or
`bodySmall` est utilisé **76 fois** dans l'UI et n'est pas défini — il retombe
sur le défaut Material 3. Idem pour `titleSmall`, `headlineSmall`,
`headlineMedium` (utilisé en `AuthScreen.kt` pour le nom de l'app) et
`labelMedium`. Le style le plus employé de l'app est donc l'un de ceux qui ne
sont pas maîtrisés.

Aucune `FontFamily` n'est déclarée : l'app utilise la police système par
défaut, sans identité propre.

## D8 — `Dimens` existe mais est contourné

`ui/theme/Dimens.kt` définit la grille. Le code la contourne massivement :
18 occurrences de `16.dp`, 12 de `6.dp`, 10 de `8.dp`, 7 de `12.dp`, 6 de
`10.dp` écrites en dur. Le `6.dp` — espacement interne de `DashCard`
(`DashboardScreen.kt:414`) — n'appartient même pas à la grille de 4dp.
La règle 4 du brief précédent est donc déjà enfreinte dans l'écran principal.

## D9 — Aucun composant `Card` Material 3, trois traitements de surface divergents

Recherché dans tout `ui/` : zéro `Card`, `ElevatedCard` ou `OutlinedCard`.
Chaque carte est un `Column` avec `.background(...)` + `.padding(...)` recopié,
et les surfaces divergent :

| Composant | Surface utilisée |
|---|---|
| `DashCard` (`DashboardScreen.kt:408`) | `colorScheme.surface` |
| `RecoveryReadinessCard` | `colorScheme.surface` |
| `StreakBadge` | `colorScheme.surfaceVariant` |
| `AskClaudeCard` | `colorScheme.tertiary` |

Conséquence directe : **aucune élévation, aucune bordure**. La carte ne se
détache du fond que par une différence de teinte. En sombre, `SurfaceDark`
(`#1C1C1E`) contre `PaperDark` (`#14171C`) donne une séparation très faible —
les cartes se fondent dans le fond.

## D10 — Tout est en petit texte gris

`onSurfaceVariant` apparaît **125 fois**, `bodySmall` **76 fois**, très
largement combinés. C'est le registre par défaut de l'app : sur la carte
« Cette semaine », quatre lignes consécutives sont en `bodySmall` +
`onSurfaceVariant`. Quand tout est secondaire, plus rien ne l'est — et
l'information vraiment importante n'a aucun moyen de ressortir.

## D11 — L'action primaire est au bout du scroll

`WorkoutLogScreen.kt:402` : « Enregistrer la séance » est le dernier `item`
d'une `LazyColumn` qui contient date, type, durée, chips, timer de repos, N
blocs d'exercices avec leurs séries, notes et historique. **Après six
exercices, valider sa séance demande de traverser tout l'écran.** C'est
exactement le geste qu'on fait en salle, une main sur le téléphone, entre deux
séries. Même problème sur `MealLogScreen` et `WeighInScreen`.

## D12 — La divulgation progressive passe par des `TextButton` qui replient du contenu

« Voir le programme de la semaine » (`DashboardScreen.kt:262`), « Voir
l'historique des séances » (`WorkoutLogScreen.kt:334`) : des `TextButton` qui
injectent du contenu **à l'intérieur d'un `item` de `LazyColumn`**. Le contenu
déplié n'est donc pas virtualisé, et la position de scroll saute. Ce sont des
sous-écrans déguisés en accordéons.

---

# PRINCIPES ISSUS DE LA RECHERCHE

Cinq principes à appliquer, tirés de `refonte_visuelle_sources.md`. Ils ne sont
pas des goûts esthétiques : chacun répond à un défaut de l'audit.

**P1 — Une seule « grande chose » par écran.** Oura structure son onglet Today
autour d'un unique score dominant ; WHOOP rend sa métrique principale à ~72pt,
« lisible à bout de bras ». La taille porte la hiérarchie à elle seule.
→ répond à D4, D10. Ici, la grande chose est la **force relative**.

**P2 — Un langage couleur étroit et répété.** WHOOP n'utilise que trois
couleurs sémantiques (prêt / entre-deux / charge haute), identiques sur tous
les écrans : « l'utilisateur apprend le langage visuel une fois ».
→ l'app a déjà `workoutTypeColor` et les couleurs de `ReadinessAction`, mais
elles ne sont pas posées comme un système. À formaliser, sans nouvelle teinte.

**P3 — Divulgation progressive en niveaux, pas en accordéons.** WHOOP empile
score → tendance → détail biométrique. Le détail est une destination, pas un
dépliage. → répond à D12, et allège D4.

**P4 — Zone du pouce et cibles généreuses.** L'action primaire vit en bas de
l'écran, dans la zone atteinte sans changer de prise ; 48dp minimum, avec
retour haptique — « des doigts moites après une série ont besoin de cibles plus
grandes qu'une app bancaire ». → répond à D11.

**P5 — Compter les actions.** MacroFactor mesure ses parcours en nombre
d'actions et en fait son argument principal (24 contre 36 sur 4 parcours face à
MyFitnessPal). Le chiffre vient de l'éditeur et est à prendre comme un ordre de
grandeur ; **la méthode, elle, est le bon critère d'acceptation** — on compte
les taps avant et après chaque lot.

---

# LOT 0 — Socle technique (BLOQUANT)

Rien de ce qui suit n'est possible sans ça. `app/build.gradle.kts` est sur
`compose-bom:2024.06.00`, soit **material3 1.2.1**, et `compileSdk = 34`.
La version stable de material3 est **1.4.0** (26 août 2026).

## À faire

- Monter `compileSdk` et `targetSdk` à 36, garder `minSdk = 26`.
- Monter le BOM Compose vers la version qui résout **material3 1.4.0 stable**.
- Monter en cohérence `activity-compose`, `lifecycle-*`, `navigation-compose`,
  `core-ktx`.
- Vérifier que le projet compile et que les tests JVM existants passent
  (`ApreEngineTest`, `TrainingLoadTest`, `MuscleZoneClassifierTest`,
  `LoggedExerciseTest`) **avant** de toucher à la moindre vue.

## Décision assumée : stable, pas alpha

Les composants Expressive (`FloatingActionButtonMenu`, `ButtonGroup`,
`FloatingToolbar`, `SplitButton`) ne sont sortis de l'expérimental qu'en
**1.5.0-alpha19 à alpha22** (mai-juin 2026). On reste sur **1.4.0 stable** :
elle apporte déjà le système de formes, les typographies emphatiques et le
mouvement à ressorts, ce qui couvre 90 % du brief. Le lot 8, optionnel, est le
seul à dépendre d'une alpha — à ne faire que si tout le reste est livré et
stable.

## Critère de sortie

Build vert, tests verts, application lancée sur téléphone, **aucun changement
visuel**. Si l'UI bouge à ce lot, c'est une régression, pas un progrès.

---

# LOT 1 — Tokens : rendre la règle 4 applicable

Aujourd'hui `Dimens` ne couvre pas assez de cas pour que la règle « pas de
valeurs codées en dur » soit tenable. On complète le socle avant de l'imposer.

## `ui/theme/Type.kt` — compléter l'échelle (D7)

Définir **les 15 styles** Material 3, pas 6. En priorité ceux qui sont déjà
utilisés et non définis : `bodySmall` (76 usages), `titleSmall`,
`headlineSmall`, `headlineMedium`, `labelMedium`, `labelLarge`.

Déclarer une `FontFamily` explicite. Contrainte : elle doit rester lisible en
condition de salle, à bout de bras, sur des chiffres. Une grotesque à chiffres
tabulaires pour les métriques (les valeurs ne doivent pas gigoter quand elles
changent), la police système pour le corps de texte reste acceptable si aucune
n'est embarquée.

Ajouter un style `metricHero` (ou étendre `displayLarge`) pour la métrique
dominante du principe P1 : nettement au-dessus des 40sp actuels.

## `ui/theme/Dimens.kt` — couvrir les cas réels (D8)

Ajouter ce qui manque et force le code à écrire des `dp` en dur : rayons
(`cornerLg`), élévations, épaisseur de bordure, hauteurs de cartes et de
graphes, taille de pastille sémantique (les `8.dp`/`10.dp` de pastille
recopiés dans `DashboardScreen`, `WorkoutLogScreen`, `RecoveryReadinessCard`),
padding interne de carte. Le `6.dp` hors grille de `DashCard` disparaît au
profit de `spaceSm`.

## `ui/theme/SemanticColors.kt` — nouveau (P2)

Formaliser le langage couleur **sans introduire de nouvelle teinte** : dériver
de `Steel`/`Clay`/`Moss` un triplet sémantique unique (nominal / vigilance /
charge haute) et l'utiliser partout où un état est signifié —
`ReadinessAction`, ACWR, écarts aux objectifs, `SoftAlertTone`. Aujourd'hui
`RecoveryReadinessCard` mappe ses couleurs localement ; ce mapping remonte ici.

Conserver `workoutTypeColor` tel quel : il fonctionne et est déjà cohérent.

## Critère de sortie

Aucun `.dp` littéral ni `typography.*` non défini dans `ui/`, hors
`ui/theme/`. Vérifiable mécaniquement.

---

# LOT 2 — `Scaffold` par écran, `TopAppBar`, et correction du bug de retour

Corrige D1 et **D2, qui est un bug, pas une préférence esthétique**.

## À faire

- Créer `ui/components/AppScaffold.kt` : un `Scaffold` par écran, avec
  `TopAppBar` (titre, navigation de retour optionnelle, actions optionnelles)
  et son propre `SnackbarHostState`.
- Chaque écran adopte `AppScaffold` et **retire son titre de la `LazyColumn`**.
  Le titre vit dans la barre, avec le comportement de collapse au scroll de
  material3 1.4.
- **Ajouter une flèche de retour aux quatre destinations poussées** :
  `TrainingPlanEditScreen`, `MmaSessionScreen`, `ImportTrainingPlanScreen`,
  `CalorieGoalScreen`. Le `RootNav` leur passe un `onBack = { navController.popBackStack() }`.
- Décider explicitement du sort de la `NavigationBar` sur ces quatre écrans :
  soit la masquer (ce sont des sous-écrans modaux), soit la garder — mais pas
  la laisser affichée par accident comme aujourd'hui. Recommandation : la
  masquer, c'est ce que fait le pattern « détail » de Material.
- L'action Actualiser du dashboard (`DashboardScreen.kt:93`) remonte dans les
  actions de la `TopAppBar` ; la salutation reste dans le contenu.
- Les `SnackbarHost` posés à la main (`WorkoutLogScreen.kt:430`, idem
  `MealLogScreen`, `WeighInScreen`) passent par le `Scaffold`.

## Critère de sortie

Depuis chaque écran de l'app, un retour est **visible** sans utiliser le geste
système. Le titre de l'écran reste lisible après un scroll complet.

---

# LOT 3 — Ramener la navigation à 5 onglets

Corrige D3.

`Pesée` est l'onglet à sortir : c'est une saisie ponctuelle (une fois par
matin, ~15 secondes), pas une section. Elle est déjà accessible par le FAB du
dashboard (`RootNav.kt:96`) et par un rappel de notification
(`WeighInReminder`). Elle devient une destination poussée avec retour, atteinte
depuis le dashboard et depuis Progression.

Onglets finaux : **Accueil · Séance · Repas · Progression · Réglages**.
`"Progr."` redevient `"Progression"` — l'abréviation n'a plus de raison d'être.

Si une 6ᵉ section devait revenir un jour, la réponse est un menu, pas un
onglet de plus.

## Critère de sortie

Cinq onglets, aucun libellé tronqué, aucune ellipse. Logger une pesée depuis
l'accueil coûte au plus un tap de plus qu'avant (à compter, cf. P5).

---

# LOT 4 — Un seul système de cartes

Corrige D9 et prépare D4.

## À faire

- Créer `ui/components/AppCard.kt`, unique conteneur de carte de l'app,
  construit sur `Card`/`ElevatedCard` de material3 1.4 — pas sur un `Column`
  avec `.background()`.
- Trois variantes, et pas plus : `Standard` (le cas courant),
  `Hero` (la métrique dominante du lot 5), `Accent` (bannières et états).
- Régler le problème de séparation en sombre : `SurfaceDark` sur `PaperDark`
  ne suffit pas. Utiliser l'élévation de material3, et/ou une bordure fine à
  faible opacité. `surfaceContainerHigh` est déjà câblé dans `Theme.kt` et
  n'est utilisé nulle part — c'est le moment de s'en servir ou de le retirer.
- Migrer vers `AppCard` : `DashCard`, `RecoveryReadinessCard`, `StreakBadge`,
  `RelativeStrengthCard`, `VolumeDistributionCard`, `NextWorkoutCard`,
  `AskClaudeCard`, `WearablesCard`, `SessionShareCard`.
- Supprimer `DashCard` de `DashboardScreen.kt`.

Le commentaire de `Theme.kt` qui explique pourquoi `DashCard` n'utilise **pas**
`PaperLightAlt`/`PaperDarkAlt` doit être mis à jour ou retiré : il documente
une décision qui ne survit pas à ce lot.

## Critère de sortie

Zéro `.background(` sur une carte hors de `AppCard.kt`. Une carte se distingue
du fond en clair **et** en sombre, vérifié sur téléphone.

---

# LOT 5 — Refonte du dashboard : une hiérarchie, pas une pile

Le lot central. Corrige D4, D5, D6, D10 en appliquant P1 et P3.

## 5a — Supprimer le faux et le mort (D6) — à faire en premier

- **Retirer `WearablesCard` du dashboard.** Elle affiche `"7h 30m"` et
  `"52 bpm"` en dur sous un libellé `"Mock data (demo)"`. Soit le composant est
  supprimé, soit il part dans Réglages derrière un état « non connecté »
  honnête. Il ne reste pas sur l'écran d'accueil à mentir.
- **Retirer `AskClaudeCard`** tant que `mockSummary` est fabriqué en dur avec
  un `"Charge max: 80kg"` inventé (`DashboardScreen.kt:158`). Le prompt coach
  existe (`coach_prompt.md`) — cette carte reviendra quand elle sera branchée
  dessus, pas avant.
- **`NextWorkoutCard` : soit câbler `onStartClick`** sur la navigation vers
  `workout` avec préremplissage depuis `suggestedExercise`, **soit retirer le
  bouton.** Un bouton primaire pleine largeur qui ne fait rien est le pire des
  deux mondes.

## 5b — Dédupliquer (D5)

- `avgCaloriesLast7Days` n'apparaît **qu'une fois**, dans la carte Nutrition.
  La ligne de la carte « Cette semaine » saute.
- Trancher entre `currentStreak`/`bestStreak` (`streakManager`) et
  `activityStreakDays`. **Un seul compteur de série sur l'écran.** Si les deux
  calculs ont un sens distinct, le second doit être renommé et expliqué ; s'ils
  mesurent la même chose, en supprimer un. Recommandation : garder
  `activityStreakDays`, cohérent avec la règle 3 (positif uniquement, cf.
  `brief_coach_autoregulation.md`), et retirer `StreakBadge`.

## 5c — Établir la hiérarchie (P1)

Trois niveaux, dans cet ordre à l'écran :

**Niveau 1 — la grande chose.** Une seule carte `Hero`, en haut, sous la
`TopAppBar` : la **force relative** (`RelativeStrengthCard`), rendue au style
`metricHero` du lot 1. C'est l'indicateur directeur du projet ; il doit être le
plus gros objet de l'app. Aujourd'hui il est 4ᵉ et de la même taille que le
reste.

**Niveau 2 — l'état du jour et l'action.** `RecoveryReadinessCard` (avec sa
pastille sémantique du lot 1) et la prochaine séance. Ce sont les deux choses
sur lesquelles on agit dans la minute.

**Niveau 3 — le reste, compacté.** Semaine, séances, tendance poids, nutrition.
Ces quatre cartes passent en résumé court — **une ligne de valeur lisible par
carte, pas quatre lignes de `bodySmall` gris** (D10) — et renvoient vers
Progression pour le détail (P3). La tendance poids reste en moyenne 7 jours
(règle 1), en niveau 3 et non en niveau 1 : le poids brut n'est pas l'objectif.

## 5d — Sortir les accordéons (D12, P3)

« Voir le programme de la semaine » (`DashboardScreen.kt:262`) devient une
destination `plan/week` avec sa `TopAppBar` et son retour, au lieu d'injecter
une liste non virtualisée dans un `item`. Même traitement pour l'historique
des séances au lot 6.

## Critère de sortie

Le dashboard passe de 14 blocs à **6 au plus** en premier écran. Un utilisateur
à qui on montre l'écran une seconde doit pouvoir dire quelle est la métrique
principale. Aucune donnée factice à l'écran.

---

# LOT 6 — Écran de séance : conçu pour être utilisé en salle

Corrige D11 et D12 sur le parcours le plus utilisé de l'app, en appliquant P4.

## À faire

- **Ancrer « Enregistrer la séance » en bas**, dans le `bottomBar` du
  `AppScaffold` ou une `BottomAppBar` persistante. Il ne défile plus
  (`WorkoutLogScreen.kt:402`). Zone du pouce, 48dp minimum, haptique à la
  validation — le pattern haptique existe déjà (`WorkoutLogScreen.kt:408`),
  il est juste au mauvais endroit.
- Réordonner : le formulaire de séance (date, type, durée, chips) est de la
  configuration, pas du logging. Il se replie en en-tête compact une fois
  renseigné ; les exercices et leurs séries occupent l'écran.
- Les cibles de `ExerciseRow` et `SetRow` — saisie reps/charge/RIR entre deux
  séries, doigts moites — passent à 48dp minimum. À vérifier composant par
  composant, c'est le cœur du principe P4.
- La rangée de 4 `IconButton` par exercice (monter, descendre, dupliquer,
  supprimer — `WorkoutLogScreen.kt:278-307`) est dense et sans libellé. La
  réduire aux actions fréquentes, déplacer le reste dans un menu de dépassement.
- L'historique (`WorkoutLogScreen.kt:334`) devient une destination, comme le
  programme au lot 5d.
- Vérifier que `RestTimer` est visible sans scroll pendant le repos — c'est le
  moment exact où l'écran est regardé.

## Critère de sortie

Compter les actions (P5) pour « logger une série sur un exercice existant »
avant et après. Le nombre doit baisser. Enregistrer une séance de 6 exercices
ne demande **aucun scroll** après la dernière saisie.

---

# LOT 7 — Aligner Repas, Pesée et Progression

Mêmes principes, sur les écrans restants. Pas de nouvelle idée ici : de la
cohérence.

- `MealLogScreen` (712 lignes, le plus gros fichier de l'app) : action primaire
  ancrée en bas, cibles à 48dp, `CalorieProgressRing` remonté en tête comme
  métrique dominante de l'écran (P1). Compter les actions pour « logger un
  aliment » (P5) — c'est le parcours le plus répété de l'app après le logging
  de série.
- `WeighInScreen` : devient une destination poussée (lot 3), avec retour.
  Saisie ultra-courte, une seule action, clavier numérique déjà en place.
- `ProgressScreen` : devient le niveau 3 de la divulgation progressive (P3) —
  la destination où atterrissent les renvois du dashboard. C'est ici que le
  détail a le droit d'être dense.
- `SettingsScreen` : regrouper en sections avec en-têtes ; c'est aujourd'hui
  une liste plate de 334 lignes.
- `AuthScreen` : `headlineMedium` y est utilisé sans être défini (D7) — vérifier
  le rendu après le lot 1.

---

# LOT 8 — Mouvement et haptique (optionnel, dépend d'une alpha)

**À ne faire que si les lots 0 à 7 sont livrés et stables.**

material3 1.4 apporte déjà le mouvement à ressorts : remplacer les
`tween(150)`/`tween(200)` de `RootNav.kt:155` et `WorkoutLogScreen.kt:423` par
les spécifications à ressorts, et généraliser l'haptique aux validations
(elle n'est aujourd'hui que sur la suppression et la sauvegarde de séance).

Le reste — `FloatingActionButtonMenu` à la place de la colonne d'`ExtendedFAB`
bricolée dans `RootNav.kt:92-118`, `FloatingToolbar`, `ButtonGroup` — exige
material3 **1.5.0-alpha19+**. C'est un vrai remplacement pour le FAB actuel,
mais passer une app personnelle qui tourne sur une alpha est un choix à faire
en connaissance de cause, pas par défaut.

---

# ORDRE, ET CE QUI SE FAIT EN PREMIER SI ON NE FAIT QU'UNE CHOSE

L'ordre 0 → 8 est contraignant : chaque lot s'appuie sur le précédent.

Si le temps manque, le sous-ensemble qui apporte le plus est
**Lot 0 → Lot 2 → Lot 5a**, dans cet ordre :

1. **Lot 2** corrige un bug — quatre écrans sans retour visible. Ce n'est pas
   du confort, c'est une impasse de navigation.
2. **Lot 5a** retire de l'écran d'accueil des données inventées (`7h 30m`,
   `52 bpm`, `Charge max: 80kg`) et un bouton primaire mort. Une app qui affiche
   du faux à son propriétaire perd la confiance qu'on met dans le reste de ses
   chiffres — y compris ceux qui sont justes.

Le reste est une refonte visuelle. Ces deux-là sont des corrections.
