package com.example.mmarecomp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mmarecomp.data.ProfileRepository
import com.example.mmarecomp.model.Phase
import com.example.mmarecomp.model.Profile
import kotlinx.coroutines.CancellationException
import com.example.mmarecomp.util.rethrowCancellation
import kotlinx.coroutines.launch

sealed class PhaseState {
    data object Loading : PhaseState()
    data class Ready(val phase: Phase) : PhaseState()
    data class Error(val message: String) : PhaseState()
}

/** Charge la phase active du profil après authentification — jamais de
 *  phase par défaut silencieuse avant la fin du fetch. */
class SessionProfileViewModel(
    private val userId: String,
    private val fetchProfile: suspend (String) -> Profile = { ProfileRepository().fetch(it) },
) : ViewModel() {

    var phaseState by mutableStateOf<PhaseState>(PhaseState.Loading)
        private set

    fun load() {
        if (phaseState is PhaseState.Ready) return
        phaseState = PhaseState.Loading
        viewModelScope.launch {
            loadProfile()
        }
    }

    /** Premier chargement uniquement — ne remet pas l'écran en chargement si la
     *  phase est déjà connue (retour au premier plan, refresh token, etc.). */
    fun ensureLoaded() {
        if (phaseState is PhaseState.Ready) return
        load()
    }

    suspend fun loadProfile() {
        if (phaseState !is PhaseState.Ready) {
            phaseState = PhaseState.Loading
        }
        try {
            val profile = fetchProfile(userId)
            phaseState = PhaseState.Ready(profile.phase)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            rethrowCancellation(e)
            when (e) {
                is java.io.IOException -> {
            phaseState = PhaseState.Error("Pas de connexion internet — réessaie dès que le réseau revient.")
                }
                else -> {
            phaseState = PhaseState.Error("Impossible de charger ton profil — réessaie.")
                }
            }
        }
    }
}
