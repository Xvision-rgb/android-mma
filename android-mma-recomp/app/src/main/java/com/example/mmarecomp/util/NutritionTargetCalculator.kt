package com.example.mmarecomp.util

import com.example.mmarecomp.model.RepasSlot
import com.example.mmarecomp.model.TypeJour
import kotlin.math.roundToInt

data class DailyTarget(val calories: Int, val proteinesG: Double)
data class SlotTarget(val calories: Int, val proteinesG: Double)

object NutritionTargetCalculator {
    private const val TRAINING_REST_SPREAD = 150

    /** Cible du jour selon calorie cycling — valeurs de repli génériques,
     *  utilisées uniquement tant qu'aucune pesée n'existe encore pour
     *  calculer une cible personnalisée (cf. targetFor / CalorieCalculator).
     *  Ne pas s'y fier pour un pratiquant de sport de combat : ces chiffres
     *  sous-estiment largement sa dépense réelle. */
    fun target(typeJour: TypeJour): DailyTarget = when (typeJour) {
        TypeJour.Training -> DailyTarget(2050, 135.0) // milieu de 2000-2100 kcal / 130-140g
        TypeJour.Repos -> DailyTarget(1800, 130.0)
    }

    /** Cycle training/repos autour d'une cible personnalisée (calculée à
     *  partir du poids réel et du mode choisi via CalorieCalculator) plutôt
     *  que des valeurs génériques figées. */
    fun targetFor(typeJour: TypeJour, baseCalories: Int, proteinesG: Int): DailyTarget = when (typeJour) {
        TypeJour.Training -> DailyTarget(baseCalories + TRAINING_REST_SPREAD, proteinesG.toDouble())
        TypeJour.Repos -> DailyTarget(baseCalories - TRAINING_REST_SPREAD, proteinesG.toDouble())
    }

    /** Répartition indicative (non bloquante) de la cible du jour sur les
     *  créneaux repas — un repas qui déborde n'est jamais signalé tant que
     *  le total du jour reste dans la cible. */
    fun indicativeSplit(calories: Int, proteinesG: Double, slots: List<RepasSlot>): Map<RepasSlot, SlotTarget> =
        slots.associateWith { slot ->
            SlotTarget(
                calories = (calories * slot.shareIndicatif).toInt(),
                proteinesG = (proteinesG * slot.shareIndicatif * 10).roundToInt() / 10.0,
            )
        }

    /** Alerte douce : plusieurs jours d'affilée nettement en dessous de la
     *  cible calorique. Ne culpabilise jamais un jour isolé en dessous de
     *  l'objectif — il faut trois jours consécutifs sous 85% de la cible. */
    fun softUnderTargetAlert(recentDailyTotals: List<Triple<String, Int, Int>>): Boolean {
        val lastThree = recentDailyTotals.takeLast(3)
        if (lastThree.size != 3) return false
        return lastThree.all { (_, calories, cible) -> calories < (cible * 0.85).toInt() }
    }
}
