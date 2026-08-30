# Lot 1 — Intégrité métier Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Corriger les calculs, fenêtres temporelles, imports et exports qui peuvent produire une recommandation ou une donnée erronée.

**Architecture:** Les règles sont extraites dans des fonctions Kotlin pures testées par JUnit. Les ViewModels orchestrent ces fonctions sans dupliquer de calcul. Les libellés scientifiques décrivent une incertitude et ne déclenchent une modulation qu’avec un historique suffisant.

**Tech Stack:** Kotlin 2.0.21, JUnit 4, Jetpack Compose, supabase-kt 3.0.0.

## Global Constraints

- Partir de `origin/master` au commit `7de2f911` ou ultérieur.
- Conserver `1 = très difficile` et `5 = facile` pour les données MMA existantes.
- Ne jamais afficher une pesée brute comme résultat de tendance.
- Aucun diagnostic médical ni conseil autonome de coupe de poids.
- Chaque changement comportemental suit RED → GREEN → REFACTOR.
- `CancellationException` ne doit jamais être absorbée.

---

### Task 1: Bootstrap de test strict

**Files:**
- Create: `gradlew`
- Create: `gradlew.bat`
- Create: `gradle/wrapper/gradle-wrapper.jar`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Modify: `Makefile`

**Interfaces:**
- Produces: `./gradlew`, `make lint`, `make test`, `make check`

- [ ] **Step 1: Générer un wrapper Gradle compatible AGP 8.5**

Run:

```bash
gradle wrapper --gradle-version 8.7 --distribution-type bin
```

Expected: les quatre fichiers wrapper sont créés et `./gradlew --version`
affiche Gradle 8.7.

- [ ] **Step 2: Rendre le Makefile strict**

Remplacer le contenu par :

```make
.PHONY: bootstrap lint test check

bootstrap:
	@test -f app/src/main/java/com/example/mmarecomp/SupabaseConfig.kt || \
		cp app/src/main/java/com/example/mmarecomp/SupabaseConfig.kt.example \
		   app/src/main/java/com/example/mmarecomp/SupabaseConfig.kt

lint: bootstrap
	./gradlew lintDebug

test: bootstrap
	./gradlew testDebugUnitTest

check: lint test
```

- [ ] **Step 3: Exécuter la baseline**

Run: `make test`

Expected: suite existante verte. Si la résolution Android est bloquée par le
réseau, conserver le journal exact et utiliser le compilateur Kotlin uniquement
comme diagnostic complémentaire, jamais comme preuve de build Android.

- [ ] **Step 4: Commit**

```bash
git add Makefile gradlew gradlew.bat gradle/wrapper
git commit -m "build: ajouter un bootstrap Gradle reproductible"
```

### Task 2: Normaliser l’intensité MMA

**Files:**
- Modify: `app/src/main/java/com/example/mmarecomp/util/TrainingLoad.kt`
- Modify: `app/src/main/java/com/example/mmarecomp/util/InterferenceChecker.kt`
- Test: `app/src/test/java/com/example/mmarecomp/util/TrainingLoadTest.kt`
- Test: `app/src/test/java/com/example/mmarecomp/util/IntegriteAppTest.kt`

**Interfaces:**
- Produces: `TrainingLoad.intensiteMma(ressenti: Int): Int?`
- Consumes: `MmaSession.ressenti` avec domaine 1..5

- [ ] **Step 1: Écrire les tests échouants**

Ajouter :

```kotlin
@Test
fun `une seance mma tres difficile charge plus qu une seance facile`() {
    assertTrue(TrainingLoad.intensiteMma(1)!! > TrainingLoad.intensiteMma(5)!!)
}

@Test
fun `le ressenti mma hors echelle est refuse`() {
    assertNull(TrainingLoad.intensiteMma(0))
    assertNull(TrainingLoad.intensiteMma(6))
}
```

Ajouter un test d’interférence où une séance MMA de ressenti 1, adjacente à un
bas du corps lourd, produit un conflit, et où un ressenti 5 n’en produit pas.

- [ ] **Step 2: Vérifier RED**

Run:

```bash
./gradlew testDebugUnitTest --tests '*TrainingLoadTest*' --tests '*IntegriteAppTest*'
```

Expected: FAIL, `intensiteMma` n’existe pas et le cas intense actuel est
impossible.

- [ ] **Step 3: Implémenter la conversion unique**

