package com.example.mmarecomp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mmarecomp.data.MealRepository
import com.example.mmarecomp.data.NutritionTargetRepository
import com.example.mmarecomp.data.ProfileRepository
import com.example.mmarecomp.data.TrainingPlanRepository
import com.example.mmarecomp.data.WeighInRepository
import com.example.mmarecomp.data.WorkoutRepository
import com.example.mmarecomp.model.Meal
import com.example.mmarecomp.model.NewTrainingPlanDay
import com.example.mmarecomp.model.NutritionTarget
import com.example.mmarecomp.model.Phase
import com.example.mmarecomp.model.PlanDayType
import com.example.mmarecomp.model.TrainingPlanDay
import com.example.mmarecomp.model.WeighIn
import com.example.mmarecomp.model.WeighInType
import com.example.mmarecomp.model.Workout
import com.example.mmarecomp.model.WorkoutType
import com.example.mmarecomp.util.DateUtils
import com.example.mmarecomp.util.MovingAverage
import com.example.mmarecomp.util.PlateauDetector
import com.example.mmarecomp.util.PlateauStatus
import com.example.mmarecomp.util.TrendDirection
import com.example.mmarecomp.util.TrendPoint
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val userId: String = "",
    private val trainingPlanRepository: TrainingPlanRepository = TrainingPlanRepository(),
    private val workoutRepository: WorkoutRepository = WorkoutRepository(),
    private val mealRepository: MealRepository = MealRepository(),
    private val weighInRepository: WeighInRepository = WeighInRepository(),
    private val nutritionTargetRepository: NutritionTargetRepository = NutritionTargetRepository(),
    private val profileRepository: ProfileRepository = ProfileRepository(),
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
    var recentTargets by mutableStateOf<List<NutritionTarget>>(emptyList())
        private set
    var poidsObjectifKg by mutableStateOf<Double?>(null)
        private set
    var bfObjectifPct by mutableStateOf<Double?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

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
            return MovingAverage.sevenDay(points)
        }

    /** Tendance %BF sur 7 jours — même principe de lissage que le poids,
     *  jamais la valeur brute d'une seule pesée. */
    val bfTrend7Day: List<TrendPoint>
        get() {
            val points = morningWeighIns.mapNotNull { w ->
                val bf = w.bfPct ?: return@mapNotNull null
                DateUtils.date(w.date)?.let { TrendPoint(it, bf) }
            }
            return MovingAverage.sevenDay(points)
        }

    val mealsLoggedToday: Int get() = mealsLast7Days.count { it.date == DateUtils.today() }

    /** Total calorique d'hier — repère de contexte, jamais une comparaison
     *  culpabilisante. Null si rien n'a été loggé hier. */
    val yesterdayCalories: Int?
        get() {
            val yesterday = DateUtils.daysAgo(1)
            val meals = mealsLast7Days.filter { it.date == yesterday }
            return if (meals.isEmpty()) null else meals.sumOf { it.calories }
        }

    /** Nombre de jours consécutifs (jusqu'à aujourd'hui) avec au moins une
     *  activité loggée (repas, séance ou pesée). Purement positif — ne
     *  redescend jamais à un nombre négatif ni n'affiche de message de
     *  "série brisée" : on compte juste ce qui est là. */
    val activityStreakDays: Int
        get() {
            val loggedDates = buildSet {
                addAll(mealsLast7Days.map { it.date })
                addAll(workoutsThisWeek.map { it.date })
                addAll(morningWeighIns.map { it.date })
            }
            var streak = 0
            var cursor = java.time.LocalDate.now()
            while (loggedDates.contains(DateUtils.string(cursor))) {
                streak++
                cursor = cursor.minusDays(1)
            }
            return streak
        }

    /** Volume total d'entraînement (séries × reps × charge réelle) cumulé sur
     *  les séances de la semaine — vue synthèse, jamais une comparaison
     *  culpabilisante d'une semaine à l'autre. */
    val weeklyTrainingVolume: Double
        get() = workoutsThisWeek.sumOf { workout ->
            workout.exercices.sumOf { it.series * it.reps * (it.chargeReelleKg ?: 0.0) }
        }

    /** Nombre de jours distincts avec au moins un repas loggé sur les 7
     *  derniers jours — mesure de régularité, jamais un jugement sur le
     *  contenu ou les quantités. */
    val daysWithMealsLast7Days: Int get() = mealsLast7Days.map { it.date }.toSet().size

    /** Tendance de poids sur la semaine — dérivée de la moyenne mobile 7j,
     *  jamais une valeur brute (cf. WeightTrendChart). */
    val weightTrendDirection: TrendDirection get() = MovingAverage.direction(weightTrend7Day)

    /** Écart entre le poids actuel (moyenne mobile 7j, jamais brut) et
     *  l'objectif défini dans le profil — positif si au-dessus de l'objectif,
     *  négatif si en dessous. Null si l'objectif ou l'historique manquent. */
    val weightGoalGapKg: Double?
        get() {
            val current = weightTrend7Day.lastOrNull()?.value ?: return null
            val target = poidsObjectifKg ?: return null
            return current - target
        }

    /** Écart entre le %BF actuel (moyenne mobile 7j, jamais brut) et
     *  l'objectif défini dans le profil — même logique que weightGoalGapKg.
     *  Null si l'objectif ou l'historique %BF manquent. */
    val bfGoalGapPct: Double?
        get() {
            val current = bfTrend7Day.lastOrNull()?.value ?: return null
            val target = bfObjectifPct ?: return null
            return current - target
        }

    /** Cible calorique moyenne sur les jours où une cible a été définie
     *  cette semaine — repère de comparaison pour avgCaloriesLast7Days dans
     *  le récap hebdomadaire, jamais recalculé différemment. */
    val avgTargetCaloriesLast7Days: Int?
        get() = recentTargets.takeIf { it.isNotEmpty() }?.map { it.caloriesCible }?.average()?.toInt()

    val seancesFaitesCount: Int get() = workoutsThisWeek.size
    val seancesPlanifieesCount: Int get() = planThisWeek.count { it.type.value != "repos" }

    /** Répartition des séances faites cette semaine par type — inspiré de la
     *  heat map de récupération musculaire de Fitbod, mais adapté à nos
     *  catégories existantes (WorkoutType) plutôt qu'un découpage par groupe
     *  musculaire qu'on n'a pas. Signal factuel de variété, jamais un
     *  jugement ("tu ne fais jamais X") : uniquement un décompte affiché tel
     *  quel côté écran. */
    val workoutTypeBreakdown: Map<WorkoutType, Int>
        get() = workoutsThisWeek.groupingBy { it.type }.eachCount()

    /** Séance prévue aujourd'hui d'après le split programmé, si elle
     *  n'a pas déjà été loguée. */
    val todayPlan: com.example.mmarecomp.model.TrainingPlanDay?
        get() {
            val jourAujourdhui = DateUtils.weekdayIso(DateUtils.today())
            val plan = planThisWeek.firstOrNull { it.jourSemaine == jourAujourdhui } ?: return null
            val dejaLoguee = workoutsThisWeek.any { it.date == DateUtils.today() }
            return if (dejaLoguee) null else plan
        }

    /** Repas nettement en dessous de la cible trois jours d'affilée — signal
     *  doux (jamais culpabilisant), utile pour repérer une sous-alimentation
     *  involontaire plutôt qu'un déficit volontaire ponctuel. */
    val showsUnderTargetPattern: Boolean
        get() {
            val dailyTotals = mealsLast7Days
                .groupBy { it.date }
                .mapValues { (_, meals) -> meals.sumOf { it.calories } }
            val triples = recentTargets.mapNotNull { target ->
                val total = dailyTotals[target.date] ?: return@mapNotNull null
                Triple(target.date, total, target.caloriesCible)
            }.sortedBy { it.first }
            return com.example.mmarecomp.util.NutritionTargetCalculator.softUnderTargetAlert(triples)
        }

    val plateauStatus: PlateauStatus
        get() {
            val points = morningWeighIns.mapNotNull { w ->
                DateUtils.date(w.date)?.let { it to w.poidsKg }
            }
            // Sans historique de charges chargé ici, on reste positif par défaut ;
            // ProgressViewModel affine ce signal avec les vraies charges loggées.
            return PlateauDetector.detect(points, performanceTrendUp = true)
        }

    /** Change le type de séance programmé pour un jour de la semaine, sans
     *  toucher aux exercices déjà définis pour ce jour. */
    fun updatePlanDayType(day: TrainingPlanDay, newType: PlanDayType) {
        viewModelScope.launch {
            val updated = NewTrainingPlanDay(
                jourSemaine = day.jourSemaine,
                type = newType,
                exercices = day.exercices,
                phase = day.phase,
                notes = day.notes,
            )
            try {
                trainingPlanRepository.upsert(updated)
                planThisWeek = planThisWeek.map { if (it.id == day.id) it.copy(type = newType) else it }
            } catch (e: Exception) {
                errorMessage = "Impossible de mettre à jour le programme."
            }
        }
    }

    fun load(phase: Phase) {
        isLoading = true
        errorMessage = null
        viewModelScope.launch {
            try {
                val mondayOfWeek = DateUtils.startOfWeek()
                val sevenDaysAgo = DateUtils.daysAgo(7)
                val today = DateUtils.today()

                planThisWeek = trainingPlanRepository.fetchWeek(phase)
                workoutsThisWeek = workoutRepository.fetchWeek(mondayOfWeek)
                mealsLast7Days = mealRepository.fetchSince(sevenDaysAgo)
                morningWeighIns = weighInRepository.fetch(sevenDaysAgo).filter { it.type == WeighInType.MatinJeun }
                todayTarget = nutritionTargetRepository.fetch(today)
                recentTargets = nutritionTargetRepository.fetchSince(sevenDaysAgo)
                if (userId.isNotBlank()) {
                    val profile = runCatching { profileRepository.fetch(userId) }.getOrNull()
                    poidsObjectifKg = profile?.poidsObjectifKg
                    bfObjectifPct = profile?.bfObjectifPct
                }
            } catch (e: java.io.IOException) {
                errorMessage = "Pas de connexion internet — le dashboard s'affichera dès que le réseau revient."
            } catch (e: Exception) {
                errorMessage = "Impossible de charger le dashboard pour le moment."
            } finally {
                isLoading = false
            }
        }
    }
}
