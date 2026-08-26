package com.example.mmarecomp.util

import android.content.Context
import java.time.LocalDate

class StreakManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("streak_prefs", Context.MODE_PRIVATE)

    fun updateStreak() {
        val today = LocalDate.now()
        val lastLoginStr = prefs.getString("lastLoginDate", null)
        val lastLogin = if (lastLoginStr != null) LocalDate.parse(lastLoginStr) else null

        val currentStreak = when {
            lastLogin == null -> 1 // First login ever
            lastLogin == today -> prefs.getInt("currentStreak", 1) // Already logged in today
            lastLogin.plusDays(1) == today -> prefs.getInt("currentStreak", 0) + 1 // Consecutive day
            else -> 1 // Streak broken
        }

        val bestStreak = maxOf(currentStreak, prefs.getInt("bestStreak", 0))

        prefs.edit().apply {
            putString("lastLoginDate", today.toString())
            putInt("currentStreak", currentStreak)
            putInt("bestStreak", bestStreak)
            apply()
        }
    }

    fun getCurrentStreak(): Int = prefs.getInt("currentStreak", 0)
    fun getBestStreak(): Int = prefs.getInt("bestStreak", 0)
}
