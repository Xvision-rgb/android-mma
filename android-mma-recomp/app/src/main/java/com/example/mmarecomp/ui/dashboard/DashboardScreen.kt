package com.example.mmarecomp.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mmarecomp.model.Phase
import com.example.mmarecomp.ui.components.ErrorBanner
import com.example.mmarecomp.ui.components.SoftAlertBanner
import com.example.mmarecomp.ui.components.WeightTrendChart
import com.example.mmarecomp.util.PlateauStatus
import com.example.mmarecomp.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(viewModel: DashboardViewModel, phase: Phase) {
    LaunchedEffect(phase) { viewModel.load(phase) }

    val hasData = viewModel.workoutsThisWeek.isNotEmpty() || viewModel.mealsLast7Days.isNotEmpty() ||
        viewModel.morningWeighIns.isNotEmpty()

    if (viewModel.isLoading && !hasData) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Dashboard", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { viewModel.load(phase) }, enabled = !viewModel.isLoading) {
                    if (viewModel.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Icon(Icons.Filled.Refresh, contentDescription = "Actualiser le dashboard")
                    }
                }
            }
        }
        viewModel.errorMessage?.let { error ->
            item { ErrorBanner(error, onRetry = { viewModel.load(phase) }) }
        }
        item {
            DashCard {
                Text("Séances", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${viewModel.seancesFaitesCount} faites / ${viewModel.seancesPlanifieesCount} prévues",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        item {
            DashCard {
                Text(
                    "Tendance poids (moyenne 7 jours)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                WeightTrendChart(points = viewModel.weightTrend7Day, modifier = Modifier.fillMaxWidth().height(140.dp))
                if (viewModel.plateauStatus == PlateauStatus.RECOMPOSITION_EN_COURS) {
                    SoftAlertBanner("Poids stable mais tes séances progressent — recomposition en cours 💪")
                }
            }
        }
        item {
            DashCard {
                Text("Nutrition", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${viewModel.avgCaloriesLast7Days} kcal/jour (moy. 7j)", style = MaterialTheme.typography.titleMedium)
                viewModel.todayTarget?.let { target ->
                    Text(
                        "Cible aujourd'hui : ${target.caloriesCible} kcal · ${target.proteinesCibleG.toInt()}g protéines",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun DashCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
        content = content,
    )
}
