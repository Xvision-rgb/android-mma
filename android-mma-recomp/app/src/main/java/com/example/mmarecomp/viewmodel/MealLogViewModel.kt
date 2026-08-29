package com.example.mmarecomp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mmarecomp.data.FoodRepository
import com.example.mmarecomp.data.MealRepository
import com.example.mmarecomp.data.NutritionTargetRepository
import com.example.mmarecomp.data.ProfileRepository
import com.example.mmarecomp.data.WeighInRepository
import com.example.mmarecomp.model.CalorieMode
import com.example.mmarecomp.model.Food
import com.example.mmarecomp.model.Meal
import com.example.mmarecomp.model.NewMeal
import com.example.mmarecomp.model.NewNutritionTarget
import com.example.mmarecomp.model.NutritionTarget
import com.example.mmarecomp.model.RepasSlot
import com.example.mmarecomp.model.TypeJour
import com.example.mmarecomp.util.CalorieCalculator
import com.example.mmarecomp.util.ConfianceSuivi
import com.example.mmarecomp.util.DailyTarget
import com.example.mmarecomp.util.DateUtils
import com.example.mmarecomp.util.DisponibiliteEnergetique
import com.example.mmarecomp.util.EnergyAvailability
import com.example.mmarecomp.util.LoggingConfidence
import com.example.mmarecomp.util.NutritionTargetCalculator
import com.example.mmarecomp.util.SlotTarget
import java.time.LocalDate
import kotlinx.coroutines.launch

