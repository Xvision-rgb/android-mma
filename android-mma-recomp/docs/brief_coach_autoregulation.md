# BRIEF — Passe "Coach autorégulé" (Lot 0 + 5 lots)

Repo : `android-mma` — module `android-mma-recomp/`
Stack : Kotlin + Jetpack Compose (Material3), MVVM, Supabase (Postgres + RLS),
SharedPreferences pour le local.
Branche : `claude/coach-autoregulation-ospdzy`. Commit + push à chaque lot.

Base de preuves et sources : voir `coach_prompt_sources.md`.
Prompt de coaching conversationnel associé : voir `coach_prompt.md`.

## OBJECTIF DE L'UTILISATEUR — cadre toutes les décisions produit

Force relative et densité musculaire — archétype lutteur. Dos, trapèzes, cou et
avant-bras épais ; chaîne postérieure lourde ; poitrine et bras non
prioritaires. Pratique MMA en parallèle. Contrainte connue : **la poigne est le
facteur limitant du tirage vertical**.

COROLLAIRE PRODUIT : l'app ne doit JAMAIS présenter la prise de masse comme un
progrès en soi. L'indicateur directeur est **1RM estimé / poids corporel**.
Toute UI qui met le poids brut en avant comme objectif est à corriger.

## RÈGLES MÉTIER EXISTANTES — non négociables, à préserver dans chaque lot

1. Le poids ne s'affiche QUE via `MovingAverage.sevenDay()`. Jamais de pesée
   brute présentée comme un résultat. (cf. `WeightTrendChart`)
2. Aucune culpabilisation sur un déficit calorique, un repas ou une séance
   manquée. Les alertes passent par `SoftAlertBanner`, ton neutre.
3. Les séries de jours consécutifs sont positives uniquement : on compte ce qui
   est là, jamais de message de "série brisée". (cf. `activityStreakDays`)
4. Palette Material3 existante uniquement (`ui/theme/Color.kt`). Espacements via
   `Dimens`. Pas de valeurs codées en dur.
5. Français dans toute l'UI. KDoc en français, cohérent avec l'existant.

---

# LOT 0 — Fondation : logging par série (BLOQUANT)

`LoggedExercise` agrège `series: Int` / `reps: Int`. L'APRE et le seuil de chute
de performance ont besoin de la donnée série par série. C'est le prérequis des
lots 1 et 3.

## Modèle

Dans `model/Exercise.kt`, ajouter :

```kotlin
@Serializable
data class LoggedSet(
    val index: Int,                                   // 1-based
    val reps: Int,
    @SerialName("charge_kg") val chargeKg: Double,
    val rir: Int? = null,                             // reps in reserve estimées
    val sangles: Boolean = false,                     // straps utilisées
    /** La série s'est arrêtée sur la poigne, pas sur le muscle cible.
     *  Empêche l'APRE de baisser la charge à tort (cf. Lot 1). */
    @SerialName("limite_poigne") val limitePoigne: Boolean = false,
    @SerialName("est_amrap") val estAmrap: Boolean = false,
)
```

Étendre `LoggedExercise` avec `val sets: List<LoggedSet> = emptyList()`.

MIGRATION — rétrocompatibilité obligatoire, la colonne `exercices` est du JSONB
et contient de l'historique : si `sets` est vide, dériver à la lecture depuis
`series` / `repsReelles` / `chargeReelleKg`. Ne casse aucune séance existante.
Ne supprime PAS les champs agrégés — les garder en lecture seule dérivée.

Remplacer `suggestionProgression` (heuristique `+2.5kg` codée en dur) par un
appel au moteur APRE du Lot 1. Le supprimer une fois le Lot 1 en place.

## UI

`ui/workout/` — la saisie de séance passe d'une ligne par exercice à une ligne
par série : reps, charge, RIR (stepper 0-5), toggle sangles, toggle
"poigne limitante". Une série marquée AMRAP est mise en évidence.

La saisie doit rester rapide : pré-remplir chaque série avec les valeurs de la
précédente. Ne pas transformer le log en formulaire de 20 champs.

