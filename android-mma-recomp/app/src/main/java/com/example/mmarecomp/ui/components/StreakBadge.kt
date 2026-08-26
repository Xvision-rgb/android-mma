package com.example.mmarecomp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mmarecomp.ui.theme.Dimens

@Composable
fun StreakBadge(
    currentStreak: Int,
    bestStreak: Int,
    modifier: Modifier = Modifier,
) {
    val isMilestone = currentStreak in listOf(7, 30, 100)
    val badgeColor by animateColorAsState(
        targetValue = if (isMilestone) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        label = "Streak color transition"
    )

    Column(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(16.dp)
            )
            .padding(Dimens.spaceMd),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
        ) {
            Icon(
                Icons.Filled.LocalFireDepartment,
                contentDescription = "Streak",
                tint = badgeColor,
            )
            Text(
                "$currentStreak jours",
                style = MaterialTheme.typography.displayLarge,
                color = badgeColor,
            )
        }
        Text(
            "d'affilée — meilleur: $bestStreak jours",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (isMilestone) {
            Text(
                "🎉 Milestone débloqué!",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}
