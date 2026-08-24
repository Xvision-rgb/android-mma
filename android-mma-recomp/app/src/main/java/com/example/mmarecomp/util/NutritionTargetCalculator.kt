package com.example.mmarecomp.util

import com.example.mmarecomp.model.Phase
import com.example.mmarecomp.model.RepasSlot
import com.example.mmarecomp.model.TypeJour
import kotlin.math.roundToInt

data class DailyTarget(val calories: Int, val proteinesG: Double)
data class SlotTarget(val calories: Int, val proteinesG: Double)
data class MacroGrams(val proteinesG: Double, val glucidesG: Double, val lipidesG: Double)

object NutritionTargetCalculator {
    /** Cible du jour selon calorie cycling : plus haut les jours training,
     *  plus bas les jours off, protéines maintenues hautes dans les deux cas.
     *  `phase` est optionnel (préférence "Cible nutrition liée à la Phase") :
     *  en curriculum MMA, les besoins caloriques/protéiques sont un peu plus
     *  élevés qu'en phase été, jamais l'inverse (jamais moins que la base). */
    fun target(typeJour: TypeJour, phase: Phase? = null): DailyTarget {
        val base = when (typeJour) {
            TypeJour.Training -> DailyTarget(2050, 135.0) // milieu de 2000-2100 kcal / 130-140g
            TypeJour.Repos -> DailyTarget(1800, 130.0)
        }
        return if (phase == Phase.CurriculumMma) {
            DailyTarget(base.calories + 100, base.proteinesG + 5.0)
        } else {
            base
        }
    }

    /** Répartition indicative des calories cible en grammes de macros selon
     *  une % personnalisée — purement informatif, n'affecte jamais la cible
     *  protéines réellement suivie par TargetVsActualBar. */
    fun macroGramsFromPercent(calories: Int, proteinPct: Int, carbsPct: Int, fatPct: Int): MacroGrams {
        val proteinesG = (calories * proteinPct / 100.0) / 4.0
        val glucidesG = (calories * carbsPct / 100.0) / 4.0
        val lipidesG = (calories * fatPct / 100.0) / 9.0
        return MacroGrams(proteinesG, glucidesG, lipidesG)
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
