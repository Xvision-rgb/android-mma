package com.example.mmarecomp.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mmarecomp.data.MealPreset
import com.example.mmarecomp.data.MealPresetStore
import com.example.mmarecomp.data.MealRepository
import com.example.mmarecomp.data.NutritionTargetRepository
import com.example.mmarecomp.model.Meal
import com.example.mmarecomp.model.NewMeal
import com.example.mmarecomp.model.NewNutritionTarget
import com.example.mmarecomp.model.NutritionTarget
import com.example.mmarecomp.model.Phase
import com.example.mmarecomp.model.RepasSlot
import com.example.mmarecomp.model.TypeJour
import com.example.mmarecomp.ui.AppPreferencesState
import com.example.mmarecomp.util.DateUtils
import com.example.mmarecomp.util.NutritionTargetCalculator
import com.example.mmarecomp.util.SlotTarget
import com.example.mmarecomp.util.toFriendlyMessage
import java.time.LocalDate
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class MealLogViewModel(
    private val mealRepository: MealRepository = MealRepository(),
    private val targetRepository: NutritionTargetRepository = NutritionTargetRepository(),
) : ViewModel() {
    var date by mutableStateOf(LocalDate.now())
    var mealsForDay by mutableStateOf<List<Meal>>(emptyList())
        private set
    var target by mutableStateOf<NutritionTarget?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var isRepeatingYesterday by mutableStateOf(false)
        private set

    /** Repas favoris réutilisables (stockage local, voir MealPresetStore). */
    var presets by mutableStateOf<List<MealPreset>>(emptyList())
        private set

    val totalCalories: Int get() = mealsForDay.sumOf { it.calories }
    val totalProteines: Double get() = mealsForDay.sumOf { it.proteinesG }

    val indicativeSplit: Map<RepasSlot, SlotTarget>
        get() {
            val t = target ?: return emptyMap()
            return NutritionTargetCalculator.indicativeSplit(t.caloriesCible, t.proteinesCibleG, RepasSlot.entries)
        }

    fun load() {
        isLoading = true
        errorMessage = null
        viewModelScope.launch {
            val dateString = DateUtils.string(date)
            try {
                coroutineScope {
                    val mealsDeferred = async { mealRepository.fetch(forDate = dateString) }
                    val targetDeferred = async { targetRepository.fetch(forDate = dateString) }
                    mealsForDay = mealsDeferred.await()
                    target = targetDeferred.await()
                }
            } catch (e: Exception) {
                errorMessage = e.toFriendlyMessage("Impossible de charger les repas du jour.")
            } finally {
                isLoading = false
            }
        }
    }

    fun setTarget(typeJour: TypeJour, phase: Phase? = null) {
        viewModelScope.launch {
            val dateString = DateUtils.string(date)
            val effectivePhase = phase.takeIf { AppPreferencesState.preferences.value.nutritionTargetByPhase }
            val computed = NutritionTargetCalculator.target(typeJour, effectivePhase)
            val newTarget = NewNutritionTarget(
                date = dateString,
                typeJour = typeJour,
                caloriesCible = computed.calories,
                proteinesCibleG = computed.proteinesG,
            )
            target = runCatching { targetRepository.set(newTarget) }.getOrNull()
        }
    }

    /** Cible saisie à la main plutôt que calculée — pour les utilisateurs qui
     *  préfèrent piloter leurs macros eux-mêmes (préférence "Cibles nutrition
     *  automatiques" coupée). Le type_jour est requis par le schéma mais n'a
     *  plus de sens une fois la cible surchargée : Training est stocké sans
     *  incidence sur l'affichage. */
    fun setManualTarget(calories: Int, proteinesG: Double, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val dateString = DateUtils.string(date)
            val newTarget = NewNutritionTarget(
                date = dateString,
                typeJour = TypeJour.Training,
                caloriesCible = calories,
                proteinesCibleG = proteinesG,
            )
            try {
                target = targetRepository.set(newTarget)
                onResult(true)
            } catch (e: Exception) {
                errorMessage = e.toFriendlyMessage("Impossible d'enregistrer cette cible.")
                onResult(false)
            }
        }
    }

    /** Ajoute une collation rapide au créneau donné — comme le schéma n'a
     *  qu'une entrée par (date, créneau), une collation sur un créneau déjà
     *  loggué s'ADDITIONNE au repas existant plutôt que de l'écraser. */
    fun quickAddSnack(slot: RepasSlot, name: String, calories: Int, proteinesG: Double, onResult: (Boolean) -> Unit = {}) {
        val existing = mealsForDay.firstOrNull { it.repas == slot.value }
        val combinedDescription = listOfNotNull(existing?.description, name).joinToString(" + ")
        logMeal(
            slot = slot,
            calories = (existing?.calories ?: 0) + calories,
            proteinesG = (existing?.proteinesG ?: 0.0) + proteinesG,
            glucidesG = existing?.glucidesG ?: 0.0,
            lipidesG = existing?.lipidesG ?: 0.0,
            description = combinedDescription,
            onResult = onResult,
        )
    }

    fun logMeal(
        slot: RepasSlot,
        calories: Int,
        proteinesG: Double,
        glucidesG: Double,
        lipidesG: Double,
        description: String,
        onResult: (Boolean) -> Unit,
    ) {
        if (calories !in 0..5000) {
            errorMessage = "Les calories doivent être entre 0 et 5000."
            onResult(false)
            return
        }
        if (listOf(proteinesG, glucidesG, lipidesG).any { it < 0.0 || it > 500.0 }) {
            errorMessage = "Une des valeurs (protéines/glucides/lipides) semble hors limites (0 à 500g)."
            onResult(false)
            return
        }

        viewModelScope.launch {
            val newMeal = NewMeal(
                date = DateUtils.string(date),
                repas = slot.value,
                calories = calories,
                proteinesG = proteinesG,
                glucidesG = glucidesG,
                lipidesG = lipidesG,
                description = description.ifBlank { null },
            )
            try {
                val saved = mealRepository.log(newMeal)
                mealsForDay = (mealsForDay.filterNot { it.repas == slot.value } + saved).sortedBy { it.repas }
                onResult(true)
            } catch (e: Exception) {
                errorMessage = e.toFriendlyMessage("Impossible d'enregistrer ce repas.")
                onResult(false)
            }
        }
    }

    /** Retrait optimiste pour l'UI — n'efface rien côté serveur. À combiner
     *  avec un snackbar "Annuler" : [commitDeleteMeal] si confirmé,
     *  [restoreMeal] sinon. */
    fun removeMealLocally(meal: Meal) {
        mealsForDay = mealsForDay.filterNot { it.id == meal.id }
    }

    fun restoreMeal(meal: Meal) {
        mealsForDay = (mealsForDay.filterNot { it.id == meal.id } + meal).sortedBy { it.repas }
    }

    fun commitDeleteMeal(meal: Meal) {
        viewModelScope.launch {
            try {
                mealRepository.delete(meal.id)
            } catch (e: Exception) {
                errorMessage = e.toFriendlyMessage("Impossible de supprimer ce repas.")
                restoreMeal(meal)
            }
        }
    }

    /** Recopie les repas d'hier qui n'ont pas déjà un équivalent aujourd'hui
     *  (créneau par créneau) — pour les journées où le rythme ne change pas.
     *  `onResult` reçoit le nombre de repas recopiés (0 si rien à faire ou
     *  en cas d'erreur). N'écrase jamais un repas déjà loggué aujourd'hui. */
    fun repeatYesterday(onResult: (Int) -> Unit) {
        isRepeatingYesterday = true
        viewModelScope.launch {
            try {
                val yesterday = DateUtils.string(date.minusDays(1))
                val yesterdayMeals = mealRepository.fetch(forDate = yesterday)
                val alreadyLoggedSlots = mealsForDay.map { it.repas }.toSet()
                val toRepeat = yesterdayMeals.filter { it.repas !in alreadyLoggedSlots }

                var repeated = mealsForDay
                var count = 0
                for (meal in toRepeat) {
                    val newMeal = NewMeal(
                        date = DateUtils.string(date),
                        repas = meal.repas,
                        calories = meal.calories,
                        proteinesG = meal.proteinesG,
                        glucidesG = meal.glucidesG,
                        lipidesG = meal.lipidesG,
                        description = meal.description,
                    )
                    val saved = mealRepository.log(newMeal)
                    repeated = (repeated.filterNot { it.repas == saved.repas } + saved).sortedBy { it.repas }
                    count++
                }
                mealsForDay = repeated
                onResult(count)
            } catch (e: Exception) {
                errorMessage = e.toFriendlyMessage("Impossible de répéter les repas d'hier.")
                onResult(0)
            } finally {
                isRepeatingYesterday = false
            }
        }
    }

    /** Alerte douce : plusieurs jours d'affilée nettement en dessous de la
     *  cible. Ne culpabilise jamais un jour isolé en dessous de l'objectif. */
    fun softUnderTargetAlert(recentDailyTotals: List<Triple<String, Int, Int>>): Boolean {
        val lastThree = recentDailyTotals.takeLast(3)
        if (lastThree.size != 3) return false
        return lastThree.all { (_, calories, cible) -> calories < (cible * 0.85).toInt() }
    }

    fun loadPresets(context: Context) {
        presets = MealPresetStore(context).load()
    }

    fun saveCurrentAsPreset(
        name: String,
        calories: Int,
        proteinesG: Double,
        glucidesG: Double,
        lipidesG: Double,
        description: String,
        context: Context,
    ) {
        if (name.isBlank()) return
        val updated = presets.filterNot { it.name == name } +
            MealPreset(
                name = name,
                calories = calories,
                proteinesG = proteinesG,
                glucidesG = glucidesG,
                lipidesG = lipidesG,
                description = description.ifBlank { null },
            )
        MealPresetStore(context).save(updated)
        presets = updated
    }

    fun deletePreset(preset: MealPreset, context: Context) {
        val updated = presets.filterNot { it.name == preset.name }
        MealPresetStore(context).save(updated)
        presets = updated
    }
}
