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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.mmarecomp.R
import com.example.mmarecomp.model.Phase
import com.example.mmarecomp.model.RepasSlot
import com.example.mmarecomp.ui.components.AchievementUnlockModal
import com.example.mmarecomp.ui.components.AppCard
import com.example.mmarecomp.ui.components.AppScaffold
import com.example.mmarecomp.ui.components.DailyCheckInSheet
import com.example.mmarecomp.ui.components.ErrorBanner
import com.example.mmarecomp.ui.components.ErrorOperation
import com.example.mmarecomp.ui.components.RelativeStrengthCard
import com.example.mmarecomp.ui.components.VolumeDistributionCard
import com.example.mmarecomp.ui.components.NextWorkoutCard
import com.example.mmarecomp.ui.components.RecoveryReadinessCard
import com.example.mmarecomp.ui.components.SoftAlertBanner
import com.example.mmarecomp.ui.components.WeightTrendChart
import com.example.mmarecomp.ui.theme.Dimens
import com.example.mmarecomp.ui.theme.workoutTypeColor
import com.example.mmarecomp.util.Formatting
import com.example.mmarecomp.util.ModulationApplier
import com.example.mmarecomp.ui.components.DailyJourneyCard
import com.example.mmarecomp.ui.components.SyncPendingBanner
import com.example.mmarecomp.util.DailyJourneyStepId
import com.example.mmarecomp.util.PlateauStatus
import com.example.mmarecomp.util.TrendDirection
import com.example.mmarecomp.util.CheckInSaveError
import com.example.mmarecomp.viewmodel.DashboardViewModel
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    phase: Phase,
    onEditPlanDay: (Int) -> Unit = {},
    onStartWorkout: () -> Unit = {},
    onOpenWeeklyProgram: () -> Unit = {},
    onOpenWeighIn: () -> Unit = {},
    onOpenMeals: () -> Unit = {},
) {
    LaunchedEffect(phase) { viewModel.load(phase) }

    var showCheckIn by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    val hasData = viewModel.workoutsThisWeek.isNotEmpty() || viewModel.mealsLast7Days.isNotEmpty() ||
        viewModel.morningWeighIns.isNotEmpty()

    val hour = java.time.LocalTime.now().hour
    val greeting = when {
        hour < 12 -> stringResource(R.string.dashboard_greeting_morning)
        hour < 18 -> stringResource(R.string.dashboard_greeting_afternoon)
        else -> stringResource(R.string.dashboard_greeting_evening)
    }

    AppScaffold(
        title = greeting,
        actions = {
            IconButton(onClick = { viewModel.load(phase) }, enabled = !viewModel.isLoading) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(Dimens.iconSm))
                } else {
                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.dashboard_refresh))
                }
            }
        },
    ) { padding ->
    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
        if (viewModel.isLoading && !hasData) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = Dimens.spaceMd, end = Dimens.spaceMd, top = Dimens.spaceMd, bottom = Dimens.scrollBottomInset,
        ),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Dimens.spaceMd),
    ) {
        // NIVEAU 1 — la seule "grande chose" de l'écran.
        // La force relative est l'indicateur directeur du projet : c'est elle
        // qui répond à la question posée (être plus fort pour son poids), là
        // où la balance seule ne tranche dans aucun sens. Elle était affichée
        // en quatrieme position, au meme poids visuel que le reste ; elle
        // ouvre desormais l'ecran, en variante Hero.
        item {
            RelativeStrengthCard(
                forces = viewModel.forcesRelatives,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (viewModel.pendingSyncCount > 0) {
            item {
                SyncPendingBanner(
                    pendingCount = viewModel.pendingSyncCount,
                    onSync = { viewModel.syncPending() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item {
            DailyJourneyCard(
                journey = viewModel.dailyJourneyState,
                onStepClick = { stepId ->
                    when (stepId) {
                        DailyJourneyStepId.CHECK_IN -> showCheckIn = true
                        DailyJourneyStepId.MORNING_WEIGH_IN -> onOpenWeighIn()
                        DailyJourneyStepId.WORKOUT -> onStartWorkout()
                        DailyJourneyStepId.NUTRITION -> onOpenMeals()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // NIVEAU 2 — l'état du jour et l'action qui en découle : les deux
        // seules choses sur lesquelles on agit dans la minute.
        item {
            RecoveryReadinessCard(
                modulation = viewModel.modulation,
                score = viewModel.scoreReadiness,
                acwr = viewModel.acwr,
                aCheckInAujourdhui = viewModel.checkInAujourdhui != null,
                onCheckIn = { showCheckIn = true },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            val suggestion = viewModel.suggestedWorkout
            NextWorkoutCard(
                sessionLabel = suggestion?.sessionLabel,
                exercises = suggestion?.exercises ?: emptyList(),
                onStartClick = onStartWorkout,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // NIVEAU 3 — le contexte, consultable mais jamais dominant.
        item {
            com.example.mmarecomp.ui.components.VolumeLandmarksCard(
                bilan = viewModel.bilanVolume,
                alertes = viewModel.alertesSurcharge,
                deadHangSec = viewModel.deadHangSec,
                lecturePoigne = viewModel.lecturePoigne,
                progressionDeadHangSec = viewModel.progressionDeadHangSec,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            VolumeDistributionCard(
                repartition = viewModel.repartitionVolume,
                ratioTiragePoussee = viewModel.ratioTiragePoussee,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        viewModel.conflitsProgrammation.forEach { conflit ->
            item {
                SoftAlertBanner(
                    message = conflit,
                    tone = com.example.mmarecomp.ui.components.SoftAlertTone.NEUTRAL,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        viewModel.screenError?.let { error ->
            item {
                ErrorBanner(
                    error = error,
                    onRetry = {
                        when (error.operation) {
                            ErrorOperation.LOAD -> viewModel.load(phase)
                            ErrorOperation.SAVE -> viewModel.load(phase)
                            ErrorOperation.UPDATE -> viewModel.load(phase)
                            ErrorOperation.DELETE -> viewModel.load(phase)
                        }
                    },
                )
            }
        }
        val activityStreakDays = viewModel.activityStreakDays
        if (activityStreakDays >= 2) {
            item {
                DashCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
                        Icon(
                            Icons.Filled.LocalFireDepartment,
                            contentDescription = stringResource(R.string.dashboard_activity_streak_cd),
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                        Text("$activityStreakDays", style = MaterialTheme.typography.displayLarge)
                        Text(
                            stringResource(R.string.dashboard_activity_streak),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                }
            }
        }
        item {
            DashCard {
                Text(stringResource(R.string.dashboard_this_week), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
                    Text(
                        stringResource(
                            R.string.dashboard_sessions_done,
                            viewModel.seancesFaitesCount,
                            viewModel.seancesPlanifieesCount,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                // Les calories moyennes 7j vivent uniquement dans la carte
                // Nutrition ci-dessous — les répéter ici affichait deux fois
                // la même valeur sur le même écran.
                Text(
                    stringResource(R.string.dashboard_meals_logged_days, viewModel.daysWithMealsLast7Days),
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
                    plan.exercices.take(3).forEach { exercice ->
                        if (exercice.nom.isNotBlank()) {
                            Text(
                                "• ${exercice.nom} — ${exercice.series}x${exercice.reps}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                val typeBreakdown = viewModel.workoutTypeBreakdown
                if (typeBreakdown.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                    ) {
                        typeBreakdown.entries.forEach { (type, count) ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs)) {
                                Box(
                                    modifier = Modifier
                                        .size(Dimens.dotSm)
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
                    TextButton(onClick = onOpenWeeklyProgram) {
                        Text(stringResource(R.string.dashboard_open_weekly_program))
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
                WeightTrendChart(points = viewModel.weightTrend7Day, modifier = Modifier.fillMaxWidth().height(Dimens.chartHeight))
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
                viewModel.avgTargetCaloriesLast7Days?.let { avgTarget ->
                    Text(
                        "Cible moyenne sur la même période : ~$avgTarget kcal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "${viewModel.mealsLoggedToday} repas loggé(s) aujourd'hui",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                RepasSlot.entries.forEach { slot ->
                    val meal = viewModel.mealsTodayBySlot[slot]
                    Text(
                        if (meal != null) {
                            "${slot.label} : ${meal.calories} kcal"
                        } else {
                            "${slot.label} : —"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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

        if (showCheckIn) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .padding(Dimens.spaceMd)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(Dimens.cornerMd)),
                ) {
                    DailyCheckInSheet(
                        onSubmit = { sommeil, courbatures, fatigue, humeur, stress, hrv, deadHang ->
                            viewModel.enregistrerCheckIn(
                                sommeil, courbatures, fatigue, humeur, stress, hrv, deadHang,
                            ) { success, modulation, saveError, pendingSync ->
                                if (success) {
                                    scope.launch {
                                        val message = if (pendingSync) {
                                            context.getString(R.string.checkin_saved_pending_sync)
                                        } else {
                                            val actions = ModulationApplier.actionsConcretes(modulation)
                                                .firstOrNull()
                                                .orEmpty()
                                            context.getString(
                                                R.string.checkin_saved_with_modulation,
                                                modulation.action.label,
                                                actions,
                                            )
                                        }
                                        snackbarHostState.showSnackbar(
                                            message = message,
                                            duration = SnackbarDuration.Long,
                                        )
                                    }
                                    showCheckIn = false
                                } else {
                                    scope.launch {
                                        val messageRes = when (saveError) {
                                            CheckInSaveError.SCHEMA -> R.string.checkin_save_failed_schema
                                            CheckInSaveError.NETWORK -> R.string.checkin_save_failed_network
                                            CheckInSaveError.OTHER, null -> R.string.checkin_save_failed
                                        }
                                        snackbarHostState.showSnackbar(
                                            message = context.getString(messageRes),
                                            duration = SnackbarDuration.Long,
                                        )
                                    }
                                }
                            }
                        },
                        onDismiss = { showCheckIn = false },
                    )
                }
            }
        }

        viewModel.unlockedAchievement?.let { achievement ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AchievementUnlockModal(
                    achievementType = achievement,
                    onDismiss = { viewModel.unlockedAchievement = null },
                )
            }
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
    }
}

/** Délègue à `AppCard` : les ~10 appels du dashboard restent inchangés, mais
 *  la carte prend l'élévation, la bordure et les tokens partagés au lieu du
 *  `Column` + `.background()` recopié qu'elle portait. */
@Composable
private fun DashCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    AppCard(content = content)
}
