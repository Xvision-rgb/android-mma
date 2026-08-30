package com.example.mmarecomp.util

import java.time.LocalDate

data class TrendPoint(val date: LocalDate, val value: Double)

enum class TrendDirection { HAUSSE, BAISSE, STABLE, INDETERMINE }

object MovingAverage {
    /**
     * Direction de la tendance moyenne mobile — jamais de valeur brute.
     *
     * Compare le dernier point au point le plus proche de [lookbackDays]
     * jours plus tôt. Comparer le premier et le dernier point d'une série
     * de 60 jours ferait passer une tendance de deux mois pour « cette
     * semaine » dès que l'historique dépasse 7 jours.
     */
    fun direction(points: List<TrendPoint>, lookbackDays: Long = 7): TrendDirection {
        if (points.size < 2) return TrendDirection.INDETERMINE
        val sorted = points.sortedBy { it.date }
        val last = sorted.last()
        val cutoff = last.date.minusDays(lookbackDays)
        val reference = sorted.lastOrNull { it.date <= cutoff } ?: sorted.first()
        if (reference.date == last.date) return TrendDirection.INDETERMINE
        return when {
            last.value > reference.value -> TrendDirection.HAUSSE
            last.value < reference.value -> TrendDirection.BAISSE
            else -> TrendDirection.STABLE
        }
    }

    /** Moyenne mobile 7 jours sur une série (date, valeur).
     *  C'est TOUJOURS cette série qu'il faut afficher pour le poids —
     *  jamais le point brut du jour — pour désamorcer l'anxiété liée
     *  aux fluctuations quotidiennes. */
    fun sevenDay(points: List<TrendPoint>): List<TrendPoint> {
        if (points.isEmpty()) return emptyList()
        val sorted = points.sortedBy { it.date }
        return sorted.map { point ->
            val windowStart = point.date.minusDays(6)
            val window = sorted.filter { it.date >= windowStart && it.date <= point.date }
            val avg = window.sumOf { it.value } / window.size
            TrendPoint(point.date, avg)
        }
    }
}
