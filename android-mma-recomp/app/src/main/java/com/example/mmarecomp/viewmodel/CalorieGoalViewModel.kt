package com.example.mmarecomp.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mmarecomp.data.MealRepository
import com.example.mmarecomp.data.NutritionTargetRepository
import com.example.mmarecomp.data.ProfileRepository
import com.example.mmarecomp.data.WeighInRepository
import com.example.mmarecomp.model.CalorieMode
import com.example.mmarecomp.model.ProfileUpdate
import com.example.mmarecomp.model.TypeJour
import com.example.mmarecomp.model.WeighIn
import com.example.mmarecomp.model.WeighInType
import com.example.mmarecomp.util.AdaptiveRecalibration
import com.example.mmarecomp.util.CalorieCalculator
import com.example.mmarecomp.util.CalorieGoal
import com.example.mmarecomp.util.ContextePreference
import com.example.mmarecomp.util.DateUtils
import com.example.mmarecomp.util.LoggingConfidence
import com.example.mmarecomp.util.MovingAverage
import com.example.mmarecomp.util.NutritionTargetDraft
import com.example.mmarecomp.util.TrendPoint
import com.example.mmarecomp.util.WeighInSelector
import java.time.temporal.ChronoUnit
import com.example.mmarecomp.util.rethrowCancellation
import kotlinx.coroutines.launch

