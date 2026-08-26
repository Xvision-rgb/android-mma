package com.example.mmarecomp.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mmarecomp.model.LoggedExercise
import com.example.mmarecomp.ui.components.SoftAlertBanner
import com.example.mmarecomp.ui.theme.Dimens
import com.example.mmarecomp.util.Formatting

@Composable
fun ExerciseRow(
    exercice: LoggedExercise,
    onChange: (LoggedExercise) -> Unit,
    lastKnownCharge: Double? = null,
    personalRecordKg: Double? = null,
) {
    val haptic = LocalHapticFeedback.current
    val isNewRecord = personalRecordKg != null && (exercice.chargeReelleKg ?: 0.0) > personalRecordKg
    LaunchedEffect(isNewRecord, exercice.chargeReelleKg) {
        if (isNewRecord) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
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
            trailingIcon = {
                if (exercice.nom.isNotEmpty()) {
                    IconButton(onClick = { onChange(exercice.copy(nom = "")) }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Effacer le nom de l'exercice")
                    }
                }
            },
            singleLine = true,
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
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
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
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
            OutlinedTextField(
                value = exercice.chargeCibleKg?.toString() ?: "",
                onValueChange = {
                    onChange(exercice.copy(chargeCibleKg = it.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0)))
                },
                label = { Text("Charge cible (kg)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = exercice.chargeReelleKg?.toString() ?: "",
                onValueChange = {
                    onChange(exercice.copy(chargeReelleKg = it.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0)))
                },
                label = { Text("Charge réelle (kg)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
        }

        lastKnownCharge?.let { charge ->
            Text(
                "Dernière fois : ${Formatting.oneDecimal(charge)}kg",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = exercice.propre,
                    role = Role.Switch,
                    onValueChange = { onChange(exercice.copy(propre = it)) },
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text("Toutes les reps faites proprement", style = MaterialTheme.typography.bodySmall)
                Text(
                    "Sans compensation ni forme dégradée sur la dernière série",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = exercice.propre, onCheckedChange = null)
        }

        exercice.suggestionProgression?.let { suggestion ->
            SoftAlertBanner(
                message = "Séance propre — essaie +2.5kg la prochaine fois (${Formatting.oneDecimal(suggestion)}kg)",
                icon = Icons.Filled.NorthEast,
            )
        }

        if (isNewRecord) {
            SoftAlertBanner(
                message = "Nouveau record sur cet exercice — ${Formatting.oneDecimal(exercice.chargeReelleKg ?: 0.0)}kg 🎉",
                icon = Icons.Filled.EmojiEvents,
            )
        }
    }
}
