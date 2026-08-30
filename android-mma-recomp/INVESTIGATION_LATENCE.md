# Investigation Latence Scroll — Lot 15/14 Aftermath

**Date**: Aug 26, 2026  
**Branche**: `claude/30-latence-investigation-ospdzy`  
**Symptôme**: Latence globale au scroll tous les écrans (DashboardScreen, MealLogScreen, etc.) depuis Lot 15 merge

## Résultats de l'enquête

### Lot 15 Changements (Theme.kt, StatusBadge.kt)
- **Alpha values**: PrimaryContainerDark (0.20→0.14), SecondaryContainerDark (0.20→0.14)
- **Commentaires**: Ajout de documentations WCAG contrast audit (ne devrait PAS causer recomposition Compose)
- **Impact**: Très peu probable. Les alpha values seules ne causent pas de latence de scroll continue.

### Lot 14 Changements (Form field consistency)
Fichiers modifiés qui utilisent LazyColumn ou scroll :
- **MealLogScreen.kt**: `LazyColumn` avec `itemsIndexed` + `items`, modification `imeAction` sur cibles custom
- **WorkoutLogScreen.kt** (via ExerciseRow.kt): `LazyColumn` dans MmaSessionScreen/WorkoutLogScreen, ajout `keyboardActions`
- **AuthScreen.kt**: Changements spacing seulement (pas de scroll dans ce screen)

### Suspects potentiels

1. **LocalFocusManager + keyboardActions en Lot 14**
   - Fichiers: ExerciseRow.kt, MealLogScreen.kt
   - Problème: Si le focus manager capture change pendant scroll, recomposition cascade possible
   - Probabilité: Faible-Moyenne (focus ne devrait pas trigger pendant scroll passif)

2. **ColorScheme recomposition via Theme.kt**
   - Probablement importé par 95% des composables
   - Changement alpha force recalc de tous les composables importants Theme
   - **Mitigation**: Les alpha values seules ne devraient pas causer lag, mais un rebuild Compose cache corruption pourrait amplifier l'effet
   - Probabilité: Moyenne (cache corruption possible)

3. **LazyColumn sans keys ou inefficaces**
   - MealLogScreen: `itemsIndexed(meals)` — devrait avoir keys basées sur `meal.id`
   - WorkoutLogScreen/MmaSessionScreen: Vérifier si exercices utilisent des keys stables
   - Probabilité: Élevée si les items recomposent pendant scroll (lazy layout redraw)

### Diagnostic Recommandé (nécessite Android Studio Layout Inspector)
```
1. Launch app with Layout Inspector
2. Navigate to scrolling screen (MealLogScreen or Dashboard)
3. Watch Compose recomposition graph during scroll
4. Look for:
   - High recomposition count on items (>10 per scroll event)
   - State hoisting issues (mutableState in wrong scope)
   - Lambdas created inline causing recompositions
```

## Court terme: Fix sans rebuild

**Hypothèse**: Le problème est probablement un **cache Gradle/Compose corrompu** après Lot 15, ou une **recomposition excessive** liée aux LazyColumn keys.

**Actions rapides** :
- [ ] Ajouter keys explicites aux LazyColumn items dans MealLogScreen (basé sur `meal.id`)
- [ ] Vérifier WorkoutLogScreen LazyColumn keys
- [ ] Extraire lambdas en functions stables si détectées

## Prochaines phases

Après fix court-terme, passer à **Phase 1–15 Engagement & Intelligence Pass**.

---

Investigators: Claude Code  
Status: Ongoing (awaiting user rebuild feedback)

---

## Suite — correctif réel des clés LazyColumn

Le « quick fix » initial de ce lot ajoutait `key = { index, _ -> index }` sur
`itemsIndexed(mealItems)`. C'était un **no-op** : l'index est déjà la clé par
défaut quand aucune n'est fournie. Ce changement ne pouvait donc rien améliorer,
ni en latence ni en correction.

Le vrai défaut était ailleurs : `MealFoodItem` n'avait aucune identité stable.
Avec des clés par index, supprimer un aliment au milieu de la liste décale tous
les suivants, et Compose réutilise les mauvaises lignes. Corrigé en donnant un
`id` stable à chaque ligne, généré à l'ajout.

À noter : ce défaut est un problème de **correction**, pas de performance. La
latence de scroll rapportée à l'origine n'a jamais été reproduite ni mesurée
ici — l'hypothèse la plus probable reste un cache Compose corrompu, résolu par
un `clean build`. Ne pas traiter ce correctif comme une explication de la
latence.