class CalorieGoalViewModel(
    private val userId: String = "",
    private val weighInRepository: WeighInRepository = WeighInRepository(),
    private val profileRepository: ProfileRepository = ProfileRepository(),
    private val targetRepository: NutritionTargetRepository = NutritionTargetRepository(),
    private val mealRepository: MealRepository = MealRepository(),
    context: Context? = null,
) : ViewModel() {
    private val contextePreference = context?.let { ContextePreference(it) }

    private val activityMultiplier: Double
        get() = contextePreference?.let { CalorieCalculator.multiplicateurPour(it.contexte) }
            ?: CalorieCalculator.ACTIVITY_MULTIPLIER_DEFAULT
    var poidsKg by mutableStateOf<Double?>(null)
        private set
    var bfPct by mutableStateOf<Double?>(null)
        private set
    var appliedMode by mutableStateOf<CalorieMode?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var isSaving by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var savedConfirmation by mutableStateOf(false)
        private set
    var lastAppliedTargetCalories by mutableStateOf<Int?>(null)
        private set

    /** Recalibrage adaptatif façon MacroFactor — null tant qu'il n'y a pas
     *  assez de données (14j+) ou que l'écart avec la maintenance formulée
     *  n'est pas assez significatif pour valoir la peine d'être proposé. */
    var recalibration by mutableStateOf<AdaptiveRecalibration?>(null)
        private set

    /** Champs éditables : pré-remplis depuis la dernière pesée si dispo, mais
     *  toujours modifiables — utile tant qu'aucune pesée n'a encore été
     *  enregistrée, ou pour simuler un autre poids/%BF sans re-peser. */
    var poidsInputKg by mutableStateOf("")
    var bfInputPct by mutableStateOf("")

    val recommendedMode: CalorieMode
        // Normalise la virgule décimale comme goalFor()/applyMode() : sans ça,
        // « 12,5 » n'était pas parsé et le mode recommandé retombait à tort sur
        // Recomposition même au-dessus du seuil de %BF de la coupe.
        get() = CalorieCalculator.recommendedMode(bfInputPct.replace(",", ".").toDoubleOrNull())

    /** Cible complète pour un mode donné, à partir des champs actuellement
     *  saisis — null si le poids n'est pas un nombre valide. */
    fun goalFor(mode: CalorieMode): CalorieGoal? {
        val poids = poidsInputKg.replace(",", ".").toDoubleOrNull() ?: return null
        val bf = bfInputPct.replace(",", ".").toDoubleOrNull()
        return CalorieCalculator.goal(poids, bf, mode, activityMultiplier)
    }

    fun load() {
        isLoading = true
        errorMessage = null
        viewModelScope.launch {
            try {
                val recent = weighInRepository.fetch(DateUtils.daysAgo(30))
                val latest = WeighInSelector.latestReference(recent)
                poidsKg = latest?.poidsKg
                bfPct = latest?.bfPct
                poidsInputKg = latest?.poidsKg?.toString() ?: ""
                bfInputPct = latest?.bfPct?.toString() ?: ""
                if (userId.isNotBlank()) {
                    appliedMode = runCatching { profileRepository.fetch(userId) }.getOrNull()?.objectifCalorieMode
                }
                lastAppliedTargetCalories = runCatching {
                    targetRepository.fetch(DateUtils.today())?.caloriesCible
                }.getOrNull()
                latest?.poidsKg?.let { poids -> loadRecalibration(poids, recent) }
            } catch (e: Throwable) {
                rethrowCancellation(e)
                when (e) {
                    is java.io.IOException -> {
                errorMessage = "Pas de connexion internet — réessaie dès que le réseau revient."
                    }
                    else -> {
                errorMessage = "Impossible de charger tes dernières mesures."
                    }
                }
            } finally {
                isLoading = false
            }
        }
    }

    /** Recalibrage adaptatif façon MacroFactor : compare la tendance de
     *  poids réelle (moyenne mobile 7j, jamais brute) aux calories
     *  réellement loguées sur la même période, plutôt que de se fier
     *  uniquement à la formule poids × 30 × multiplicateur.
     *  N'affiche rien tant que la fenêtre disponible est sous 14 jours. */
    private suspend fun loadRecalibration(currentPoidsKg: Double, recentWeighIns: List<WeighIn>) {
        recalibration = null
        val points = recentWeighIns
            .filter { it.type == WeighInType.MatinJeun }
            .mapNotNull { w -> DateUtils.date(w.date)?.let { TrendPoint(it, w.poidsKg) } }
        if (points.isEmpty()) return
        val trend = MovingAverage.sevenDay(points)
        if (trend.size < 2) return
        val first = trend.first()
        val last = trend.last()
        val periodDays = ChronoUnit.DAYS.between(first.date, last.date).toInt()
        if (periodDays < 14) return

        val meals = runCatching { mealRepository.fetchSince(DateUtils.string(first.date)) }
            .getOrDefault(emptyList())
            .filter { meal ->
                val d = DateUtils.date(meal.date) ?: return@filter false
                !d.isBefore(first.date) && !d.isAfter(last.date)
            }
        val caloriesByDate = meals.groupBy { it.date }.mapValues { (_, dayMeals) -> dayMeals.sumOf { it.calories } }
        if (caloriesByDate.isEmpty()) return
        val avgLoggedCalories = caloriesByDate.values.average()

        val staticMaintenance = CalorieCalculator.maintenanceCalories(currentPoidsKg, activityMultiplier)
        val confiance = LoggingConfidence.evaluer(meals, periodDays)
        val result = CalorieCalculator.adaptiveRecalibration(
            weightChangeKg = last.value - first.value,
            periodDays = periodDays,
            avgLoggedCalories = avgLoggedCalories,
            staticMaintenanceCalories = staticMaintenance,
            completudeSuivi = confiance.completude,
        ) ?: return
        if (CalorieCalculator.isRecalibrationSignificant(result)) {
            recalibration = result
        }
    }

    /** Applique la dépense réelle estimée par le recalibrage comme nouvelle
     *  base de maintenance pour le mode actuel, au lieu de la formule
     *  statique — mêmes offsets/macros/plancher que applyMode(). */
    fun applyRecalibration() {
        val poids = poidsInputKg.replace(",", ".").toDoubleOrNull() ?: return
        val bf = bfInputPct.replace(",", ".").toDoubleOrNull()
        val estimated = recalibration?.estimatedExpenditureCalories ?: return
        val mode = appliedMode ?: recommendedMode
        isSaving = true
        savedConfirmation = false
        errorMessage = null
        viewModelScope.launch {
            try {
                val goal = CalorieCalculator.goalFromMaintenance(estimated, poids, bf, mode)
                targetRepository.set(
                    NutritionTargetDraft.fromGoal(DateUtils.today(), TypeJour.Training, goal),
                )
                recalibration = null
                savedConfirmation = true
            } catch (e: Throwable) {
                rethrowCancellation(e)
                errorMessage = "Impossible d'appliquer le recalibrage — réessaie."
            } finally {
                isSaving = false
            }
        }
    }

    /** Enregistre le mode choisi dans le profil ET pousse la cible du jour
     *  correspondante — même mécanisme que "Cible personnalisée" côté écran
     *  Repas, pour que le changement soit visible immédiatement. */
    fun applyMode(mode: CalorieMode) {
        val goal = goalFor(mode) ?: return
        isSaving = true
        savedConfirmation = false
        errorMessage = null
        viewModelScope.launch {
            try {
                if (userId.isNotBlank()) {
                    profileRepository.update(userId, ProfileUpdate(objectifCalorieMode = mode))
                }
                targetRepository.set(
                    NutritionTargetDraft.fromGoal(DateUtils.today(), TypeJour.Training, goal),
                )
                appliedMode = mode
                lastAppliedTargetCalories = goal.targetCalories
                savedConfirmation = true
            } catch (e: Throwable) {
                rethrowCancellation(e)
                errorMessage = "Impossible d'enregistrer ce mode — réessaie."
            } finally {
                isSaving = false
            }
        }
    }

    /** Réapplique la dernière cible enregistrée pour aujourd'hui (même mode actif). */
    fun restoreLastTarget() {
        val mode = appliedMode ?: recommendedMode
        goalFor(mode) ?: return
        applyMode(mode)
    }
}
