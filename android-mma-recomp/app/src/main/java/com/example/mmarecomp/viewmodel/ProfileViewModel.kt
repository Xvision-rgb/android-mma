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
import com.example.mmarecomp.ui.components.ErrorOperation
import com.example.mmarecomp.ui.components.ScreenError
import com.example.mmarecomp.util.rethrowCancellation
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
    var errorOperation by mutableStateOf(ErrorOperation.LOAD)
        private set

    val screenError: ScreenError?
        get() = errorMessage?.let { ScreenError(it, errorOperation) }

    private var pendingSave: (() -> Unit)? = null

    private fun reportError(message: String, operation: ErrorOperation) {
        errorMessage = message
        errorOperation = operation
    }

    fun load() {
        viewModelScope.launch {
            try {
                val fetched = repository.fetch(userId)
                profile = fetched
                poidsObjectifKg = fetched.poidsObjectifKg.toString()
                bfObjectifPct = fetched.bfObjectifPct.toString()
                phase = fetched.phase
            } catch (e: Throwable) {
                rethrowCancellation(e)
                when (e) {
                    is java.io.IOException -> {
                reportError("Pas de connexion internet — réessaie dès que le réseau revient.", ErrorOperation.LOAD)
                    }
                    else -> {
                reportError("Impossible de charger le profil.", ErrorOperation.LOAD)
                    }
                }
            }
        }
    }

    fun save(onSaved: (Phase) -> Unit) {
        isSaving = true
        pendingSave = { save(onSaved) }
        viewModelScope.launch {
            val patch = ProfileUpdate(
                poidsObjectifKg = poidsObjectifKg.replace(",", ".").toDoubleOrNull(),
                bfObjectifPct = bfObjectifPct.replace(",", ".").toDoubleOrNull(),
                phase = phase,
            )
            try {
                repository.update(userId, patch)
                onSaved(phase)
                pendingSave = null
            } catch (e: Throwable) {
                rethrowCancellation(e)
                reportError("Impossible d'enregistrer le profil.", ErrorOperation.SAVE)
            } finally {
                isSaving = false
            }
        }
    }

    fun retrySave() {
        pendingSave?.invoke()
    }
}
