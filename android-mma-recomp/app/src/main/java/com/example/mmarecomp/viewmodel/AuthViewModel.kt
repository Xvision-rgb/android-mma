package com.example.mmarecomp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mmarecomp.data.AuthRepository
import com.example.mmarecomp.util.toFriendlyMessage
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository = AuthRepository()) : ViewModel() {
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var isSubmitting by mutableStateOf(false)
        private set

    private fun validCredentials(): Boolean {
        if (!email.contains("@") || email.isBlank()) {
            errorMessage = "Adresse email invalide."
            return false
        }
        if (password.length < 6) {
            errorMessage = "Le mot de passe doit faire au moins 6 caractères."
            return false
        }
        return true
    }

    fun signIn() {
        errorMessage = null
        if (!validCredentials()) return
        isSubmitting = true
        viewModelScope.launch {
            try {
                authRepository.signIn(email, password)
            } catch (e: Exception) {
                errorMessage = e.toFriendlyMessage("Connexion impossible. Vérifie ton email et mot de passe.")
            } finally {
                isSubmitting = false
            }
        }
    }

    fun signUp() {
        errorMessage = null
        if (!validCredentials()) return
        isSubmitting = true
        viewModelScope.launch {
            try {
                authRepository.signUp(email, password)
            } catch (e: Exception) {
                errorMessage = e.toFriendlyMessage("Inscription impossible. Réessaie.")
            } finally {
                isSubmitting = false
            }
        }
    }
}
