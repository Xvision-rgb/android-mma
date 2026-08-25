package com.example.mmarecomp.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val hour = java.time.LocalTime.now().hour
                val greeting = when {
                    hour < 12 -> "Bonjour"
                    hour < 18 -> "Bon après-midi"
                    else -> "Bonsoir"
                }
                Text(greeting, style = MaterialTheme.typography.titleLarge)
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
        if (viewModel.activityStreakDays >= 2) {
            item {
                DashCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            Icons.Filled.LocalFireDepartment,
                            contentDescription = "Série d'activité",
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                        Text(
                            "${viewModel.activityStreakDays} jours d'affilée avec au moins une activité loggée",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
        item {
            DashCard {
                Text("Séances", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${viewModel.seancesFaitesCount} faites / ${viewModel.seancesPlanifieesCount} prévues",
                    style = MaterialTheme.typography.titleMedium,
                )
                viewModel.todayPlan?.let { plan ->
                    Text(
                        "Aujourd'hui : ${plan.type.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (viewModel.planThisWeek.isNotEmpty()) {
                    var showProgram by remember { mutableStateOf(false) }
                    TextButton(onClick = { showProgram = !showProgram }) {
                        Text(if (showProgram) "Masquer le programme" else "Voir le programme de la semaine")
                    }
                    if (showProgram) {
                        viewModel.planThisWeek.sortedBy { it.jourSemaine }.forEach { day ->
                            var expanded by remember(day.id) { mutableStateOf(false) }
                            Box {
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable { expanded = true },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        com.example.mmarecomp.model.joursLabels[day.jourSemaine] ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(day.type.label, style = MaterialTheme.typography.bodySmall)
                                }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    com.example.mmarecomp.model.PlanDayType.entries.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option.label) },
                                            onClick = {
                                                viewModel.updatePlanDayType(day, option)
                                                expanded = false
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
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
                Text(
                    "${viewModel.mealsLoggedToday} repas loggé(s) aujourd'hui",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                viewModel.yesterdayCalories?.let { hier ->
                    Text(
                        "Hier : $hier kcal — juste un repère, pas une comparaison",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (viewModel.showsUnderTargetPattern) {
                    SoftAlertBanner(
                        "Tu manges nettement moins que ta cible depuis quelques jours — pense à ajuster un peu si ce n'est pas volontaire, sans stress.",
                        tone = com.example.mmarecomp.ui.components.SoftAlertTone.NEUTRAL,
                    )
                }
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
