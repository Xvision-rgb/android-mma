package com.example.mmarecomp.util

import com.example.mmarecomp.model.MuscleZone
import com.example.mmarecomp.model.Workout

enum class ZoneVolume(val label: String) {
    SOUS_MAINTIEN("Sous le maintien"),
    MAINTIEN("Maintien"),
    PRODUCTIF("Zone productive"),
    PLAFOND("Proche du plafond"),
    AU_DESSUS("Au-dessus du récupérable"),
}

data class BilanVolume(
    val zone: MuscleZone,
    val series: Int,
    val situation: ZoneVolume,
    val message: String,
)

/** Repères de volume hebdomadaire, en SÉRIES par muscle et par semaine.
 *
 *  L'app comptait le volume en tonnage (Σ reps × charge). C'est utile pour
 *  suivre une progression de charge, mais ce n'est pas l'unité dans laquelle
 *  la littérature raisonne, et surtout ce n'est pas ce qui pilote
 *  l'adaptation : les méta-analyses convergent sur le VOLUME HEBDOMADAIRE
 *  EN SÉRIES comme moteur principal, avec des rendements décroissants.
 *
 *  Corollaire important, et contre-intuitif : à volume égal, la FRÉQUENCE
 *  ne change quasiment rien, de 1 à 6 jours par semaine. S'entraîner tous les
 *  jours est une façon de distribuer un gros volume en séances courtes — pas
 *  un levier d'adaptation en soi. Ce qu'il faut surveiller, c'est le total.
 *
 *  Repères usuels chez le pratiquant entraîné, avec une variabilité
 *  individuelle importante :
 *   - maintien       ~6 séries
 *   - minimum utile  4-8 séries
 *   - zone productive 10-20 séries
 *   - plafond        18-25+ séries, très individuel */
object VolumeLandmarks {

    const val MAINTIEN = 6
    const val MINIMUM_UTILE = 8
    const val PRODUCTIF_HAUT = 20
    const val PLAFOND_INDICATIF = 25

    /** Séries hebdomadaires par zone, sur les séances fournies.
     *
     *  Une série ne compte que si elle a été réellement travaillée : une
     *  série à vide ou à zéro rep gonflerait le décompte sans rien stimuler. */
    fun seriesParZone(workouts: List<Workout>): Map<MuscleZone, Int> {
        val compte = mutableMapOf<MuscleZone, Int>()
        workouts.forEach { workout ->
            workout.exercices.forEach { exercice ->
                if (exercice.nom.isBlank()) return@forEach
                val zone = MuscleZoneClassifier.classifier(exercice.nom)
                val series = exercice.effectiveSets.count { it.reps > 0 && it.chargeKg > 0 }
                if (series > 0) compte[zone] = (compte[zone] ?: 0) + series
            }
        }
        return compte
    }

    fun situationPour(series: Int): ZoneVolume = when {
        series < MAINTIEN -> ZoneVolume.SOUS_MAINTIEN
        series < MINIMUM_UTILE -> ZoneVolume.MAINTIEN
        series <= PRODUCTIF_HAUT -> ZoneVolume.PRODUCTIF
        series <= PLAFOND_INDICATIF -> ZoneVolume.PLAFOND
        else -> ZoneVolume.AU_DESSUS
    }

    fun bilan(workouts: List<Workout>): List<BilanVolume> {
        val parZone = seriesParZone(workouts)
        return MuscleZone.entries.map { zone ->
            val series = parZone[zone] ?: 0
            val situation = situationPour(series)
            BilanVolume(zone, series, situation, message(zone, series, situation))
        }
    }

    /** Registre factuel : on situe par rapport à un repère, on ne note pas. */
    private fun message(zone: MuscleZone, series: Int, situation: ZoneVolume): String = when (situation) {
        ZoneVolume.SOUS_MAINTIEN ->
            "$series séries — sous le seuil de maintien (~$MAINTIEN). Cette zone se conserve mal à ce niveau."
        ZoneVolume.MAINTIEN ->
            "$series séries — de quoi maintenir, pas encore de quoi construire."
        ZoneVolume.PRODUCTIF ->
            "$series séries — dans la zone où les gains se font."
        ZoneVolume.PLAFOND ->
            "$series séries — haut de fourchette. Tenable si la récupération suit, à surveiller sinon."
        ZoneVolume.AU_DESSUS ->
            "$series séries — au-delà de ce que la plupart récupèrent durablement. " +
                "Si la force stagne ou recule, c'est le premier endroit où couper."
    }

    /** Zones qu'il faudrait remonter en priorité, de la plus déficitaire à la
     *  moins. Ne remonte que ce qui est réellement sous le minimum utile. */
    fun zonesADevelopper(workouts: List<Workout>): List<BilanVolume> =
        bilan(workouts)
            .filter { it.situation == ZoneVolume.SOUS_MAINTIEN || it.situation == ZoneVolume.MAINTIEN }
            .sortedBy { it.series }
}
