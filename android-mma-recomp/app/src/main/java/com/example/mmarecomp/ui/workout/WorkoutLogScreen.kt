package com.example.mmarecomp.ui.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mmarecomp.model.LoggedExercise
import com.example.mmarecomp.model.Phase
import com.example.mmarecomp.model.WorkoutType
import com.example.mmarecomp.ui.components.AppScaffold
import com.example.mmarecomp.ui.components.DateField
import com.example.mmarecomp.ui.components.EmptyState
import com.example.mmarecomp.ui.components.ErrorBanner
import com.example.mmarecomp.ui.components.PrimaryActionBar
import com.example.mmarecomp.ui.theme.Dimens
import com.example.mmarecomp.ui.theme.workoutTypeColor
import com.example.mmarecomp.viewmodel.WorkoutLogViewModel
import kotlinx.coroutines.launch

@Composable
fun WorkoutLogScreen(viewModel: WorkoutLogViewModel, phase: Phase, onOpenMmaSheet: () -> Unit) {
    var showSavedMessage by remember { mutableStateOf(false) }
    LaunchedEffect(showSavedMessage) {
        if (showSavedMessage) {
            kotlinx.coroutines.delay(4000)
            showSavedMessage = false
        }
    }
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun removeWithUndo(index: Int, exercice: LoggedExercise) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.removeExercise(index)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "Exercice retiré",
                actionLabel = "Annuler",
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.restoreExercise(index, exercice)
        }
    }

    fun deleteWorkoutWithUndo(workout: com.example.mmarecomp.model.Workout) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.deleteFromHistory(workout) {
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "Séance supprimée",
                    actionLabel = "Annuler",
                    duration = SnackbarDuration.Long,
                )
                if (result == SnackbarResult.ActionPerformed) viewModel.restoreToHistory(workout)
            }
        }
    }

    LaunchedEffect(viewModel.date, phase) { viewModel.loadPlan(phase) }
    LaunchedEffect(Unit) { viewModel.loadRecent() }

    AppScaffold(
        title = "Log séance",
        bottomBar = {
            PrimaryActionBar(
                label = if (viewModel.isSaving) "Enregistrement…" else "Enregistrer la séance",
                enabled = !viewModel.isSaving,
                onClick = {
                    viewModel.save { saved ->
                        showSavedMessage = saved
                        if (saved) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.loadRecent()
                        }
                    }
                },
            )
        },
    ) { padding ->
    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Dimens.spaceMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
    ) {
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
                        DropdownMenuItem(
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(Dimens.dotMd)
                                        .background(workoutTypeColor(option), androidx.compose.foundation.shape.CircleShape),
                                )
                            },
                            text = { Text(option.label) },
                            onClick = {
                                viewModel.type = option
                                expanded = false
                            },
                        )
                    }
                }
            }
        }

        if (viewModel.type == WorkoutType.MmaWod) {
            item { TextButton(onClick = onOpenMmaSheet) { Text("Ouvrir le log WOD MMA") } }
        }

        item {
            var duplicateFeedback by remember { mutableStateOf<Boolean?>(null) }
            LaunchedEffect(duplicateFeedback) {
                if (duplicateFeedback != null) {
                    kotlinx.coroutines.delay(4000)
                    duplicateFeedback = null
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
                TextButton(onClick = { viewModel.duplicateFromYesterday { found -> duplicateFeedback = found } }) {
                    Text("Reprendre la séance d'hier")
                }
                TextButton(onClick = { viewModel.resetForm() }) { Text("Vider le formulaire") }
            }
            duplicateFeedback?.let { found ->
                Text(
                    if (found) "Séance d'hier reprise ✓" else "Rien à reprendre — pas de séance loguée hier",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
            OutlinedTextField(
                value = viewModel.dureeMin,
                onValueChange = { viewModel.dureeMin = it },
                label = { Text("Durée (min)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onDone = { focusManager.clearFocus() },
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                listOf(30, 45, 60, 90).forEach { minutes ->
                    FilterChip(
                        selected = viewModel.dureeMin == minutes.toString(),
                        onClick = { viewModel.dureeMin = minutes.toString() },
                        label = { Text("${minutes}min") },
                    )
                }
            }
        }

        item { HorizontalDivider() }
        if (viewModel.prefilledFromPlan) {
            item {
                Text(
                    "Pré-rempli depuis ton split programmé — modifie librement si besoin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            val totalSeries = viewModel.exercices.sumOf { it.effectiveSets.size }
            Text(
                when {
                    viewModel.exercices.isEmpty() -> "Exercices"
                    else -> "Exercices (${viewModel.exercices.size}) · $totalSeries séries au total"
                },
                style = MaterialTheme.typography.titleMedium,
            )
            val volumeTotal = viewModel.exercices.sumOf { it.volumeTotal }
            if (volumeTotal > 0) {
                Text(
                    "Volume estimé : ${volumeTotal.toInt()}kg (Σ reps × charge, série par série)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                viewModel.previousSessionVolume?.let { previous ->
                    Text(
                        "Dernière séance ${viewModel.type.label} : ${previous.toInt()}kg — juste un repère, pas un objectif à battre.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item { RestTimer() }

        if (viewModel.exercices.isEmpty()) {
            item {
                EmptyState(
                    title = "Aucun exercice pour l'instant",
                    subtitle = "Ajoute-en un pour commencer ta séance.",
                    icon = Icons.Filled.FitnessCenter,
                    actionLabel = "Ajouter un exercice",
                    onAction = { viewModel.addExercise() },
                )
            }
        }

        itemsIndexed(viewModel.exercices) { index, exercice ->
            Column {
                ExerciseRow(
                    exercice = exercice,
                    onChange = { updated ->
                        viewModel.updateExercise(index, updated)
                        if (updated.nom != exercice.nom) viewModel.prefillChargeFromLastKnown(index, updated.nom)
                    },
                    lastKnownCharge = viewModel.lastKnownCharge(exercice.nom),
                    personalRecordKg = viewModel.personalRecordCharge(exercice.nom),
                    protocole = viewModel.protocoleApre,
                    incrementKg = viewModel.incrementChargeKg,
                    biaisRir = viewModel.biaisRir,
                    seuilChuteStrict = com.example.mmarecomp.util.SetStopAdvisor.estStrict(viewModel.type),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.moveExerciseUp(index) }, enabled = index > 0) {
                        Icon(
                            Icons.Filled.KeyboardArrowUp,
                            contentDescription = "Monter cet exercice",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { viewModel.moveExerciseDown(index) }, enabled = index < viewModel.exercices.size - 1) {
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
                    IconButton(onClick = { removeWithUndo(index, exercice) }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Retirer cet exercice",
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
                label = { Text("Notes, ressenti…") },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (viewModel.recentWorkouts.isNotEmpty()) {
            item {
                var showHistory by remember { mutableStateOf(false) }
                var historyTypeFilter by remember { mutableStateOf<WorkoutType?>(null) }
                Column {
                    TextButton(onClick = { showHistory = !showHistory }) {
                        Text(if (showHistory) "Masquer l'historique des séances" else "Voir l'historique des séances")
                    }
                    if (showHistory) {
                        val availableTypes = viewModel.recentWorkouts.map { it.type }.distinct()
                        if (availableTypes.size > 1) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
                                modifier = Modifier
                                    .padding(bottom = Dimens.spaceSm)
                                    .horizontalScroll(rememberScrollState()),
                            ) {
                                availableTypes.forEach { availableType ->
                                    FilterChip(
                                        selected = historyTypeFilter == availableType,
                                        onClick = {
                                            historyTypeFilter = if (historyTypeFilter == availableType) null else availableType
                                        },
                                        label = { Text(availableType.label) },
                                    )
                                }
                            }
                        }
                        val filteredHistory = historyTypeFilter?.let { filter ->
                            viewModel.recentWorkouts.filter { it.type == filter }
                        } ?: viewModel.recentWorkouts
                        filteredHistory.forEach { workout ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
                                    Box(
                                        modifier = Modifier
                                            .size(Dimens.dotSm)
                                            .background(workoutTypeColor(workout.type), androidx.compose.foundation.shape.CircleShape),
                                    )
                                    Text(
                                        "${workout.date} · ${workout.type.label}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "${workout.exercices.size} exercice(s)" + (workout.dureeMin?.let { " · ${it}min" } ?: ""),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    IconButton(onClick = { deleteWorkoutWithUndo(workout) }) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "Supprimer cette séance du ${workout.date}",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        viewModel.errorMessage?.let { error ->
            item { ErrorBanner(error, onRetry = { viewModel.loadPlan(phase) }) }
        }

        item {
            AnimatedVisibility(
                visible = showSavedMessage,
                enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.9f, animationSpec = tween(200)),
                exit = fadeOut(tween(200)),
            ) {
                Text("Séance enregistrée 💪", color = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
    }
}
