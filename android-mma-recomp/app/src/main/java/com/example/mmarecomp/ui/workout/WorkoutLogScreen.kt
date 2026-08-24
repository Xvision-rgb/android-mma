package com.example.mmarecomp.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mmarecomp.model.Phase
import com.example.mmarecomp.model.WorkoutType
import com.example.mmarecomp.ui.AppPreferencesState
import com.example.mmarecomp.ui.components.DateField
import com.example.mmarecomp.ui.components.SoftAlertBanner
import com.example.mmarecomp.ui.components.VoiceInputButton
import com.example.mmarecomp.util.celebrationVibration
import com.example.mmarecomp.viewmodel.WorkoutLogViewModel
import kotlinx.coroutines.launch

@Composable
fun WorkoutLogScreen(viewModel: WorkoutLogViewModel, phase: Phase, onOpenMmaSheet: () -> Unit) {
    var showSavedMessage by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs by AppPreferencesState.preferences

    LaunchedEffect(viewModel.date, phase) { viewModel.loadPlan(phase) }
    LaunchedEffect(viewModel.type) { viewModel.autoFillLastDurationIfNeeded() }
    LaunchedEffect(Unit) { viewModel.loadHistoryIfNeeded() }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(scaffoldPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { Text("Log séance", style = MaterialTheme.typography.titleLarge) }

            item { DateField("Date", viewModel.date, { viewModel.date = it }, modifier = Modifier.fillMaxWidth()) }

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
                        WorkoutType.entries.forEach { option ->
                            DropdownMenuItem(text = { Text(option.label) }, onClick = {
                                viewModel.type = option
                                expanded = false
                            })
                        }
                    }
                }
            }

            if (viewModel.type == WorkoutType.MmaWod) {
                item { TextButton(onClick = onOpenMmaSheet) { Text("Ouvrir le log WOD MMA") } }
            }

            item {
                OutlinedTextField(
                    value = viewModel.dureeMin,
                    onValueChange = { viewModel.dureeMin = it },
                    label = { Text("Durée (min)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item { HorizontalDivider() }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Exercices", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = {
                        viewModel.duplicateLastWorkout { duplicated ->
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (duplicated) "Dernière séance ${viewModel.type.label} recopiée" else "Pas de séance précédente de ce type",
                                )
                            }
                        }
                    }) { Text("Dupliquer la dernière") }
                }
            }

            itemsIndexed(viewModel.exercices) { index, exercice ->
                Column {
                    ExerciseRow(
                        exercice = exercice,
                        onChange = { viewModel.updateExercise(index, it) },
                        history = viewModel.recentHistory,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            viewModel.removeExercise(index)
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Exercice retiré",
                                    actionLabel = "Annuler",
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.insertExercise(index, exercice)
                                }
                            }
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Retirer cet exercice")
                        }
                        Text("Retirer cet exercice", style = MaterialTheme.typography.bodySmall)
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
                    label = { Text("Notes, ressenti…") },
                    trailingIcon = {
                        VoiceInputButton { spoken ->
                            viewModel.notes = if (viewModel.notes.isBlank()) spoken else "${viewModel.notes} $spoken"
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
                            showSavedMessage = saved
                            if (saved && prefs.celebratePrWithVibration && viewModel.newRecords.isNotEmpty()) {
                                celebrationVibration(context)
                            }
                        }
                    },
                    enabled = !viewModel.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (viewModel.isSaving) "Enregistrement…" else "Enregistrer la séance")
                }
            }

            if (showSavedMessage) {
                item { Text("Séance enregistrée 💪", color = MaterialTheme.colorScheme.tertiary) }
                if (viewModel.newRecords.isNotEmpty()) {
                    item {
                        val label = if (viewModel.newRecords.size == 1) {
                            "Nouveau record personnel sur ${viewModel.newRecords.first()} 🏆"
                        } else {
                            "Nouveaux records personnels : ${viewModel.newRecords.joinToString(", ")} 🏆"
                        }
                        SoftAlertBanner(message = label, icon = Icons.Filled.EmojiEvents)
                    }
                }
            }
        }
    }
}
