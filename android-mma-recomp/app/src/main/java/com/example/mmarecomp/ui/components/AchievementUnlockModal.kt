package com.example.mmarecomp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.mmarecomp.model.AchievementType
import kotlinx.coroutines.delay

@Composable
fun AchievementUnlockModal(
    achievementType: AchievementType,
    onDismiss: () -> Unit,
) {
    var isVisible by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        delay(300)
        isVisible = true
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(3000)
            isVisible = false
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(24.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                Icon(
                    achievementType.icon,
                    contentDescription = achievementType.label,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onTertiary,
                )
                Text(
                    "🎉 ${achievementType.label}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiary,
                )
                Text(
                    "Débloqué!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiary.copy(alpha = 0.7f),
                )
            }
        }
    }

    LaunchedEffect(isVisible) {
        if (!isVisible) {
            onDismiss()
        }
    }
}
