package com.example.mmarecomp.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import com.example.mmarecomp.R
import com.example.mmarecomp.model.Phase
import com.example.mmarecomp.model.PlanDayType
import com.example.mmarecomp.ui.components.AppScaffold
import com.example.mmarecomp.ui.components.ErrorBanner
import com.example.mmarecomp.ui.components.ErrorOperation
import com.example.mmarecomp.ui.theme.Dimens
import com.example.mmarecomp.viewmodel.WeeklyProgramViewModel

@Composable
fun WeeklyProgramScreen(
    viewModel: WeeklyProgramViewModel,
    phase: Phase,
    onEditPlanDay: (Int) -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(phase) { viewModel.load(phase) }

    AppScaffold(
        title = stringResource(R.string.weekly_program_title),
        onBack = onBack,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (viewModel.isLoading && viewModel.planDays.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Dimens.spaceMd),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
                ) {
                    viewModel.screenError?.let { error ->
                        item {
                            ErrorBanner(
                                error = error,
                                onRetry = { viewModel.load(phase) },
                            )
                        }
                    }

                    if (viewModel.planDays.isEmpty() && !viewModel.isLoading) {
                        item {
                            Text(
                                "Aucun jour programmé pour cette phase — importe ou édite un programme depuis les Réglages.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    viewModel.planDays.sortedBy { it.jourSemaine }.forEach { day ->
                        item(key = day.id) {
                            var expanded by remember(day.id) { mutableStateOf(false) }
                            val isToday = day.jourSemaine == com.example.mmarecomp.util.DateUtils.weekdayIso(
                                com.example.mmarecomp.util.DateUtils.today(),
                            )
                            Column(modifier = Modifier.fillMaxWidth()) {
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
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(day.type.label, style = MaterialTheme.typography.bodySmall)
                                        IconButton(
                                            onClick = { onEditPlanDay(day.jourSemaine) },
                                            modifier = Modifier.defaultMinSize(
                                                minWidth = Dimens.minTouchTarget,
                                                minHeight = Dimens.minTouchTarget,
                                            ),
                                        ) {
                                            Icon(
                                                Icons.Filled.Edit,
                                                contentDescription = stringResource(
                                                    R.string.dashboard_edit_exercises_cd,
                                                    com.example.mmarecomp.model.joursLabels[day.jourSemaine] ?: "jour",
                                                ),
                                            )
                                        }
                                    }
                                }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    PlanDayType.entries.forEach { option ->
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
                            if (isToday && day.exercices.isNotEmpty()) {
                                day.exercices.take(3).forEach { exercice ->
                                    if (exercice.nom.isNotBlank()) {
                                        Text(
                                            "• ${exercice.nom} — ${exercice.series}x${exercice.reps}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.tertiary,
                                        )
                                    }
                                }
                            } else if (day.exercices.isNotEmpty()) {
                                Text(
                                    day.exercices.take(2).joinToString(" · ") { it.nom.ifBlank { "—" } },
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
    }
}
