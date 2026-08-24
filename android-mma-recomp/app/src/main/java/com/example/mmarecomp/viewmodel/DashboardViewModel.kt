package com.example.mmarecomp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mmarecomp.data.MealRepository
import com.example.mmarecomp.data.NutritionTargetRepository
import com.example.mmarecomp.data.TrainingPlanRepository
import com.example.mmarecomp.data.WeighInRepository
import com.example.mmarecomp.data.WorkoutRepository
import com.example.mmarecomp.model.Meal
import com.example.mmarecomp.model.NutritionTarget
import com.example.mmarecomp.model.Phase
import com.example.mmarecomp.model.TrainingPlanDay
import com.example.mmarecomp.model.WeighIn
import com.example.mmarecomp.model.WeighInType
import com.example.mmarecomp.model.Workout
import com.example.mmarecomp.util.DateUtils
import com.example.mmarecomp.util.MovingAverage
import com.example.mmarecomp.util.PlateauDetector
import com.example.mmarecomp.util.PlateauStatus
import com.example.mmarecomp.util.StreakCalculator
import com.example.mmarecomp.util.TrendPoint
import com.example.mmarecomp.util.toFriendlyMessage
import com.example.mmarecomp.ui.AppPreferencesState
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val trainingPlanRepository: TrainingPlanRepository = TrainingPlanRepository(),
    private val workoutRepository: WorkoutRepository = WorkoutRepository(),
    private val mealRepository: MealRepository = MealRepository(),
    private val weighInRepository: WeighInRepository = WeighInRepository(),
    private val nutritionTargetRepository: NutritionTargetRepository = NutritionTargetRepository(),
) : ViewModel() {
    var planThisWeek by mutableStateOf<List<TrainingPlanDay>>(emptyList())
        private set
    var workoutsThisWeek by mutableStateOf<List<Workout>>(emptyList())
        private set
    var mealsLast7Days by mutableStateOf<List<Meal>>(emptyList())
        private set
    var morningWeighIns by mutableStateOf<List<WeighIn>>(emptyList())
        private set
    var todayTarget by mutableStateOf<NutritionTarget?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    /** Série de constance positive : jours consécutifs avec au moins une
     *  séance ou un repas loggué. Jamais basée sur le poids. */
    var consistencyStreak by mutableStateOf(0)
        private set

    /** Vrai tant qu'aucune séance, repas ou pesée n'a jamais été loggué —
     *  sert à afficher un message d'accueil plutôt que des cartes à "0". */
    val hasAnyData: Boolean
        get() = workoutsThisWeek.isNotEmpty() || mealsLast7Days.isNotEmpty() || morningWeighIns.isNotEmpty()

    val avgCaloriesLast7Days: Int
        get() {
            if (mealsLast7Days.isEmpty()) return 0
            val total = mealsLast7Days.sumOf { it.calories }
            val days = mealsLast7Days.map { it.date }.toSet().size
            return if (days > 0) total / days else 0
        }

    val weightTrend7Day: List<TrendPoint>
        get() {
            val points = morningWeighIns.mapNotNull { w ->
                DateUtils.date(w.date)?.let { TrendPoint(it, w.poidsKg) }
            }
            return MovingAverage.windowed(points, AppPreferencesState.preferences.value.movingAverageWindow.days)
        }

    val seancesFaitesCount: Int get() = workoutsThisWeek.size
    val seancesPlanifieesCount: Int get() = planThisWeek.count { it.type.value != "repos" }

    val plateauStatus: PlateauStatus
        get() {
            val points = morningWeighIns.mapNotNull { w ->
                DateUtils.date(w.date)?.let { it to w.poidsKg }
            }
            // Sans historique de charges chargé ici, on reste positif par défaut ;
            // ProgressViewModel affine ce signal avec les vraies charges loggées.
            return PlateauDetector.detect(points, performanceTrendUp = true)
        }

    fun load(phase: Phase) {
        isLoading = true
        errorMessage = null
        viewModelScope.launch {
            try {
                val mondayOfWeek = DateUtils.startOfWeek(weekStart = AppPreferencesState.preferences.value.weekStart)
                val sevenDaysAgo = DateUtils.daysAgo(7)
                val today = DateUtils.today()
                val thirtyDaysAgo = DateUtils.daysAgo(30)

                // Sept requêtes indépendantes — lancées en parallèle plutôt
                // qu'attendues une par une, pour un chargement nettement
                // plus rapide sur une connexion mobile.
                coroutineScope {
                    val planDeferred = async { trainingPlanRepository.fetchWeek(phase) }
                    val workoutsWeekDeferred = async { workoutRepository.fetchWeek(mondayOfWeek) }
                    val mealsWeekDeferred = async { mealRepository.fetch(sevenDaysAgo) }
                    val weighInsDeferred = async { weighInRepository.fetch(sevenDaysAgo) }
                    val targetDeferred = async { nutritionTargetRepository.fetch(today) }
                    val recentWorkoutsDeferred = async { workoutRepository.fetchWeek(thirtyDaysAgo) }
                    val recentMealsDeferred = async { mealRepository.fetch(thirtyDaysAgo) }

                    planThisWeek = planDeferred.await()
                    workoutsThisWeek = workoutsWeekDeferred.await()
                    mealsLast7Days = mealsWeekDeferred.await()
                    morningWeighIns = weighInsDeferred.await().filter { it.type == WeighInType.MatinJeun }
                    todayTarget = targetDeferred.await()

                    val recentWorkouts = recentWorkoutsDeferred.await()
                    val recentMeals = recentMealsDeferred.await()
                    val activeDates = (recentWorkouts.mapNotNull { DateUtils.date(it.date) } +
                        recentMeals.mapNotNull { DateUtils.date(it.date) }).toSet()
                    consistencyStreak = StreakCalculator.currentStreak(activeDates)
                }
            } catch (e: Exception) {
                errorMessage = e.toFriendlyMessage("Impossible de charger le dashboard pour le moment.")
            } finally {
                isLoading = false
            }
        }
    }
}
