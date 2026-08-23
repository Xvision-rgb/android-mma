package com.example.mmarecomp.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mmarecomp.model.LoggedExercise
import com.example.mmarecomp.ui.components.SoftAlertBanner

@Composable
fun ExerciseRow(exercice: LoggedExercise, onChange: (LoggedExercise) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        OutlinedTextField(
            value = exercice.nom,
            onValueChange = { onChange(exercice.copy(nom = it)) },
            label = { Text("Nom de l'exercice") },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = exercice.series.toString(),
                onValueChange = { onChange(exercice.copy(series = it.toIntOrNull() ?: exercice.series)) },
                label = { Text("Séries") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = exercice.reps.toString(),
                onValueChange = { onChange(exercice.copy(reps = it.toIntOrNull() ?: exercice.reps)) },
                label = { Text("Reps") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = exercice.chargeCibleKg?.toString() ?: "",
                onValueChange = { onChange(exercice.copy(chargeCibleKg = it.replace(",", ".").toDoubleOrNull())) },
                label = { Text("Charge cible (kg)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = exercice.chargeReelleKg?.toString() ?: "",
                onValueChange = { onChange(exercice.copy(chargeReelleKg = it.replace(",", ".").toDoubleOrNull())) },
                label = { Text("Charge réelle (kg)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Toutes les reps faites proprement", style = MaterialTheme.typography.bodySmall)
            Switch(checked = exercice.propre, onCheckedChange = { onChange(exercice.copy(propre = it)) })
        }

        exercice.suggestionProgression?.let { suggestion ->
            SoftAlertBanner(
                message = "Séance propre — essaie +2.5kg la prochaine fois (%.1fkg)".format(suggestion),
                icon = Icons.Filled.NorthEast,
            )
        }
    }
}
