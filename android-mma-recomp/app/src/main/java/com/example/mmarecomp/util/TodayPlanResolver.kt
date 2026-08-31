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
data class WorkoutSuggestion(
    val sessionLabel: String,
    val exercises: List<String>,
)

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
        val suggestion = suggestedExercises(planThisWeek, workouts, today) ?: return null
        val first = suggestion.exercises.firstOrNull() ?: return null
        return first to suggestion.sessionLabel
    }

    /** Jusqu'à trois exercices du plan du jour (ou du prochain jour d'entraînement)
     *  — déterministe, jamais aléatoire. */
    fun suggestedExercises(
        planThisWeek: List<TrainingPlanDay>,
        workouts: List<Workout>,
        today: LocalDate = LocalDate.now(),
        limit: Int = 3,
    ): WorkoutSuggestion? {
        unresolvedToday(planThisWeek, workouts, today)
            ?.let { plan -> return exercisesFromPlan(plan, limit) }

        val jourAujourdhui = DateUtils.weekdayIso(DateUtils.string(today))
        val suivants = planThisWeek
            .filter { it.type.toWorkoutTypeOrNull() != null && it.exercices.isNotEmpty() }
            .sortedBy { day ->
                val delta = (day.jourSemaine - jourAujourdhui + 7) % 7
                if (delta == 0) 7 else delta
            }
        return suivants.firstOrNull()?.let { exercisesFromPlan(it, limit) }
    }

    private fun exercisesFromPlan(plan: TrainingPlanDay, limit: Int): WorkoutSuggestion? {
        val noms = plan.exercices.map { it.nom.trim() }.filter { it.isNotBlank() }.take(limit)
        if (noms.isEmpty()) return null
        return WorkoutSuggestion(sessionLabel = plan.type.label, exercises = noms)
    }
}
