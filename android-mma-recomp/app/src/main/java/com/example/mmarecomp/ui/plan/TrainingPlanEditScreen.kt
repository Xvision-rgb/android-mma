package com.example.mmarecomp.ui.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mmarecomp.model.Phase
import com.example.mmarecomp.model.PlanDayType
import com.example.mmarecomp.model.joursLabels
import com.example.mmarecomp.ui.components.EmptyState
import com.example.mmarecomp.ui.components.ErrorBanner
import com.example.mmarecomp.viewmodel.TrainingPlanEditViewModel
import kotlinx.coroutines.launch

/** Édite les exercices programmés d'un jour du split hebdo — brouillon local,
 *  un seul enregistrement explicite (bouton "Enregistrer"), rien n'est écrit
 *  en base à chaque frappe. */
@Composable
fun TrainingPlanEditScreen(
    viewModel: TrainingPlanEditViewModel,
    jourSemaine: Int,
    phase: Phase,
    onSaved: () -> Unit,
) {
    LaunchedEffect(jourSemaine, phase) { viewModel.load(jourSemaine, phase) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (viewModel.isLoading && viewModel.exercices.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Box
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(
                    "Programme — ${joursLabels[jourSemaine] ?: ""}",
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            item {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = viewModel.type.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type de séance") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        PlanDayType.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = { viewModel.type = option; expanded = false },
                            )
                        }
                    }
                }
            }

            item { HorizontalDivider() }
            item {
                Text(
                    "Exercices (${viewModel.exercices.size})",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            if (viewModel.exercices.isEmpty()) {
                item {
                    EmptyState(
                        title = "Aucun exercice programmé pour ce jour",
                        subtitle = "Ajoute-en un avec le bouton ci-dessous, ou importe un programme complet depuis Réglages.",
                    )
                }
            }

            itemsIndexed(viewModel.exercices) { index, exercice ->
                Column {
                    PlannedExerciseRow(
                        exercice = exercice,
                        onChange = { viewModel.updateExercise(index, it) },
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.moveExerciseUp(index) }, enabled = index > 0) {
                            Icon(
                                Icons.Filled.KeyboardArrowUp,
                                contentDescription = "Monter cet exercice",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(
                            onClick = { viewModel.moveExerciseDown(index) },
                            enabled = index < viewModel.exercices.size - 1,
                        ) {
                            Icon(
                                Icons.Filled.KeyboardArrowDown,
                                contentDescription = "Descendre cet exercice",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { viewModel.duplicateExercise(index) }) {
                            Icon(
                                Icons.Filled.ContentCopy,
                                contentDescription = "Dupliquer cet exercice",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { viewModel.removeExercise(index) }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Retirer cet exercice du programme",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item {
                TextButton(onClick = { viewModel.addExercise() }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Ajouter un exercice")
                }
            }

            item { HorizontalDivider() }

            item {
                OutlinedTextField(
                    value = viewModel.notes,
                    onValueChange = { viewModel.notes = it },
                    label = { Text("Notes (optionnel)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            viewModel.errorMessage?.let { error ->
                item { ErrorBanner(error, onRetry = { viewModel.load(jourSemaine, phase) }) }
            }

            if (viewModel.hasUnsavedChanges) {
                item {
                    Text(
                        "Modifications non enregistrées",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                val scope = rememberCoroutineScope()
                Button(
                    onClick = {
                        viewModel.save { saved ->
                            if (saved) {
                                scope.launch {
                                    kotlinx.coroutines.delay(500)
                                    onSaved()
                                }
                            }
                        }
                    },
                    enabled = !viewModel.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (viewModel.isSaving) "Enregistrement…" else "Enregistrer le programme")
                }
            }
        }
    }
}
