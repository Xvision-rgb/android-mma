package com.example.mmarecomp.util

import kotlin.math.roundToInt

enum class EaStatut(val label: String) {
    CORRECTE("Disponibilité énergétique correcte"),
    VIGILANCE("Disponibilité énergétique à surveiller"),
    BASSE("Disponibilité énergétique basse"),
}

data class DisponibiliteEnergetique(
    val kcalParKgMasseMaigre: Double,
    val statut: EaStatut,
    val apportKcal: Int,
    val depenseExerciceKcal: Int,
    val masseMaigreKg: Double,
    /** Apport qu'il faudrait atteindre pour repasser au-dessus du seuil, ou
     *  null si l'apport y est déjà. */
    val apportPourSeuilKcal: Int?,
    val message: String,
)

/** Disponibilité énergétique : énergie restante pour les fonctions
 *  physiologiques une fois l'entraînement payé.
 *
 *      EA = (apport − dépense de l'exercice) / masse maigre
 *
 *  C'est la métrique de référence en sport de combat, et elle dit autre chose
 *  qu'un plancher calorique sur le poids de corps. À 75 kg et 12 % de masse
 *  grasse, un plancher à 25 kcal/kg autorise 1 875 kcal ; avec 800 kcal
 *  dépensés à l'entraînement ce jour-là, l'EA réelle tombe à ~16 kcal/kg de
 *  masse maigre — la moitié du seuil admis, sans qu'aucune alerte ne parte.
 *
 *  Une faible disponibilité énergétique dégrade sommeil, fonction hormonale,
 *  immunité et performance : c'est ce qui fait échouer le reste du plan, pas
 *  le détail des macros. */
object EnergyAvailability {

    /** Seuil bas admis, en kcal par kg de masse maigre et par jour. */
    const val SEUIL_CORRECT = 30.0
    const val SEUIL_BAS = 25.0

    /** Coût énergétique approximatif d'une unité de charge interne
     *  (session-RPE = RPE × minutes).
     *
     *  Calibrage : une séance de 60 min à RPE 7 vaut 420 unités et coûte
     *  grossièrement 500 kcal à un athlète de ~75 kg, d'où ~1,2 kcal/unité.
     *  C'est une ESTIMATION assumée, pas une mesure — elle sert à faire
     *  bouger l'EA dans le bon sens, pas à donner un chiffre exact. */
    const val KCAL_PAR_UNITE_CHARGE = 1.2

    fun depuisChargeInterne(chargeInterne: Double): Int =
        (chargeInterne * KCAL_PAR_UNITE_CHARGE).roundToInt()

    fun calculer(
        apportKcal: Int,
        depenseExerciceKcal: Int,
        masseMaigreKg: Double,
    ): DisponibiliteEnergetique? {
        if (masseMaigreKg <= 0.0) return null

        val ea = (apportKcal - depenseExerciceKcal) / masseMaigreKg
        val statut = when {
            ea >= SEUIL_CORRECT -> EaStatut.CORRECTE
            ea >= SEUIL_BAS -> EaStatut.VIGILANCE
            else -> EaStatut.BASSE
        }

        val apportCible = (SEUIL_CORRECT * masseMaigreKg + depenseExerciceKcal).roundToInt()
        val manque = if (statut == EaStatut.CORRECTE) null else apportCible

        return DisponibiliteEnergetique(
            kcalParKgMasseMaigre = ea,
            statut = statut,
            apportKcal = apportKcal,
            depenseExerciceKcal = depenseExerciceKcal,
            masseMaigreKg = masseMaigreKg,
            apportPourSeuilKcal = manque,
            message = message(statut, ea, apportCible, apportKcal),
        )
    }

    /** Registre volontairement factuel et orienté action : on dit combien
     *  ajouter, jamais que la journée a été mal gérée. */
    private fun message(statut: EaStatut, ea: Double, apportCible: Int, apportKcal: Int): String {
        val valeur = Formatting.oneDecimal(ea)
        return when (statut) {
            EaStatut.CORRECTE ->
                "$valeur kcal/kg de masse maigre après l'entraînement — au-dessus du seuil de 30."
            EaStatut.VIGILANCE ->
                "$valeur kcal/kg de masse maigre après l'entraînement. " +
                    "Environ ${apportCible - apportKcal} kcal de glucides en plus repasseraient au-dessus de 30."
            EaStatut.BASSE ->
                "$valeur kcal/kg de masse maigre une fois l'entraînement payé. " +
                    "C'est le niveau où le sommeil et la performance en salle décrochent en premier. " +
                    "Viser ~$apportCible kcal aujourd'hui, en ajoutant surtout des glucides."
        }
    }
}
