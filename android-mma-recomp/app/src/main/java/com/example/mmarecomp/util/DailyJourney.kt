package com.example.mmarecomp.util

import com.example.mmarecomp.model.DailyCheckIn
import com.example.mmarecomp.model.Meal
import com.example.mmarecomp.model.MmaSession
import com.example.mmarecomp.model.PlanDayType
import com.example.mmarecomp.model.TrainingPlanDay
import com.example.mmarecomp.model.WeighIn
import com.example.mmarecomp.model.WeighInType
import com.example.mmarecomp.model.Workout
import java.time.LocalDate
import java.time.LocalTime

enum class DailyJourneyStepId {
    CHECK_IN,
    MORNING_WEIGH_IN,
    WORKOUT,
    NUTRITION,
}

data class DailyJourneyStep(
    val id: DailyJourneyStepId,
    val label: String,
    val detail: String,
    val done: Boolean,
    val optional: Boolean = false,
)

data class DailyJourneyState(
    val steps: List<DailyJourneyStep>,
    val completedCount: Int,
    val totalCount: Int,
    val progress: Float,
)

/** Agrège les étapes de la journée type depuis les données déjà chargées. */
object DailyJourney {
    fun compute(
        today: LocalDate = LocalDate.now(),
        checkInToday: DailyCheckIn?,
        weighInsToday: List<WeighIn>,
        workoutsToday: List<Workout>,
        mmaSessionsToday: List<MmaSession>,
        mealsToday: List<Meal>,
        planToday: TrainingPlanDay?,
        hourOfDay: Int = LocalTime.now().hour,
    ): DailyJourneyState {
        val todayStr = DateUtils.string(today)
        val isRestDay = planToday?.type == PlanDayType.Repos
        val workoutDone = workoutsToday.any { it.date == todayStr } ||
            mmaSessionsToday.any { it.date == todayStr }
        val distinctMealSlots = mealsToday.map { it.repas }.toSet().size
        val nutritionDone = nutritionDone(hourOfDay, distinctMealSlots)

        val steps = listOf(
            DailyJourneyStep(
                id = DailyJourneyStepId.CHECK_IN,
                label = "Point du jour",
                detail = "Sommeil, fatigue, humeur — 20 s",
                done = checkInToday != null,
            ),
            DailyJourneyStep(
                id = DailyJourneyStepId.MORNING_WEIGH_IN,
                label = "Pesée matin",
                detail = "À jeun, idéalement avant 10 h",
                done = weighInsToday.any { it.type == WeighInType.MatinJeun },
            ),
            DailyJourneyStep(
                id = DailyJourneyStepId.WORKOUT,
                label = if (isRestDay) "Repos prévu" else "Séance du jour",
                detail = when {
                    isRestDay -> "Jour off selon ton plan — optionnel"
                    planToday != null -> "Type prévu : ${planToday.type.label}"
                    else -> "Salle ou MMA"
                },
                done = workoutDone || isRestDay,
                optional = isRestDay,
            ),
            DailyJourneyStep(
                id = DailyJourneyStepId.NUTRITION,
                label = "Nutrition",
                detail = nutritionDetail(hourOfDay, distinctMealSlots),
                done = nutritionDone,
            ),
        )

        val required = steps.filterNot { it.optional }
        val completed = required.count { it.done }
        val total = required.size.coerceAtLeast(1)
        return DailyJourneyState(
            steps = steps,
            completedCount = completed,
            totalCount = total,
            progress = completed.toFloat() / total.toFloat(),
        )
    }

    /** Avant 11 h : 1 créneau suffit ; ensuite il faut au moins 2 créneaux
     *  distincts pour marquer l'étape (évite qu'un snack coche toute la journée). */
    internal fun nutritionDone(hour: Int, distinctSlots: Int): Boolean =
        if (hour < 11) distinctSlots >= 1 else distinctSlots >= 2

    private fun nutritionDetail(hour: Int, distinctSlots: Int): String = when {
        distinctSlots >= 2 -> "$distinctSlots créneaux logués aujourd'hui"
        distinctSlots == 1 && hour < 11 -> "1 créneau — continue sur la journée"
        distinctSlots == 1 -> "1 créneau — encore au moins un repas"
        hour < 11 -> "Petit-déjeuner à saisir"
        hour < 15 -> "Déjeuner à saisir"
        hour < 20 -> "Collation ou dîner à saisir"
        else -> "Bilan du jour — au moins 2 créneaux"
    }
}
