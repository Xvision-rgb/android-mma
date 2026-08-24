package com.example.mmarecomp.ui

import androidx.compose.runtime.mutableStateOf
import com.example.mmarecomp.data.UserPreferences

/**
 * État de préférences partagé globalement, même approche que
 * [com.example.mmarecomp.ui.theme.AppThemeState] : un seul écran (Réglages)
 * l'écrit, plusieurs écrans le lisent, un objet Compose state simple suffit
 * sans conteneur DI.
 */
object AppPreferencesState {
    val preferences = mutableStateOf(UserPreferences())
}
