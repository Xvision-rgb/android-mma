package com.example.mmarecomp.util

import java.time.LocalDate

data class TrendPoint(val date: LocalDate, val value: Double)

object MovingAverage {
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
