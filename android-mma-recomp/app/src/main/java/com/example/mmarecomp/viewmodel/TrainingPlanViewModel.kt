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
import com.example.mmarecomp.util.toFriendlyMessage
import kotlinx.coroutines.launch

class TrainingPlanViewModel(
    private val repository: TrainingPlanRepository = TrainingPlanRepository(),
) : ViewModel() {
    var days by mutableStateOf<List<TrainingPlanDay>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    /** jourSemaine (1-7) actuellement en cours d'enregistrement, ou null. */
    var savingDay by mutableStateOf<Int?>(null)
        private set

    fun load(phase: Phase) {
        isLoading = true
        errorMessage = null
        viewModelScope.launch {
            try {
                val fetched = repository.fetchWeek(phase)
                // La semaine est toujours affichée complète (7 jours), même
                // pour les jours pas encore programmés en base.
                days = (1..7).map { jour ->
                    fetched.firstOrNull { it.jourSemaine == jour } ?: emptyDay(jour, phase)
                }
            } catch (e: Exception) {
                errorMessage = e.toFriendlyMessage("Impossible de charger le split.")
            } finally {
                isLoading = false
            }
        }
    }

    private fun emptyDay(jour: Int, phase: Phase) = TrainingPlanDay(
        id = "",
        userId = "",
        jourSemaine = jour,
        type = PlanDayType.Repos,
        exercices = emptyList(),
        phase = phase,
        notes = null,
        actif = true,
    )

    fun updateDay(jourSemaine: Int, updated: TrainingPlanDay) {
        days = days.map { if (it.jourSemaine == jourSemaine) updated else it }
    }

    fun saveDay(day: TrainingPlanDay, onResult: (Boolean) -> Unit) {
        savingDay = day.jourSemaine
        viewModelScope.launch {
            val payload = NewTrainingPlanDay(
                jourSemaine = day.jourSemaine,
                type = day.type,
                // Comme pour le log séance, on ignore les lignes laissées sans nom.
                exercices = day.exercices.filter { it.nom.isNotBlank() },
                phase = day.phase,
                notes = day.notes,
            )
            try {
                repository.upsert(payload)
                onResult(true)
            } catch (e: Exception) {
                errorMessage = e.toFriendlyMessage("Impossible d'enregistrer ce jour.")
                onResult(false)
            } finally {
                savingDay = null
            }
        }
    }
}
