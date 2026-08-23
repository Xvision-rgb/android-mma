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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mmarecomp.ui.components.EmptyState
import com.example.mmarecomp.ui.components.WeightTrendChart
import com.example.mmarecomp.viewmodel.ProgressViewModel

@Composable
fun ProgressScreen(viewModel: ProgressViewModel) {
    LaunchedEffect(viewModel.windowWeeks) { viewModel.load() }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
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

        item {
            ProgressCard(title = "Poids (moy. 7j)") {
                WeightTrendChart(points = viewModel.weightTrend, modifier = Modifier.fillMaxWidth().height(130.dp))
            }
        }

        if (viewModel.bfTrend.isNotEmpty()) {
            item {
                ProgressCard(title = "% Masse grasse (moy. 7j)") {
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
                        Text("%.1f kg".format(best), style = MaterialTheme.typography.bodyMedium)
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
                        Text(workout.date, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${workout.type.label} · ${workout.exercices.size} exercice(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { viewModel.deleteWorkout(workout) {} }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Supprimer la séance du ${workout.date}")
                    }
                }
            }
        }
    }
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
