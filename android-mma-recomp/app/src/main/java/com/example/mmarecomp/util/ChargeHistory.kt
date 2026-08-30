package com.example.mmarecomp.util

import com.example.mmarecomp.model.Workout

/**
 * Historique de charge lu sur les séries réelles ([LoggedExercise.chargeMaxKg]),
 * pas sur l'agrégat [LoggedExercise.chargeReelleKg] qui peut rester figé
 * après une saisie série par série.
 *
 * La comparaison des noms passe par [ExerciseName] : `equals(ignoreCase)` seul
 * laissait un espace en trop ou un accent composé différemment casser la
 * correspondance, et le record comme le préremplissage échouaient en silence.
 */
object ChargeHistory {

    fun personalRecordKg(workouts: List<Workout>, exerciseName: String): Double? {
        if (exerciseName.isBlank()) return null
        return workouts
            .flatMap { it.exercices }
            .filter { ExerciseName.memeExercice(it.nom, exerciseName) }
            .mapNotNull { it.chargeMaxKg }
            .maxOrNull()
    }

    fun lastKnownChargeKg(workouts: List<Workout>, exerciseName: String): Double? {
        if (exerciseName.isBlank()) return null
        return workouts
            .sortedByDescending { it.date }
            .flatMap { it.exercices }
            .firstOrNull { ExerciseName.memeExercice(it.nom, exerciseName) }
            ?.chargeMaxKg
    }
}
