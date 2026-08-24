package com.example.mmarecomp.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
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
import com.example.mmarecomp.model.WeighIn
import com.example.mmarecomp.model.Workout
import com.example.mmarecomp.ui.AppPreferencesState
import com.example.mmarecomp.ui.components.ConfirmDeleteDialog
import com.example.mmarecomp.ui.components.EmptyState
import com.example.mmarecomp.ui.components.densityItemGap
import com.example.mmarecomp.ui.components.densitySpacing
import com.example.mmarecomp.ui.components.PullToRefreshWrapper
import com.example.mmarecomp.ui.components.WeightTrendChart
import com.example.mmarecomp.ui.components.toSnackbarDuration
import com.example.mmarecomp.util.DateUtils
import com.example.mmarecomp.util.formatWeight
import com.example.mmarecomp.viewmodel.ProgressViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ProgressScreen(viewModel: ProgressViewModel) {
    LaunchedEffect(viewModel.windowWeeks) { viewModel.load() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { scaffoldPadding ->
        PullToRefreshWrapper(
            isLoading = viewModel.isLoading,
            onRefresh = { viewModel.load() },
            modifier = Modifier.padding(scaffoldPadding),
        ) {
            ProgressContent(viewModel, snackbarHostState, scope)
        }
    }
}

@Composable
private fun ProgressContent(viewModel: ProgressViewModel, snackbarHostState: SnackbarHostState, scope: CoroutineScope) {
    val prefs by AppPreferencesState.preferences
    var pendingDeleteWorkout by remember { mutableStateOf<Workout?>(null) }
    var pendingDeleteWeighIn by remember { mutableStateOf<WeighIn?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(densitySpacing(prefs.displayDensity)),
        verticalArrangement = Arrangement.spacedBy(densityItemGap(prefs.displayDensity)),
    ) {
        item { Text("Progression", style = MaterialTheme.typography.titleLarge) }

        if (!viewModel.isLoading && viewModel.weightTrend.isEmpty() && viewModel.chargeProgressionByExercise.isEmpty()) {
            item {
                EmptyState(
                    title = "Pas encore de quoi comparer",
                    message = "Reviens ici dans quelques séances et pesées — ta progression sur 4 à 8 semaines s'affichera automatiquement.",
                )
            }
        }

        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf(4, 8).forEachIndexed { index, weeks ->
                    SegmentedButton(
                        selected = viewModel.windowWeeks == weeks,
                        onClick = { viewModel.windowWeeks = weeks },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                    ) { Text("$weeks semaines") }
                }
            }
        }

        viewModel.weightGoalEtaText?.let { eta ->
            item {
                Text(
                    eta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        val averageWindowDays = AppPreferencesState.preferences.value.movingAverageWindow.days
        item {
            ProgressCard(title = "Poids (moy. ${averageWindowDays}j)") {
                WeightTrendChart(points = viewModel.weightTrend, modifier = Modifier.fillMaxWidth().height(130.dp))
            }
        }

        if (AppPreferencesState.preferences.value.showBodyFat && viewModel.bfTrend.isNotEmpty()) {
            item {
                ProgressCard(title = "% Masse grasse (moy. ${averageWindowDays}j)") {
                    WeightTrendChart(
                        points = viewModel.bfTrend,
                        modifier = Modifier.fillMaxWidth().height(110.dp),
                        lineColor = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }

        item { Text("Charges — progression par exercice", style = MaterialTheme.typography.titleMedium) }

        val charges = viewModel.chargeProgressionByExercise
        if (charges.isEmpty()) {
            item {
                Text(
                    "Log des séances avec charge réelle pour voir apparaître ta progression ici.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        charges.toSortedMap().forEach { (name, series) ->
            item {
                ProgressCard(title = name) {
                    WeightTrendChart(
                        points = series.map { com.example.mmarecomp.util.TrendPoint(it.date, it.chargeKg) },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        lineColor = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }

        if (viewModel.personalBests.isNotEmpty()) {
            item { HorizontalDivider() }
            item { Text("Records personnels", style = MaterialTheme.typography.titleMedium) }
            items(viewModel.personalBests, key = { it.first }) { (name, best) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(name, style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.height(18.dp),
                        )
                        Text(formatWeight(best, AppPreferencesState.preferences.value.weightUnit), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        if (viewModel.recentWorkoutsDescending.isNotEmpty()) {
            item { HorizontalDivider() }
            item { Text("Séances récentes", style = MaterialTheme.typography.titleMedium) }
            items(viewModel.recentWorkoutsDescending, key = { it.id }) { workout ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(DateUtils.forDisplay(workout.date, prefs.dateFormat), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${workout.type.label} · ${workout.exercices.size} exercice(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = {
                        if (prefs.confirmBeforeDelete) {
                            pendingDeleteWorkout = workout
                        } else {
                            viewModel.removeWorkoutLocally(workout)
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Séance supprimée",
                                    actionLabel = "Annuler",
                                    duration = prefs.undoDuration.toSnackbarDuration(),
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.restoreWorkout(workout)
                                } else {
                                    viewModel.commitDeleteWorkout(workout)
                                }
                            }
                        }
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Supprimer la séance du ${workout.date}")
                    }
                }
            }
        }

        if (viewModel.recentWeighInsDescending.isNotEmpty()) {
            item { HorizontalDivider() }
            item { Text("Pesées récentes", style = MaterialTheme.typography.titleMedium) }
            items(viewModel.recentWeighInsDescending, key = { it.id }) { weighIn ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(DateUtils.forDisplay(weighIn.date, prefs.dateFormat), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${weighIn.type.label} · ${formatWeight(weighIn.poidsKg, prefs.weightUnit)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = {
                        if (prefs.confirmBeforeDelete) {
                            pendingDeleteWeighIn = weighIn
                        } else {
                            viewModel.removeWeighInLocally(weighIn)
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Pesée supprimée",
                                    actionLabel = "Annuler",
                                    duration = prefs.undoDuration.toSnackbarDuration(),
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.restoreWeighIn(weighIn)
                                } else {
                                    viewModel.commitDeleteWeighIn(weighIn)
                                }
                            }
                        }
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Supprimer la pesée du ${weighIn.date}")
                    }
                }
            }
        }
    }

    ConfirmDeleteDialog(
        item = pendingDeleteWorkout,
        title = "Supprimer la séance du ${pendingDeleteWorkout?.date} ?",
        onConfirm = { workout ->
            viewModel.removeWorkoutLocally(workout)
            viewModel.commitDeleteWorkout(workout)
            pendingDeleteWorkout = null
        },
        onDismiss = { pendingDeleteWorkout = null },
    )

    ConfirmDeleteDialog(
        item = pendingDeleteWeighIn,
        title = "Supprimer la pesée du ${pendingDeleteWeighIn?.date} ?",
        onConfirm = { weighIn ->
            viewModel.removeWeighInLocally(weighIn)
            viewModel.commitDeleteWeighIn(weighIn)
            pendingDeleteWeighIn = null
        },
        onDismiss = { pendingDeleteWeighIn = null },
    )
}

@Composable
private fun ProgressCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}
