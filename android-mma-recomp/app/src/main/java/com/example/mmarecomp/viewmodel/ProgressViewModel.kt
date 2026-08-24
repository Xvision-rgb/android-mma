package com.example.mmarecomp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mmarecomp.data.ProfileRepository
import com.example.mmarecomp.data.WeighInRepository
import com.example.mmarecomp.data.WorkoutRepository
import com.example.mmarecomp.model.WeighIn
import com.example.mmarecomp.model.WeighInType
import com.example.mmarecomp.model.Workout
import com.example.mmarecomp.util.DateUtils
import com.example.mmarecomp.util.MovingAverage
import com.example.mmarecomp.util.PersonalRecordDetector
import com.example.mmarecomp.util.TrendPoint
import com.example.mmarecomp.util.toFriendlyMessage
import com.example.mmarecomp.ui.AppPreferencesState
import java.time.LocalDate
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

data class ChargePoint(val date: LocalDate, val chargeKg: Double)

class ProgressViewModel(
    private val userId: String,
    private val weighInRepository: WeighInRepository = WeighInRepository(),
    private val workoutRepository: WorkoutRepository = WorkoutRepository(),
    private val profileRepository: ProfileRepository = ProfileRepository(),
) : ViewModel() {
    var weighIns by mutableStateOf<List<WeighIn>>(emptyList())
        private set
    var workouts by mutableStateOf<List<Workout>>(emptyList())
        private set
    var allTimeWorkouts by mutableStateOf<List<Workout>>(emptyList())
        private set
    var poidsObjectifKg by mutableStateOf<Double?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var windowWeeks by mutableStateOf(4)

    val weightTrend: List<TrendPoint>
        get() {
            val points = weighIns
                .filter { it.type == WeighInType.MatinJeun }
                .mapNotNull { w -> DateUtils.date(w.date)?.let { TrendPoint(it, w.poidsKg) } }
            return MovingAverage.windowed(points, AppPreferencesState.preferences.value.movingAverageWindow.days)
        }

    val bfTrend: List<TrendPoint>
        get() {
            val points = weighIns
                .filter { it.type == WeighInType.MatinJeun }
                .mapNotNull { w ->
                    val d = DateUtils.date(w.date)
                    val bf = w.bfPct
                    if (d != null && bf != null) TrendPoint(d, bf) else null
                }
            return MovingAverage.windowed(points, AppPreferencesState.preferences.value.movingAverageWindow.days)
        }

    /** Progression de charge par exercice sur la fenêtre sélectionnée. */
    val chargeProgressionByExercise: Map<String, List<ChargePoint>>
        get() {
            val result = mutableMapOf<String, MutableList<ChargePoint>>()
            for (workout in workouts) {
                val date = DateUtils.date(workout.date) ?: continue
                for (exercice in workout.exercices) {
                    val charge = exercice.chargeReelleKg ?: continue
                    result.getOrPut(exercice.nom) { mutableListOf() }.add(ChargePoint(date, charge))
                }
            }
            return result.mapValues { (_, points) -> points.sortedBy { it.date } }
        }

    val performanceTrendUp: Boolean
        get() = chargeProgressionByExercise.values.any { series ->
            val first = series.firstOrNull()?.chargeKg
            val last = series.lastOrNull()?.chargeKg
            first != null && last != null && last > first
        }

    /** Meilleure charge jamais loggée par exercice, sur tout l'historique
     *  (pas seulement la fenêtre 4/8 semaines) — toujours une célébration,
     *  jamais affiché comme "manquant" pour un exercice jamais fait. */
    val personalBests: List<Pair<String, Double>>
        get() {
            val displayNameByKey = LinkedHashMap<String, String>()
            for (workout in allTimeWorkouts) {
                for (exercice in workout.exercices) {
                    val key = exercice.nom.trim().lowercase()
                    displayNameByKey.putIfAbsent(key, exercice.nom.trim())
                }
            }
            return displayNameByKey.values
                .mapNotNull { name -> PersonalRecordDetector.bestKnownLoad(name, allTimeWorkouts)?.let { name to it } }
                .sortedBy { it.first }
        }

    fun load() {
        isLoading = true
        errorMessage = null
        viewModelScope.launch {
            val since = DateUtils.daysAgo(windowWeeks * 7L)
            try {
                coroutineScope {
                    val weighInsDeferred = async { weighInRepository.fetch(since) }
                    val workoutsDeferred = async { workoutRepository.fetchWeek(since) }
                    val allTimeDeferred = async { workoutRepository.fetchAll() }
                    val profileDeferred = if (AppPreferencesState.preferences.value.weightGoalEtaEnabled) {
                        async { runCatching { profileRepository.fetch(userId) }.getOrNull() }
                    } else {
                        null
                    }
                    weighIns = weighInsDeferred.await()
                    workouts = workoutsDeferred.await()
                    allTimeWorkouts = allTimeDeferred.await()
                    poidsObjectifKg = profileDeferred?.await()?.poidsObjectifKg
                }
            } catch (e: Exception) {
                errorMessage = e.toFriendlyMessage("Impossible de charger la progression.")
            } finally {
                isLoading = false
            }
        }
    }

    /** Estimation neutre de la date d'atteinte de l'objectif, basée sur la
     *  pente récente de la moyenne mobile — jamais une pression ("il te
     *  reste X jours"), juste une indication à titre informatif. `null` si
     *  pas assez de recul ou si la tendance actuelle ne va pas vers
     *  l'objectif (pas de date "négative" absurde). */
    val weightGoalEtaText: String?
        get() {
            val goal = poidsObjectifKg ?: return null
            val trend = weightTrend
            if (trend.size < 2) return null
            val first = trend.first()
            val last = trend.last()
            val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(first.date, last.date)
            if (daysBetween < 7) return null
            val slopePerDay = (last.value - first.value) / daysBetween
            if (slopePerDay == 0.0) return null
            val remaining = goal - last.value
            // La pente doit aller dans le même sens que ce qu'il reste à parcourir.
            if ((remaining > 0) != (slopePerDay > 0)) return null
            val daysToGoal = (remaining / slopePerDay).toLong()
            if (daysToGoal !in 1..730) return null
            val etaDate = last.date.plusDays(daysToGoal)
            val formatter = java.time.format.DateTimeFormatter.ofPattern("d MMMM", java.util.Locale.FRENCH)
            return "Vers le ${etaDate.format(formatter)} si le rythme actuel se maintient"
        }

    /** Séances les plus récentes en premier — pour l'historique avec suppression. */
    val recentWorkoutsDescending: List<Workout>
        get() = workouts.sortedByDescending { it.date }

    /** Retrait optimiste pour l'UI — n'efface rien côté serveur. À combiner
     *  avec un snackbar "Annuler" : [commitDeleteWorkout] si confirmé,
     *  [restoreWorkout] sinon. */
    fun removeWorkoutLocally(workout: Workout) {
        workouts = workouts.filterNot { it.id == workout.id }
    }

    fun restoreWorkout(workout: Workout) {
        workouts = (workouts.filterNot { it.id == workout.id } + workout).sortedBy { it.date }
    }

    fun commitDeleteWorkout(workout: Workout) {
        viewModelScope.launch {
            try {
                workoutRepository.delete(workout.id)
            } catch (e: Exception) {
                errorMessage = e.toFriendlyMessage("Impossible de supprimer cette séance.")
                restoreWorkout(workout)
            }
        }
    }
}
