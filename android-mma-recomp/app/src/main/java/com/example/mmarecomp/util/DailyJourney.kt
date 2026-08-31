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
    ): DailyJourneyState {
        val todayStr = DateUtils.string(today)
        val isRestDay = planToday?.type == PlanDayType.Repos
        val workoutDone = workoutsToday.any { it.date == todayStr } ||
            mmaSessionsToday.any { it.date == todayStr }

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
                detail = nutritionDetail(LocalTime.now().hour, mealsToday.size),
                done = mealsToday.isNotEmpty(),
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

    private fun nutritionDetail(hour: Int, mealsLogged: Int): String = when {
        mealsLogged > 0 -> "$mealsLogged repas logué(s) aujourd'hui"
        hour < 11 -> "Petit-déjeuner à saisir"
        hour < 15 -> "Déjeuner à saisir"
        hour < 20 -> "Collation ou dîner à saisir"
        else -> "Bilan du jour — au moins un repas"
    }
}
