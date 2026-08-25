package com.example.mmarecomp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mmarecomp.data.FoodRepository
import com.example.mmarecomp.data.MealRepository
import com.example.mmarecomp.data.NutritionTargetRepository
import com.example.mmarecomp.model.Food
import com.example.mmarecomp.model.Meal
import com.example.mmarecomp.model.NewMeal
import com.example.mmarecomp.model.NewNutritionTarget
import com.example.mmarecomp.model.NutritionTarget
import com.example.mmarecomp.model.RepasSlot
import com.example.mmarecomp.model.TypeJour
import com.example.mmarecomp.util.DateUtils
import com.example.mmarecomp.util.NutritionTargetCalculator
import com.example.mmarecomp.util.SlotTarget
import java.time.LocalDate
import kotlinx.coroutines.launch

class MealLogViewModel(
    private val mealRepository: MealRepository = MealRepository(),
    private val targetRepository: NutritionTargetRepository = NutritionTargetRepository(),
    private val foodRepository: FoodRepository = FoodRepository(),
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

    // Bibliothèque d'aliments préchargée (recherche par nom + calcul des
    // macros au grammage saisi, pour pré-remplir le formulaire de repas).
    var foods by mutableStateOf<List<Food>>(emptyList())
        private set
    var foodQuery by mutableStateOf("")
    var foodCategoryFilter by mutableStateOf<String?>(null)
    val foodCategories: List<String> get() = foods.map { it.categorie }.distinct().sorted()
    val filteredFoods: List<Food>
        get() {
            if (foodQuery.isBlank() && foodCategoryFilter == null) return emptyList()
            return foods
                .filter { foodQuery.isBlank() || it.nom.contains(foodQuery, ignoreCase = true) }
                .filter { foodCategoryFilter == null || it.categorie == foodCategoryFilter }
                .take(8)
        }

    fun loadFoods() {
        viewModelScope.launch {
            foods = runCatching { foodRepository.fetchAll() }.getOrDefault(emptyList())
        }
    }

    val totalCalories: Int get() = mealsForDay.sumOf { it.calories }
    val totalProteines: Double get() = mealsForDay.sumOf { it.proteinesG }
    val totalGlucides: Double get() = mealsForDay.sumOf { it.glucidesG }
    val totalLipides: Double get() = mealsForDay.sumOf { it.lipidesG }

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
                mealsForDay = mealRepository.fetchForDate(forDate = dateString)
                target = targetRepository.fetch(forDate = dateString)
            } catch (e: Exception) {
                errorMessage = "Impossible de charger les repas du jour."
            } finally {
                isLoading = false
            }
        }
    }

    fun setTarget(typeJour: TypeJour) {
        viewModelScope.launch {
            val dateString = DateUtils.string(date)
            val computed = NutritionTargetCalculator.target(typeJour)
            val newTarget = NewNutritionTarget(
                date = dateString,
                typeJour = typeJour,
                caloriesCible = computed.calories,
                proteinesCibleG = computed.proteinesG,
            )
            target = runCatching { targetRepository.set(newTarget) }.getOrNull()
        }
    }

    /** Cible personnalisée saisie librement — les préréglages training/repos
     *  restent la valeur par défaut recommandée, mais rien n'empêche
     *  d'ajuster manuellement si besoin. */
    fun setCustomTarget(calories: Int, proteinesG: Double) {
        viewModelScope.launch {
            val newTarget = NewNutritionTarget(
                date = DateUtils.string(date),
                typeJour = target?.typeJour ?: TypeJour.Training,
                caloriesCible = calories,
                proteinesCibleG = proteinesG,
            )
            target = runCatching { targetRepository.set(newTarget) }.getOrNull()
        }
    }

    /** Suppression optimiste : retire immédiatement de la liste, restaure en
     *  cas d'échec réseau. Permet le pattern "undo" côté écran. */
    fun deleteMeal(meal: Meal, onDeleted: () -> Unit) {
        val previous = mealsForDay
        mealsForDay = mealsForDay.filterNot { it.id == meal.id }
        viewModelScope.launch {
            try {
                mealRepository.delete(meal.id)
                onDeleted()
            } catch (e: Exception) {
                mealsForDay = previous
                errorMessage = "Impossible de supprimer ce repas."
            }
        }
    }

    /** Réenregistre un repas supprimé par erreur (action "Annuler" du snackbar). */
    fun restoreMeal(meal: Meal) {
        viewModelScope.launch {
            val restored = NewMeal(
                date = meal.date,
                repas = meal.repas,
                calories = meal.calories,
                proteinesG = meal.proteinesG,
                glucidesG = meal.glucidesG,
                lipidesG = meal.lipidesG,
                description = meal.description,
            )
            try {
                val saved = mealRepository.log(restored)
                mealsForDay = (mealsForDay.filterNot { it.repas == saved.repas } + saved).sortedBy { it.repas }
            } catch (e: Exception) {
                errorMessage = "Impossible de restaurer ce repas."
            }
        }
    }

    /** Reprend le repas loggé hier sur ce créneau, pour aujourd'hui — évite
     *  de re-saisir un repas répétitif. Ne fait rien si rien n'a été loggé
     *  hier sur ce créneau. */
    fun duplicateFromYesterday(slot: RepasSlot, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val yesterday = DateUtils.string(date.minusDays(1))
                val match = mealRepository.fetchForDate(yesterday).firstOrNull { it.repas == slot.value }
                if (match == null) {
                    onResult(false)
                    return@launch
                }
                val newMeal = NewMeal(
                    date = DateUtils.string(date),
                    repas = slot.value,
                    calories = match.calories,
                    proteinesG = match.proteinesG,
                    glucidesG = match.glucidesG,
                    lipidesG = match.lipidesG,
                    description = match.description,
                )
                val saved = mealRepository.log(newMeal)
                mealsForDay = (mealsForDay.filterNot { it.repas == slot.value } + saved).sortedBy { it.repas }
                onResult(true)
            } catch (e: Exception) {
                errorMessage = "Impossible de dupliquer le repas d'hier."
                onResult(false)
            }
        }
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
                errorMessage = "Impossible d'enregistrer ce repas."
                onResult(false)
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
}
