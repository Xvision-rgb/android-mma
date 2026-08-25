package com.example.mmarecomp.ui.weighin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mmarecomp.model.WeighInType
import com.example.mmarecomp.ui.components.DateField
import com.example.mmarecomp.ui.components.ErrorBanner
import com.example.mmarecomp.ui.components.SoftAlertBanner
import com.example.mmarecomp.ui.components.WeightTrendChart
import com.example.mmarecomp.util.PlateauStatus
import com.example.mmarecomp.viewmodel.WeighInViewModel

@Composable
fun WeighInScreen(viewModel: WeighInViewModel) {
    LaunchedEffect(Unit) { viewModel.loadHistory() }

    var showSaved by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Text("Log pesée", style = MaterialTheme.typography.titleLarge) }

        item {
            Text(
                "Tendance (moyenne 7 jours, matin à jeun)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            WeightTrendChart(points = viewModel.trend7Day, modifier = Modifier.fillMaxWidth().height(140.dp))
        }
        if (viewModel.plateauStatus == PlateauStatus.RECOMPOSITION_EN_COURS) {
            item { SoftAlertBanner("Poids stable mais tu progresses — recomposition en cours 💪") }
        }

        item { HorizontalDivider() }
        item { Text("Nouvelle pesée", style = MaterialTheme.typography.titleMedium) }

        item { DateField("Date", viewModel.date, { viewModel.date = it }, modifier = Modifier.fillMaxWidth()) }

        item {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = viewModel.type.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    WeighInType.entries.forEach { option ->
                        DropdownMenuItem(text = { Text(option.label) }, onClick = { viewModel.type = option; expanded = false })
                    }
                }
            }
        }

        item {
            val poidsInvalide = viewModel.poidsKg.isNotBlank() && viewModel.poidsKg.replace(",", ".").toDoubleOrNull() == null
            OutlinedTextField(
                value = viewModel.poidsKg,
                onValueChange = { viewModel.poidsKg = it },
                label = { Text("Poids (kg)") },
                isError = poidsInvalide,
                supportingText = if (poidsInvalide) {
                    { Text("Entre un nombre valide, ex. 82.5") }
                } else null,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = viewModel.bfPct,
                onValueChange = { viewModel.bfPct = it },
                label = { Text("% masse grasse (optionnel)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            ToggleRow("Créatine reprise récemment", viewModel.creatineRecente) { viewModel.creatineRecente = it }
        }
        item {
            ToggleRow("Alcool récent", viewModel.alcoolRecent) { viewModel.alcoolRecent = it }
        }
        item {
            ToggleRow("Juste après une séance intense", viewModel.postTraining) { viewModel.postTraining = it }
        }

        viewModel.errorMessage?.let { error ->
            item { ErrorBanner(error, onRetry = { viewModel.loadHistory() }) }
        }

        item {
            Button(
                onClick = {
                    viewModel.save { saved ->
                        showSaved = saved
                        if (saved) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.poidsKg = ""; viewModel.bfPct = ""
                        }
                    }
                },
                enabled = !viewModel.isSaving && viewModel.poidsKg.replace(",", ".").toDoubleOrNull() != null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (viewModel.isSaving) "Enregistrement…" else "Enregistrer la pesée") }
        }

        item {
            AnimatedVisibility(visible = showSaved, enter = fadeIn() + scaleIn(initialScale = 0.9f), exit = fadeOut()) {
                Text("Pesée enregistrée ✓", color = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
