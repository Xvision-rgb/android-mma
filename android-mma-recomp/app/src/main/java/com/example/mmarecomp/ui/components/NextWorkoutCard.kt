package com.example.mmarecomp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mmarecomp.ui.theme.Dimens

@Composable
fun NextWorkoutCard(
    exerciseName: String?,
    muscleGroup: String?,
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (exerciseName == null) return

    AppCard(modifier = modifier) {
        Text("Ton prochain workout", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(exerciseName, style = MaterialTheme.typography.titleMedium)
        Text(
            "Parfait pour travailler ${muscleGroup ?: "ton programme"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Volontairement PAS en pleine largeur. Le FAB du dashboard flotte au
        // coin bas-droit et mène déjà à l'écran Séance : un bouton pleine
        // largeur passait littéralement dessous quand la carte défilait à
        // cette hauteur, et proposait deux fois la même action. Aligné à
        // gauche, il ne croise plus le FAB et redevient ce qu'il est — un
        // raccourci secondaire, pas l'action principale de l'écran.
        FilledTonalButton(onClick = onStartClick) {
            Icon(Icons.Filled.FitnessCenter, contentDescription = null, modifier = Modifier.padding(end = Dimens.spaceSm))
            Text("Lancer la séance")
        }
    }
}
