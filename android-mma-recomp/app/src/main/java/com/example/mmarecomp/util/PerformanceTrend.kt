package com.example.mmarecomp.util

import com.example.mmarecomp.model.Workout

/** Évalue si les charges loggées récemment progressent, exercice par
 *  exercice — sert à qualifier un plateau de poids stable en "recomposition
 *  en cours" plutôt que de l'afficher par défaut sans preuve. Conservateur :
 *  sans assez de données, ne prétend jamais une progression (le plateau
 *  reste alors simplement silencieux, jamais une alerte). */
object PerformanceTrend {
    fun isImproving(workouts: List<Workout>): Boolean {
        val byExercise = workouts
            .sortedBy { it.date }
            .flatMap { workout -> workout.exercices.map { it } }
            .filter { it.chargeReelleKg != null }
            .groupBy { it.nom }

        var improving = 0
        var declining = 0
        byExercise.values.forEach { entries ->
            if (entries.size < 2) return@forEach
            val first = entries.first().chargeReelleKg!!
            val last = entries.last().chargeReelleKg!!
            when {
                last > first -> improving++
                last < first -> declining++
            }
        }
        return improving > 0 && improving >= declining
    }
}
