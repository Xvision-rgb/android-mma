package com.example.mmarecomp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.mmarecomp.model.WorkoutType

/** Couleur stable et distincte par WorkoutType, réutilisée partout où le
 *  type de séance est affiché visuellement (dropdown, répartition hebdo du
 *  Dashboard, historique). Reste dans la palette du thème (primary/
 *  secondary/tertiary) plutôt que d'introduire de nouvelles teintes — les 6
 *  types se distinguent par 3 teintes × 2 intensités. */
@Composable
fun workoutTypeColor(type: WorkoutType): Color {
    val scheme = MaterialTheme.colorScheme
    return when (type) {
        WorkoutType.JambesForce -> scheme.primary
        WorkoutType.TorseForce -> scheme.secondary
        WorkoutType.Hiit -> scheme.tertiary
        WorkoutType.JambesHypertrophie -> scheme.primary.copy(alpha = 0.6f)
        WorkoutType.TorseHypertrophie -> scheme.secondary.copy(alpha = 0.6f)
        WorkoutType.MmaWod -> scheme.tertiary.copy(alpha = 0.6f)
    }
}
