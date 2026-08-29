package com.example.mmarecomp.util

import com.example.mmarecomp.model.CalorieMode
import kotlin.math.roundToInt

data class CalorieGoal(
    val mode: CalorieMode,
    val maintenanceCalories: Int,
    val targetCalories: Int,
    val offsetCalories: Int,
    val proteinesG: Int,
    val glucidesG: Int,
    val lipidesG: Int,
    val warning: String?,
    /** Planchers macro qui ont dû relever la cible, avec leur raison.
     *  Affiché tel quel pour que l'utilisateur voie que le calcul a bougé. */
    val macroCorrections: List<String> = emptyList(),
)

/** Résultat d'un recalibrage adaptatif — cf. CalorieCalculator.adaptiveRecalibration. */
data class AdaptiveRecalibration(
    val estimatedExpenditureCalories: Int,
    val staticMaintenanceCalories: Int,
    val differenceCalories: Int,
    val periodDays: Int,
)

/**
 * Formule calorique pensée pour un pratiquant de sport de combat (force +
 * hypertrophie + cardio combat plusieurs fois par semaine) plutôt qu'une
 * formule générique type Mifflin-St Jeor pensée population sédentaire, qui
 * sous-estimerait largement la dépense réelle d'un lutteur qui s'entraîne
 * 6-7x/semaine — c'était la cause du déficit bien trop agressif (~-600 cal)
 * calculé auparavant.
 */
object CalorieCalculator {
    const val ACTIVITY_MULTIPLIER_LOW = 1.4
    const val ACTIVITY_MULTIPLIER_HIGH = 1.6
    const val ACTIVITY_MULTIPLIER_DEFAULT = 1.5

    private const val BULK_OFFSET = 400 // milieu de la plage recommandée 300-500
    private const val COUPE_OFFSET = 250 // milieu de la plage recommandée 200-300, jamais -600

    private const val DEFICIT_WARNING_THRESHOLD = 400
    private const val SURPLUS_WARNING_THRESHOLD = 500
    private const val HIGH_BF_COUPE_THRESHOLD = 18.0

    /** Attention à la base : ce ratio porte sur la MASSE MAIGRE, pas sur le
     *  poids total. À 12 % de masse grasse, 2,0 g/kg de masse maigre valent
     *  ~1,76 g/kg de poids de corps — dans la fourchette recommandée
     *  (1,2–2,4 g/kg) mais sous la cible usuelle de ~2 g/kg de poids total.
     *  Choix assumé : l'assiette maigre est la base la plus défendable quand
     *  le %BF est connu. Voir MacroFloors.PROTEINES_CIBLE_G_PAR_KG pour la
     *  cible exprimée en poids de corps. */
    private const val PROTEIN_G_PER_KG_LEAN_MASS = 2.0
    private const val FAT_SHARE_OF_CALORIES = 0.275 // milieu de la plage recommandée 25-30%
    private const val FALLBACK_LEAN_MASS_SHARE = 0.85 // si le %BF n'est pas encore connu

    /** Plancher absolu, quel que soit le mode — un pratiquant de sport de
     *  combat qui s'entraîne intensément ne doit jamais descendre en dessous,
     *  même en coupe. Filet de sécurité indépendant des bornes par mode
     *  ci-dessus (qui restent déjà largement au-dessus en pratique). */
    private const val MINIMUM_CALORIES_PER_KG = 25

    /** 1kg de masse corporelle ≈ 7700 kcal (équivalence énergétique standard,
     *  approximative pour un mélange masse grasse/maigre — suffisant pour un
     *  recalibrage indicatif, pas une mesure de laboratoire). */
    private const val KCAL_PER_KG_BODY_MASS = 7700.0
    private const val RECALIBRATION_MIN_DAYS = 14

    /** Écart minimum entre dépense réelle estimée et maintenance statique
     *  pour proposer un recalibrage — sous ce seuil, la différence est dans
     *  la marge de bruit normale d'une estimation par tendance de poids
     *  (milieu de la plage 300-400 kcal évoquée pour ce type de recalibrage). */
    private const val RECALIBRATION_DIFFERENCE_THRESHOLD = 350

    fun maintenanceCalories(poidsKg: Double, activityMultiplier: Double = ACTIVITY_MULTIPLIER_DEFAULT): Int =
        (poidsKg * 30 * activityMultiplier).roundToInt()

    fun offsetForMode(mode: CalorieMode): Int = when (mode) {
        CalorieMode.Bulk -> BULK_OFFSET
        CalorieMode.Recomposition -> 0
        CalorieMode.Coupe -> -COUPE_OFFSET
    }

    /** Masse maigre estimée à partir du poids et du %BF — sert de base au
     *  calcul protéique, faute d'une vraie mesure (DEXA, impédancemétrie pro). */
    fun leanMassKg(poidsKg: Double, bfPct: Double?): Double =
        bfPct?.let { poidsKg * (1 - it / 100.0) } ?: (poidsKg * FALLBACK_LEAN_MASS_SHARE)

    /** La recomposition reste la recommandation par défaut pour un pratiquant
     *  de sport de combat qui veut rester sec — une vraie coupe n'est
     *  suggérée qu'au-delà d'un %BF où un déficit devient plus pertinent que
     *  la lenteur (mais la sûreté musculaire) de la recomposition. */
    fun recommendedMode(bfPct: Double?): CalorieMode = when {
        bfPct != null && bfPct >= HIGH_BF_COUPE_THRESHOLD -> CalorieMode.Coupe
        else -> CalorieMode.Recomposition
    }