```kotlin
fun intensiteMma(ressenti: Int): Int? =
    ressenti.takeIf { it in 1..5 }?.let { (6 - it) * 2 }
```

`chargeSeance(MmaSession)` multiplie cette intensité par la durée par défaut.
`InterferenceChecker` appelle cette fonction et compare au seuil RPE 7.

- [ ] **Step 4: Vérifier GREEN**

Run: commande de l’étape 2.

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/mmarecomp/util/TrainingLoad.kt \
  app/src/main/java/com/example/mmarecomp/util/InterferenceChecker.kt \
  app/src/test/java/com/example/mmarecomp/util/TrainingLoadTest.kt \
  app/src/test/java/com/example/mmarecomp/util/IntegriteAppTest.kt
git commit -m "fix: corriger l'échelle de charge MMA"
```

### Task 3: Préserver la cible calorique

**Files:**
- Modify: `app/src/main/java/com/example/mmarecomp/util/NutritionTargetCalculator.kt`
- Modify: `app/src/main/java/com/example/mmarecomp/viewmodel/CalorieGoalViewModel.kt`
- Test: `app/src/test/java/com/example/mmarecomp/util/NutritionTest.kt`

**Interfaces:**
- Consumes: `baseCalories`, protéines, lipides, poids et charge du jour
- Produces: `DailyTarget` dont l’énergie reste cohérente avec le mode choisi

- [ ] **Step 1: Écrire les tests échouants**

Ajouter trois tests pour 75 kg :

```kotlin
assertTrue(bulk.calories > recomp.calories)
assertTrue(recomp.calories > cut.calories)
assertEquals(baseCalories.toDouble(), result.calories.toDouble(), 20.0)
```

Ajouter un test d’intégration qui fournit 14 jours de fenêtre mais moins de
67 % de prises et attend l’absence de recalibrage.

- [ ] **Step 2: Vérifier RED**

Run: `./gradlew testDebugUnitTest --tests '*NutritionTest*'`

Expected: FAIL car `targetFor` reconstruit actuellement une énergie différente
de `baseCalories`.

- [ ] **Step 3: Implémenter**

Calculer le solde glucidique :

```kotlin
val glucidesParCalories =
    ((baseCalories - proteinesG * 4 - lipidesG * 9) / 4.0)
        .coerceAtLeast(0.0)
        .roundToInt()
```

Appliquer les planchers macro ensuite et exposer toute hausse de calories dans
`macroCorrections`. Dans `CalorieGoalViewModel`, borner les repas à la date du
premier point de tendance jusqu’au dernier, puis passer explicitement
`LoggingConfidence.evaluer(meals, periodDays).completude`.

- [ ] **Step 4: Vérifier GREEN**

Run: commande de l’étape 2.

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/mmarecomp/util/NutritionTargetCalculator.kt \
  app/src/main/java/com/example/mmarecomp/viewmodel/CalorieGoalViewModel.kt \
  app/src/test/java/com/example/mmarecomp/util/NutritionTest.kt
git commit -m "fix: préserver la cible énergétique des modes"
```

### Task 4: Fiabiliser charge, RIR, APRE et HRV

**Files:**
- Modify: `app/src/main/java/com/example/mmarecomp/util/TrainingLoad.kt`
- Modify: `app/src/main/java/com/example/mmarecomp/util/RirCalibration.kt`
- Modify: `app/src/main/java/com/example/mmarecomp/util/RirTargets.kt`
- Modify: `app/src/main/java/com/example/mmarecomp/util/ApreEngine.kt`
- Modify: `app/src/main/java/com/example/mmarecomp/viewmodel/DashboardViewModel.kt`
- Test: `app/src/test/java/com/example/mmarecomp/util/TrainingLoadTest.kt`
- Test: `app/src/test/java/com/example/mmarecomp/util/ApreEngineTest.kt`
- Test: `app/src/test/java/com/example/mmarecomp/util/IntegriteAppTest.kt`

**Interfaces:**
- Produces: indicateurs n’utilisant que des données fraîches et suffisantes

- [ ] **Step 1: Écrire les tests échouants**

Cas obligatoires :

```kotlin
// ACWR indisponible avec moins de 21 jours de couverture.
assertNull(TrainingLoad.acwr(singleSessionLoads, today))

// Pas de HRV du jour : aucun écart calculé.
assertNull(TrainingLoad.ecartHrvEnSigma(historyEndingYesterday, today))

// Toute série RIR 0 produit une note.
assertNotNull(RirTargets.note(exerciseWithRirs(0, 4)))

// Sous-performance à 1 kg : la charge suivante ne monte pas.
assertTrue(nextLoad <= 1.0)
```

