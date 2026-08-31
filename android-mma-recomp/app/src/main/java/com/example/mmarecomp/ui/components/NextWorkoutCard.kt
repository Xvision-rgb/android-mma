package com.example.mmarecomp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.mmarecomp.ui.theme.Dimens

@Composable
fun NextWorkoutCard(
    sessionLabel: String?,
    exercises: List<String>,
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (sessionLabel == null || exercises.isEmpty()) return

    AppCard(modifier = modifier) {
        Text("Ton prochain workout", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(sessionLabel, style = MaterialTheme.typography.titleMedium)
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs)) {
            exercises.forEach { name ->
                Text(
                    "• $name",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            "Parfait pour travailler $sessionLabel",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FilledTonalButton(onClick = onStartClick) {
            Icon(Icons.Filled.FitnessCenter, contentDescription = null, modifier = Modifier.padding(end = Dimens.spaceSm))
            Text("Lancer la séance")
        }
    }
}
