package com.example.mmarecomp.ui.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.example.mmarecomp.model.PlannedExercise
import com.example.mmarecomp.model.PlannedExerciseUnit
import com.example.mmarecomp.ui.theme.Dimens

/** Ligne d'édition d'un exercice programmé (training_plan) — plus simple que
 *  ExerciseRow (log séance) : pas de charge réelle/reps réelles/case
 *  "propre", ce sont des champs de performance qui n'ont de sens qu'au
 *  moment du log, pas dans le plan prévisionnel.
 *  Unité au choix : reps, secondes, minutes, mètres. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannedExerciseRow(exercice: PlannedExercise, onChange: (PlannedExercise) -> Unit) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.spaceSm),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
    ) {
        OutlinedTextField(
            value = exercice.nom,
            onValueChange = { onChange(exercice.copy(nom = it)) },
            label = { Text("Nom de l'exercice") },
            isError = exercice.nom.isBlank(),
            supportingText = if (exercice.nom.isBlank()) {
                { Text("Le nom ne peut pas être vide") }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = androidx.compose.ui.text.input.ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
            var seriesText by remember(exercice.series) { mutableStateOf(exercice.series.toString()) }
            OutlinedTextField(
                value = seriesText,
                onValueChange = { text ->
                    seriesText = text
                    text.toIntOrNull()?.takeIf { it >= 1 }?.let { onChange(exercice.copy(series = it)) }
                },
                label = { Text("Séries") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                ),
                modifier = Modifier.weight(1f),
            )
            var qtyText by remember(exercice.reps) { mutableStateOf(exercice.reps.toString()) }
            val qtyLabel = when (exercice.unite) {
                PlannedExerciseUnit.Reps -> "Reps"
                PlannedExerciseUnit.Secondes -> "Secondes"
                PlannedExerciseUnit.Minutes -> "Minutes"
                PlannedExerciseUnit.Metres -> "Mètres"
            }
            OutlinedTextField(
                value = qtyText,
                onValueChange = { text ->
                    qtyText = text
                    text.toIntOrNull()?.takeIf { it >= 1 }?.let { onChange(exercice.copy(reps = it)) }
                },
                label = { Text(qtyLabel) },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                ),
                modifier = Modifier.weight(1f),
            )
        }
        var unitExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = unitExpanded, onExpandedChange = { unitExpanded = it }) {
            OutlinedTextField(
                value = exercice.unite.label,
                onValueChange = {},
                readOnly = true,
                label = { Text("Unité") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
            )
            androidx.compose.material3.DropdownMenu(
                expanded = unitExpanded,
                onDismissRequest = { unitExpanded = false },
            ) {
                PlannedExerciseUnit.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            onChange(exercice.copy(unite = option))
                            unitExpanded = false
                        },
                    )
                }
            }
        }
        val showCharge = exercice.unite == PlannedExerciseUnit.Reps ||
            exercice.unite == PlannedExerciseUnit.Secondes ||
            exercice.unite == PlannedExerciseUnit.Metres
        if (showCharge) {
            OutlinedTextField(
                value = exercice.chargeCibleKg?.toString() ?: "",
                onValueChange = {
                    onChange(exercice.copy(chargeCibleKg = it.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0)))
                },
                label = { Text("Charge cible (kg, optionnel)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onDone = { focusManager.clearFocus() },
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