Le test de calibration vérifie qu’une marge surestimée diminue la correction,
et qu’une marge sous-estimée l’augmente.

- [ ] **Step 2: Vérifier RED**

Run:

```bash
./gradlew testDebugUnitTest \
  --tests '*TrainingLoadTest*' --tests '*ApreEngineTest*' --tests '*IntegriteAppTest*'
```

Expected: FAIL sur chaque comportement ci-dessus.

- [ ] **Step 3: Implémenter par petites fonctions**

- Ajouter la date attendue à `ecartHrvEnSigma`.
- Exiger une couverture chronique minimale avant l’indicateur de variation.
- Vérifier `rir == 0` avant toute moyenne.
- Corriger le signe de calibration et stocker les six dernières mesures plutôt
  qu’une moyenne exponentielle présentée comme une fenêtre.
- Autoriser zéro changement APRE après sous-performance ; valider que
  l’incrément est strictement positif.
- Passer seulement les séances des sept derniers jours à `VolumeLandmarks`.

- [ ] **Step 4: Vérifier GREEN**

Run: commande de l’étape 2.

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/mmarecomp/util \
  app/src/main/java/com/example/mmarecomp/viewmodel/DashboardViewModel.kt \
  app/src/test/java/com/example/mmarecomp/util
git commit -m "fix: fiabiliser les indicateurs de récupération"
```

### Task 5: Charger la phase avant le programme

**Files:**
- Modify: `app/src/main/java/com/example/mmarecomp/ui/RootScreen.kt`
- Create: `app/src/main/java/com/example/mmarecomp/viewmodel/SessionProfileViewModel.kt`
- Test: `app/src/test/java/com/example/mmarecomp/viewmodel/SessionProfileViewModelTest.kt`

**Interfaces:**
- Produces: état de phase `Loading | Ready(Phase) | Error`
- Consumes: une fonction injectée `suspend (String) -> Profile`

- [ ] **Step 1: Écrire un test échouant avec fake repository**

Le fake retourne `Phase.CurriculumMma`. Vérifier que la destination principale
ne reçoit jamais `Phase.Ete` avant la fin du chargement.

- [ ] **Step 2: Vérifier RED**

Run: `./gradlew testDebugUnitTest --tests '*SessionProfileViewModelTest*'`

Expected: FAIL car `RootScreen` initialise immédiatement `Phase.Ete`.

- [ ] **Step 3: Implémenter**

Créer `SessionProfileViewModel` avec une fonction de chargement injectée et un
état scellé. L’instance de production délègue à `ProfileRepository.fetch`.
Créer un état de session applicatif qui charge le profil après authentification.
Afficher un chargement tant que la phase n’est pas connue. Une erreur expose
une action de nouvelle tentative ; elle ne sélectionne pas silencieusement une
phase.

- [ ] **Step 4: Vérifier GREEN**

Run: commande de l’étape 2.

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/mmarecomp/ui/RootScreen.kt \
  app/src/main/java/com/example/mmarecomp/viewmodel/SessionProfileViewModel.kt \
  app/src/test/java/com/example/mmarecomp/viewmodel/SessionProfileViewModelTest.kt
git commit -m "fix: charger la phase active du profil"
```

### Task 6: Rendre l’import explicite et robuste

**Files:**
- Modify: `app/src/main/java/com/example/mmarecomp/util/TrainingPlanParser.kt`
- Modify: `app/src/main/java/com/example/mmarecomp/viewmodel/ImportTrainingPlanViewModel.kt`
- Test: `app/src/test/java/com/example/mmarecomp/util/TrainingPlanParserTest.kt`

**Interfaces:**
- Produces: `PlanParseResult(days, ignoredLines)`

- [ ] **Step 1: Écrire les tests échouants**

Vérifier :

```kotlin
parse("Lundi\nSquat 3x5 @82,5kg")
```

produit une charge de 82,5 kg ; un nouveau jour avec exercices n’est pas
`Repos` ; une ligne non reconnue apparaît dans `ignoredLines`.

- [ ] **Step 2: Vérifier RED**

Run: `./gradlew testDebugUnitTest --tests '*TrainingPlanParserTest*'`

