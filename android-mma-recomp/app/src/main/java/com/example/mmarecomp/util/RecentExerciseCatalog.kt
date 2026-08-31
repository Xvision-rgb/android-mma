package com.example.mmarecomp.util

import com.example.mmarecomp.model.LoggedExercise
import com.example.mmarecomp.model.LoggedSet
import com.example.mmarecomp.model.Workout
import com.example.mmarecomp.model.withSets

/** Exercice issu de l'historique, prêt à être ajouté ou à remplacer un mouvement du plan. */
data class RecentExerciseEntry(
    val nom: String,
    val derniereChargeKg: Double?,
    val nbSeries: Int,
    val template: LoggedExercise,
    val derniereDate: String,
)

object RecentExerciseCatalog {

    fun fromWorkouts(workouts: List<Workout>, limit: Int = 14): List<RecentExerciseEntry> {
        val seen = linkedMapOf<String, Pair<String, LoggedExercise>>()
        for (workout in workouts.sortedByDescending { it.date }) {
            for (exercice in workout.exercices) {
                if (exercice.nom.isBlank()) continue
                val key = ExerciseName.cle(exercice.nom)
                if (!seen.containsKey(key)) {
                    seen[key] = workout.date to exercice
                }
            }
        }
        return seen.values.take(limit).map { (date, exercice) ->
            RecentExerciseEntry(
                nom = ExerciseName.propre(exercice.nom),
                derniereChargeKg = exercice.chargeMaxKg,
                nbSeries = exercice.effectiveSets.size.coerceAtLeast(1),
                template = exercice,
                derniereDate = date,
            )
        }
    }

    /** Remplace le nom et les charges tout en gardant le nombre de séries du plan. */
    fun replaceKeepingStructure(current: LoggedExercise, template: LoggedExercise): LoggedExercise {
        val targetCount = current.effectiveSets.size.coerceAtLeast(1)
        val templateSets = template.effectiveSets
        if (templateSets.isEmpty()) {
            return current.copy(
                nom = ExerciseName.propre(template.nom),
                chargeCibleKg = template.chargeCibleKg ?: template.chargeMaxKg,
            )
        }
        val newSets = (0 until targetCount).map { index ->
            val source = templateSets.getOrElse(index) { templateSets.last() }
            source.copy(
                index = index + 1,
                estAmrap = index == targetCount - 1,
            )
        }
        return current.copy(
            nom = ExerciseName.propre(template.nom),
            chargeCibleKg = template.chargeCibleKg ?: template.chargeMaxKg,
        ).withSets(newSets)
    }

    fun copyForAdd(template: LoggedExercise): LoggedExercise =
        template.copy(
            nom = ExerciseName.propre(template.nom),
            sets = template.effectiveSets.map { it.copy() },
        )
}
