package com.example.mmarecomp.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import com.example.mmarecomp.model.LoggedExercise
import com.example.mmarecomp.model.LoggedSet
import com.example.mmarecomp.model.withSets
import com.example.mmarecomp.ui.components.SoftAlertBanner
import com.example.mmarecomp.ui.theme.Dimens
import com.example.mmarecomp.util.ApreEngine
import com.example.mmarecomp.util.ApreProtocol
import com.example.mmarecomp.util.Formatting
import com.example.mmarecomp.util.RirTargets
import com.example.mmarecomp.util.SetStopAdvisor

@Composable
fun ExerciseRow(
    exercice: LoggedExercise,
    onChange: (LoggedExercise) -> Unit,
    lastKnownCharge: Double? = null,
    personalRecordKg: Double? = null,
    protocole: ApreProtocol = ApreProtocol.APRE_10,
    incrementKg: Double = ApreEngine.INCREMENT_DEFAUT,
    biaisRir: Double = 0.0,
    seuilChuteStrict: Boolean = false,
) {
    val haptic = LocalHapticFeedback.current
    val chargeMax = exercice.chargeMaxKg
    val isNewRecord = personalRecordKg != null && (chargeMax ?: 0.0) > personalRecordKg
    LaunchedEffect(isNewRecord, chargeMax) {
        if (isNewRecord) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    val sets = exercice.effectiveSets

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
            trailingIcon = {
                if (exercice.nom.isNotEmpty()) {
                    IconButton(onClick = { onChange(exercice.copy(nom = "")) }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Effacer le nom de l'exercice")
                    }
                }
            },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = androidx.compose.ui.text.input.ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = exercice.chargeCibleKg?.toString() ?: "",
            onValueChange = {
                onChange(exercice.copy(chargeCibleKg = it.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0)))
            },
            label = { Text("Charge cible (kg)") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = androidx.compose.ui.text.input.ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        lastKnownCharge?.let { charge ->
            Text(
                "Dernière fois : ${Formatting.oneDecimal(charge)}kg",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        val cibleRir = if (exercice.nom.isNotBlank()) RirTargets.cible(exercice) else null
        Text(
            if (cibleRir != null) "Séries — RIR cible ${cibleRir.label}" else "Séries",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (exercice.setsSontDerives && sets.isNotEmpty()) {
            Text(
                "Séries reconstruites depuis un ancien format de log — RIR et marqueurs non renseignés.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        sets.forEachIndexed { index, set ->
            SetRow(
                set = set,
                onChange = { updated ->
                    onChange(exercice.withSets(sets.toMutableList().also { it[index] = updated }))
                },
                onRemove = if (sets.size > 1) {
                    { onChange(exercice.withSets(sets.filterIndexed { i, _ -> i != index })) }
                } else {
                    null
                },
            )
        }

        TextButton(
            onClick = {
                // Pré-remplie avec la série précédente : sur une même séance,
                // la charge et les reps varient peu d'une série à l'autre, et
                // retaper quatre fois la même chose tue l'adoption du log.
                val precedente = sets.lastOrNull()
                val nouvelle = LoggedSet(
                    index = sets.size + 1,
                    reps = precedente?.reps ?: exercice.reps,
                    chargeKg = precedente?.chargeKg ?: exercice.chargeCibleKg ?: 0.0,
                    sangles = precedente?.sangles ?: false,
                )
                onChange(exercice.withSets(sets + nouvelle))
            },
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text("Ajouter une série")
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

        ApreEngine.prescrire(exercice, protocole, incrementKg, biaisRir)?.let { prescription ->
            SoftAlertBanner(
                message = "Prochaine fois : ${Formatting.oneDecimal(prescription.chargeKg)}kg — ${prescription.justification}",
                icon = Icons.Filled.NorthEast,
            )
        }

        RirTargets.note(exercice)?.let { note ->
            SoftAlertBanner(
                message = note,
                tone = com.example.mmarecomp.ui.components.SoftAlertTone.NEUTRAL,
            )
        }

        SetStopAdvisor.conseil(exercice, strict = seuilChuteStrict)?.let { conseil ->
            SoftAlertBanner(
                message = conseil,
                tone = com.example.mmarecomp.ui.components.SoftAlertTone.NEUTRAL,
            )
        }

        if (isNewRecord) {
            SoftAlertBanner(
                message = "Nouveau record sur cet exercice — ${Formatting.oneDecimal(chargeMax ?: 0.0)}kg 🎉",
                icon = Icons.Filled.EmojiEvents,
            )
        }
    }
}
