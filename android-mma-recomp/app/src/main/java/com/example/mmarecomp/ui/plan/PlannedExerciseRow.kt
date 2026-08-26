package com.example.mmarecomp.ui.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mmarecomp.model.PlannedExercise
import com.example.mmarecomp.ui.theme.Dimens

/** Ligne d'édition d'un exercice programmé (training_plan) — plus simple que
 *  ExerciseRow (log séance) : pas de charge réelle/reps réelles/case
 *  "propre", ce sont des champs de performance qui n'ont de sens qu'au
 *  moment du log, pas dans le plan prévisionnel. */
@Composable
fun PlannedExerciseRow(exercice: PlannedExercise, onChange: (PlannedExercise) -> Unit) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.spaceSm),
        verticalArrangement = Arrangement.spacedBy(6.dp),
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
            // value lié directement à exercice.series.toString() empêchait de vider le
            // champ pour retaper un nombre : toIntOrNull() sur "" retombait sur l'ancienne
            // valeur, donc le texte affiché ne changeait jamais visuellement à l'effacement.
            // Un texte local (resynchronisé seulement quand la valeur committée change)
            // laisse l'utilisateur taper/effacer librement.
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
            var repsText by remember(exercice.reps) { mutableStateOf(exercice.reps.toString()) }
            OutlinedTextField(
                value = repsText,
                onValueChange = { text ->
                    repsText = text
                    text.toIntOrNull()?.takeIf { it >= 1 }?.let { onChange(exercice.copy(reps = it)) }
                },
                label = { Text("Reps") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                ),
                modifier = Modifier.weight(1f),
            )
        }
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
