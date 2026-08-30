# BRIEF — Passe "Nutrition autorégulée" (5 lots)

Repo : `android-mma` — module `android-mma-recomp/`
Branche : `claude/nutrition-autoregulation-ospdzy`. Commit + push à chaque lot.

Suite de `brief_coach_autoregulation.md` (entraînement). Sources et réserves
méthodologiques dans `coach_prompt_sources.md`.

## ÉTAT DES LIEUX — ce qui existe déjà et ne doit PAS être refait

`CalorieCalculator` est solide et ne demande pas de refonte :
- recalibrage adaptatif façon MacroFactor (dépense réelle déduite de la
  tendance de poids lissée, pas d'une formule figée)
- multiplicateur d'activité calibré pour un pratiquant de combat (1,4–1,6),
  pas pour un sédentaire
- offsets bornés (coupe −250, bulk +400), plancher de sécurité, avertissements
- protéines assises sur la masse maigre, pas sur le poids total

Les lots ci-dessous comblent des manques, ils ne remplacent pas ce calcul.

## OBJECTIF DE L'UTILISATEUR — cadre toutes les décisions produit

Force relative et densité (archétype lutteur), pratique MMA en parallèle. Le
poids est une variable à CONTRAINDRE pendant que la force monte, jamais une
quantité à maximiser ni à minimiser. L'indicateur directeur reste
`1RM estimé / poids de corps` (moyenne mobile 7 jours).

## RÈGLES MÉTIER — non négociables

1. Le poids ne s'affiche QUE via `MovingAverage.sevenDay()`.
2. Aucune culpabilisation sur un déficit, un repas ou une journée. Alertes via
   `SoftAlertBanner`, ton neutre. Voir `softUnderTargetAlert` pour le registre.
3. Séries de jours consécutifs positives uniquement.
4. Palette Material3 existante, espacements via `Dimens`.
5. Français dans toute l'UI et les KDoc.

---

# LOT 1 — Disponibilité énergétique (EA)

`CalorieCalculator.MINIMUM_CALORIES_PER_KG = 25` est un plancher sur le POIDS
DE CORPS. Ce n'est pas la métrique de référence en sport de combat, et elle
ne voit pas le cas dangereux : à 75 kg / 12 % BF, elle autorise 1 875 kcal ;
avec 800 kcal dépensés à l'entraînement, l'EA réelle tombe à ~16 kcal/kg de
masse maigre, soit la moitié du seuil admis.

## Nouveau : `util/EnergyAvailability.kt`

```
EA = (apport kcal − dépense de l'exercice kcal) / masse maigre kg
```

Seuils : **≥ 30 kcal/kg FFM** correct ; 25–30 zone de vigilance ; < 25 faible
disponibilité énergétique.

La dépense d'exercice se dérive du session-RPE déjà loggué (cf. `TrainingLoad`,
lot entraînement) : les deux moitiés du calcul existent déjà dans l'app mais ne
se parlent pas. Documenter l'estimation comme telle — ce n'est pas une mesure.

Ajouter le contrôle EA en sortie de `goalFromMaintenance`, en complément du
plancher existant (ne pas le supprimer : c'est un filet indépendant).

## Critères d'acceptation
- Une journée à fort volume d'entraînement fait baisser l'EA, à apport égal.
- L'alerte EA basse est neutre et propose d'ajouter des glucides, jamais de
  « mieux manger ».

---

# LOT 2 — Périodisation réelle des glucides

`NutritionTargetCalculator.targetFor()` fait `baseCalories ± 150` selon
`TypeJour`, protéines constantes. Deux problèmes :
- rien n'impose que le swing vienne des GLUCIDES (principe « fuel for the work
  required ») ;
- `NutritionTarget` ne persiste ni glucides ni lipides — la cible du jour ne
  transporte donc pas l'information.

## Modèle
Ajouter `glucidesCibleG` et `lipidesCibleG` à `NutritionTarget` et
`NewNutritionTarget`. Migration SQL `supabase/007_nutrition_target_macros.sql`
avec colonnes nullables (l'historique n'en a pas).

## Calcul
- Fourchette pour un athlète de force : **4–7 g/kg/jour** de glucides.
- Le swing training/repos porte sur les glucides, lipides et protéines
  constants. ±150 kcal est timide : viser plutôt ±80 à 100 g de glucides entre
  un jour de sparring lourd et un jour de repos.
