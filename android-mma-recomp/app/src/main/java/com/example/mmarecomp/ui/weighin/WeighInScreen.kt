package com.example.mmarecomp.ui.weighin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.mmarecomp.R
import com.example.mmarecomp.model.WeighIn
import com.example.mmarecomp.model.WeighInType
import com.example.mmarecomp.ui.components.AppScaffold
import com.example.mmarecomp.ui.components.DateField
import com.example.mmarecomp.ui.components.ErrorBanner
import com.example.mmarecomp.ui.components.ErrorOperation
import com.example.mmarecomp.ui.components.PrimaryActionBar
import com.example.mmarecomp.ui.components.SoftAlertBanner
import com.example.mmarecomp.ui.components.WeightTrendChart
import com.example.mmarecomp.ui.theme.Dimens
import com.example.mmarecomp.util.Formatting
import com.example.mmarecomp.util.PlateauStatus
import com.example.mmarecomp.viewmodel.WeighInViewModel
import kotlinx.coroutines.launch

@Composable
fun WeighInScreen(viewModel: WeighInViewModel, onBack: () -> Unit = {}) {
    LaunchedEffect(Unit) { viewModel.loadHistory() }

    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showResetConfirm by remember { mutableStateOf(false) }
    val existingEntry = viewModel.existingEntryForSelection

    LaunchedEffect(viewModel.date, viewModel.type, viewModel.history) {
        viewModel.loadEntryForCurrentSelection()
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(stringResource(R.string.weigh_in_reset_confirm_title)) },
            text = { Text(stringResource(R.string.weigh_in_reset_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetForm()
                    showResetConfirm = false
                }) { Text(stringResource(R.string.weigh_in_reset_confirm_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(stringResource(R.string.weigh_in_reset_confirm_cancel))
                }
            },
        )
    }

    fun deleteWithUndo(entry: WeighIn) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        val undoMessage = context.getString(R.string.weigh_in_undo)
        val undoAction = context.getString(R.string.weigh_in_undo_action)
        viewModel.deleteFromHistory(entry) {
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = undoMessage,
                    actionLabel = undoAction,
                    duration = SnackbarDuration.Long,
                )
                if (result == SnackbarResult.ActionPerformed) viewModel.restoreToHistory(entry)
            }
        }
    }

    fun performSave() {
        val savedType = viewModel.type
        val savedDate = viewModel.date
        viewModel.save { saved ->
            if (saved) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = context.getString(
                            if (existingEntry != null) R.string.weigh_in_saved_update else R.string.weigh_in_saved,
                        ),
                        duration = SnackbarDuration.Short,
                    )
                }
                if (savedType == WeighInType.MatinJeun && savedDate == java.time.LocalDate.now()) {
                    com.example.mmarecomp.notification.WeighInReminder.markLoggedToday(
                        context,
                        com.example.mmarecomp.util.DateUtils.string(savedDate),
                    )
                }
            }
        }
    }

    AppScaffold(
        title = stringResource(R.string.weigh_in_title),
        onBack = onBack,
        bottomBar = {
            PrimaryActionBar(
                label = if (viewModel.isSaving) {
                    stringResource(R.string.weigh_in_saving)
                } else {
                    stringResource(
                        if (existingEntry != null) R.string.weigh_in_save_update else R.string.weigh_in_save,
                    )
                },
                enabled = !viewModel.isSaving &&
                    (viewModel.poidsKg.replace(",", ".").toDoubleOrNull() ?: -1.0).let { it in 20.0..400.0 },
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

        item {
            Text(
                stringResource(R.string.weigh_in_trend_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            WeightTrendChart(points = viewModel.trend7Day, modifier = Modifier.fillMaxWidth().height(Dimens.chartHeight))
        }
        item {
            val direction = com.example.mmarecomp.util.MovingAverage.direction(viewModel.trend7Day)
            val label = when (direction) {
                com.example.mmarecomp.util.TrendDirection.HAUSSE -> "Tendance : légère hausse"
                com.example.mmarecomp.util.TrendDirection.BAISSE -> "Tendance : légère baisse"
                com.example.mmarecomp.util.TrendDirection.STABLE -> "Tendance : stable"
                com.example.mmarecomp.util.TrendDirection.INDETERMINE -> null
            }
            label?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (viewModel.plateauStatus == PlateauStatus.RECOMPOSITION_EN_COURS) {
            item { SoftAlertBanner("Poids stable mais tu progresses — recomposition en cours 💪") }
        }
        viewModel.daysSinceLastMorningEntry?.let { days ->
            item {
                val label = when (days) {
                    0L -> "Dernière pesée : aujourd'hui"
                    1L -> "Dernière pesée : hier"
                    else -> "Dernière pesée : il y a $days jours"
                }
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (viewModel.history.isNotEmpty()) {
            item {
                var showHistory by remember { mutableStateOf(false) }
                Column {
                    TextButton(onClick = { showHistory = !showHistory }) {
                        Text(
                            if (showHistory) {
                                stringResource(R.string.weigh_in_hide_history)
                            } else {
                                stringResource(R.string.weigh_in_show_history)
                            },
                        )
                    }
                    if (showHistory) {
                        viewModel.history.sortedByDescending { it.date }.take(20).forEach { entry ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.loadEntryIntoForm(entry) }
                                    .padding(vertical = Dimens.spaceXs),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "${entry.date} · ${entry.type.label}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "${entry.poidsKg} kg" + (entry.bfPct?.let { " · ${Formatting.oneDecimal(it)}%" } ?: ""),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                        IconButton(onClick = { deleteWithUndo(entry) }) {
                                            Icon(
                                                Icons.Filled.Delete,
                                                contentDescription = "Supprimer cette pesée du ${entry.date}",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                                if (entry.contexte.hasAnyFlag) {
                                    Text(
                                        entry.contexte.flagLabels.joinToString(" · "),
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

        item { HorizontalDivider() }
        item { Text("Nouvelle pesée", style = MaterialTheme.typography.titleMedium) }

        item { DateField("Date", viewModel.date, { viewModel.date = it }, modifier = Modifier.fillMaxWidth()) }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                WeighInType.entries.forEach { option ->
                    val logged = viewModel.history.any {
                        it.date == com.example.mmarecomp.util.DateUtils.string(viewModel.date) && it.type == option
                    }
                    FilterChip(
                        selected = viewModel.type == option,
                        onClick = { viewModel.selectType(option) },
                        label = { Text(if (logged) "${option.label} ✓" else option.label) },
                    )
                }
            }
            existingEntry?.let {
                Text(
                    stringResource(R.string.weigh_in_slot_logged, it.type.label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = Dimens.spaceXs),
                )
            }
        }

        if (viewModel.history.any { it.type == viewModel.type }) {
            item {
                TextButton(onClick = { viewModel.prefillFromLastEntry() }) {
                    Text("Reprendre les valeurs de la dernière pesée")
                }
            }
        }

        item {
            Text(
                stringResource(R.string.weigh_in_tap_history_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = viewModel.type.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type (autre liste)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    WeighInType.entries.forEach { option ->
                        DropdownMenuItem(text = { Text(option.label) }, onClick = { viewModel.selectType(option); expanded = false })
                    }
                }
            }
        }

        item {
            val poidsValeur = viewModel.poidsKg.replace(",", ".").toDoubleOrNull()
            val poidsInvalide = viewModel.poidsKg.isNotBlank() && (poidsValeur == null || poidsValeur < 20 || poidsValeur > 400)
            OutlinedTextField(
                value = viewModel.poidsKg,
                onValueChange = { viewModel.poidsKg = it },
                label = { Text("Poids (kg)") },
                isError = poidsInvalide,
                supportingText = if (poidsInvalide) {
                    { Text("Entre un poids réaliste, ex. 82.5") }
                } else null,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
            val bfInvalide = viewModel.bfPct.isNotBlank() &&
                (viewModel.bfPct.replace(",", ".").toDoubleOrNull() ?: -1.0).let { it < 0 || it > 60 }
            OutlinedTextField(
                value = viewModel.bfPct,
                onValueChange = { viewModel.bfPct = it },
                label = { Text("% masse grasse (optionnel)") },
                isError = bfInvalide,
                supportingText = if (bfInvalide) { { Text("Entre un pourcentage entre 0 et 60") } } else null,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onDone = { focusManager.clearFocus() },
                ),
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

        viewModel.screenError?.let { error ->
            item {
                ErrorBanner(
                    error = error,
                    onRetry = {
                        when (error.operation) {
                            ErrorOperation.LOAD -> viewModel.loadHistory()
                            ErrorOperation.SAVE -> performSave()
                            ErrorOperation.DELETE -> viewModel.retryPendingDelete()
                            ErrorOperation.UPDATE -> viewModel.loadHistory()
                        }
                    },
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
                TextButton(
                    onClick = { showResetConfirm = true },
                    modifier = Modifier.defaultMinSize(minHeight = Dimens.minTouchTarget),
                ) {
                    Text(stringResource(R.string.weigh_in_clear))
                }
            }
        }

    }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = null)
    }
}
