package com.example.mmarecomp.ui.workout

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material3.FilterChip
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
import com.example.mmarecomp.model.ExerciseModality
import com.example.mmarecomp.model.LoggedExercise
import com.example.mmarecomp.model.LoggedSet
import com.example.mmarecomp.model.asCardio
import com.example.mmarecomp.model.asStrength
import com.example.mmarecomp.model.withSets
import com.example.mmarecomp.ui.components.SoftAlertBanner
import com.example.mmarecomp.ui.theme.Dimens
import com.example.mmarecomp.util.ApreEngine
import com.example.mmarecomp.util.ApreProtocol
import com.example.mmarecomp.util.CardioEnergy
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
    rirBonusModulation: Int = 0,
    seuilChuteStrict: Boolean = false,
    poidsCorpsKg: Double? = null,
) {
    val haptic = LocalHapticFeedback.current
    val chargeMax = exercice.chargeMaxKg
    val isNewRecord = !exercice.isCardio &&
        personalRecordKg != null &&
        (chargeMax ?: 0.0) > personalRecordKg
    LaunchedEffect(isNewRecord, chargeMax) {
        if (isNewRecord) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.spaceSm),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
    ) {
        OutlinedTextField(
            value = exercice.nom,
            onValueChange = { nom ->
                val updated = exercice.copy(nom = nom)
                onChange(CardioEnergy.maybeAutodetect(updated))
            },
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
        ) {
            FilterChip(
                selected = !exercice.isCardio,
                onClick = { if (exercice.isCardio) onChange(exercice.asStrength()) },
                label = { Text("Force") },
            )
            FilterChip(
                selected = exercice.isCardio,
                onClick = { if (!exercice.isCardio) onChange(exercice.asCardio()) },
                label = { Text("Cardio") },
            )
        }

        if (exercice.isCardio) {
            CardioFields(exercice = exercice, onChange = onChange, poidsCorpsKg = poidsCorpsKg)
        } else {
            StrengthFields(
                exercice = exercice,
                onChange = onChange,
                lastKnownCharge = lastKnownCharge,
                isNewRecord = isNewRecord,
                chargeMax = chargeMax,
                protocole = protocole,
                incrementKg = incrementKg,
                biaisRir = biaisRir,
                rirBonusModulation = rirBonusModulation,
                seuilChuteStrict = seuilChuteStrict,
            )
        }
    }
}

@Composable
private fun CardioFields(
    exercice: LoggedExercise,
    onChange: (LoggedExercise) -> Unit,
    poidsCorpsKg: Double?,
) {
    OutlinedTextField(
        value = exercice.dureeMin?.toString().orEmpty(),
        onValueChange = {
            onChange(exercice.copy(dureeMin = it.filter(Char::isDigit).toIntOrNull()?.coerceAtLeast(1)))
        },
        label = { Text("Durée (min)") },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = androidx.compose.ui.text.input.ImeAction.Next,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = exercice.distanceKm?.toString().orEmpty(),
        onValueChange = {
            onChange(
                exercice.copy(
                    distanceKm = it.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(0.0),
                ),
            )
        },
        label = { Text("Distance (km) — optionnel") },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = androidx.compose.ui.text.input.ImeAction.Next,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = exercice.intensite?.toString().orEmpty(),
        onValueChange = {
            onChange(
                exercice.copy(
                    intensite = it.filter(Char::isDigit).toIntOrNull()?.coerceIn(1, 10),
                ),
            )
        },
        label = { Text("Intensité ressentie (1-10)") },
        supportingText = { Text("5 = rythme confortable, 8–9 = dur.") },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = KeyboardType.Number,
        ),
        modifier = Modifier.fillMaxWidth(),
    )

    val kcal = CardioEnergy.kcalForExercise(exercice, poidsCorpsKg)
    val allure = exercice.allureMinParKm
    val details = buildList {
        exercice.dureeMin?.let { add("${it} min") }
        exercice.distanceKm?.let { add("${Formatting.oneDecimal(it)} km") }
        allure?.let { add("allure ${Formatting.oneDecimal(it)} min/km") }
        if (kcal > 0) add("~$kcal kcal")
    }
    if (details.isNotEmpty()) {
        Text(
            details.joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StrengthFields(
    exercice: LoggedExercise,
    onChange: (LoggedExercise) -> Unit,
    lastKnownCharge: Double?,
    isNewRecord: Boolean,
    chargeMax: Double?,
    protocole: ApreProtocol,
    incrementKg: Double,
    biaisRir: Double,
    rirBonusModulation: Int,
    seuilChuteStrict: Boolean,
) {
    val sets = exercice.effectiveSets

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
    val cibleLabel = when {
        cibleRir == null -> null
        rirBonusModulation > 0 -> "${cibleRir.label} (+$rirBonusModulation modulation)"
        else -> cibleRir.label
    }
    Text(
        if (cibleLabel != null) "Séries — RIR cible $cibleLabel" else "Séries",
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
