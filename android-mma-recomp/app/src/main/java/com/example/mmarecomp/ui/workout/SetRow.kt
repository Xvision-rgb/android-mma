package com.example.mmarecomp.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mmarecomp.model.LoggedSet
import com.example.mmarecomp.ui.theme.Dimens

/** Saisie d'une série individuelle.
 *
 *  Volontairement compacte : la saisie d'une séance passe de ~4 champs par
 *  exercice à ~4 champs par série, donc chaque champ superflu se paie quatre
 *  fois. Le RIR et les deux marqueurs (sangles, poigne) restent optionnels et
 *  ne bloquent jamais l'enregistrement. */
@Composable
fun SetRow(
    set: LoggedSet,
    onChange: (LoggedSet) -> Unit,
    onRemove: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (set.estAmrap) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                RoundedCornerShape(12.dp),
            )
            .padding(Dimens.spaceSm),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "S${set.index}",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.widthIn(min = 28.dp),
            )

            var repsText by remember(set.reps) { mutableStateOf(set.reps.toString()) }
            OutlinedTextField(
                value = repsText,
                onValueChange = { text ->
                    repsText = text
                    text.toIntOrNull()?.takeIf { it >= 0 }?.let { onChange(set.copy(reps = it)) }
                },
                label = { Text("Reps") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                ),
                modifier = Modifier.weight(1f),
            )

            var chargeText by remember(set.chargeKg) { mutableStateOf(formatCharge(set.chargeKg)) }
            OutlinedTextField(
                value = chargeText,
                onValueChange = { text ->
                    chargeText = text
                    text.replace(",", ".").toDoubleOrNull()
                        ?.coerceAtLeast(0.0)
                        ?.let { onChange(set.copy(chargeKg = it)) }
                },
                label = { Text("kg") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                ),
                modifier = Modifier.weight(1f),
            )

            var rirText by remember(set.rir) { mutableStateOf(set.rir?.toString() ?: "") }
            OutlinedTextField(
                value = rirText,
                onValueChange = { text ->
                    rirText = text
                    onChange(set.copy(rir = text.toIntOrNull()?.coerceIn(0, 10)))
                },
                label = { Text("RIR") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                ),
                modifier = Modifier.weight(1f),
            )

            if (onRemove != null) {
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Supprimer la série ${set.index}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs)) {
            FilterChip(
                selected = set.estAmrap,
                onClick = { onChange(set.copy(estAmrap = !set.estAmrap)) },
                label = { Text("AMRAP") },
            )
            FilterChip(
                selected = set.sangles,
                onClick = { onChange(set.copy(sangles = !set.sangles)) },
                label = { Text("Sangles") },
            )
            FilterChip(
                selected = set.limitePoigne,
                onClick = { onChange(set.copy(limitePoigne = !set.limitePoigne)) },
                label = { Text("Poigne") },
            )
        }

        if (set.limitePoigne) {
            Text(
                "Série arrêtée par la poigne — la charge ne sera pas baissée.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Évite le "80.0" disgracieux sur des charges entières, sans dépendre de
 *  Formatting qui arrondit systématiquement à une décimale. */
private fun formatCharge(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
