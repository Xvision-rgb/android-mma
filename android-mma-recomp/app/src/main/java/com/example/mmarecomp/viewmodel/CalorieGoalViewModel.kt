package com.example.mmarecomp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mmarecomp.data.NutritionTargetRepository
import com.example.mmarecomp.data.ProfileRepository
import com.example.mmarecomp.data.WeighInRepository
import com.example.mmarecomp.model.CalorieMode
import com.example.mmarecomp.model.NewNutritionTarget
import com.example.mmarecomp.model.ProfileUpdate
import com.example.mmarecomp.model.TypeJour
import com.example.mmarecomp.util.CalorieCalculator
import com.example.mmarecomp.util.CalorieGoal
import com.example.mmarecomp.util.DateUtils
import kotlinx.coroutines.launch

class CalorieGoalViewModel(
    private val userId: String = "",
    private val weighInRepository: WeighInRepository = WeighInRepository(),
    private val profileRepository: ProfileRepository = ProfileRepository(),
    private val targetRepository: NutritionTargetRepository = NutritionTargetRepository(),
) : ViewModel() {
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

    /** Champs éditables : pré-remplis depuis la dernière pesée si dispo, mais
     *  toujours modifiables — utile tant qu'aucune pesée n'a encore été
     *  enregistrée, ou pour simuler un autre poids/%BF sans re-peser. */
    var poidsInputKg by mutableStateOf("")
    var bfInputPct by mutableStateOf("")

    val recommendedMode: CalorieMode get() = CalorieCalculator.recommendedMode(bfInputPct.toDoubleOrNull())

    /** Cible complète pour un mode donné, à partir des champs actuellement
     *  saisis — null si le poids n'est pas un nombre valide. */
    fun goalFor(mode: CalorieMode): CalorieGoal? {
        val poids = poidsInputKg.replace(",", ".").toDoubleOrNull() ?: return null
        val bf = bfInputPct.replace(",", ".").toDoubleOrNull()
        return CalorieCalculator.goal(poids, bf, mode)
    }

    fun load() {
        isLoading = true
        errorMessage = null
        viewModelScope.launch {
            try {
                val recent = weighInRepository.fetch(DateUtils.daysAgo(30))
                val latest = recent.lastOrNull { it.poidsKg > 0 }
                poidsKg = latest?.poidsKg
                bfPct = latest?.bfPct
                poidsInputKg = latest?.poidsKg?.toString() ?: ""
                bfInputPct = latest?.bfPct?.toString() ?: ""
                if (userId.isNotBlank()) {
                    appliedMode = runCatching { profileRepository.fetch(userId) }.getOrNull()?.objectifCalorieMode
                }
            } catch (e: java.io.IOException) {
                errorMessage = "Pas de connexion internet — réessaie dès que le réseau revient."
            } catch (e: Exception) {
                errorMessage = "Impossible de charger tes dernières mesures."
            } finally {
                isLoading = false
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
                    NewNutritionTarget(
                        date = DateUtils.today(),
                        typeJour = TypeJour.Training,
                        caloriesCible = goal.targetCalories,
                        proteinesCibleG = goal.proteinesG.toDouble(),
                    ),
                )
                appliedMode = mode
                savedConfirmation = true
            } catch (e: Exception) {
                errorMessage = "Impossible d'enregistrer ce mode — réessaie."
            } finally {
                isSaving = false
            }
        }
    }
}
