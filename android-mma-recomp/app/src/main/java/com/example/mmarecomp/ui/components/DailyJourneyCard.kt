package com.example.mmarecomp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.mmarecomp.ui.theme.Dimens
import com.example.mmarecomp.util.DailyJourneyState
import com.example.mmarecomp.util.DailyJourneyStepId

@Composable
fun DailyJourneyCard(
    journey: DailyJourneyState,
    onStepClick: (DailyJourneyStepId) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        Text(
            "Ta journée",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "${journey.completedCount}/${journey.totalCount} étapes",
            style = MaterialTheme.typography.titleMedium,
        )
        LinearProgressIndicator(
            progress = { journey.progress },
            modifier = Modifier.fillMaxWidth(),
        )
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs)) {
            journey.steps.forEach { step ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !step.done) { onStepClick(step.id) }
                        .padding(vertical = Dimens.spaceXs),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (step.done) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (step.done) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            step.label + if (step.optional) " (optionnel)" else "",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            step.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (!step.done) {
                        Text(
                            "→",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SyncPendingBanner(
    pendingCount: Int,
    onSync: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (pendingCount <= 0) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                androidx.compose.foundation.shape.RoundedCornerShape(Dimens.cornerSm),
            )
            .padding(Dimens.spaceMd),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "$pendingCount élément(s) en attente de sync",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        androidx.compose.material3.TextButton(onClick = onSync) {
            Text("Synchroniser")
        }
    }
}