class MealLogViewModel(
    private val userId: String = "",
    private val mealRepository: MealRepository = MealRepository(),
    private val targetRepository: NutritionTargetRepository = NutritionTargetRepository(),
    private val foodRepository: FoodRepository = FoodRepository(),
    private val weighInRepository: WeighInRepository = WeighInRepository(),
    private val profileRepository: ProfileRepository = ProfileRepository(),
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
    var recentMeals by mutableStateOf<List<Meal>>(emptyList())
        private set

    /** Poids et %BF de la dernière pesée connue — mémorisés au calcul de la
     *  cible pour que la disponibilité énergétique n'ait pas à refaire un
     *  aller-retour réseau. */
    var poidsCorpsKg by mutableStateOf<Double?>(null)
        private set
    var bfPct by mutableStateOf<Double?>(null)
        private set

    /** Charge interne de la journée (session-RPE × durée), injectée par
     *  l'écran depuis les séances du jour. Module la périodisation glucidique
     *  et sert de base à l'estimation de dépense d'exercice. */
    var chargeInterneDuJour by mutableStateOf<Double?>(null)

    val caloriesDuJour: Int get() = mealsForDay.sumOf { it.calories }

    /** Disponibilité énergétique du jour : ce qui reste une fois
     *  l'entraînement payé. Null tant que le poids n'est pas connu — on
     *  n'invente pas une masse maigre. */
    val disponibiliteEnergetique: DisponibiliteEnergetique?
        get() {
            val poids = poidsCorpsKg ?: return null
            val masseMaigre = CalorieCalculator.leanMassKg(poids, bfPct)
            val depense = chargeInterneDuJour
                ?.let { EnergyAvailability.depuisChargeInterne(it) }
                ?: 0
            return EnergyAvailability.calculer(caloriesDuJour, depense, masseMaigre)
        }

    /** Complétude du suivi sur deux semaines — conditionne le recalibrage
     *  adaptatif et le niveau de confiance affiché. */
    val confianceSuivi: ConfianceSuivi
        get() = LoggingConfidence.evaluer(recentMeals + mealsForDay, jours = 14)

    /** Repas des deux dernières semaines, hors jour actuellement affiché —
     *  pour l'historique dépliable (lecture seule pour l'instant, la
     *  suppression du jour courant reste gérée par mealsForDay/deleteMeal). */
    fun loadRecentHistory() {
        viewModelScope.launch {
            try {
                recentMeals = mealRepository.fetchSince(DateUtils.daysAgo(13))
                    .filter { it.date != DateUtils.string(date) }
                    .sortedWith(compareByDescending<Meal> { it.date }.thenBy { it.repas })
            } catch (e: Exception) {
                // Historique secondaire : un échec ici n'empêche pas d'utiliser l'écran.
            }
        }
    }

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

    /** Jusqu'à 5 aliments distincts les plus récemment loggés, déduits des
     *  descriptions déjà chargées (mealsForDay + recentMeals) — pas de
     *  requête serveur supplémentaire, cf. MyFitnessPal qui met en avant les
     *  aliments récents pour éviter de re-rechercher chaque jour. Limite
     *  assumée : une description tapée à la main (descriptionIsAuto=false
     *  côté écran) peut produire une entrée qui ne correspond à aucun
     *  aliment de la bibliothèque — sans conséquence, le clic dessus se
     *  comporte alors comme une recherche simple sans résultat. */
    val recentFoodLabels: List<String>
        get() = (mealsForDay + recentMeals)
            .flatMap { it.description.orEmpty().split(",") }
            .map { it.trim() }
            .filter { it.isNotBlank() && it != "Repas repris" }
            .distinct()
            .take(5)

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
            } catch (e: java.io.IOException) {
                errorMessage = "Pas de connexion internet — réessaie dès que le réseau revient."
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
            val computed = personalizedTarget(typeJour)
            val newTarget = NewNutritionTarget(
                date = dateString,
                typeJour = typeJour,
                caloriesCible = computed.calories,
                proteinesCibleG = computed.proteinesG,
                glucidesCibleG = computed.glucidesG.takeIf { it > 0 },
                lipidesCibleG = computed.lipidesG.takeIf { it > 0 },
            )
            target = runCatching { targetRepository.set(newTarget) }.getOrNull()
        }
    }

    /** Cible personnalisée à partir du poids réel (dernière pesée) et du mode
     *  choisi dans le profil, plutôt que les valeurs génériques figées qui
     *  sous-estimaient largement la dépense d'un pratiquant de sport de
     *  combat (ex. ~2000 cal au lieu de ~3900 pour 87kg à intensité
     *  training/hypertrophie 6j/semaine). Retombe sur les anciennes valeurs
     *  figées si aucune pesée n'est encore enregistrée — jamais d'erreur
     *  bloquante faute de données. */
    private suspend fun personalizedTarget(typeJour: TypeJour): DailyTarget {
        val latestWeighIn = runCatching { weighInRepository.fetch(DateUtils.daysAgo(30)) }
            .getOrDefault(emptyList())
            .lastOrNull { it.poidsKg > 0 }
            ?: return NutritionTargetCalculator.target(typeJour)
        val mode = if (userId.isNotBlank()) {
            runCatching { profileRepository.fetch(userId) }.getOrNull()?.objectifCalorieMode
                ?: CalorieMode.Recomposition
        } else {
            CalorieMode.Recomposition
        }
        val goal = CalorieCalculator.goal(latestWeighIn.poidsKg, latestWeighIn.bfPct, mode)
        poidsCorpsKg = latestWeighIn.poidsKg
        bfPct = latestWeighIn.bfPct
        return NutritionTargetCalculator.targetFor(
            typeJour = typeJour,
            baseCalories = goal.targetCalories,
            proteinesG = goal.proteinesG,
            lipidesG = goal.lipidesG,
            poidsKg = latestWeighIn.poidsKg,
            chargeInterne = chargeInterneDuJour,
        )
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

    /** Cherche le repas loggé hier sur ce créneau — pour proposer de le
     *  reprendre dans le formulaire en cours (pas d'enregistrement direct
     *  ici : c'est l'écran qui l'ajoute au repas en cours de composition,
     *  pour ne jamais écraser silencieusement un repas déjà loggé aujourd'hui
     *  sur ce créneau, ni ce que l'utilisateur a déjà commencé à saisir). */
    fun findYesterdayMeal(slot: RepasSlot, onResult: (Meal?) -> Unit) {
        viewModelScope.launch {
            try {
                val yesterday = DateUtils.string(date.minusDays(1))
                val match = mealRepository.fetchForDate(yesterday).firstOrNull { it.repas == slot.value }
                onResult(match)
            } catch (e: Exception) {
                errorMessage = "Impossible de reprendre le repas d'hier."
                onResult(null)
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
}
