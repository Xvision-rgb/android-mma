package com.example.mmarecomp.util

import android.content.Context
import com.example.mmarecomp.model.Achievement
import com.example.mmarecomp.model.AchievementType
import java.time.LocalDate

class AchievementManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("achievements_prefs", Context.MODE_PRIVATE)

    fun checkAndUnlockFirstWorkout(hasLoggedWorkout: Boolean): Boolean {
        if (hasLoggedWorkout && !isUnlocked(AchievementType.FIRST_WORKOUT)) {
            unlock(AchievementType.FIRST_WORKOUT)
            return true
        }
        return false
    }

    fun checkAndUnlockFiveConsecutiveDays(currentStreak: Int): Boolean {
        // `>= 5` plutôt que `== 5` : une série d'activité peut sauter la valeur
        // exacte 5 (ex. plusieurs jours renseignés d'un coup) ; le garde
        // `!isUnlocked` garantit malgré tout un unique déblocage.
        if (currentStreak >= 5 && !isUnlocked(AchievementType.FIVE_CONSECUTIVE_DAYS)) {
            unlock(AchievementType.FIVE_CONSECUTIVE_DAYS)
            return true
        }
        return false
    }

    fun checkAndUnlockTenKgPR(newPR: Double, oldPR: Double?): Boolean {
        val prGain = if (oldPR != null) newPR - oldPR else newPR
        if (prGain >= 10.0 && !isUnlocked(AchievementType.TEN_KG_PR)) {
            unlock(AchievementType.TEN_KG_PR)
            return true
        }
        return false
    }

    fun checkAndUnlockFiveHundredKcal(caloriesBurned: Int): Boolean {
        if (caloriesBurned >= 500 && !isUnlocked(AchievementType.FIVE_HUNDRED_KCAL_SESSION)) {
            unlock(AchievementType.FIVE_HUNDRED_KCAL_SESSION)
            return true
        }
        return false
    }

    fun checkAndUnlockWeekPerfect(allWorkoutsLogged: Boolean, allMealsLogged: Boolean): Boolean {
        if (allWorkoutsLogged && allMealsLogged && !isUnlocked(AchievementType.WEEK_PERFECT)) {
            unlock(AchievementType.WEEK_PERFECT)
            return true
        }
        return false
    }

    fun isUnlocked(type: AchievementType): Boolean {
        return prefs.contains("achievement_${type.name}")
    }

    private fun unlock(type: AchievementType) {
        prefs.edit().putString("achievement_${type.name}", LocalDate.now().toString()).apply()
    }

    fun getUnlockedAchievements(): List<Achievement> {
        return AchievementType.entries.mapNotNull { type ->
            val dateStr = prefs.getString("achievement_${type.name}", null)
            if (dateStr != null) {
                Achievement(type, LocalDate.parse(dateStr))
            } else null
        }
    }
}
