package com.example.mmarecomp.ui.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.mmarecomp.R
import com.example.mmarecomp.model.LoggedExercise
import com.example.mmarecomp.model.Phase
import com.example.mmarecomp.model.WorkoutType
import com.example.mmarecomp.ui.components.AppScaffold
import com.example.mmarecomp.ui.components.DateField
import com.example.mmarecomp.ui.components.EmptyState
import com.example.mmarecomp.ui.components.ErrorBanner
import com.example.mmarecomp.ui.components.ErrorOperation
import com.example.mmarecomp.ui.components.PrimaryActionBar
import com.example.mmarecomp.ui.components.SessionModulationBanner
import com.example.mmarecomp.ui.theme.Dimens
import com.example.mmarecomp.ui.theme.workoutTypeColor
import com.example.mmarecomp.viewmodel.WorkoutLogViewModel
import kotlinx.coroutines.launch

@Composable
fun WorkoutLogScreen(viewModel: WorkoutLogViewModel, phase: Phase, onOpenMmaSheet: () -> Unit) {
    // Le biais d'estimation du RIR vit dans SharedPreferences (donc a besoin
    // d'un Context, que le ViewModel n'a pas) : l'écran fait le pont. Sans
    // cette ligne, biaisRir restait à 0 et la correction n'agissait jamais.
    val calibrationContext = androidx.compose.ui.platform.LocalContext.current
    val rirCalibration = remember(calibrationContext) {
        com.example.mmarecomp.util.RirCalibration(calibrationContext)
    }
    LaunchedEffect(rirCalibration) { viewModel.biaisRir = rirCalibration.biais }

    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showResetConfirm by remember { mutableStateOf(false) }
    var replacePickerIndex by remember { mutableStateOf<Int?>(null) }
    val existingWorkout = viewModel.existingWorkoutForType

    fun removeWithUndo(index: Int, exercice: LoggedExercise) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.removeExercise(index)
        val undoMessage = context.getString(R.string.workout_undo_exercise)
        val undoAction = context.getString(R.string.workout_undo)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = undoMessage,
                actionLabel = undoAction,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.restoreExercise(index, exercice)
        }
    }

    fun deleteWorkoutWithUndo(workout: com.example.mmarecomp.model.Workout) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        val undoMessage = context.getString(R.string.workout_undo_session)
        val undoAction = context.getString(R.string.workout_undo)
        viewModel.deleteFromHistory(workout) {
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = undoMessage,
                    actionLabel = undoAction,
                    duration = SnackbarDuration.Long,
                )
                if (result == SnackbarResult.ActionPerformed) viewModel.restoreToHistory(workout)
            }
        }
    }

    fun performSave() {
        val wasUpdate = viewModel.existingWorkoutForType != null
        viewModel.save { saved ->
            if (saved) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = context.getString(
                            if (wasUpdate) R.string.workout_save_update else R.string.workout_saved,
                        ),
                        duration = SnackbarDuration.Short,
                    )
                }
                viewModel.loadRecent()
                viewModel.loadWorkoutsForDate()
            }
        }
    }

    LaunchedEffect(viewModel.date, phase) { viewModel.loadPlan(phase) }
    LaunchedEffect(viewModel.date) {
        viewModel.loadWorkoutsForDate()
        viewModel.loadReadiness()
    }
    LaunchedEffect(Unit) { viewModel.loadRecent() }

    LaunchedEffect(viewModel.modulationSnackbarMessage) {
        viewModel.modulationSnackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Long)
            viewModel.clearModulationSnackbar()
        }
    }

    fun applyModulation() {
        if (viewModel.applyModulationToWorkout()) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            scope.launch {
                val message = viewModel.dernierResumeModulation.joinToString(" · ")
                    .ifBlank { context.getString(R.string.workout_modulation_applied) }
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Long,
                )
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(stringResource(R.string.workout_reset_confirm_title)) },
            text = { Text(stringResource(R.string.workout_reset_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetForm()
                    showResetConfirm = false
                }) { Text(stringResource(R.string.workout_reset_confirm_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(stringResource(R.string.workout_reset_confirm_cancel))
                }
            },
        )
    }

    AppScaffold(
        title = stringResource(R.string.workout_log_title),
        bottomBar = {
            PrimaryActionBar(
                label = if (viewModel.isSaving) {
                    stringResource(R.string.workout_saving)
                } else {
                    stringResource(
                        if (existingWorkout != null) R.string.workout_save_update else R.string.workout_save,
                    )
                },
                enabled = !viewModel.isSaving,
                onClick = { performSave() },
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
            SessionModulationBanner(
                modulation = viewModel.modulation,
                scoreReadiness = viewModel.scoreReadiness,
                aCheckInAujourdhui = viewModel.aCheckInAujourdhui,
                modulationApplied = viewModel.modulationApplied,
                resumeModulation = viewModel.dernierResumeModulation,
                peutAppliquer = viewModel.exercices.isNotEmpty(),
                onApply = { applyModulation() },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                WorkoutType.entries.forEach { option ->
                    val logged = viewModel.loggedTypesForDate.contains(option)
                    FilterChip(
                        selected = viewModel.type == option,
                        onClick = { viewModel.selectType(option) },
                        label = { Text(if (logged) "${option.label} ✓" else option.label) },
                    )
                }
            }
            existingWorkout?.let {
                Text(
                    stringResource(R.string.workout_type_logged, it.type.label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = Dimens.spaceXs),
                )
            }
            Text(
                stringResource(R.string.workout_tap_history_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Dimens.spaceXs),
            )
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
                TextButton(onClick = { showResetConfirm = true }) { Text("Vider le formulaire") }
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

        if (viewModel.recentWorkouts.isNotEmpty()) {
            item {
                RecentExerciseChipsRow(
                    entries = viewModel.recentExercisesForAdd(),
                    onSelect = { entry ->
                        viewModel.addExerciseFromRecent(entry)
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = context.getString(R.string.workout_exercise_added, entry.nom),
                                duration = SnackbarDuration.Short,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item {
            val strengthCount = viewModel.exercices.count { !it.isCardio }
            val cardioCount = viewModel.exercices.count { it.isCardio }
            val totalSeries = viewModel.exercices.filterNot { it.isCardio }.sumOf { it.effectiveSets.size }
            Text(
                when {
                    viewModel.exercices.isEmpty() -> "Exercices"
                    else -> buildString {
                        append("Exercices (${viewModel.exercices.size})")
                        if (strengthCount > 0) append(" · $totalSeries séries force")
                        if (cardioCount > 0) append(" · $cardioCount cardio")
                    }
                },
                style = MaterialTheme.typography.titleMedium,
            )
            val volumeTotal = viewModel.exercices.sumOf { it.volumeTotal }
            if (volumeTotal > 0) {
                Text(
                    "Volume force : ${volumeTotal.toInt()}kg (Σ reps × charge)",
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
            val (cardioMin, cardioKcal) = viewModel.cardioSummary()
            if (cardioMin > 0 || cardioKcal > 0) {
                Text(
                    buildString {
                        append("Cardio : ")
                        if (cardioMin > 0) append("${cardioMin} min")
                        if (cardioKcal > 0) {
                            if (cardioMin > 0) append(" · ")
                            append("~$cardioKcal kcal estimées")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Text(
                "Presets cardio — tap pour ajouter",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            ) {
                com.example.mmarecomp.util.CardioEnergy.PRESETS.forEach { preset ->
                    FilterChip(
                        selected = false,
                        onClick = { viewModel.addCardioPreset(preset.nom) },
                        label = { Text(preset.nom) },
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = viewModel.rpe,
                onValueChange = { viewModel.rpe = it.filter { c -> c.isDigit() }.take(2) },
                label = { Text("RPE de la séance (1-10)") },
                supportingText = {
                    Text("Difficulté ressentie globale. Multipliée par la durée, elle donne la charge interne dont dépend l'état du jour.")
                },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
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
                    rirBonusModulation = viewModel.rirBonusModulation,
                    seuilChuteStrict = com.example.mmarecomp.util.SetStopAdvisor.estStrict(viewModel.type),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (viewModel.recentWorkouts.isNotEmpty()) {
                        TextButton(onClick = { replacePickerIndex = index }) {
                            Text(stringResource(R.string.workout_replace_exercise))
                        }
                    }
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
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.loadWorkoutIntoForm(workout) }
                                    .padding(vertical = Dimens.spaceXs),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(Dimens.dotSm)
                                                .background(
                                                    workoutTypeColor(workout.type),
                                                    androidx.compose.foundation.shape.CircleShape,
                                                ),
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
                                if (workout.exercices.isNotEmpty()) {
                                    Text(
                                        workout.exercices.take(3).joinToString(" · ") { it.nom.ifBlank { "—" } } +
                                            if (workout.exercices.size > 3) "…" else "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        viewModel.screenError?.let { error ->
            item {
                ErrorBanner(
                    error = error,
                    onRetry = {
                        when (error.operation) {
                            ErrorOperation.LOAD -> {
                                viewModel.loadPlan(phase)
                                viewModel.loadRecent()
                                viewModel.loadWorkoutsForDate()
                            }
                            ErrorOperation.SAVE -> performSave()
                            ErrorOperation.DELETE -> viewModel.retryPendingDelete()
                            ErrorOperation.UPDATE -> viewModel.loadPlan(phase)
                        }
                    },
                )
            }
        }

    }
        replacePickerIndex?.let { index ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center,
            ) {
                ExercisePickerSheet(
                    title = stringResource(R.string.workout_picker_replace_title),
                    entries = viewModel.recentExercisesForReplace(index),
                    onSelect = { entry ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.replaceExerciseFromRecent(index, entry)
                        replacePickerIndex = null
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = context.getString(R.string.workout_exercise_replaced, entry.nom),
                                duration = SnackbarDuration.Short,
                            )
                        }
                    },
                    onDismiss = { replacePickerIndex = null },
                    modifier = Modifier.padding(Dimens.spaceMd),
                )
            }
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
    }
}
