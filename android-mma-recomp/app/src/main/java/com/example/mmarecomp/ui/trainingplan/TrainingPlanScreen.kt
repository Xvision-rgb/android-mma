package com.example.mmarecomp.ui.trainingplan

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.mmarecomp.model.Phase
import com.example.mmarecomp.model.PlanDayType
import com.example.mmarecomp.model.PlannedExercise
import com.example.mmarecomp.model.TrainingPlanDay
import com.example.mmarecomp.model.joursLabels
import com.example.mmarecomp.ui.AppPreferencesState
import com.example.mmarecomp.util.celebrationVibration
import com.example.mmarecomp.viewmodel.TrainingPlanViewModel

@Composable
fun TrainingPlanScreen(viewModel: TrainingPlanViewModel, phase: Phase, onBack: () -> Unit) {
    LaunchedEffect(phase) { viewModel.load(phase) }

    val prefs by AppPreferencesState.preferences
    val context = LocalContext.current
    var showDiscardConfirm by remember { mutableStateOf(false) }

    BackHandler(enabled = prefs.confirmDiscardUnsavedChanges && viewModel.hasUnsavedChanges) {
        showDiscardConfirm = true
    }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text("Quitter sans enregistrer ?") },
            text = { Text("Des modifications du split ne sont pas encore enregistrées pour au moins un jour.") },
            confirmButton = {
                TextButton(onClick = { showDiscardConfirm = false; onBack() }) { Text("Quitter") }
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
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Split hebdomadaire", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Programme ta semaine type — utilisé pour pré-remplir le log séance chaque jour.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        viewModel.errorMessage?.let { error ->
            item { Text(error, color = MaterialTheme.colorScheme.error) }
        }

        items(viewModel.days, key = { it.jourSemaine }) { day ->
            DayEditorCard(
                day = day,
                isSaving = viewModel.savingDay == day.jourSemaine,
                onChange = { viewModel.updateDay(day.jourSemaine, it) },
                onSave = {
                    viewModel.saveDay(day) { saved ->
                        if (saved && prefs.vibrateOnAnySave) celebrationVibration(context)
                    }
                },
            )
        }
    }
}

@Composable
private fun DayEditorCard(
    day: TrainingPlanDay,
    isSaving: Boolean,
    onChange: (TrainingPlanDay) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(joursLabels[day.jourSemaine] ?: "Jour ${day.jourSemaine}", style = MaterialTheme.typography.titleMedium)

        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = day.type.label,
                onValueChange = {},
                readOnly = true,
                label = { Text("Type") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                PlanDayType.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            onChange(day.copy(type = option))
                            expanded = false
                        },
                    )
                }
            }
        }

        if (day.type != PlanDayType.Repos) {
            day.exercices.forEachIndexed { index, exercice ->
                PlannedExerciseRow(
                    exercice = exercice,
                    onChange = { updated ->
                        onChange(day.copy(exercices = day.exercices.toMutableList().also { it[index] = updated }))
                    },
                    onRemove = {
                        onChange(day.copy(exercices = day.exercices.filterIndexed { i, _ -> i != index }))
                    },
                )
            }
            TextButton(onClick = {
                onChange(day.copy(exercices = day.exercices + PlannedExercise(nom = "", series = 3, reps = 10)))
            }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Ajouter un exercice")
            }
        }

        Button(onClick = onSave, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) {
            Text(if (isSaving) "Enregistrement…" else "Enregistrer")
        }
    }
}
