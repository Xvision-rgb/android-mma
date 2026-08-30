package com.example.mmarecomp.util

import com.example.mmarecomp.model.Workout

/** Force relative sur un mouvement : 1RM estimé rapporté au poids de corps. */
data class ForceRelative(
    val exercice: String,
    val unRmEstimeKg: Double,
    val poidsCorpsKg: Double,
    val ratio: Double,
)

/** Indicateur directeur de progression.
 *
 *  Quand l'objectif est la force relative et la densité, ni le poids sur la
 *  balance ni la charge absolue ne suffisent : prendre 4 kg qui ne produisent
 *  pas de force proportionnelle est un recul. Le ratio 1RM/poids de corps est
 *  le seul chiffre qui tranche, et il se lit dans les deux sens — une perte de
 *  poids à ratio croissant est un progrès.
 *
 *  Le poids de corps vient TOUJOURS de la moyenne mobile 7 jours, jamais d'une
 *  pesée brute : sinon le ratio hériterait du bruit quotidien de la balance. */
object RelativeStrength {

    /** Formule d'Epley — au-delà d'une dizaine de reps, l'estimation dérive,
     *  d'où le plafond. */
    private const val REPS_MAX_FIABLES = 12

    fun unRmEpley(chargeKg: Double, reps: Int): Double? {
        if (chargeKg <= 0.0 || reps < 1 || reps > REPS_MAX_FIABLES) return null
        return chargeKg * (1 + reps / 30.0)
    }

    /** Meilleur 1RM estimé pour un exercice sur les séances fournies. */
    fun meilleur1Rm(exerciseName: String, workouts: List<Workout>): Double? =
        workouts
            .flatMap { it.exercices }
            .filter { ExerciseName.memeExercice(it.nom, exerciseName) }
            .flatMap { it.effectiveSets }
            .mapNotNull { unRmEpley(it.chargeKg, it.reps) }
            .maxOrNull()

    /** Force relative par mouvement principal, triée du meilleur ratio au
     *  moins bon. [poidsCorpsKg] doit être la moyenne mobile 7 jours. */
    fun parExercice(
        workouts: List<Workout>,
        poidsCorpsKg: Double?,
        minSeances: Int = 1,
    ): List<ForceRelative> {
        if (poidsCorpsKg == null || poidsCorpsKg <= 0.0) return emptyList()
        return workouts
            .flatMap { it.exercices }
            .filter { it.nom.isNotBlank() && MuscleZoneClassifier.estPolyarticulaire(it.nom) }
            .groupBy { ExerciseName.cle(it.nom) }
            .filterValues { it.size >= minSeances }
            .mapNotNull { (_, exercices) ->
                val nom = ExerciseName.propre(exercices.first().nom)
                val unRm = exercices
                    .flatMap { it.effectiveSets }
                    .mapNotNull { unRmEpley(it.chargeKg, it.reps) }
                    .maxOrNull() ?: return@mapNotNull null
                ForceRelative(
                    exercice = nom,
                    unRmEstimeKg = unRm,
                    poidsCorpsKg = poidsCorpsKg,
                    ratio = unRm / poidsCorpsKg,
                )
            }
            .sortedByDescending { it.ratio }
    }
}

/** Repères de préhension, et rappel de la spécificité du type de poigne.
 *
 *  Chez les lutteurs d'élite, la force de préhension corrèle fortement avec
 *  celle du haut du corps et du cou. Mais les trois poignes sont des qualités
 *  distinctes : une traction échoue sur le SUPPORT grip (maintien isométrique
 *  dans le temps), que les pinces — qui entraînent le CRUSH grip — ne
 *  développent pas. D'où l'absence délibérée de toute suggestion de pince
 *  dans l'app. */
object GripBenchmarks {

    const val SEUIL_DEFICIT_SEC = 30
    const val SEUIL_CONFORTABLE_SEC = 60

    fun lecture(deadHangSec: Int): String = when {
        deadHangSec < SEUIL_DEFICIT_SEC ->
            "Sous 30 s — la poigne est bien le facteur limitant. Sangles sur le tirage " +
                "lourd, et suspensions en fin de séance."
        deadHangSec < SEUIL_CONFORTABLE_SEC ->
            "Entre 30 et 60 s — dans la norme, à développer. Suspensions et farmer's walks."
        else ->
            "Au-dessus de 60 s — la poigne est solide ; si le tirage plafonne encore, " +
                "regarder la prise (magnésie, pouce enroulé, épaisseur de barre)."
    }

    /** Exercices de support grip — les seuls pertinents pour un tirage limité
     *  par la poigne. */
    val exercicesRecommandes = listOf(
        "Suspension à la barre (dead hang)",
        "Farmer's walk",
        "Suspension lestée",
        "Rowing en prise épaisse (fat grips / serviette)",
    )
}
