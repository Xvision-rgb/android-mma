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

    private const val PROTEIN_G_PER_KG_LEAN_MASS = 2.0
    private const val FAT_SHARE_OF_CALORIES = 0.275 // milieu de la plage recommandée 25-30%
    private const val FALLBACK_LEAN_MASS_SHARE = 0.85 // si le %BF n'est pas encore connu

    /** Plancher absolu, quel que soit le mode — un pratiquant de sport de
     *  combat qui s'entraîne intensément ne doit jamais descendre en dessous,
     *  même en coupe. Filet de sécurité indépendant des bornes par mode
     *  ci-dessus (qui restent déjà largement au-dessus en pratique). */
    private const val MINIMUM_CALORIES_PER_KG = 25

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
    ): CalorieGoal {
        val maintenance = maintenanceCalories(poidsKg, activityMultiplier)
        val offset = offsetForMode(mode)
        val minimumCalories = (poidsKg * MINIMUM_CALORIES_PER_KG).roundToInt()
        val target = (maintenance + offset).coerceAtLeast(minimumCalories)
        val leanMass = leanMassKg(poidsKg, bfPct)
        val proteinesG = (leanMass * PROTEIN_G_PER_KG_LEAN_MASS).roundToInt()
        val lipidesG = (target * FAT_SHARE_OF_CALORIES / 9.0).roundToInt()
        val glucidesG = ((target - proteinesG * 4 - lipidesG * 9) / 4.0).coerceAtLeast(0.0).roundToInt()
        return CalorieGoal(
            mode = mode,
            maintenanceCalories = maintenance,
            targetCalories = target,
            offsetCalories = offset,
            proteinesG = proteinesG,
            glucidesG = glucidesG,
            lipidesG = lipidesG,
            warning = warningFor(offset),
        )
    }
}