## Critères d'acceptation
- Une séance historique (sans `sets`) s'ouvre et s'affiche sans crash.
- Une nouvelle séance enregistre et relit correctement 4 séries distinctes.
- Test unitaire sur la dérivation rétrocompatible.

---

# LOT 1 — Moteur APRE (autorégulation de la charge)

Fondement : network meta-analysis 2025 — APRE arrive premier pour le gain de
1RM (SUCRA 93,0 %) devant RPE (66,8 %), VBT (27,0 %) et %1RM (13,2 %).

## Nouveau : `util/ApreEngine.kt`

```kotlin
enum class ApreProtocol(val label: String, val repsCible: Int) {
    APRE_3("Force max", 3),
    APRE_6("Force base", 6),
    APRE_10("Hypertrophie", 10),
}

data class ApreProchaineSeance(
    val chargeKg: Double,
    val deltaKg: Double,
    val justification: String,   // affiché tel quel à l'utilisateur
)
```

Logique : à partir de la série AMRAP (`estAmrap = true`, sinon la dernière série
travaillée), comparer les reps réalisées aux reps cibles du protocole.

| Écart aux reps cibles | Ajustement charge |
|---|---|
| ≤ −4 | −5 à −7 % |
| −1 à −3 | −2,5 % |
| 0 | inchangée |
| +1 à +3 | +2,5 % |
| ≥ +4 | +5 % |

RÈGLE CRITIQUE : si la série AMRAP a `limitePoigne = true`, **ne pas baisser la
charge**. La série a échoué sur la poigne, pas sur le muscle cible. Retourner la
charge inchangée avec la justification « série limitée par la poigne — charge
maintenue, sangles recommandées ».

Arrondir à l'incrément disponible (paramètre réglable dans Settings, défaut
2,5 kg). Ne jamais retourner une charge non chargeable.

## UI
Sur l'écran de séance, chaque exercice affiche la charge prescrite calculée par
le moteur, avec la justification en une ligne. L'utilisateur peut la surcharger
manuellement — l'override est loggé et le moteur repart de la charge réelle.

## Critères d'acceptation
- Tests unitaires couvrant les 5 branches de la table + le cas `limitePoigne`.
- Arrondi correct pour des incréments de 1, 2,5 et 5 kg.

---

# LOT 2 — RIR différencié par type d'exercice + calibration

Fondement : méta-régression 2024 — les gains de FORCE sont plats sur une large
plage de RIR, alors que l'hypertrophie s'améliore près de l'échec. Comme
l'objectif est la force relative et non la masse maximale, le modèle est
**sous-maximal haute fréquence** (référence Prilepin : à 70-80 % 1RM, 3-6 reps
par série, 15-24 reps totales par exercice).

## Nouveau : `util/RirTargets.kt`

Cibles par catégorie d'exercice :
- Polyarticulaire : **2-3 RIR**. Jamais l'échec.
- Isolation : **1-2 RIR**. Jamais 0.

La catégorie est dérivée de `MuscleZoneClassifier` (Lot 5).

## Calibration du biais personnel

L'estimation RIR est significativement moins précise chez les non-experts, avec
surestimation systématique de la distance à l'échec.

- Toutes les 2 semaines, l'app propose UNE série d'isolation à l'échec réel.
- L'utilisateur saisit son RIR estimé avant, le nombre de reps réel après.
- Stocker le biais moyen glissant dans SharedPreferences
  (`util/RirCalibration.kt`).
- Appliquer le biais dans le moteur APRE : un « 3 RIR » déclaré avec un biais de
  +2 est traité comme 5 RIR réels.

Ton de la proposition de calibration : optionnelle, jamais insistante, jamais
répétée si refusée deux fois d'affilée.

## Critères d'acceptation
- Un exercice hors cible RIR est signalé en note neutre, pas en alerte rouge.
- Le biais s'applique effectivement à la sortie du moteur APRE.

---

# LOT 3 — Seuil d'arrêt de série (proxy velocity loss)

Fondement : méta-analyse 2022 sur les seuils de perte de vitesse. VL ≤ 20-25 %
supérieur pour la force et la performance neuromusculaire ; VL > 25 % supérieur
pour l'hypertrophie. Avec du MMA en parallèle, la fatigue neuromusculaire
économisée en salle est celle qui reste disponible au sparring.

