package com.example.mmarecomp.util

import com.example.mmarecomp.model.TrainingPlanDay
import com.example.mmarecomp.model.Workout
import com.example.mmarecomp.model.toWorkoutTypeOrNull
import java.time.LocalDate

/**
 * Résout la séance prévue aujourd'hui et l'exercice à proposer, sans
 * tirer au sort à chaque recomposition et sans masquer le plan dès
 * qu'une séance d'un autre type a été loguée le même jour.
 */
object TodayPlanResolver {

    /**
     * Jour programmé encore à faire : on ne le considère logué que si une
     * séance du MÊME type existe aujourd'hui. Un HIIT du matin ne doit pas
     * faire disparaître la séance de force prévue.
     */
    fun unresolvedToday(
        planThisWeek: List<TrainingPlanDay>,
        workouts: List<Workout>,
        today: LocalDate = LocalDate.now(),
    ): TrainingPlanDay? {
        val jour = DateUtils.weekdayIso(DateUtils.string(today))
        val plan = planThisWeek.firstOrNull { it.jourSemaine == jour } ?: return null
        val typeAttendu = plan.type.toWorkoutTypeOrNull() ?: return plan
        val dejaLoguee = workouts.any { it.date == DateUtils.string(today) && it.type == typeAttendu }
        return if (dejaLoguee) null else plan
    }

    /**
     * Premier exercice du plan du jour s'il reste à faire, sinon premier
     * exercice du prochain jour d'entraînement de la semaine. Déterministe :
     * le même plan produit toujours la même suggestion.
     */
    fun suggestedExercise(
        planThisWeek: List<TrainingPlanDay>,
        workouts: List<Workout>,
        today: LocalDate = LocalDate.now(),
    ): Pair<String, String>? {
        unresolvedToday(planThisWeek, workouts, today)
            ?.let { plan -> firstExercise(plan)?.let { return it } }

        val jourAujourdhui = DateUtils.weekdayIso(DateUtils.string(today))
        val suivants = planThisWeek
            .filter { it.type.toWorkoutTypeOrNull() != null && it.exercices.isNotEmpty() }
            .sortedBy { day ->
                val delta = (day.jourSemaine - jourAujourdhui + 7) % 7
                if (delta == 0) 7 else delta
            }
        return suivants.firstOrNull()?.let { firstExercise(it) }
    }

    private fun firstExercise(plan: TrainingPlanDay): Pair<String, String>? {
        val nom = plan.exercices.firstOrNull { it.nom.isNotBlank() }?.nom ?: return null
        return nom to plan.type.label
    }
}
