package com.example.mmarecomp.util

import com.example.mmarecomp.model.ExerciseModality
import com.example.mmarecomp.model.LoggedExercise
import com.example.mmarecomp.model.LoggedSet
import com.example.mmarecomp.model.Workout
import com.example.mmarecomp.model.asCardio
import com.example.mmarecomp.model.asStrength
import com.example.mmarecomp.model.withSets

/** Exercice issu de l'historique, prêt à être ajouté ou à remplacer un mouvement du plan. */
data class RecentExerciseEntry(
    val nom: String,
    val derniereChargeKg: Double?,
    val nbSeries: Int,
    val template: LoggedExercise,
    val derniereDate: String,
    val modality: ExerciseModality = ExerciseModality.Strength,
    val derniereDureeMin: Int? = null,
    val derniereDistanceKm: Double? = null,
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
                nbSeries = if (exercice.isCardio) 0 else exercice.effectiveSets.size.coerceAtLeast(1),
                template = exercice,
                derniereDate = date,
                modality = exercice.modality,
                derniereDureeMin = exercice.dureeMin,
                derniereDistanceKm = exercice.distanceKm,
            )
        }
    }

    /** Remplace le nom et les charges tout en gardant le nombre de séries du plan.
     *  Si le template est cardio, bascule tout le slot en cardio. */
    fun replaceKeepingStructure(current: LoggedExercise, template: LoggedExercise): LoggedExercise {
        if (template.isCardio) {
            return current.copy(nom = ExerciseName.propre(template.nom)).asCardio(
                dureeMin = template.dureeMin ?: current.dureeMin ?: 30,
                distanceKm = template.distanceKm ?: current.distanceKm,
                intensite = template.intensite ?: current.intensite ?: 5,
            )
        }
        val strengthCurrent = if (current.isCardio) current.asStrength() else current
        val targetCount = strengthCurrent.effectiveSets.size.coerceAtLeast(1)
        val templateSets = template.effectiveSets
        if (templateSets.isEmpty()) {
            return strengthCurrent.copy(
                nom = ExerciseName.propre(template.nom),
                chargeCibleKg = template.chargeCibleKg ?: template.chargeMaxKg,
                modality = ExerciseModality.Strength,
            )
        }
        val newSets = (0 until targetCount).map { index ->
            val source = templateSets.getOrElse(index) { templateSets.last() }
            source.copy(
                index = index + 1,
                estAmrap = index == targetCount - 1,
            )
        }
        return strengthCurrent.copy(
            nom = ExerciseName.propre(template.nom),
            chargeCibleKg = template.chargeCibleKg ?: template.chargeMaxKg,
            modality = ExerciseModality.Strength,
        ).withSets(newSets)
    }

    fun copyForAdd(template: LoggedExercise): LoggedExercise {
        val propre = template.copy(nom = ExerciseName.propre(template.nom))
        return if (propre.isCardio) {
            propre.asCardio(
                dureeMin = propre.dureeMin ?: 30,
                distanceKm = propre.distanceKm,
                intensite = propre.intensite ?: 5,
            )
        } else {
            propre.copy(sets = propre.effectiveSets.map { it.copy() })
        }
    }
}