Pas d'encodeur linéaire disponible → deux proxys, aucun SDK externe.

## Proxy A — chute de reps inter-séries
Dans `util/SetStopAdvisor.kt` : si les reps d'une série chutent de plus de 20 %
par rapport à la première série travaillée à charge égale, signaler que la série
suivante peut être coupée. Signal informatif, jamais bloquant.

## Proxy B — chronomètre de série
Bouton chrono optionnel pendant la série. Si la durée par rep de la dernière rep
dépasse ~2× celle de la première, afficher un indicateur « série terminée ».

Seuil différencié selon `WorkoutType` : les types `*Force` utilisent le seuil
strict (20 %), les types `*Hypertrophie` le seuil permissif (25-30 %).

## Critères d'acceptation
- Aucune interruption forcée : l'app suggère, l'utilisateur décide.
- Le proxy B est entièrement optionnel et n'alourdit pas la saisie par défaut.

---

# LOT 4 — Readiness quotidien et modulation de la séance

C'est le maillon manquant entre « je logue » et « je m'ajuste ».
`RecoveryReadinessCard` existe déjà mais tourne sur des données mockées — le
remplacer par du réel.

## Nouveau modèle : `DailyCheckIn`

```kotlin
@Serializable
data class DailyCheckIn(
    val id: String,
    @SerialName("user_id") val userId: String,
    val date: String,
    val sommeil: Int,      // 1-5
    val courbatures: Int,  // 1-5
    val fatigue: Int,      // 1-5
    val humeur: Int,       // 1-5
    val stress: Int,       // 1-5
    @SerialName("hrv_rmssd") val hrvRmssd: Double? = null,
    @SerialName("dead_hang_sec") val deadHangSec: Int? = null,  // cf. Lot 5
) { val score: Int get() = sommeil + courbatures + fatigue + humeur + stress }
```

Migration SQL dans `supabase/006_daily_checkin.sql`, RLS alignée sur les tables
existantes. Repository `data/DailyCheckInRepository.kt`.

## Charge interne : session-RPE et ACWR

Ajouter `rpe: Int?` (1-10) sur `Workout` et `NewWorkout`. Réutiliser
`MmaSession.ressenti` comme RPE pour les séances MMA.

Dans `util/TrainingLoad.kt` :
- charge de séance = `rpe × dureeMin`
- ACWR = charge 7 jours / moyenne 28 jours
- Cible **0,8-1,3**. **> 1,5 = risque de blessure nettement élevé.**

## Matrice de modulation — module le VOLUME, pas la charge

| Condition | Action |
|---|---|
| Score ≥ 20 et ACWR 0,8-1,3 | Séance nominale |
| Score 15-19 ou HRV < −0,5 SD de la moyenne 7j | −20/25 % volume (couper le dernier set des accessoires), charges inchangées |
| Score < 15 ou ACWR > 1,5 | −40 % volume, charges −10 %, RIR +2 |
| 3 jours consécutifs en rouge | Deload réactif : 1 semaine à ~50 % du volume, charges maintenues |

RÈGLE ABSOLUE : ne jamais proposer un repos complet en réponse à une mauvaise
journée. La dose minimale maintient l'adaptation. Formuler comme un ajustement,
jamais comme une sanction.

Une seule valeur HRV basse ne déclenche rien — comparer à la moyenne mobile
7 jours, cohérent avec la règle métier n°1.

## Critères d'acceptation
- Le check-in du matin prend moins de 20 secondes (5 sliders, un bouton).
- `RecoveryReadinessCard` consomme des données réelles, plus de mock.
- L'ACWR est calculé sur 28 jours glissants, robuste aux jours manquants.

---

# LOT 5 — Répartition du volume, cou/poigne, anti-interférence

## A. Classification par zone musculaire

`model/MuscleZone.kt` :

```kotlin
enum class MuscleZone(val label: String, val partCible: Double) {
    TIRAGE("Tirage / dos", 0.35),
    CHAINE_POSTERIEURE("Chaîne postérieure", 0.25),
    COU_POIGNE("Cou / poigne", 0.15),
    POUSSEE("Poussée", 0.15),
    QUADS_BRAS("Quadriceps / bras", 0.10),
}
```

