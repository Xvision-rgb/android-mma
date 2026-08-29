package com.example.mmarecomp.util

import kotlin.math.roundToInt

data class PlanchersMacro(
    val glucidesG: Int,
    val proteinesG: Int,
    val lipidesG: Int,
) {
    val caloriesMinimum: Int get() = glucidesG * 4 + proteinesG * 4 + lipidesG * 9
}

data class MacrosAjustees(
    val glucidesG: Int,
    val proteinesG: Int,
    val lipidesG: Int,
    val caloriesTotales: Int,
    /** Ce qui a été relevé et pourquoi — affiché tel quel, pour que
     *  l'utilisateur voie que la cible a bougé et comprenne la raison. */
    val corrections: List<String>,
)

/** Planchers macro en descente de poids, pour un athlète de sport de combat.
 *
 *  Une cible calorique peut être atteignable tout en étant mal composée : un
 *  déficit tenu en coupant les glucides sabote l'entraînement bien avant de
 *  faire perdre du gras. Ces planchers existent pour empêcher ce cas.
 *
 *  Valeurs par kg de poids de corps et par jour :
 *  glucides 3,0–4,0 · protéines 1,2–2,0 · lipides 0,5–1,0. */
object MacroFloors {

    const val GLUCIDES_MIN_G_PAR_KG = 3.0
    const val PROTEINES_MIN_G_PAR_KG = 1.2
    const val LIPIDES_MIN_G_PAR_KG = 0.5

    /** Fourchette recommandée de glucides pour un athlète de force, hors
     *  descente de poids. Le bas de fourchette sert les jours de repos, le
     *  haut les jours de charge. */
    const val GLUCIDES_CIBLE_BASSE_G_PAR_KG = 4.0
    const val GLUCIDES_CIBLE_HAUTE_G_PAR_KG = 7.0

    /** Cible protéique visée, en g/kg de poids de corps. */
    const val PROTEINES_CIBLE_G_PAR_KG = 2.0

    /** Perte de poids hebdomadaire au-delà de laquelle la masse maigre part
     *  avec le gras. */
    const val PERTE_HEBDO_MAX_KG = 1.0

    /** Nombre de prises visé sur la journée. */
    const val PRISES_MIN = 4
    const val PRISES_MAX = 6

    fun planchers(poidsKg: Double) = PlanchersMacro(
        glucidesG = (poidsKg * GLUCIDES_MIN_G_PAR_KG).roundToInt(),
        proteinesG = (poidsKg * PROTEINES_MIN_G_PAR_KG).roundToInt(),
        lipidesG = (poidsKg * LIPIDES_MIN_G_PAR_KG).roundToInt(),
    )

    /** Relève une répartition qui passerait sous un plancher.
     *
     *  Les calories totales suivent la correction plutôt que d'être
     *  redistribuées : rogner ailleurs pour tenir un total ferait juste
     *  passer le problème d'un macro à l'autre. */
    fun appliquer(
        poidsKg: Double,
        glucidesG: Int,
        proteinesG: Int,
        lipidesG: Int,
    ): MacrosAjustees {
        val min = planchers(poidsKg)
        val corrections = mutableListOf<String>()

        val glucides = if (glucidesG < min.glucidesG) {
            corrections += "Glucides relevés à ${min.glucidesG}g " +
                "(plancher de ${Formatting.oneDecimal(GLUCIDES_MIN_G_PAR_KG)}g/kg — en dessous, " +
                "les séances se dégradent avant que la perte de gras n'accélère)."
            min.glucidesG
        } else {
            glucidesG
        }

        val proteines = if (proteinesG < min.proteinesG) {
            corrections += "Protéines relevées à ${min.proteinesG}g " +
                "(plancher de ${Formatting.oneDecimal(PROTEINES_MIN_G_PAR_KG)}g/kg pour préserver la masse maigre)."
            min.proteinesG
        } else {
            proteinesG
        }

        val lipides = if (lipidesG < min.lipidesG) {
            corrections += "Lipides relevés à ${min.lipidesG}g " +
                "(plancher de ${Formatting.oneDecimal(LIPIDES_MIN_G_PAR_KG)}g/kg — en dessous, " +
                "la fonction hormonale suit)."
            min.lipidesG
        } else {
            lipidesG
        }

        return MacrosAjustees(
            glucidesG = glucides,
            proteinesG = proteines,
            lipidesG = lipides,
            caloriesTotales = glucides * 4 + proteines * 4 + lipides * 9,
            corrections = corrections,
        )
    }

    /** Vitesse de perte de poids sur la fenêtre, en kg/semaine. Le poids doit
     *  venir de la moyenne mobile 7 jours, jamais de pesées brutes. */
    fun perteHebdomadaireKg(variationKg: Double, jours: Int): Double? {
        if (jours < 7) return null
        return -variationKg * 7.0 / jours
    }

    /** Alerte si la descente va trop vite. Null si le rythme est correct. */
    fun alertePerteTropRapide(variationKg: Double, jours: Int): String? {
        val parSemaine = perteHebdomadaireKg(variationKg, jours) ?: return null
        if (parSemaine <= PERTE_HEBDO_MAX_KG) return null
        return "Descente à ${Formatting.oneDecimal(parSemaine)}kg/semaine sur les $jours derniers jours. " +
            "Au-delà d'1kg, la masse maigre part avec le gras — remonter un peu les calories " +
            "garderait la force sans changer la trajectoire."
    }
}

/** Suivi de la durée passée en déficit, pour proposer une pause.
 *
 *  La littérature sur la recomposition penche pour une restriction
 *  intermittente et progressive plutôt qu'un déficit continu agressif, qui
 *  dégrade sommeil, hormones, motivation et performance en salle. La pause
 *  est une proposition — jamais imposée, jamais présentée comme un échec. */
object DietBreak {

    /** Semaines consécutives en déficit au-delà desquelles une pause à
     *  maintenance devient pertinente. */
    const val SEMAINES_AVANT_PAUSE = 8

    const val DUREE_PAUSE_SEMAINES = 1

    /** [joursEnDeficit] se compte sur les cibles réellement enregistrées,
     *  pas sur une intention déclarée dans le profil. */
    fun proposerPause(joursEnDeficit: Int): String? {
        if (joursEnDeficit < SEMAINES_AVANT_PAUSE * 7) return null
        val semaines = joursEnDeficit / 7
        return "$semaines semaines de suite en déficit. Une semaine à maintenance " +
            "relance souvent la suite mieux que de continuer tout droit — " +
            "et ça ne fait pas repartir de zéro."
    }
}
