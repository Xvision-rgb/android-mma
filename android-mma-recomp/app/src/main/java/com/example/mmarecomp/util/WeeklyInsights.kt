package com.example.mmarecomp.util

import com.example.mmarecomp.model.DailyCheckIn
import com.example.mmarecomp.model.Meal
import com.example.mmarecomp.model.MmaSession
import com.example.mmarecomp.model.NutritionTarget
import com.example.mmarecomp.model.WeighIn
import com.example.mmarecomp.model.WeighInType
import com.example.mmarecomp.model.Workout
import java.time.LocalDate

data class WeeklyInsights(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val avgReadinessScore: Double?,
    val workoutsLogged: Int,
    val mmaSessionsLogged: Int,
    val avgCaloriesPerDay: Int,
    val daysOnCalorieTarget: Int,
    val daysWithMeals: Int,
    val weightDeltaKg: Double?,
    val alerts: List<String>,
)

/** Synthèse hebdomadaire type « coach » pour l'onglet Progression. */
object WeeklyInsightsCalculator {
    fun compute(
        today: LocalDate = LocalDate.now(),
        checkIns: List<DailyCheckIn>,
        workouts: List<Workout>,
        mmaSessions: List<MmaSession>,
        meals: List<Meal>,
        weighIns: List<WeighIn>,
        targets: List<NutritionTarget>,
    ): WeeklyInsights {
        val weekStart = today.minusDays(6)
        val weekDates = (0..6).map { weekStart.plusDays(it.toLong()) }
        val weekDateStrings = weekDates.map { DateUtils.string(it) }.toSet()

        val weekCheckIns = checkIns.filter { it.date in weekDateStrings }
        val avgScore = weekCheckIns.map { it.score }.takeIf { it.isNotEmpty() }?.average()

        val weekWorkouts = workouts.count { it.date in weekDateStrings }
        val weekMma = mmaSessions.count { it.date in weekDateStrings }

        val mealsByDay = meals.filter { it.date in weekDateStrings }.groupBy { it.date }
        val daysWithMeals = mealsByDay.size
        val dailyTotals = mealsByDay.mapValues { (_, dayMeals) -> dayMeals.sumOf { it.calories } }
        val avgCalories = if (dailyTotals.isEmpty()) {
            0
        } else {
            dailyTotals.values.sum() / dailyTotals.size
        }

        val targetByDate = targets.associateBy { it.date }
        val daysOnTarget = weekDateStrings.count { date ->
            val total = dailyTotals[date] ?: return@count false
            val target = targetByDate[date]?.caloriesCible ?: return@count false
            val tolerance = (target * 0.1).toInt().coerceAtLeast(100)
            kotlin.math.abs(total - target) <= tolerance
        }

        val morningWeighIns = weighIns
            .filter { it.type == WeighInType.MatinJeun && it.date in weekDateStrings }
            .sortedBy { it.date }
        val weightDelta = if (morningWeighIns.size >= 2) {
            morningWeighIns.last().poidsKg - morningWeighIns.first().poidsKg
        } else {
            null
        }

        val alerts = buildList {
            if (weekCheckIns.isNotEmpty() && (avgScore ?: 25.0) < 15) {
                add("Score de forme bas cette semaine — pense à alléger le volume.")
            }
            if (daysWithMeals < 4) {
                add("Peu de jours avec repas logués — la nutrition guide mal la recomposition.")
            }
            if (weekWorkouts + weekMma == 0) {
                add("Aucune séance cette semaine — même une séance courte maintient l'adaptation.")
            }
            if (weightDelta != null && kotlin.math.abs(weightDelta) > 1.0) {
                add("Variation de poids > 1 kg sur 7 j — vérifie l'hydratation et la régularité des pesées.")
            }
        }

        return WeeklyInsights(
            weekStart = weekStart,
            weekEnd = today,
            avgReadinessScore = avgScore,
            workoutsLogged = weekWorkouts,
            mmaSessionsLogged = weekMma,
            avgCaloriesPerDay = avgCalories,
            daysOnCalorieTarget = daysOnTarget,
            daysWithMeals = daysWithMeals,
            weightDeltaKg = weightDelta,
            alerts = alerts,
        )
    }
}
