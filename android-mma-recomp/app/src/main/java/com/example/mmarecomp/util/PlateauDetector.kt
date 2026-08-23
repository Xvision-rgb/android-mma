package com.example.mmarecomp.util

import java.time.LocalDate

enum class PlateauStatus {
    NONE,

    /** Poids stable mais performances en hausse -> message positif,
     *  jamais une alerte de stagnation. */
    RECOMPOSITION_EN_COURS,
}

object PlateauDetector {
    /** Poids stable ±0.5kg sur 2+ semaines (14 jours). Si en plus les
     *  performances progressent sur la même période, on affiche un message
     *  positif de recomposition plutôt qu'une alerte de stagnation. */
    fun detect(
        morningWeighIns: List<Pair<LocalDate, Double>>,
        performanceTrendUp: Boolean,
    ): PlateauStatus {
        val cutoff = LocalDate.now().minusDays(14)
        val recent = morningWeighIns.filter { it.first >= cutoff }.sortedBy { it.first }
        if (recent.size < 2) return PlateauStatus.NONE

        val minW = recent.minOf { it.second }
        val maxW = recent.maxOf { it.second }
        val isStable = (maxW - minW) <= 0.5

        return if (isStable && performanceTrendUp) PlateauStatus.RECOMPOSITION_EN_COURS else PlateauStatus.NONE
    }
}
