package com.example.mmarecomp.ui.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mmarecomp.model.PlannedExercise

/** Ligne d'édition d'un exercice programmé (training_plan) — plus simple que
 *  ExerciseRow (log séance) : pas de charge réelle/reps réelles/case
 *  "propre", ce sont des champs de performance qui n'ont de sens qu'au
 *  moment du log, pas dans le plan prévisionnel. */
@Composable
fun PlannedExerciseRow(exercice: PlannedExercise, onChange: (PlannedExercise) -> Unit) {
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
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = exercice.series.toString(),
                onValueChange = { onChange(exercice.copy(series = it.toIntOrNull()?.coerceAtLeast(1) ?: exercice.series)) },
                label = { Text("Séries") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = exercice.reps.toString(),
                onValueChange = { onChange(exercice.copy(reps = it.toIntOrNull()?.coerceAtLeast(1) ?: exercice.reps)) },
                label = { Text("Reps") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }
        OutlinedTextField(
            value = exercice.chargeCibleKg?.toString() ?: "",
            onValueChange = {
                onChange(exercice.copy(chargeCibleKg = it.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0)))
            },
            label = { Text("Charge cible (kg, optionnel)") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
