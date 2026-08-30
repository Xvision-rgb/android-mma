package com.example.mmarecomp.util

import java.time.LocalDate

/**
 * Série de jours consécutifs, jusqu'à aujourd'hui, avec au moins une
 * activité loggée. Purement positif : on compte ce qui est là, jamais
 * un message de série brisée.
 */
object ActivityStreak {

    fun days(loggedDates: Set<String>, today: LocalDate = LocalDate.now()): Int {
        var streak = 0
        var cursor = today
        while (loggedDates.contains(DateUtils.string(cursor))) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }
}
