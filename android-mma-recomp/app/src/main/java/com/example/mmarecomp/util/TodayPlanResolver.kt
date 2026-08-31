package com.example.mmarecomp.util

import com.example.mmarecomp.model.PlanCreneau
import com.example.mmarecomp.model.TrainingPlanDay
import com.example.mmarecomp.model.Workout
import com.example.mmarecomp.model.toWorkoutTypeOrNull
import java.time.LocalDate

/**
 * Résout la séance prévue aujourd'hui et l'exercice à proposer, sans
 * tirer au sort à chaque recomposition et sans masquer le plan dès
 * qu'une séance d'un autre type a été loguée le même jour.
 *
 * Deux créneaux/jour : matin puis soir — un HIIT du matin ne masque pas
 * la force du soir, et réciproquement si les types diffèrent.
 */
data class WorkoutSuggestion(
    val sessionLabel: String,
    val exercises: List<String>,
)

object TodayPlanResolver {

    private fun plansForDay(
        planThisWeek: List<TrainingPlanDay>,
        jour: Int,
    ): List<TrainingPlanDay> =
        planThisWeek
            .filter { it.jourSemaine == jour }
            .sortedBy { it.creneau.ordinal }

    /**
     * Premier créneau du jour encore à faire : on ne le considère logué que
     * si une séance du MÊME type existe aujourd'hui. Un HIIT du matin ne doit
     * pas faire disparaître la séance de force prévue (même créneau ou l'autre).
     */
    fun unresolvedToday(
        planThisWeek: List<TrainingPlanDay>,
        workouts: List<Workout>,
        today: LocalDate = LocalDate.now(),
    ): TrainingPlanDay? {
        val jour = DateUtils.weekdayIso(today)
        val todayStr = DateUtils.string(today)
        val loggedTypes = workouts.filter { it.date == todayStr }.map { it.type }.toSet()
        return plansForDay(planThisWeek, jour).firstOrNull { plan ->
            val typeAttendu = plan.type.toWorkoutTypeOrNull()
            typeAttendu == null || typeAttendu !in loggedTypes
        }
    }

    /** Tous les créneaux du jour encore non logués (matin d'abord). */
    fun unresolvedSlotsToday(
        planThisWeek: List<TrainingPlanDay>,
        workouts: List<Workout>,
        today: LocalDate = LocalDate.now(),
    ): List<TrainingPlanDay> {
        val jour = DateUtils.weekdayIso(today)
        val todayStr = DateUtils.string(today)
        val loggedTypes = workouts.filter { it.date == todayStr }.map { it.type }.toSet()
        return plansForDay(planThisWeek, jour).filter { plan ->
            val typeAttendu = plan.type.toWorkoutTypeOrNull()
            typeAttendu == null || typeAttendu !in loggedTypes
        }
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
            ?.let { plan -> return exercisesFromPlan(plan, planThisWeek, limit) }

        val jourAujourdhui = DateUtils.weekdayIso(today)
        val suivants = planThisWeek
            .filter { it.type.toWorkoutTypeOrNull() != null && it.exercices.isNotEmpty() }
            .sortedWith(
                compareBy(
                    { day ->
                        val delta = (day.jourSemaine - jourAujourdhui + 7) % 7
                        if (delta == 0) 7 else delta
                    },
                    { it.creneau.ordinal },
                ),
            )
        return suivants.firstOrNull()?.let { exercisesFromPlan(it, planThisWeek, limit) }
    }

    private fun exercisesFromPlan(
        plan: TrainingPlanDay,
        allPlans: List<TrainingPlanDay>,
        limit: Int,
    ): WorkoutSuggestion? {
        val noms = plan.exercices.map { it.nom.trim() }.filter { it.isNotBlank() }.take(limit)
        if (noms.isEmpty()) return null
        return WorkoutSuggestion(sessionLabel = sessionLabel(plan, allPlans), exercises = noms)
    }

    /** Libellé avec créneau si le jour en a deux, ou si ce n'est pas le matin. */
    fun sessionLabel(plan: TrainingPlanDay, allPlans: List<TrainingPlanDay>): String {
        val multi = allPlans.count { it.jourSemaine == plan.jourSemaine } > 1 ||
            plan.creneau != PlanCreneau.Matin
        return if (multi) "${plan.type.label} · ${plan.creneau.label}" else plan.type.label
    }
}
