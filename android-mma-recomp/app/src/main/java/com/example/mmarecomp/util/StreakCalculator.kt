package com.example.mmarecomp.util

import java.time.LocalDate

/**
 * Série de constance positive : nombre de jours consécutifs avec au moins
 * une séance OU un repas loggué. Jamais basée sur le poids (conforme à la
 * règle UX : le poids brut n'est jamais une donnée de suivi au quotidien).
 */
object StreakCalculator {
    /**
     * @param activeDates dates (n'importe quel ordre) où au moins une activité
     *   a été loggée.
     * @param today permet de fixer la date de référence dans les tests.
     * Aujourd'hui n'est pas obligatoire pour garder la série vivante — tant
     * qu'hier est couvert, la série continue de compter (on ne veut pas
     * qu'elle retombe à zéro juste parce que la journée n'est pas finie).
     */
    fun currentStreak(activeDates: Set<LocalDate>, today: LocalDate = LocalDate.now()): Int {
        if (activeDates.isEmpty()) return 0

        var streak = 0
        var cursor = if (today in activeDates) today else today.minusDays(1)
        while (cursor in activeDates) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }
}
