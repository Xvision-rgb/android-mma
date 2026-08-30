package com.example.mmarecomp.util

import com.example.mmarecomp.model.RepasSlot
import com.example.mmarecomp.model.TypeJour
import kotlin.math.roundToInt

data class DailyTarget(
    val calories: Int,
    val proteinesG: Double,
    val glucidesG: Double = 0.0,
    val lipidesG: Double = 0.0,
)

data class SlotTarget(val calories: Int, val proteinesG: Double)

object NutritionTargetCalculator {
    /** Écart calorique historique entre jour training et jour repos. Conservé
     *  pour les cibles génériques de repli ; les cibles personnalisées passent
     *  désormais par la périodisation glucidique (cf. targetFor). */
    private const val TRAINING_REST_SPREAD = 150

    /** Cible du jour selon calorie cycling — valeurs de repli génériques,
     *  utilisées uniquement tant qu'aucune pesée n'existe encore pour
     *  calculer une cible personnalisée (cf. targetFor / CalorieCalculator).
     *  Ne pas s'y fier pour un pratiquant de sport de combat : ces chiffres
     *  sous-estiment largement sa dépense réelle. */
    fun target(typeJour: TypeJour): DailyTarget = when (typeJour) {
        TypeJour.Training -> DailyTarget(2050, 135.0) // milieu de 2000-2100 kcal / 130-140g
        TypeJour.Repos -> DailyTarget(1800, 130.0)
    }

    /** Cycle training/repos autour d'une cible personnalisée.
     *
     *  Le swing porte sur les GLUCIDES, pas sur un total calorique abstrait :
     *  c'est le principe « fuel for the work required ». Les protéines
     *  restent constantes — elles servent à préserver la masse maigre, un
     *  besoin qui ne baisse pas les jours de repos — et les lipides aussi,
     *  pour la fonction hormonale.
     *
     *  [chargeInterne] est la charge de la journée (session-RPE × durée). Si
     *  elle est connue, elle module finement ; sinon on retombe sur le
     *  booléen training/repos. */
    fun targetFor(
        typeJour: TypeJour,
        baseCalories: Int,
        proteinesG: Int,
        lipidesG: Int = 0,
        poidsKg: Double? = null,
        chargeInterne: Double? = null,
    ): DailyTarget {
        // Sans poids connu, on ne peut pas raisonner en g/kg : on garde
        // l'ancien comportement calorique plutôt que d'inventer une base.
        if (poidsKg == null || poidsKg <= 0.0 || lipidesG <= 0) {
            val calories = when (typeJour) {
                TypeJour.Training -> baseCalories + TRAINING_REST_SPREAD
                TypeJour.Repos -> baseCalories - TRAINING_REST_SPREAD
            }
            return DailyTarget(calories, proteinesG.toDouble())
        }

        val glucidesParKg = glucidesParKgPour(typeJour, chargeInterne)
        val glucidesBruts = (poidsKg * glucidesParKg).roundToInt()

        val ajustees = MacroFloors.appliquer(
            poidsKg = poidsKg,
            glucidesG = glucidesBruts,
            proteinesG = proteinesG,
            lipidesG = lipidesG,
        )

        return DailyTarget(
            calories = ajustees.caloriesTotales,
            proteinesG = ajustees.proteinesG.toDouble(),
            glucidesG = ajustees.glucidesG.toDouble(),
            lipidesG = ajustees.lipidesG.toDouble(),
        )
    }

    /** Glucides du jour, en g/kg de poids de corps.
     *
     *  Un jour de repos vise le bas de la fourchette athlète de force
     *  (4 g/kg), un jour chargé le haut (7 g/kg). L'écart correspondant est
     *  de l'ordre de 200 à 250 g de glucides pour un athlète de 75 kg, très
     *  au-delà des ±150 kcal de l'ancienne formule — c'est justement ce que
     *  la périodisation glucidique cherche à produire. */
    internal fun glucidesParKgPour(typeJour: TypeJour, chargeInterne: Double?): Double {
        if (typeJour == TypeJour.Repos) return MacroFloors.GLUCIDES_CIBLE_BASSE_G_PAR_KG

        val basse = MacroFloors.GLUCIDES_CIBLE_BASSE_G_PAR_KG
        val haute = MacroFloors.GLUCIDES_CIBLE_HAUTE_G_PAR_KG
        val charge = chargeInterne ?: return (basse + haute) / 2

        // 700 unités de charge ≈ une grosse séance (RPE 8 × 90 min) : au-delà,
        // on est au plafond de la fourchette.
        val fraction = (charge / 700.0).coerceIn(0.0, 1.0)
        return basse + (haute - basse) * fraction
    }

    /** Répartition indicative (non bloquante) de la cible du jour sur les
     *  créneaux repas — un repas qui déborde n'est jamais signalé tant que
     *  le total du jour reste dans la cible.
     *
     *  Les CALORIES suivent la part indicative du créneau, mais les
     *  PROTÉINES sont réparties à plat. La synthèse protéique est
     *  réfractaire : une fois le signal saturé, la protéine en plus d'un gros
     *  repas ne sert à rien, alors qu'elle manquerait à un créneau plus
     *  léger. Répartir les protéines au prorata des calories — ce que faisait
     *  cette fonction — donnait 20 % du total à l'après-midi et 30 % au
     *  post-training, exactement l'inverse de ce qu'il faut. */
    fun indicativeSplit(calories: Int, proteinesG: Double, slots: List<RepasSlot>): Map<RepasSlot, SlotTarget> {
        if (slots.isEmpty()) return emptyMap()
        val proteinesParPrise = (proteinesG / slots.size * 10).roundToInt() / 10.0
        return slots.associateWith { slot ->
            SlotTarget(
                calories = (calories * slot.shareIndicatif).toInt(),
                proteinesG = proteinesParPrise,
            )
        }
    }

    /** Seuil de protéines par prise en dessous duquel la stimulation n'est
     *  pas maximale : environ 20 à 30 g de protéines de qualité, soit ~2-3 g
     *  de leucine. Au-delà de la borne haute, le signal est saturé — la
     *  protéine supplémentaire n'est pas perdue, elle ne stimule simplement
     *  plus davantage. */
    const val PROTEINES_PAR_PRISE_MIN_G = 20.0
    const val PROTEINES_PAR_PRISE_SATURATION_G = 30.0

    /** Note factuelle sur une prise, ou null si elle est dans la zone utile.
     *  Jamais un échec : le total de la journée reste ce qui compte. */
    fun notePriseProteique(proteinesG: Double): String? = when {
        proteinesG < PROTEINES_PAR_PRISE_MIN_G ->
            "${Formatting.oneDecimal(proteinesG)}g de protéines — sous les ~20g qui stimulent pleinement la synthèse."
        proteinesG > PROTEINES_PAR_PRISE_SATURATION_G * 2 ->
            "${Formatting.oneDecimal(proteinesG)}g sur une seule prise — le signal sature vers 30g, " +
                "étaler sur une prise de plus rendrait la journée plus efficace."
        else -> null
    }

    /** Alerte douce : plusieurs jours d'affilée nettement en dessous de la
     *  cible calorique. Ne culpabilise jamais un jour isolé en dessous de
     *  l'objectif — il faut trois jours consécutifs sous 85% de la cible. */
    fun softUnderTargetAlert(recentDailyTotals: List<Triple<String, Int, Int>>): Boolean {
        val lastThree = recentDailyTotals.takeLast(3)
        if (lastThree.size != 3) return false
        return lastThree.all { (_, calories, cible) -> calories < (cible * 0.85).toInt() }
    }
}
