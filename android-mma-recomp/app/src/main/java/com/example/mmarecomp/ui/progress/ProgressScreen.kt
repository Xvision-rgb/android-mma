package com.example.mmarecomp.ui.progress

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.mmarecomp.ui.components.EmptyState
import com.example.mmarecomp.ui.components.ErrorBanner
import com.example.mmarecomp.ui.components.WeightTrendChart
import com.example.mmarecomp.util.CsvExport
import com.example.mmarecomp.viewmodel.ProgressViewModel

@Composable
fun ProgressScreen(viewModel: ProgressViewModel) {
    LaunchedEffect(viewModel.windowWeeks) { viewModel.load() }
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Text("Progression", style = MaterialTheme.typography.titleLarge) }

        viewModel.errorMessage?.let { error ->
            item { ErrorBanner(error, onRetry = { viewModel.load() }) }
        }

        if (viewModel.weighIns.isNotEmpty()) {
            item {
                OutlinedButton(
                    onClick = {
                        val csv = CsvExport.weighIns(viewModel.weighIns)
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_SUBJECT, "Historique pesées")
                            putExtra(Intent.EXTRA_TEXT, csv)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Exporter l'historique des pesées"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Text("  Exporter l'historique des pesées (CSV)")
                }
            }
        }
        if (viewModel.workouts.isNotEmpty()) {
            item {
                OutlinedButton(
                    onClick = {
                        val csv = CsvExport.workouts(viewModel.workouts)
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_SUBJECT, "Historique séances")
                            putExtra(Intent.EXTRA_TEXT, csv)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Exporter l'historique des séances"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Text("  Exporter l'historique des séances (CSV)")
                }
            }
        }

        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf(4, 8, 12).forEachIndexed { index, weeks ->
                    SegmentedButton(
                        selected = viewModel.windowWeeks == weeks,
                        onClick = { viewModel.windowWeeks = weeks },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 3),
                    ) { Text("$weeks semaines") }
                }
            }
        }

        if (viewModel.weighIns.isNotEmpty() || viewModel.workouts.isNotEmpty()) {
            item {
                ProgressCard(title = "Résumé sur ${viewModel.windowWeeks} semaines") {
                    Text(
                        "${viewModel.workouts.size} séance(s) loguée(s) · ${viewModel.weighIns.size} pesée(s) loguée(s)",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (viewModel.workoutTypeBreakdown.isNotEmpty()) {
                        Text(
                            viewModel.workoutTypeBreakdown.joinToString(" · ") { (type, count) -> "$count ${type.label}" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
        } else if (viewModel.weighIns.isNotEmpty()) {
            item {
                Text(
                    "Ajoute un % de masse grasse à tes pesées pour voir cette courbe apparaître ici.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (viewModel.weeklyVolumeTrend.isNotEmpty()) {
            item {
                ProgressCard(title = "Volume d'entraînement (par semaine)") {
                    WeightTrendChart(
                        points = viewModel.weeklyVolumeTrend,
                        modifier = Modifier.fillMaxWidth().height(110.dp),
                        lineColor = MaterialTheme.colorScheme.tertiary,
                    )
                    Text(
                        "Séries × reps × charge réelle, cumulés semaine par semaine.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (viewModel.caloriesTrend.isNotEmpty()) {
            item {
                ProgressCard(title = "Calories (moyenne 7j)") {
                    WeightTrendChart(
                        points = viewModel.caloriesTrend,
                        modifier = Modifier.fillMaxWidth().height(110.dp),
                        lineColor = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        item { Text("Charges — progression par exercice", style = MaterialTheme.typography.titleMedium) }

        val charges = viewModel.chargeProgressionByExercise
        if (charges.isEmpty()) {
            item {
                EmptyState(
                    title = "Pas encore de progression à afficher",
                    subtitle = "Log tes séances avec la charge réelle pour la voir apparaître ici.",
                )
            }
        }
        charges.toSortedMap().forEach { (name, series) ->
            item {
                val record = series.maxByOrNull { it.chargeKg }
                ProgressCard(title = name) {
                    WeightTrendChart(
                        points = series.map { com.example.mmarecomp.util.TrendPoint(it.date, it.chargeKg) },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        lineColor = MaterialTheme.colorScheme.tertiary,
                    )
                    record?.let {
                        Text(
                            "Record personnel : ${com.example.mmarecomp.util.Formatting.oneDecimal(it.chargeKg)}kg",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
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
