package com.example.mmarecomp.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
                EmptyState(
                    title = "Pas encore de progression à afficher",
                    subtitle = "Log tes séances avec la charge réelle pour la voir apparaître ici.",
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
