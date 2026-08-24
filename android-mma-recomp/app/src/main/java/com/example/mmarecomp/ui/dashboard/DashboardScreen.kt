package com.example.mmarecomp.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.mmarecomp.data.DashboardCard
import com.example.mmarecomp.data.HydrationStore
import com.example.mmarecomp.model.Phase
import com.example.mmarecomp.ui.AppPreferencesState
import com.example.mmarecomp.ui.components.EmptyState
import com.example.mmarecomp.ui.components.PullToRefreshWrapper
import com.example.mmarecomp.ui.components.densityItemGap
import com.example.mmarecomp.ui.components.densitySpacing
import com.example.mmarecomp.ui.components.SoftAlertBanner
import com.example.mmarecomp.ui.components.WeightTrendChart
import com.example.mmarecomp.util.DateUtils
import com.example.mmarecomp.util.PlateauStatus
import com.example.mmarecomp.util.formatWeight
import com.example.mmarecomp.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(viewModel: DashboardViewModel, phase: Phase) {
    LaunchedEffect(phase) { viewModel.load(phase) }

    PullToRefreshWrapper(isLoading = viewModel.isLoading, onRefresh = { viewModel.load(phase) }) {
        DashboardContent(viewModel)
    }
}

@Composable
private fun DashboardContent(viewModel: DashboardViewModel) {
    val visibleCards = AppPreferencesState.preferences.value.visibleDashboardCards
    val averageWindowDays = AppPreferencesState.preferences.value.movingAverageWindow.days
    val weightUnit = AppPreferencesState.preferences.value.weightUnit
    val density = AppPreferencesState.preferences.value.displayDensity

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(densitySpacing(density)),
        verticalArrangement = Arrangement.spacedBy(densityItemGap(density)),
    ) {
        if (!viewModel.isLoading && !viewModel.hasAnyData) {
            item {
                EmptyState(
                    title = "Bienvenue 👋",
                    message = "Log ta première séance, ton premier repas ou ta pesée du matin pour voir ton tableau de bord prendre vie.",
                )
            }
        }
        if (DashboardCard.SEANCES in visibleCards) {
            item {
                DashCard {
                    Text("Séances", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${viewModel.seancesFaitesCount} faites / ${viewModel.seancesPlanifieesCount} prévues",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
        if (DashboardCard.CONSTANCE in visibleCards && viewModel.consistencyStreak >= 1) {
            item {
                DashCard {
                    Text("Constance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val jours = if (viewModel.consistencyStreak == 1) "jour" else "jours"
                    Text("${viewModel.consistencyStreak} $jours de suite 🔥", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Au moins une séance ou un repas loggué chaque jour — jamais basé sur le poids.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (DashboardCard.POIDS in visibleCards) {
            item {
                DashCard {
                    Text(
                        "Tendance poids (moyenne $averageWindowDays jours)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    WeightTrendChart(points = viewModel.weightTrend7Day, modifier = Modifier.fillMaxWidth().height(140.dp))
                    viewModel.weightTrend7Day.lastOrNull()?.let { latest ->
                        Text(formatWeight(latest.value, weightUnit), style = MaterialTheme.typography.titleMedium)
                    }
                    if (viewModel.plateauStatus == PlateauStatus.RECOMPOSITION_EN_COURS) {
                        SoftAlertBanner("Poids stable mais tes séances progressent — recomposition en cours 💪")
                    }
                }
            }
        }
        if (DashboardCard.NUTRITION in visibleCards) {
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
        if (AppPreferencesState.preferences.value.hydrationEnabled) {
            item { HydrationCard() }
        }
    }
}

@Composable
private fun HydrationCard() {
    val context = LocalContext.current
    val today = DateUtils.today()
    var count by remember(today) { mutableStateOf(HydrationStore(context).countForToday(today)) }

    DashCard {
        Text("Hydratation", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconButton(onClick = {
                count = (count - 1).coerceAtLeast(0)
                HydrationStore(context).setCountForToday(today, count)
            }) { Icon(Icons.Filled.Remove, contentDescription = "Un verre de moins") }
            Text("$count verre(s)", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = {
                count += 1
                HydrationStore(context).setCountForToday(today, count)
            }) { Icon(Icons.Filled.Add, contentDescription = "Un verre de plus") }
        }
        Text(
            "Sans cible imposée — juste pour garder une trace si ça t'aide.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DashCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}
