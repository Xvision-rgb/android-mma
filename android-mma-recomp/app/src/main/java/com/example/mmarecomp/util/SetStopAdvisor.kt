package com.example.mmarecomp.util

import com.example.mmarecomp.model.LoggedExercise
import com.example.mmarecomp.model.WorkoutType

/** Proxy de perte de vitesse, sans encodeur linéaire.
 *
 *  Un seuil d'arrêt de série bas (~20 % de perte) préserve la performance
 *  neuromusculaire ; un seuil haut (>25 %) accumule plus de volume relatif au
 *  prix de l'explosivité. Avec du MMA en parallèle, la fatigue neuromusculaire
 *  économisée en salle est celle qui reste disponible au sparring : les
 *  séances de force utilisent donc le seuil strict.
 *
 *  Faute de capteur de vitesse, on mesure la chute de reps à charge égale
 *  entre la première série travaillée et les suivantes — corrélat grossier
 *  mais gratuit, et directement lisible dans les données déjà loguées.
 *
 *  Le conseil est toujours informatif : l'app ne coupe jamais une série. */
object SetStopAdvisor {

    /** Seuil de chute de reps toléré avant de suggérer de couper. */
    const val SEUIL_STRICT = 0.20
    const val SEUIL_PERMISSIF = 0.30

    fun seuilPour(type: WorkoutType): Double = when (type) {
        WorkoutType.JambesForce, WorkoutType.TorseForce -> SEUIL_STRICT
        else -> SEUIL_PERMISSIF
    }

    fun estStrict(type: WorkoutType): Boolean = seuilPour(type) == SEUIL_STRICT

    /** Chute relative de reps entre la première série travaillée et la
     *  dernière, à charge comparable. Null si la comparaison n'a pas de sens
     *  (moins de deux séries, ou charges trop différentes pour comparer). */
    fun chuteRelative(exercice: LoggedExercise): Double? {
        val sets = exercice.effectiveSets.filter { it.reps > 0 && it.chargeKg > 0 }
        if (sets.size < 2) return null

        val reference = sets.first()
        // Une série faite à une charge nettement différente ne mesure pas la
        // même chose : on ne compare qu'à ±5 % de la charge de référence.
        val comparables = sets.drop(1).filter {
            kotlin.math.abs(it.chargeKg - reference.chargeKg) / reference.chargeKg <= 0.05
        }
        val derniere = comparables.lastOrNull() ?: return null

        return (reference.reps - derniere.reps).toDouble() / reference.reps
    }

    /** Message à afficher, ou null si rien à signaler. */
    fun conseil(exercice: LoggedExercise, strict: Boolean): String? {
        // Les séries reconstruites depuis l'ancien format sont toutes
        // identiques par construction : la chute y est structurellement nulle,
        // donc l'analyse n'a aucun sens.
        if (exercice.setsSontDerives) return null

        val chute = chuteRelative(exercice) ?: return null
        val seuil = if (strict) SEUIL_STRICT else SEUIL_PERMISSIF
        if (chute < seuil) return null

        val pct = (chute * 100).toInt()
        return if (strict) {
            "Chute de $pct % des reps à charge égale — sur une séance de force, " +
                "la série suivante peut être coupée pour préserver la fraîcheur."
        } else {
            "Chute de $pct % des reps à charge égale — proche de la limite utile de la série."
        }
    }
}
