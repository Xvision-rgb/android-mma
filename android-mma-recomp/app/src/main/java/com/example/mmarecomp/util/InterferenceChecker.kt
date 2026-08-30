package com.example.mmarecomp.util

import com.example.mmarecomp.model.MmaSession
import com.example.mmarecomp.model.MuscleZone
import com.example.mmarecomp.model.Workout
import com.example.mmarecomp.model.WorkoutType
import java.time.LocalDate

/** Détection des conflits de programmation entre musculation et MMA.
 *
 *  L'effet d'interférence existe mais se module surtout par l'ordre et
 *  l'espacement : séances accolées = interférence maximale, et la qualité
 *  travaillée en second est celle qui est pénalisée. Les conflits sont donc
 *  signalés, jamais bloqués — c'est un arbitrage, pas une faute. */
object InterferenceChecker {

    /** Au-delà de ce ressenti, une séance MMA compte comme intense. */
    private const val SEUIL_MMA_INTENSE = 7

    private val basDuCorpsLourd = setOf(
        WorkoutType.JambesForce,
        WorkoutType.JambesHypertrophie,
    )

    /** Conflits sur la journée [date], au format affichable. */
    fun conflits(
        date: LocalDate,
        workouts: List<Workout>,
        mmaSessions: List<MmaSession>,
    ): List<String> {
        val messages = mutableListOf<String>()
        val jour = DateUtils.string(date)
        val veille = DateUtils.string(date.minusDays(1))
        val lendemain = DateUtils.string(date.plusDays(1))

        val musculationDuJour = workouts.filter { it.date == jour && it.type !in cardioTypes }
        val basDuCorps = musculationDuJour.any { it.type in basDuCorpsLourd }

        val mmaIntense = { d: String ->
            mmaSessions.any {
                it.date == d && (TrainingLoad.intensiteMma(it.ressenti ?: return@any false) ?: 0) >= SEUIL_MMA_INTENSE
            }
        }

        if (basDuCorps && (mmaIntense(jour) || mmaIntense(veille) || mmaIntense(lendemain))) {
            messages += "Bas du corps lourd à moins de 24 h d'un sparring intense — " +
                "les deux qualités se marchent dessus. Décaler l'un des deux si possible."
        }

        if (musculationDuJour.isNotEmpty() && workouts.any { it.date == jour && it.type in cardioTypes }) {
            messages += "Musculation et cardio le même jour — viser au moins 6 h d'écart, " +
                "et faire la force en premier si les deux s'enchaînent."
        }

        return messages
    }

    /** Aligné sur [EnduranceInterference] : Course + HIIT. MmaWod n'est
     *  pas du cardio d'endurance — le compter ici faisait rater les
     *  sorties course et confondait un WOD combat avec une sortie. */
    private val cardioTypes = setOf(WorkoutType.Hiit, WorkoutType.Course)

    /** Signale un travail de poigne programmé avant du tirage : la poigne
     *  fatiguée redevient le facteur limitant du dos, exactement le problème
     *  que le découplage cherche à résoudre. */
    fun poigneAvantTirage(exercices: List<String>): Boolean {
        val zones = exercices.map { MuscleZoneClassifier.classifier(it) }
        val premierePoigne = zones.indexOf(MuscleZone.COU_POIGNE)
        val dernierTirage = zones.lastIndexOf(MuscleZone.TIRAGE)
        return premierePoigne != -1 && dernierTirage != -1 && premierePoigne < dernierTirage
    }
}
