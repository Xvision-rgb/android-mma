package com.example.mmarecomp.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
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
import com.example.mmarecomp.ui.components.StreakBadge
import com.example.mmarecomp.ui.components.WeightTrendChart
import com.example.mmarecomp.ui.theme.Dimens
import com.example.mmarecomp.ui.theme.workoutTypeColor
import com.example.mmarecomp.util.Formatting
import com.example.mmarecomp.util.PlateauStatus
import com.example.mmarecomp.util.TrendDirection
import com.example.mmarecomp.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(viewModel: DashboardViewModel, phase: Phase, onEditPlanDay: (Int) -> Unit = {}) {
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
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = Dimens.spaceMd, end = Dimens.spaceMd, top = Dimens.spaceMd, bottom = 96.dp,
        ),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Dimens.spaceMd),
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
        item {
            StreakBadge(
                currentStreak = viewModel.currentStreak,
                bestStreak = viewModel.bestStreak,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        viewModel.errorMessage?.let { error ->
            item { ErrorBanner(error, onRetry = { viewModel.load(phase) }) }
        }
        val activityStreakDays = viewModel.activityStreakDays
        if (activityStreakDays >= 2) {
            item {
                DashCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
                        Icon(
                            Icons.Filled.LocalFireDepartment,
                            contentDescription = "Série d'activité",
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                        Text("$activityStreakDays", style = MaterialTheme.typography.displayLarge)
                        Text(
                            "jours d'affilée avec au moins une activité loggée",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                }
            }
        }
        item {
            DashCard {
                Text("Cette semaine", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${viewModel.seancesFaitesCount}", style = MaterialTheme.typography.displayLarge)
                    Text(
                        "séance(s) faites / ${viewModel.seancesPlanifieesCount} prévues",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                val avgTarget = viewModel.avgTargetCaloriesLast7Days
                Text(
                    if (avgTarget != null) {
                        "${viewModel.avgCaloriesLast7Days} kcal/jour en moyenne (cible ~$avgTarget kcal)"
                    } else {
                        "${viewModel.avgCaloriesLast7Days} kcal/jour en moyenne"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${viewModel.daysWithMealsLast7Days} jour(s) sur 7 avec au moins un repas loggé",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val weeklyTrendLabel = when (viewModel.weightTrendDirection) {
                    TrendDirection.HAUSSE -> "Poids : légère hausse cette semaine"
                    TrendDirection.BAISSE -> "Poids : légère baisse cette semaine"
                    TrendDirection.STABLE -> "Poids : stable cette semaine"
                    TrendDirection.INDETERMINE -> null
                }
                weeklyTrendLabel?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            DashCard {
                Text("Séances", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val volume = viewModel.weeklyTrainingVolume
                if (volume > 0) {
                    Text(
                        "${volume.toInt()}kg de volume d'entraînement cumulé (séries × reps × charge réelle)",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                viewModel.todayPlan?.let { plan ->
                    Text(
                        "Aujourd'hui : ${plan.type.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val typeBreakdown = viewModel.workoutTypeBreakdown
                if (typeBreakdown.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                    ) {
                        typeBreakdown.entries.forEach { (type, count) ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs)) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(workoutTypeColor(type), androidx.compose.foundation.shape.CircleShape),
                                )
                                Text(
                                    "${type.label} ×$count",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
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
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = Dimens.minTouchTarget)
                                        .clickable { expanded = true },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        com.example.mmarecomp.model.joursLabels[day.jourSemaine] ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(day.type.label, style = MaterialTheme.typography.bodySmall)
                                        IconButton(onClick = { onEditPlanDay(day.jourSemaine) }) {
                                            Icon(
                                                Icons.Filled.Edit,
                                                contentDescription = "Modifier les exercices du ${com.example.mmarecomp.model.joursLabels[day.jourSemaine] ?: "jour"}",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
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
                viewModel.weightGoalGapKg?.let { gap ->
                    val label = when {
                        gap > 0.5 -> "Encore ${Formatting.oneDecimal(gap)}kg pour atteindre ton objectif (moyenne 7j)."
                        gap < -0.5 -> "Tu es ${Formatting.oneDecimal(-gap)}kg en dessous de ton objectif (moyenne 7j)."
                        else -> "Tu es tout proche de ton objectif de poids 🎯 (moyenne 7j)."
                    }
                    Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                viewModel.bfGoalGapPct?.let { gap ->
                    val label = when {
                        gap > 0.3 -> "Encore ${Formatting.oneDecimal(gap)} points de %BF pour atteindre ton objectif (moyenne 7j)."
                        gap < -0.3 -> "Tu es ${Formatting.oneDecimal(-gap)} points de %BF en dessous de ton objectif (moyenne 7j)."
                        else -> "Tu es tout proche de ton objectif de %BF 🎯 (moyenne 7j)."
                    }
                    Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
