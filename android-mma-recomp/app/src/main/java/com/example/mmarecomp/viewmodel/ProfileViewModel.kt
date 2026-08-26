package com.example.mmarecomp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mmarecomp.data.ProfileRepository
import com.example.mmarecomp.model.Phase
import com.example.mmarecomp.model.Profile
import com.example.mmarecomp.model.ProfileUpdate
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userId: String,
    private val repository: ProfileRepository = ProfileRepository(),
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

    fun load() {
        viewModelScope.launch {
            try {
                val fetched = repository.fetch(userId)
                profile = fetched
                poidsObjectifKg = fetched.poidsObjectifKg.toString()
                bfObjectifPct = fetched.bfObjectifPct.toString()
                phase = fetched.phase
            } catch (e: java.io.IOException) {
                errorMessage = "Pas de connexion internet — réessaie dès que le réseau revient."
            } catch (e: Exception) {
                errorMessage = "Impossible de charger le profil."
            }
        }
    }

    fun save(onSaved: (Phase) -> Unit) {
        isSaving = true
        viewModelScope.launch {
            val patch = ProfileUpdate(
                poidsObjectifKg = poidsObjectifKg.replace(",", ".").toDoubleOrNull(),
                bfObjectifPct = bfObjectifPct.replace(",", ".").toDoubleOrNull(),
                phase = phase,
            )
            try {
                repository.update(userId, patch)
                onSaved(phase)
            } catch (e: Exception) {
                errorMessage = "Impossible d'enregistrer le profil."
            } finally {
                isSaving = false
            }
        }
    }
}
