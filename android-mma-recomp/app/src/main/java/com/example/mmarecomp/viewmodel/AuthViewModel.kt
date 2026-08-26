package com.example.mmarecomp.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mmarecomp.data.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository = AuthRepository()) : ViewModel() {
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var isSubmitting by mutableStateOf(false)
        private set
    var signUpSuccessMessage by mutableStateOf<String?>(null)
        private set

    fun signIn() {
        errorMessage = null
        signUpSuccessMessage = null
        isSubmitting = true
        viewModelScope.launch {
            try {
                authRepository.signIn(email, password)
            } catch (e: java.io.IOException) {
                errorMessage = "Pas de connexion internet — vérifie ton réseau et réessaie."
            } catch (e: Exception) {
                errorMessage = "Connexion impossible. Vérifie ton email et mot de passe."
            } finally {
                isSubmitting = false
            }
        }
    }

    fun signUp() {
        errorMessage = null
        signUpSuccessMessage = null
        isSubmitting = true
        viewModelScope.launch {
            try {
                authRepository.signUp(email, password)
                signUpSuccessMessage = "Compte créé — vérifie tes emails si une confirmation est demandée, sinon connecte-toi directement."
            } catch (e: java.io.IOException) {
                errorMessage = "Pas de connexion internet — vérifie ton réseau et réessaie."
            } catch (e: Exception) {
                errorMessage = "Inscription impossible. Réessaie."
            } finally {
                isSubmitting = false
            }
        }
    }
}