- Moduler par la charge réelle du jour (`TrainingLoad.chargeSeance`) plutôt que
  par le seul booléen training/repos.

## Critères d'acceptation
- Les macros de la cible somment aux calories de la cible (±2 %).
- Un jour de repos ne descend jamais sous le plancher glucidique du lot 5.

---

# LOT 3 — Distribution des protéines par prise

La synthèse protéique est réfractaire : une fois le signal saturé, la leucine
supplémentaire n'apporte rien. D'où **20–30 g de protéines de qualité par
prise** (~2–3 g de leucine), en **3–4 impulsions** réparties, plutôt qu'une
grosse prise.

## Bug à corriger
`NutritionTargetCalculator.indicativeSplit()` répartit les protéines au prorata
de `RepasSlot.shareIndicatif`, qui est une part CALORIQUE (0,25 / 0,30 / 0,20 /
0,25). L'après-midi se voit donc attribuer 20 % des protéines et le
post-training 30 %. Les protéines doivent être réparties **à plat** entre les
prises ; seules les calories et les glucides suivent la part calorique.

## UI
Sur `MealLogScreen`, indiquer par prise si l'apport protéique atteint le seuil
utile (~20–30 g), en note factuelle. Jamais en rouge, jamais en échec.

## Critères d'acceptation
- Protéines réparties uniformément, calories et glucides au prorata.
- Test unitaire sur la somme et l'uniformité.

---

# LOT 4 — Confiance du suivi et correction du sous-report

`CalorieCalculator.adaptiveRecalibration()` déduit la dépense réelle de
`avgLoggedCalories`. Un sous-report la fausse d'autant — et le sous-report est
le mode d'échec NORMAL, pas l'exception : erreur moyenne de 30–50 % chez les
non-formés, ~15 % chez des diététiciens entraînés.

## Nouveau : `util/LoggingConfidence.kt`
- Complétude = prises loguées / prises attendues sur la fenêtre.
- **Ne recalibrer que si complétude ≥ 67 %** — ce seuil capture environ 90 % du
  bénéfice du suivi, et sous ce niveau l'estimation n'a plus de sens.
- Exposer un niveau de confiance (élevée / moyenne / insuffisante) affiché à
  côté du recalibrage, plutôt qu'un nombre à fausse précision.

## Ton
Le suivi alimentaire est le meilleur prédicteur comportemental de résultat :
l'objectif n'est pas de logger parfaitement mais de savoir quand le calcul
mérite confiance. Une complétude basse est un fait affiché, jamais un reproche.

## Critères d'acceptation
- Sous 67 % de complétude, `adaptiveRecalibration` ne propose rien.
- Aucun message de complétude ne contient de jugement.

---

# LOT 5 — Planchers macro et restriction intermittente

La position ISSN 2025 sur les sports de combat donne des planchers explicites
en descente de poids, qu'aucun calcul de l'app n'applique aujourd'hui.

## Nouveau : `util/MacroFloors.kt`

| Macro | Plancher (g/kg de poids de corps) |
|---|---|
| Glucides | 3,0–4,0 |
| Protéines | 1,2–2,0 |
| Lipides | 0,5–1,0 |

Plus : perte de **0,5–1 kg/semaine** maximum, **4–6 prises**/jour, protéines
**1,2–2,4 g/kg** avec ~2 g/kg comme cible.

Vérifier au passage `PROTEIN_G_PER_KG_LEAN_MASS = 2.0` : il porte sur la MASSE
MAIGRE, donc ~1,76 g/kg de poids total à 12 % de BF. Dans la fourchette, mais
plus bas que la cible affichée — à assumer explicitement en commentaire plutôt
que par accident.

## Diet break
`CalorieMode` est figé (Bulk / Recomposition / Coupe). Ajouter la notion de
pause planifiée : la littérature recomposition penche pour une restriction
**intermittente et progressive** plutôt qu'un déficit continu agressif, qui
dégrade sommeil, hormones et performance en salle.

Proposer un retour à maintenance après N semaines consécutives en déficit.
Proposition, jamais imposition.

## Critères d'acceptation
- Une cible qui violerait un plancher est relevée, et la raison est affichée.
- La suggestion de diet break se base sur les cibles réellement enregistrées.

# WORKFLOW

Un commit par lot, message en français. Tests unitaires sur
`EnergyAvailability`, `MacroFloors`, `LoggingConfidence` et la répartition
protéique. Compilation vérifiée avant chaque push.
