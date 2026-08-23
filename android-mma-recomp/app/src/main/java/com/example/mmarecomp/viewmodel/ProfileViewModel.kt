package com.example.mmarecomp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mmarecomp.data.MealRepository
import com.example.mmarecomp.data.ProfileRepository
import com.example.mmarecomp.data.WeighInRepository
import com.example.mmarecomp.data.WorkoutRepository
import com.example.mmarecomp.model.Phase
import com.example.mmarecomp.model.Profile
import com.example.mmarecomp.model.ProfileUpdate
import com.example.mmarecomp.util.CsvExporter
import com.example.mmarecomp.util.DateUtils
import com.example.mmarecomp.util.toFriendlyMessage
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userId: String,
    private val repository: ProfileRepository = ProfileRepository(),
    private val workoutRepository: WorkoutRepository = WorkoutRepository(),
    private val mealRepository: MealRepository = MealRepository(),
    private val weighInRepository: WeighInRepository = WeighInRepository(),
) : ViewModel() {
    var profile by mutableStateOf<Profile?>(null)
        private set
    var poidsObjectifKg by mutableStateOf("")
    var bfObjectifPct by mutableStateOf("")
    var phase by mutableStateOf(Phase.Ete)
    var isSaving by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var isExporting by mutableStateOf(false)
        private set

    fun load() {
        viewModelScope.launch {
            try {
                val fetched = repository.fetch(userId)
                profile = fetched
                poidsObjectifKg = fetched.poidsObjectifKg.toString()
                bfObjectifPct = fetched.bfObjectifPct.toString()
                phase = fetched.phase
            } catch (e: Exception) {
                errorMessage = e.toFriendlyMessage("Impossible de charger le profil.")
            }
        }
    }

    fun save(onSaved: (Phase) -> Unit) {
        val poids = poidsObjectifKg.replace(",", ".").toDoubleOrNull()
        val bf = bfObjectifPct.replace(",", ".").toDoubleOrNull()
        if (poids == null || poids !in 30.0..300.0) {
            errorMessage = "Le poids objectif doit être un nombre entre 30 et 300 kg."
            return
        }
        if (bf == null || bf !in 3.0..60.0) {
            errorMessage = "Le %BF objectif doit être un nombre entre 3 et 60."
            return
        }

        errorMessage = null
        isSaving = true
        viewModelScope.launch {
            val patch = ProfileUpdate(poidsObjectifKg = poids, bfObjectifPct = bf, phase = phase)
            try {
                repository.update(userId, patch)
                onSaved(phase)
            } catch (e: Exception) {
                errorMessage = e.toFriendlyMessage("Impossible d'enregistrer le profil.")
            } finally {
                isSaving = false
            }
        }
    }

    /** Construit un export texte (CSV) de tout l'historique — séances, repas,
     *  pesées — pour sauvegarde personnelle. L'écriture du fichier (qui
     *  nécessite un Context/Uri) reste côté écran ; le ViewModel ne fait que
     *  récupérer et formater les données. */
    suspend fun buildExportCsv(): String {
        isExporting = true
        try {
            val since = DateUtils.daysAgo(3650) // ~10 ans : en pratique "tout l'historique"
            val workouts = workoutRepository.fetchWeek(since)
            val meals = mealRepository.fetch(since)
            val weighIns = weighInRepository.fetch(since)
            return buildString {
                appendLine("=== Séances ===")
                append(CsvExporter.workoutsToCsv(workouts))
                appendLine()
                appendLine("=== Repas ===")
                append(CsvExporter.mealsToCsv(meals))
                appendLine()
                appendLine("=== Pesées ===")
                append(CsvExporter.weighInsToCsv(weighIns))
            }
        } finally {
            isExporting = false
        }
    }
}
