package com.example.mmarecomp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.mmarecomp.ui.theme.Dimens
import com.example.mmarecomp.util.Formatting
import com.example.mmarecomp.util.WeeklyInsights

@Composable
fun WeeklyInsightsCard(
    insights: WeeklyInsights,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        Text(
            "Synthèse 7 jours",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "${insights.weekStart.dayOfMonth}/${insights.weekStart.monthValue} → " +
                "${insights.weekEnd.dayOfMonth}/${insights.weekEnd.monthValue}",
            style = MaterialTheme.typography.titleMedium,
        )

        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs)) {
            insights.avgReadinessScore?.let {
                Text("Score forme moyen : ${Formatting.oneDecimal(it)}/25", style = MaterialTheme.typography.bodySmall)
            }
            Text(
                "Séances : ${insights.workoutsLogged} salle · ${insights.mmaSessionsLogged} MMA",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "Nutrition : ~${insights.avgCaloriesPerDay} kcal/j · ${insights.daysWithMeals}/7 jours logués",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "Cible calorique respectée : ${insights.daysOnCalorieTarget}/7 jours",
                style = MaterialTheme.typography.bodySmall,
            )
            insights.weightDeltaKg?.let { delta ->
                val sign = if (delta >= 0) "+" else ""
                Text(
                    "Variation poids (matin) : $sign${Formatting.oneDecimal(delta)} kg",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        if (insights.alerts.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs)) {
                Text("Points d'attention", style = MaterialTheme.typography.labelSmall)
                insights.alerts.forEach { alert ->
                    SoftAlertBanner(message = alert, tone = SoftAlertTone.NEUTRAL)
                }
            }
        }
    }
}
