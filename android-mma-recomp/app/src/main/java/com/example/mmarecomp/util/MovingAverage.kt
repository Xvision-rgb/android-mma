package com.example.mmarecomp.util

import java.time.LocalDate

data class TrendPoint(val date: LocalDate, val value: Double)

enum class TrendDirection { HAUSSE, BAISSE, STABLE, INDETERMINE }

object MovingAverage {
    /** Direction de la tendance moyenne mobile — jamais de valeur brute,
     *  juste hausse/baisse/stable entre le premier et le dernier point de
     *  la fenêtre. */
    fun direction(points: List<TrendPoint>): TrendDirection = when {
        points.size < 2 -> TrendDirection.INDETERMINE
        points.last().value > points.first().value -> TrendDirection.HAUSSE
        points.last().value < points.first().value -> TrendDirection.BAISSE
        else -> TrendDirection.STABLE
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
