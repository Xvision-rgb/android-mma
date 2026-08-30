package com.example.mmarecomp.util

import com.example.mmarecomp.model.Meal

enum class NiveauConfiance(val label: String) {
    ELEVEE("Suivi complet"),
    MOYENNE("Suivi partiel"),
    INSUFFISANTE("Suivi trop partiel pour recalibrer"),
}

data class ConfianceSuivi(
    val completude: Double,
    val niveau: NiveauConfiance,
    val prisesLoguees: Int,
    val prisesAttendues: Int,
    val message: String,
) {
    /** Le recalibrage adaptatif n'a de sens qu'au-dessus du seuil : en
     *  dessous, il mesure surtout ce qui n'a pas été loggué. */
    val autoriseRecalibrage: Boolean get() = niveau != NiveauConfiance.INSUFFISANTE
}

/** Complétude du suivi alimentaire, et ce qu'on a le droit d'en déduire.
 *
 *  `CalorieCalculator.adaptiveRecalibration` déduit la dépense réelle des
 *  calories loguées. Si l'apport est sous-reporté, la dépense estimée est
 *  fausse d'autant — et le sous-report est le mode d'échec NORMAL du suivi
 *  alimentaire, pas l'exception : l'erreur moyenne atteint 30 à 50 % chez les
 *  non-formés, contre ~15 % chez des diététiciens entraînés.
 *
 *  On ne peut pas corriger le contenu de ce qui a été loggué. On peut en
 *  revanche savoir QUAND le calcul mérite confiance, et le dire.
 *
 *  Le seuil de 67 % vient du constat que logger environ deux tiers des prises
 *  capture déjà l'essentiel du bénéfice du suivi : l'objectif n'est pas un
 *  suivi parfait, c'est un suivi assez régulier pour être exploitable. */
object LoggingConfidence {

    /** Complétude minimale pour autoriser un recalibrage. */
    const val SEUIL_RECALIBRAGE = 0.67

    /** Au-dessus, le suivi est considéré comme complet. */
    const val SEUIL_ELEVE = 0.85

    /** Prises attendues par jour — cohérent avec les 4 à 6 repas visés. */
    const val PRISES_ATTENDUES_PAR_JOUR = MacroFloors.PRISES_MIN

    fun evaluer(repas: List<Meal>, jours: Int): ConfianceSuivi {
        val attendues = (jours * PRISES_ATTENDUES_PAR_JOUR).coerceAtLeast(1)
        // Une prise loguée deux fois le même créneau ne compte pas double :
        // on mesure la couverture, pas le nombre d'entrées.
        val loguees = repas.map { it.date to it.repas }.distinct().size
        val completude = (loguees.toDouble() / attendues).coerceAtMost(1.0)

        val niveau = when {
            completude >= SEUIL_ELEVE -> NiveauConfiance.ELEVEE
            completude >= SEUIL_RECALIBRAGE -> NiveauConfiance.MOYENNE
            else -> NiveauConfiance.INSUFFISANTE
        }

        return ConfianceSuivi(
            completude = completude,
            niveau = niveau,
            prisesLoguees = loguees,
            prisesAttendues = attendues,
            message = message(niveau, completude),
        )
    }

    /** Registre factuel : on décrit ce que la donnée permet de dire, jamais
     *  ce que l'utilisateur aurait dû faire. */
    private fun message(niveau: NiveauConfiance, completude: Double): String {
        val pct = (completude * 100).toInt()
        return when (niveau) {
            NiveauConfiance.ELEVEE ->
                "$pct % des prises loguées — l'estimation de dépense est fiable."
            NiveauConfiance.MOYENNE ->
                "$pct % des prises loguées — l'estimation reste exploitable, " +
                    "avec une marge un peu plus large."
            NiveauConfiance.INSUFFISANTE ->
                "$pct % des prises loguées sur la période. En dessous des deux tiers, " +
                    "le calcul mesurerait surtout ce qui manque : pas de recalibrage cette fois."
        }
    }
}
