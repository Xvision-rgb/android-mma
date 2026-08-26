package com.example.mmarecomp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class ReadinessStatus(val emoji: String, val label: String, val recommendation: String) {
    READY("💚", "Ready to train", "Train hard today!"),
    CAUTIOUS("⚠️", "Take it easy", "Light training recommended"),
    REST("😴", "Rest recommended", "Recovery day — active rest only"),
}

@Composable
fun RecoveryReadinessCard(
    status: ReadinessStatus,
    weightTrendDown: Boolean,
    sleepHours: Double?,
    intensityPercent: Int,
    daysSinceLastRest: Int,
    modifier: Modifier = Modifier,
) {
    val statusColor = when (status) {
        ReadinessStatus.READY -> MaterialTheme.colorScheme.tertiary
        ReadinessStatus.CAUTIOUS -> MaterialTheme.colorScheme.secondary
        ReadinessStatus.REST -> MaterialTheme.colorScheme.error
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(statusColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(status.emoji, style = MaterialTheme.typography.headlineMedium)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(status.label, style = MaterialTheme.typography.titleMedium)
                Text(status.recommendation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Poids", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (weightTrendDown) "↓ Trending down" else "→ Stable", style = MaterialTheme.typography.bodySmall)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Sommeil", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    sleepHours?.let { "${"%.1f".format(it)}h" } ?: "—",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Intensité", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$intensityPercent%", style = MaterialTheme.typography.bodySmall)
            }
        }

        Text(
            "Dernier repos il y a $daysSinceLastRest jour(s)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
