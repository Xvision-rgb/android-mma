package com.example.mmarecomp.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mmarecomp.data.DailyCheckInRepository
import com.example.mmarecomp.data.MealRepository
import com.example.mmarecomp.data.MmaSessionRepository
import com.example.mmarecomp.model.AchievementType
import com.example.mmarecomp.data.NutritionTargetRepository
import com.example.mmarecomp.data.ProfileRepository
import com.example.mmarecomp.data.TrainingPlanRepository
import com.example.mmarecomp.data.WeighInRepository
import com.example.mmarecomp.data.WorkoutRepository
import com.example.mmarecomp.model.DailyCheckIn
import com.example.mmarecomp.model.Meal
import com.example.mmarecomp.model.MmaSession
import com.example.mmarecomp.model.MuscleZone
import com.example.mmarecomp.model.NewDailyCheckIn
import com.example.mmarecomp.model.NewTrainingPlanDay
import com.example.mmarecomp.model.NutritionTarget
import com.example.mmarecomp.model.Phase
import com.example.mmarecomp.model.PlanDayType
import com.example.mmarecomp.model.TrainingPlanDay
import com.example.mmarecomp.model.WeighIn
import com.example.mmarecomp.model.WeighInType
import com.example.mmarecomp.model.Workout
import com.example.mmarecomp.model.WorkoutType
import com.example.mmarecomp.util.ExerciseName
import com.example.mmarecomp.util.DateUtils
import com.example.mmarecomp.util.MovingAverage
import com.example.mmarecomp.util.PlateauDetector
import com.example.mmarecomp.util.AchievementManager
import com.example.mmarecomp.util.PlateauStatus
import com.example.mmarecomp.util.StreakManager
import com.example.mmarecomp.util.TrendDirection
import com.example.mmarecomp.util.ForceRelative
import com.example.mmarecomp.util.BilanVolume
import com.example.mmarecomp.util.ContextePreference
import com.example.mmarecomp.util.EnduranceInterference
import com.example.mmarecomp.util.GripBenchmarks
import com.example.mmarecomp.util.InterferenceChecker
import com.example.mmarecomp.util.OverreachingDetector
import com.example.mmarecomp.util.VolumeLandmarks
import com.example.mmarecomp.util.ModulationSeance
import com.example.mmarecomp.util.MuscleZoneClassifier
import com.example.mmarecomp.util.RelativeStrength
import com.example.mmarecomp.util.TrainingLoad
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
    private val dailyCheckInRepository: DailyCheckInRepository = DailyCheckInRepository(),
    private val mmaSessionRepository: MmaSessionRepository = MmaSessionRepository(),
    private val context: Context? = null,
) : ViewModel() {
    private val streakManager = context?.let { StreakManager(it) }
    private val achievementManager = context?.let { AchievementManager(it) }
    private val contextePreference = context?.let { ContextePreference(it) }

    /** Contexte de pratique. Sans sport de combat, les règles d'interférence
     *  changent de cible (la course remplace le sparring) et la fenêtre de
     *  construction de force s'ouvre. */
    val contexteSportif: com.example.mmarecomp.model.ContexteSportif
        get() = contextePreference?.contexte
            ?: com.example.mmarecomp.model.ContexteSportif.SalleUniquement
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
    var checkInsRecents by mutableStateOf<List<DailyCheckIn>>(emptyList())
        private set
    var workoutsLast28Days by mutableStateOf<List<Workout>>(emptyList())
        private set
    var mmaSessions by mutableStateOf<List<MmaSession>>(emptyList())
        private set
    var unlockedAchievement by mutableStateOf<AchievementType?>(null)

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

    /** Volume total d'entraînement (Σ reps × charge, série par série) cumulé
     *  sur les séances de la semaine — vue synthèse, jamais une comparaison
     *  culpabilisante d'une semaine à l'autre. */
    val weeklyTrainingVolume: Double
        get() = workoutsThisWeek.sumOf { workout ->
            workout.exercices.sumOf { it.volumeTotal }
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

    val currentStreak: Int get() = streakManager?.getCurrentStreak() ?: 0
    val bestStreak: Int get() = streakManager?.getBestStreak() ?: 0

    /** Modulation de la séance du jour, dérivée du check-in réel et de la
     *  charge interne — plus aucune donnée mockée ici. */
    val modulation: ModulationSeance
        get() = TrainingLoad.moduler(
            score = checkInAujourdhui?.score,
            acwr = acwr,
            ecartHrvSigma = TrainingLoad.ecartHrvEnSigma(checkInsRecents),
            joursConsecutifsEnRouge = TrainingLoad.joursConsecutifsEnRouge(checkInsRecents),
        )

    /** Séances sur la fenêtre chronique, sans doublon : workoutsLast28Days
     *  recouvre déjà la semaine en cours, et compter deux fois gonflerait la
     *  charge aiguë. */
    private val workoutsFenetreChronique: List<Workout>
        get() = (workoutsLast28Days + workoutsThisWeek).distinctBy { it.id }

    val acwr: Double?
        get() = TrainingLoad.acwr(TrainingLoad.chargesParJour(workoutsFenetreChronique, mmaSessions))

    val checkInAujourdhui: DailyCheckIn?
        get() = checkInsRecents.firstOrNull { it.date == DateUtils.today() }

    val scoreReadiness: Int? get() = checkInAujourdhui?.score

    /** Répartition du volume par zone sur les séances de la semaine. */
    val repartitionVolume: Map<MuscleZone, Double>
        get() = MuscleZoneClassifier.repartition(workoutsThisWeek.flatMap { it.exercices })

    val ratioTiragePoussee: Double?
        get() = MuscleZoneClassifier.ratioTiragePoussee(workoutsThisWeek.flatMap { it.exercices })

    /** Indicateur directeur : 1RM estimé / poids de corps en moyenne mobile
     *  7 jours. Jamais une pesée brute — le ratio hériterait de son bruit. */
    val forcesRelatives: List<ForceRelative>
        get() = RelativeStrength.parExercice(
            workouts = workoutsFenetreChronique,
            poidsCorpsKg = weightTrend7Day.lastOrNull()?.value,
        )

    /** Conflits de programmation du jour. La source change avec le contexte :
     *  sans sport de combat, c'est la course qui interfère avec la force du
     *  bas du corps, pas le sparring. */
    val conflitsProgrammation: List<String>
        get() = if (contexteSportif.sansCombat) {
            EnduranceInterference.conflits(java.time.LocalDate.now(), workoutsThisWeek) +
                listOfNotNull(EnduranceInterference.noteVolumeCourse(sortiesEnduranceCetteSemaine))
        } else {
            InterferenceChecker.conflits(
                date = java.time.LocalDate.now(),
                workouts = workoutsThisWeek,
                mmaSessions = mmaSessions,
            )
        }

    private val sortiesEnduranceCetteSemaine: Int
        get() = workoutsThisWeek.count {
            it.type == WorkoutType.Course || it.type == WorkoutType.Hiit
        }

    /** Séries par zone face aux repères de volume — le moteur réel de
     *  l'adaptation, que le tonnage en kg ne mesurait pas. */
    val bilanVolume: List<BilanVolume>
        get() = VolumeLandmarks.bilan(workoutsFenetreChronique)

    val zonesADevelopper: List<BilanVolume>
        get() = VolumeLandmarks.zonesADevelopper(workoutsThisWeek)

    /** Dernier dead hang mesuré et sa lecture. La poigne étant le facteur
     *  limitant du tirage, c'est une métrique de première classe : elle était
     *  stockée depuis le check-in mais jamais relue. */
    val deadHangSec: Int?
        get() = checkInsRecents.sortedBy { it.date }.lastOrNull { it.deadHangSec != null }?.deadHangSec

    val lecturePoigne: String? get() = deadHangSec?.let { GripBenchmarks.lecture(it) }

    /** Progression du dead hang entre la première et la dernière mesure. */
    val progressionDeadHangSec: Int?
        get() {
            val mesures = checkInsRecents.sortedBy { it.date }.mapNotNull { it.deadHangSec }
            if (mesures.size < 2) return null
            return mesures.last() - mesures.first()
        }

    /** Exercices dont la charge recule plusieurs séances de suite : signe que
     *  le volume dépasse ce qui est récupérable. */
    val alertesSurcharge: List<String>
        get() = workoutsFenetreChronique
            .flatMap { it.exercices }
            .filter { it.nom.isNotBlank() }
            .groupBy { ExerciseName.cle(it.nom) }
            .mapNotNull { (_, exercices) ->
                val charges = exercices.mapNotNull { it.chargeMaxKg }
                OverreachingDetector.alerte(exercices.first().nom, charges)
            }

    val weightTrendingDown: Boolean get() = weightTrendDirection == TrendDirection.BAISSE
    val activeIntensityPercent: Int
        get() {
            if (workoutsThisWeek.isEmpty()) return 0
            val cleanWorkouts = workoutsThisWeek.count { w -> w.exercices.any { it.propre } }
            return (cleanWorkouts * 100) / workoutsThisWeek.size
        }

    val daysSinceLastRest: Int
        get() {
            val allLoggedDates = buildSet {
                addAll(mealsLast7Days.map { it.date })
                addAll(workoutsThisWeek.map { it.date })
                addAll(morningWeighIns.map { it.date })
            }
            var cursor = java.time.LocalDate.now()
            var days = 0
            while (allLoggedDates.contains(DateUtils.string(cursor))) {
                days++
                cursor = cursor.minusDays(1)
            }
            return days
        }

    val suggestedExercise: Pair<String, String>?
        get() {
            if (planThisWeek.isEmpty()) return null
            val exercises = planThisWeek.flatMap { day -> day.exercices.map { it.nom to day.type.label } }
            return exercises.randomOrNull()
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
        streakManager?.updateStreak()
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

                // Fenêtre de 28 jours : c'est la base chronique de l'ACWR.
                // Une fenêtre plus courte rendrait le ratio instable.
                val vingtHuitJours = DateUtils.daysAgo(28)
                workoutsLast28Days = runCatching {
                    workoutRepository.fetchWeek(vingtHuitJours)
                }.getOrDefault(emptyList())
                checkInsRecents = runCatching {
                    dailyCheckInRepository.fetchSince(vingtHuitJours)
                }.getOrDefault(emptyList())
                mmaSessions = runCatching {
                    mmaSessionRepository.fetchSince(vingtHuitJours)
                }.getOrDefault(emptyList())
                if (userId.isNotBlank()) {
                    val profile = runCatching { profileRepository.fetch(userId) }.getOrNull()
                    poidsObjectifKg = profile?.poidsObjectifKg
                    bfObjectifPct = profile?.bfObjectifPct
                }

                checkAchievements()
            } catch (e: java.io.IOException) {
                errorMessage = "Pas de connexion internet — le dashboard s'affichera dès que le réseau revient."
            } catch (e: Exception) {
                errorMessage = "Impossible de charger le dashboard pour le moment."
            } finally {
                isLoading = false
            }
        }
    }

    /** Enregistre le point de forme du matin. Échoue silencieusement côté
     *  UI plutôt que de bloquer le dashboard : un check-in raté ne doit
     *  jamais empêcher de consulter ses données. */
    fun enregistrerCheckIn(
        sommeil: Int,
        courbatures: Int,
        fatigue: Int,
        humeur: Int,
        stress: Int,
        hrvRmssd: Double?,
        deadHangSec: Int?,
    ) {
        viewModelScope.launch {
            val nouveau = NewDailyCheckIn(
                date = DateUtils.today(),
                sommeil = sommeil.coerceIn(1, 5),
                courbatures = courbatures.coerceIn(1, 5),
                fatigue = fatigue.coerceIn(1, 5),
                humeur = humeur.coerceIn(1, 5),
                stress = stress.coerceIn(1, 5),
                hrvRmssd = hrvRmssd,
                deadHangSec = deadHangSec,
            )
            try {
                val enregistre = dailyCheckInRepository.log(nouveau)
                checkInsRecents = checkInsRecents.filterNot { it.date == enregistre.date } + enregistre
            } catch (e: Exception) {
                errorMessage = "Impossible d'enregistrer le point du jour."
            }
        }
    }

    private fun checkAchievements() {
        achievementManager?.let {
            if (it.checkAndUnlockFirstWorkout(workoutsThisWeek.isNotEmpty())) {
                unlockedAchievement = AchievementType.FIRST_WORKOUT
            } else if (it.checkAndUnlockFiveConsecutiveDays(currentStreak)) {
                unlockedAchievement = AchievementType.FIVE_CONSECUTIVE_DAYS
            }
        }
    }
}
