package com.example.mmarecomp.util

import com.example.mmarecomp.model.RepasSlot
import com.example.mmarecomp.model.TypeJour
import kotlin.math.roundToInt

data class DailyTarget(val calories: Int, val proteinesG: Double)
data class SlotTarget(val calories: Int, val proteinesG: Double)

object NutritionTargetCalculator {
    /** Cible du jour selon calorie cycling : plus haut les jours training,
     *  plus bas les jours off, protéines maintenues hautes dans les deux cas. */
    fun target(typeJour: TypeJour): DailyTarget = when (typeJour) {
        TypeJour.Training -> DailyTarget(2050, 135.0) // milieu de 2000-2100 kcal / 130-140g
        TypeJour.Repos -> DailyTarget(1800, 130.0)
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
}
