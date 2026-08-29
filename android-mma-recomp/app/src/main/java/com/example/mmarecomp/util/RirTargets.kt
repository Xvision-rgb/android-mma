package com.example.mmarecomp.util

import com.example.mmarecomp.model.LoggedExercise
import com.example.mmarecomp.model.MuscleZone

/** Fourchette de reps in reserve visée pour un exercice. */
data class RirCible(val min: Int, val max: Int) {
    fun contient(rir: Int): Boolean = rir in min..max
    val label: String get() = "$min-$max RIR"
}

/** Cibles de proximité à l'échec, différenciées par type d'exercice.
 *
 *  Les gains de force sont plats sur une large plage de RIR, alors que
 *  l'hypertrophie s'améliore à mesure qu'on se rapproche de l'échec. Comme
 *  l'objectif est la force relative et la densité — pas la masse maximale —
 *  le modèle retenu est sous-maximal et à haute fréquence : le volume vient
 *  de la répétition des séances, pas de la destruction de chaque série.
 *
 *  Conséquence pratique : aller à l'échec sur un polyarticulaire ne rapporte
 *  rien en force et coûte une fatigue qui manquera au sparring. */
object RirTargets {

    val POLYARTICULAIRE = RirCible(2, 3)
    val ISOLATION = RirCible(1, 2)

    fun cible(zone: MuscleZone, estPolyarticulaire: Boolean): RirCible = when {
        zone == MuscleZone.COU_POIGNE -> ISOLATION
        estPolyarticulaire -> POLYARTICULAIRE
        else -> ISOLATION
    }

    fun cible(exercice: LoggedExercise): RirCible {
        val zone = MuscleZoneClassifier.classifier(exercice.nom)
        return cible(zone, MuscleZoneClassifier.estPolyarticulaire(exercice.nom))
    }

    /** Note factuelle quand la séance sort de la fourchette visée, ou null si
     *  tout est dans la cible. Jamais formulé comme une faute : le RIR est
     *  une estimation, pas une mesure. */
    fun note(exercice: LoggedExercise): String? {
        val cible = cible(exercice)
        val rirs = exercice.effectiveSets.mapNotNull { it.rir }
        if (rirs.isEmpty() || exercice.setsSontDerives) return null

        val moyen = rirs.average()
        return when {
            moyen < cible.min ->
                "RIR moyen ${Formatting.oneDecimal(moyen)} — plus près de l'échec que la cible " +
                    "(${cible.label}). Utile ponctuellement, coûteux si c'est systématique."
            moyen > cible.max ->
                "RIR moyen ${Formatting.oneDecimal(moyen)} — au-dessus de la cible (${cible.label}). " +
                    "La charge peut monter."
            else -> null
        }
    }
}