`util/MuscleZoneClassifier.kt` : classification par mots-clés sur
`LoggedExercise.nom` (les noms sont des chaînes libres), avec override manuel
persistant par nom d'exercice. Couvrir au minimum : rowing, traction, tirage,
soulevé de terre, hip thrust, squat, zercher, développé, dips, curl, dead hang,
farmer, cou, pont.

## B. Carte "Répartition du volume"

Nouvelle carte dashboard : part réelle du volume hebdomadaire par zone vs cible,
et le **ratio tirage:poussée** (cible ≈ 2:1).

C'est l'écart le plus probable entre la pratique actuelle et l'objectif.
L'afficher factuellement — un chiffre et sa cible, pas de jugement.
Réutiliser le style de `TargetVsActualBar`.

## C. Suivi cou et poigne comme qualités primaires

Chez les lutteurs d'élite, la préhension corrèle fortement avec la force du haut
du corps et du cou (repère : 42-83 kg au dynamomètre selon la catégorie). La
force isométrique est identifiée comme facteur de succès en lutte.

- **Dead hang** : métrique suivie de première classe (`DailyCheckIn.deadHangSec`),
  courbe de progression dans `ProgressScreen`.
  Seuils de lecture : < 30 s déficit franc / 30-60 s normal / > 60 s ce n'est
  plus la poigne le facteur limitant.
- **Spécificité du type de poigne** : les tractions échouent sur le SUPPORT grip
  (maintien isométrique), que les grippers (CRUSH grip) n'entraînent pas.
  L'app ne doit jamais suggérer de grippers pour ce problème — uniquement
  suspensions, farmer's walks, suspensions lestées.
- **Placement** : le travail de poigne est prescrit en FIN de séance. Si un
  exercice de zone `COU_POIGNE` est programmé avant un exercice `TIRAGE`,
  le signaler.
- Fréquence cible cou : 3×/semaine, isométries 20-45 s.

## D. Détection de conflit MMA

`util/InterferenceChecker.kt`, croisant `MmaSession` et `Workout` :
- Alerte si bas du corps lourd < 24 h d'une séance MMA intense
  (`ressenti` élevé).
- Alerte si musculation et cardio/MMA à moins de 6 h d'intervalle.
- En intra-séance, la force passe avant le cardio.

Alertes via `SoftAlertBanner`, ton neutre, jamais bloquantes.

## E. Indicateur directeur : force relative

Dans `ProgressViewModel` : pour chaque mouvement principal,
**1RM estimé (Epley : `charge × (1 + reps/30)`) / poids corporel en moyenne
mobile 7 jours**.

C'est LE chiffre de progression de l'app. Le mettre au-dessus du poids dans la
hiérarchie visuelle du dashboard. Un gain de poids sans gain de ratio n'est pas
un progrès ; une perte de poids à ratio croissant en est un. Ne jamais commenter
le poids seul.

## Critères d'acceptation
- Le ratio tirage:poussée se calcule sur les 7 derniers jours loggés.
- La classification tombe juste sur les exercices réels de l'historique.
- Aucune suggestion de grippers nulle part dans le code.

---

# NETTOYAGE — dette de la passe précédente

`ui/components/MockCardsLots6to15.kt` regroupe plusieurs composants mockés dans
un seul fichier. Éclater en fichiers par composant et supprimer ceux que les
lots ci-dessus remplacent par du réel (`RecoveryReadinessCard` en particulier).

`WearablesCard` reste un skeleton assumé — `DailyCheckIn.hrvRmssd` est saisi à
la main pour l'instant. Ne pas intégrer de SDK santé dans cette passe.

# WORKFLOW

Un commit par lot, message en français décrivant le quoi et le pourquoi.
Push après chaque lot. Les lots 1 et 3 dépendent du Lot 0 — le faire en premier.
Tests unitaires obligatoires sur `ApreEngine`, `TrainingLoad`,
`MuscleZoneClassifier`. Compilation vérifiée avant chaque push.
