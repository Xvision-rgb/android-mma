package com.example.mmarecomp.util

import com.example.mmarecomp.model.Workout

/**
 * Détecte les records personnels (charge la plus lourde jamais loggée pour
 * un exercice donné). Toujours utilisé pour célébrer, jamais pour signaler
 * l'absence de record — un exercice jamais loggué avant n'est jamais un
 * "échec", juste silencieux.
 */
object PersonalRecordDetector {
    /** Meilleure charge déjà enregistrée pour cet exercice dans l'historique,
     *  comparaison insensible à la casse et aux espaces superflus. */
    fun bestKnownLoad(exerciseName: String, history: List<Workout>): Double? {
        val normalized = exerciseName.trim().lowercase()
        return history
            .flatMap { it.exercices }
            .filter { it.nom.trim().lowercase() == normalized }
            .mapNotNull { it.chargeReelleKg }
            .maxOrNull()
    }

    /** Vrai si `newLoad` dépasse strictement la meilleure charge connue.
     *  Une première charge jamais comparée n'est jamais comptée comme record. */
    fun isNewRecord(exerciseName: String, newLoad: Double, history: List<Workout>): Boolean {
        val best = bestKnownLoad(exerciseName, history) ?: return false
        return newLoad > best
    }
}
