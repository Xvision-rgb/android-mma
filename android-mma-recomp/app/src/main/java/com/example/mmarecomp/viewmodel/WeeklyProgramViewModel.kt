package com.example.mmarecomp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mmarecomp.data.TrainingPlanRepository
import com.example.mmarecomp.model.NewTrainingPlanDay
import com.example.mmarecomp.model.Phase
import com.example.mmarecomp.model.PlanDayType
import com.example.mmarecomp.model.TrainingPlanDay
import com.example.mmarecomp.ui.components.ErrorOperation
import com.example.mmarecomp.ui.components.ScreenError
import com.example.mmarecomp.util.rethrowCancellation
import kotlinx.coroutines.launch

class WeeklyProgramViewModel(
    private val trainingPlanRepository: TrainingPlanRepository = TrainingPlanRepository(),
) : ViewModel() {
    var planDays by mutableStateOf<List<TrainingPlanDay>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var errorOperation by mutableStateOf(ErrorOperation.LOAD)
        private set

    val screenError: ScreenError?
        get() = errorMessage?.let { ScreenError(it, errorOperation) }

    fun load(phase: Phase) {
        isLoading = true
        errorMessage = null
        viewModelScope.launch {
            try {
                planDays = trainingPlanRepository.fetchWeek(phase)
            } catch (e: Throwable) {
                rethrowCancellation(e)
                errorOperation = ErrorOperation.LOAD
                errorMessage = if (e is java.io.IOException) {
                    "Pas de connexion internet — réessaie dès que le réseau revient."
                } else {
                    "Impossible de charger le programme de la semaine."
                }
            } finally {
                isLoading = false
            }
        }
    }

    fun updatePlanDayType(day: TrainingPlanDay, newType: PlanDayType) {
        viewModelScope.launch {
            val updated = NewTrainingPlanDay(
                jourSemaine = day.jourSemaine,
                type = newType,
                exercices = day.exercices,
                phase = day.phase,
                notes = day.notes,
                creneau = day.creneau,
            )
            try {
                trainingPlanRepository.upsert(updated)
                planDays = planDays.map { if (it.id == day.id) it.copy(type = newType) else it }
            } catch (e: Throwable) {
                rethrowCancellation(e)
                errorOperation = ErrorOperation.UPDATE
                errorMessage = "Impossible de mettre à jour le programme."
            }
        }
    }
}