Expected: FAIL car la virgule est utilisée comme séparateur global et les
lignes ignorées ne sont pas retournées.

- [ ] **Step 3: Implémenter**

Ne découper une ligne sur virgule que si elle n’est pas située entre chiffres.
Retourner un résultat structuré avec lignes ignorées. Pour un nouveau jour avec
au moins un exercice, choisir un type d’entraînement explicite et éditable,
jamais `Repos`.

- [ ] **Step 4: Vérifier GREEN et commit**

Run: commande de l’étape 2.

```bash
git add app/src/main/java/com/example/mmarecomp/util/TrainingPlanParser.kt \
  app/src/main/java/com/example/mmarecomp/viewmodel/ImportTrainingPlanViewModel.kt \
  app/src/test/java/com/example/mmarecomp/util/TrainingPlanParserTest.kt
git commit -m "fix: sécuriser l'import des programmes"
```

### Task 7: Produire un export CSV sûr et complet

**Files:**
- Modify: `app/src/main/java/com/example/mmarecomp/util/CsvExport.kt`
- Test: `app/src/test/java/com/example/mmarecomp/util/CsvExportTest.kt`

**Interfaces:**
- Produces: `CsvExport.workouts(workouts: List<Workout>): String`
- Produces: encodeur RFC 4180 interne commun

- [ ] **Step 1: Écrire les tests échouants**

Tester guillemets, virgule, CR/LF, notes commençant par `=`, exercices et séries
avec charge, répétitions, RIR, AMRAP et sangles. Vérifier que chaque cellule
dangereuse est quotée et qu’un préfixe de formule est neutralisé.

- [ ] **Step 2: Vérifier RED**

Run: `./gradlew testDebugUnitTest --tests '*CsvExportTest*'`

Expected: FAIL car l’export courant remplace les caractères et omet les séries.

- [ ] **Step 3: Implémenter**

Créer un encodeur qui double `"` et entoure toute cellule de `"`. Préfixer les
cellules commençant par `=`, `+`, `-` ou `@` avec une apostrophe. Exporter une
ligne par série avec identifiants de séance et exercice.

- [ ] **Step 4: Vérifier GREEN et commit**

Run: commande de l’étape 2.

```bash
git add app/src/main/java/com/example/mmarecomp/util/CsvExport.kt \
  app/src/test/java/com/example/mmarecomp/util/CsvExportTest.kt
git commit -m "fix: sécuriser et compléter les exports CSV"
```

### Task 8: Reformuler les limites scientifiques

**Files:**
- Modify: `app/src/main/java/com/example/mmarecomp/util/TrainingLoad.kt`
- Modify: `app/src/main/java/com/example/mmarecomp/util/EnergyAvailability.kt`
- Modify: `app/src/main/java/com/example/mmarecomp/util/CalorieCalculator.kt`
- Modify: `app/src/main/java/com/example/mmarecomp/util/RelativeStrength.kt`
- Modify: `README.md`
- Create: `docs/references_metier.md`
- Test: `app/src/test/java/com/example/mmarecomp/util/SafetyCopyTest.kt`

**Interfaces:**
- Produces: textes d’aide non diagnostiques et sources vérifiables

- [ ] **Step 1: Écrire un test échouant**

Le test vérifie que les textes publics ne contiennent plus les affirmations
absolues « risque minimal », « jamais un repos complet » ou « ce n’est plus la
poigne qui limite ».

- [ ] **Step 2: Vérifier RED**

Run: `./gradlew testDebugUnitTest --tests '*SafetyCopyTest*'`

Expected: FAIL sur les formulations actuelles.

- [ ] **Step 3: Reformuler et documenter**

Présenter ACWR et disponibilité énergétique comme indicateurs contextuels.
Ajouter un chemin explicite d’arrêt pour douleur aiguë, maladie ou commotion
suspectée. Documenter DOI/URLs et date de consultation sans faire passer une
source marketing pour une preuve scientifique.

- [ ] **Step 4: Vérifier GREEN**

Run: commande de l’étape 2.

Expected: PASS.

- [ ] **Step 5: Vérification globale et commit**

Run: `make check`

Expected: lint et tests verts.

```bash
git add app/src/main/java/com/example/mmarecomp/util README.md \
  docs/references_metier.md app/src/test/java/com/example/mmarecomp/util/SafetyCopyTest.kt
git commit -m "docs: encadrer les indicateurs de santé"
```