    fun warningFor(offsetCalories: Int): String? = when {
        offsetCalories <= -DEFICIT_WARNING_THRESHOLD ->
            "⚠️ Risque de perte musculaire — déficit de ${-offsetCalories} cal, garde les protéines hautes."
        offsetCalories >= SURPLUS_WARNING_THRESHOLD ->
            "⚠️ Risque de prise de gras — surplus de $offsetCalories cal, surveille le %BF chaque semaine."
        else -> null
    }

    /** Cible complète (calories + macros) pour un mode donné, à partir du
     *  poids réel et du %BF le plus récent (peut être null si pas encore
     *  mesuré — la masse maigre retombe alors sur une estimation prudente). */
    fun goal(
        poidsKg: Double,
        bfPct: Double?,
        mode: CalorieMode,
        activityMultiplier: Double = ACTIVITY_MULTIPLIER_DEFAULT,
    ): CalorieGoal = goalFromMaintenance(maintenanceCalories(poidsKg, activityMultiplier), poidsKg, bfPct, mode)

    /** Même calcul que goal(), mais à partir d'une maintenance déjà connue
     *  (ex. dépense réelle estimée par adaptiveRecalibration) plutôt que
     *  recalculée depuis la formule poids × 30 × multiplicateur — sert à
     *  appliquer un recalibrage adaptatif tout en gardant la même logique
     *  d'offsets/macros/plancher que goal(). */
    fun goalFromMaintenance(
        maintenance: Int,
        poidsKg: Double,
        bfPct: Double?,
        mode: CalorieMode,
    ): CalorieGoal {
        val offset = offsetForMode(mode)
        val minimumCalories = (poidsKg * MINIMUM_CALORIES_PER_KG).roundToInt()
        val target = (maintenance + offset).coerceAtLeast(minimumCalories)
        val leanMass = leanMassKg(poidsKg, bfPct)
        val proteinesBruts = (leanMass * PROTEIN_G_PER_KG_LEAN_MASS).roundToInt()
        val lipidesBruts = (target * FAT_SHARE_OF_CALORIES / 9.0).roundToInt()
        val glucidesBruts = ((target - proteinesBruts * 4 - lipidesBruts * 9) / 4.0)
            .coerceAtLeast(0.0).roundToInt()

        // Une cible calorique atteignable peut rester mal composée : un
        // déficit tenu en coupant les glucides sabote l'entraînement bien
        // avant de faire perdre du gras.
        val ajustees = MacroFloors.appliquer(poidsKg, glucidesBruts, proteinesBruts, lipidesBruts)

        return CalorieGoal(
            mode = mode,
            maintenanceCalories = maintenance,
            targetCalories = maxOf(target, ajustees.caloriesTotales),
            offsetCalories = offset,
            proteinesG = ajustees.proteinesG,
            glucidesG = ajustees.glucidesG,
            lipidesG = ajustees.lipidesG,
            warning = warningFor(offset),
            macroCorrections = ajustees.corrections,
        )
    }

    /** Recalibrage adaptatif façon MacroFactor : plutôt qu'une formule figée
     *  seule, compare la dépense réelle déduite de la tendance de poids
     *  observée aux calories réellement loguées sur la période.
     *
     *  Bilan énergétique : variation de poids (kg) sur la période ≈
     *  (calories loguées - dépense réelle) × jours / 7700. En isolant la
     *  dépense réelle : dépense réelle = calories loguées moyennes -
     *  (variation de poids × 7700 / nombre de jours).
     *
     *  Retourne null si moins de 14 jours de données — pas de recalibrage
     *  fiable sur une fenêtre trop courte (le bruit des pesées domine).
     *  Le poids doit venir d'une tendance déjà lissée (moyenne mobile
     *  7 jours, jamais une pesée brute) côté appelant. */
    fun adaptiveRecalibration(
        weightChangeKg: Double,
        periodDays: Int,
        avgLoggedCalories: Double,
        staticMaintenanceCalories: Int,
        /** Complétude du suivi sur la période (cf. LoggingConfidence).
         *  Le calcul déduit la dépense des calories LOGUÉES : sous-reporter
         *  fausse l'estimation d'autant, et le sous-report est le mode
         *  d'échec normal du suivi alimentaire, pas l'exception. En dessous
         *  du seuil, on ne recalibre pas — on mesurerait surtout les repas
         *  manquants. */
        completudeSuivi: Double = 1.0,
    ): AdaptiveRecalibration? {
        if (periodDays < RECALIBRATION_MIN_DAYS) return null
        if (completudeSuivi < LoggingConfidence.SEUIL_RECALIBRAGE) return null
        val dailyChangeCalories = weightChangeKg * KCAL_PER_KG_BODY_MASS / periodDays
        val estimatedExpenditure = (avgLoggedCalories - dailyChangeCalories).roundToInt()
        return AdaptiveRecalibration(
            estimatedExpenditureCalories = estimatedExpenditure,
            staticMaintenanceCalories = staticMaintenanceCalories,
            differenceCalories = estimatedExpenditure - staticMaintenanceCalories,
            periodDays = periodDays,
        )
    }

    /** Écart jugé assez significatif pour proposer le recalibrage à
     *  l'utilisateur plutôt que de le garder sous silence (bruit normal). */
    fun isRecalibrationSignificant(recalibration: AdaptiveRecalibration): Boolean =
        kotlin.math.abs(recalibration.differenceCalories) >= RECALIBRATION_DIFFERENCE_THRESHOLD
}
