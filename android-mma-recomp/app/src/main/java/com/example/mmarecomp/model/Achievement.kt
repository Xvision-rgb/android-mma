package com.example.mmarecomp.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector
import java.time.LocalDate

enum class AchievementType(val icon: ImageVector, val label: String) {
    FIRST_WORKOUT(Icons.Filled.CheckCircle, "First Workout"),
    FIVE_CONSECUTIVE_DAYS(Icons.Filled.LocalFireDepartment, "5 Consecutive Days"),
    TEN_KG_PR(Icons.Filled.TrendingUp, "10kg Personal Record"),
    FIVE_HUNDRED_KCAL_SESSION(Icons.Filled.FitnessCenter, "500kcal Session"),
    WEEK_PERFECT(Icons.Filled.EmojiEvents, "Week Perfect"),
}

data class Achievement(
    val type: AchievementType,
    val unlockedAt: LocalDate? = null,
) {
    val isUnlocked: Boolean get() = unlockedAt != null
}
