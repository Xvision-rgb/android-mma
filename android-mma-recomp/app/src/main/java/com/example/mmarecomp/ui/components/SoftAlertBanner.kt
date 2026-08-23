package com.example.mmarecomp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class SoftAlertTone { POSITIVE, NEUTRAL }

/** Bandeau non culpabilisant : jamais de rouge, jamais de ton alarmiste —
 *  utilisé pour les messages de recomposition, suggestions de charge, etc. */
@Composable
fun SoftAlertBanner(
    message: String,
    tone: SoftAlertTone = SoftAlertTone.POSITIVE,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Filled.AutoAwesome,
    modifier: Modifier = Modifier,
) {
    val color = if (tone == SoftAlertTone.POSITIVE) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

val ProgressionIcon = Icons.Filled.TrendingUp
