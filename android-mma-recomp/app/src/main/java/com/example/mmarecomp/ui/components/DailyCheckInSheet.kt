package com.example.mmarecomp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.example.mmarecomp.ui.theme.Dimens

/** Point de forme du matin.
 *
 *  Cinq curseurs et un bouton : au-delà, le check-in quotidien n'est plus fait
 *  quotidiennement. La HRV et le dead hang restent facultatifs et repliés en
 *  bas, pour ne pas donner l'impression qu'il faut les mesurer.
 *
 *  Tous les items vont de 1 (mauvais) à 5 (bon), pour que le score se lise
 *  toujours dans le même sens. */
@Composable
fun DailyCheckInSheet(
    onSubmit: (sommeil: Int, courbatures: Int, fatigue: Int, humeur: Int, stress: Int, hrv: Double?, deadHang: Int?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var sommeil by remember { mutableFloatStateOf(3f) }
    var courbatures by remember { mutableFloatStateOf(3f) }
    var fatigue by remember { mutableFloatStateOf(3f) }
    var humeur by remember { mutableFloatStateOf(3f) }
    var stress by remember { mutableFloatStateOf(3f) }
    var hrvText by remember { mutableStateOf("") }
    var deadHangText by remember { mutableStateOf("") }
    var optionnelsVisibles by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Dimens.spaceMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
    ) {
        Text("Le point du jour", style = MaterialTheme.typography.titleMedium)
        Text(
            "1 = difficile, 5 = au top. Aucune mauvaise réponse — c'est un repère, pas une note.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ItemCurseur("Sommeil", sommeil) { sommeil = it }
        ItemCurseur("Courbatures", courbatures) { courbatures = it }
        ItemCurseur("Fatigue générale", fatigue) { fatigue = it }
        ItemCurseur("Humeur", humeur) { humeur = it }
        ItemCurseur("Stress", stress) { stress = it }

        TextButton(onClick = { optionnelsVisibles = !optionnelsVisibles }) {
            Text(if (optionnelsVisibles) "Masquer les mesures optionnelles" else "Ajouter HRV / dead hang (optionnel)")
        }

        if (optionnelsVisibles) {
            OutlinedTextField(
                value = hrvText,
                onValueChange = { hrvText = it },
                label = { Text("HRV rMSSD au réveil (ms)") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = deadHangText,
                onValueChange = { deadHangText = it },
                label = { Text("Dead hang max (secondes)") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
        ) {
            TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                Text("Plus tard")
            }
            Button(
                onClick = {
                    onSubmit(
                        sommeil.toInt(),
                        courbatures.toInt(),
                        fatigue.toInt(),
                        humeur.toInt(),
                        stress.toInt(),
                        hrvText.replace(",", ".").toDoubleOrNull(),
                        deadHangText.toIntOrNull(),
                    )
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("Enregistrer")
            }
        }
    }
}

@Composable
private fun ItemCurseur(label: String, valeur: Float, onChange: (Float) -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("${valeur.toInt()}/5", style = MaterialTheme.typography.labelMedium)
        }
        Slider(
            value = valeur,
            onValueChange = onChange,
            valueRange = 1f..5f,
            steps = 3,
        )
    }
}
