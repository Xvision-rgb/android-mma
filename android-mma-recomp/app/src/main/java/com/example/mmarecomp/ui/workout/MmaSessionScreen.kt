package com.example.mmarecomp.ui.workout

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.mmarecomp.ui.AppPreferencesState
import com.example.mmarecomp.ui.components.KeepScreenOn
import com.example.mmarecomp.ui.components.VoiceInputButton
import com.example.mmarecomp.util.celebrationVibration
import com.example.mmarecomp.viewmodel.MmaSessionViewModel

@Composable
fun MmaSessionScreen(viewModel: MmaSessionViewModel, onSaved: () -> Unit) {
    val context = LocalContext.current
    var newTemplateName by remember { mutableStateOf("") }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    val prefs by AppPreferencesState.preferences

    val hasUnsavedChanges = viewModel.wodContent.isNotBlank() ||
        viewModel.roundsSets.isNotBlank() ||
        viewModel.notesTechnique.isNotBlank() ||
        viewModel.ressenti != 3

    LaunchedEffect(Unit) { viewModel.loadTemplates(context) }
    KeepScreenOn(enabled = prefs.keepScreenOnWhileLogging)

    BackHandler(enabled = prefs.confirmDiscardUnsavedChanges && hasUnsavedChanges) {
        showDiscardConfirm = true
    }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text("Quitter sans enregistrer ?") },
            text = { Text("Ta séance MMA en cours de saisie n'a pas été enregistrée.") },
            confirmButton = {
                TextButton(onClick = { showDiscardConfirm = false; onSaved() }) { Text("Quitter") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) { Text("Continuer la saisie") }
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Text("Log MMA", style = MaterialTheme.typography.titleLarge) }

        if (viewModel.templates.isNotEmpty()) {
            item {
                Text("Modèles enregistrés", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    viewModel.templates.forEach { template ->
                        FilterChip(
                            selected = false,
                            onClick = { viewModel.applyTemplate(template) },
                            label = { Text(template.name) },
                            trailingIcon = {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Supprimer le modèle ${template.name}",
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .clickable { viewModel.deleteTemplate(template, context) },
                                )
                            },
                        )
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = viewModel.wodContent,
                onValueChange = { viewModel.wodContent = it },
                label = { Text("WOD du coach (collé depuis WhatsApp)") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newTemplateName,
                    onValueChange = { newTemplateName = it },
                    label = { Text("Nom du modèle (ex: WOD standard)") },
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = {
                        viewModel.saveCurrentAsTemplate(newTemplateName, context)
                        newTemplateName = ""
                    },
                    enabled = newTemplateName.isNotBlank() && viewModel.wodContent.isNotBlank(),
                ) { Text("Sauver") }
            }
        }

        val parsed = remember(viewModel.wodContent) { viewModel.parsedMovements }
        if (parsed.isNotEmpty()) {
            item {
                Text("Mouvements détectés", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    parsed.take(4).forEach { movement ->
                        FilterChip(
                            selected = false,
                            onClick = {},
                            label = { Text(if (movement.quantite != null) "${movement.nom} ×${movement.quantite}" else movement.nom) },
                        )
                    }
                }
            }
        }

        item { HorizontalDivider() }

        item {
            OutlinedTextField(
                value = viewModel.roundsSets,
                onValueChange = { viewModel.roundsSets = it },
                label = { Text("Rounds / Sets (ex: 5 rounds x 3min)") },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            Text("Ressenti", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                (1..5).forEach { value ->
                    SegmentedButton(
                        selected = viewModel.ressenti == value,
                        onClick = { viewModel.ressenti = value },
                        shape = SegmentedButtonDefaults.itemShape(index = value - 1, count = 5),
                    ) { Text(value.toString()) }
                }
            }
        }

        item {
            OutlinedTextField(
                value = viewModel.notesTechnique,
                onValueChange = { viewModel.notesTechnique = it },
                label = { Text("Notes technique") },
                trailingIcon = {
                    VoiceInputButton { spoken ->
                        viewModel.notesTechnique = if (viewModel.notesTechnique.isBlank()) {
                            spoken
                        } else {
                            "${viewModel.notesTechnique} $spoken"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        viewModel.errorMessage?.let { error ->
            item { Text(error, color = MaterialTheme.colorScheme.error) }
        }

        item {
            Button(
                onClick = {
                    viewModel.save { saved ->
                        if (saved) {
                            if (prefs.vibrateOnAnySave) celebrationVibration(context)
                            onSaved()
                        }
                    }
                },
                enabled = !viewModel.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (viewModel.isSaving) "Enregistrement…" else "Enregistrer la séance MMA")
            }
        }
    }
}
