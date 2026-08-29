package com.example.mmarecomp.util

import com.example.mmarecomp.model.MuscleZone
import com.example.mmarecomp.model.Workout
import com.example.mmarecomp.model.WorkoutType
import java.time.LocalDate

/** Interférence entre course à pied et travail de force.
 *
 *  Trois faits qui orientent toute la logique de ce fichier :
 *
 *  1. L'interférence touche surtout la FORCE et la PUISSANCE du bas du corps.
 *     L'hypertrophie est peu affectée — inutile de s'inquiéter pour la masse.
 *  2. Elle est plus marquée avec la COURSE qu'avec le vélo. C'est la modalité
 *     la plus contraignante quand la priorité est la force.
 *  3. Elle est plus nette chez les pratiquants entraînés que chez les
 *     débutants, chez qui elle est négligeable.
 *
 *  Conséquence pratique quand l'objectif est la force et la masse : la course
 *  ne disparaît pas, elle passe en DOSE D'ENTRETIEN. Le VO2max se maintient
 *  avec 2 séances par semaine à condition de garder l'INTENSITÉ — c'est
 *  baisser l'intensité qui le fait chuter, pas baisser le volume. On préserve
 *  donc le moteur en libérant la récupération pour la salle. */
object EnduranceInterference {

    /** Séances de course par semaine suffisantes pour maintenir le VO2max,
     *  à intensité préservée. */
    const val DOSE_ENTRETIEN_PAR_SEMAINE = 2

    /** Au-delà, la course commence à peser sur la progression en force quand
     *  celle-ci est la priorité. */
    const val SEUIL_VOLUME_ELEVE_PAR_SEMAINE = 4

    private val typesEndurance = setOf(WorkoutType.Hiit, WorkoutType.MmaWod)

    private val basDuCorps = setOf(
        WorkoutType.JambesForce,
        WorkoutType.JambesHypertrophie,
    )

    /** Conflits de programmation sur la journée [date]. */
    fun conflits(
        date: LocalDate,
        workouts: List<Workout>,
    ): List<String> {
        val messages = mutableListOf<String>()
        val jour = DateUtils.string(date)
        val veille = DateUtils.string(date.minusDays(1))

        val forceBasDuCorps = workouts.any { it.date == jour && it.type in basDuCorps }
        val enduranceJour = workouts.any { it.date == jour && it.type in typesEndurance }
        val enduranceVeille = workouts.any { it.date == veille && it.type in typesEndurance }

        if (forceBasDuCorps && (enduranceJour || enduranceVeille)) {
            messages += "Course et bas du corps lourd à moins de 24 h. L'interférence porte " +
                "surtout sur la force des jambes — décaler la sortie, ou la placer un jour de haut du corps."
        }

        if (forceBasDuCorps && enduranceJour) {
            messages += "Si les deux tiennent dans la même journée, faire la force en premier : " +
                "la qualité travaillée en second est celle qui trinque."
        }

        return messages
    }

    /** Lecture du volume de course de la semaine, quand la priorité est la
     *  force. Null si le volume est déjà dans la fourchette utile. */
    fun noteVolumeCourse(sortiesCetteSemaine: Int): String? = when {
        sortiesCetteSemaine == 0 ->
            "Aucune sortie cette semaine — le VO2max se maintient avec " +
                "$DOSE_ENTRETIEN_PAR_SEMAINE sorties, à intensité conservée."
        sortiesCetteSemaine >= SEUIL_VOLUME_ELEVE_PAR_SEMAINE ->
            "$sortiesCetteSemaine sorties cette semaine. Avec la force en priorité, " +
                "$DOSE_ENTRETIEN_PAR_SEMAINE sorties intenses suffisent à garder le moteur — " +
                "le reste se paie sur les jambes."
        else -> null
    }

    /** La course ne compte pas dans le volume de force, mais elle sollicite
     *  bien les jambes : à signaler quand le volume quadriceps est déjà haut. */
    fun cumuleAvecZone(zone: MuscleZone): Boolean =
        zone == MuscleZone.QUADS_BRAS || zone == MuscleZone.CHAINE_POSTERIEURE
}

/** Détection d'une approche du plafond de volume récupérable.
 *
 *  Les signes décrits dans la littérature sont comportementaux avant d'être
 *  physiologiques : la force qui décline d'une séance à l'autre, la fatigue
 *  qui s'accumule de semaine en semaine, des muscles qui ne récupèrent plus
 *  avec les jours de repos habituels. La réponse est une baisse de volume de
 *  20 à 30 %, pas un arrêt.
 *
 *  L'app produit déjà ce signal sans le lire : quand l'autorégulation
 *  prescrit une BAISSE de charge plusieurs séances de suite sur le même
 *  exercice, c'est une performance qui décline. */
object OverreachingDetector {

    /** Séances consécutives en baisse à partir desquelles on alerte. */
    const val SEANCES_EN_BAISSE = 3

    /** Réduction de volume proposée. */
    const val REDUCTION_VOLUME = 0.25

    /** Compte les baisses consécutives de charge de travail sur un exercice,
     *  de la séance la plus récente vers la plus ancienne. */
    fun seancesConsecutivesEnBaisse(chargesParSeance: List<Double>): Int {
        if (chargesParSeance.size < 2) return 0
        var baisses = 0
        // Parcours du plus récent au plus ancien : une remontée quelque part
        // dans l'historique interrompt la série, mais ne l'annule pas.
        for (i in chargesParSeance.lastIndex downTo 1) {
            if (chargesParSeance[i] < chargesParSeance[i - 1]) baisses++ else break
        }
        return baisses
    }

    /** Alerte si un exercice décline de façon soutenue. Null sinon. */
    fun alerte(exercice: String, chargesParSeance: List<Double>): String? {
        val baisses = seancesConsecutivesEnBaisse(chargesParSeance)
        if (baisses < SEANCES_EN_BAISSE) return null
        val pct = (REDUCTION_VOLUME * 100).toInt()
        return "$exercice recule depuis $baisses séances. Quand la force décline pendant que " +
            "le volume tient, c'est le volume qui est au-dessus du récupérable : " +
            "couper $pct % sur cette zone pendant une semaine remet souvent tout en marche."
    }
}
