package com.example.mmarecomp.ui.theme

import androidx.compose.runtime.mutableStateOf
import com.example.mmarecomp.data.ThemeMode

/**
 * État de thème partagé globalement. Ce scaffold n'a pas de conteneur DI —
 * un seul écran (Réglages) l'écrit, un seul point (MainActivity) le
 * consomme, donc un objet Compose state simple suffit sans over-engineering.
 */
object AppThemeState {
    val mode = mutableStateOf(ThemeMode.SYSTEM)
}
