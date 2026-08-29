package com.example.mmarecomp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Exercice tel que programmé dans le split hebdo (training_plan.exercices). */
@Serializable
data class PlannedExercise(
    val nom: String,
    val series: Int,
    val reps: Int,
    @SerialName("charge_cible_kg") val chargeCibleKg: Double? = null,
)

/** Une série réelle, loguée individuellement.
 *
 *  L'autorégulation de charge (cf. [com.example.mmarecomp.util.ApreEngine]) lit
 *  la série AMRAP précisément, et le seuil d'arrêt de série
 *  ([com.example.mmarecomp.util.SetStopAdvisor]) compare les séries entre
 *  elles : les deux ont besoin de la donnée série par série, que les champs
 *  agrégés de [LoggedExercise] ne portent pas. */
@Serializable
data class LoggedSet(
    /** 1-based, tel qu'affiché à l'utilisateur. */
    val index: Int,
    val reps: Int,
    @SerialName("charge_kg") val chargeKg: Double,
    /** Reps in reserve estimées. Null si l'utilisateur ne l'a pas saisi. */
    val rir: Int? = null,
    /** Sangles utilisées — découple la poigne du muscle cible sur le tirage. */
    val sangles: Boolean = false,
    /** La série s'est arrêtée sur la poigne, pas sur le muscle cible.
     *  Empêche l'autorégulation de baisser la charge à tort : l'échec n'est
     *  pas celui du dos. */
    @SerialName("limite_poigne") val limitePoigne: Boolean = false,
    /** Série menée jusqu'à la limite — c'est elle que lit l'autorégulation. */
    @SerialName("est_amrap") val estAmrap: Boolean = false,
) {
    val volume: Double get() = reps * chargeKg
}

/** Exercice tel que loggué après une séance réelle (workouts.exercices).
 *
 *  [sets] est la source de vérité depuis le passage au logging par série.
 *  Les champs agrégés ([series], [reps], [chargeReelleKg], [repsReelles]) sont
 *  conservés : la colonne Postgres `exercices` est du JSONB et contient tout
 *  l'historique antérieur, qui n'a pas de `sets`. Ne jamais lire ces champs
 *  directement pour un calcul — passer par [effectiveSets], qui les dérive
 *  quand `sets` est vide. */
@Serializable
data class LoggedExercise(
    val nom: String,
    val series: Int,
    val reps: Int,
    @SerialName("charge_cible_kg") val chargeCibleKg: Double? = null,
    @SerialName("charge_reelle_kg") val chargeReelleKg: Double? = null,
    @SerialName("reps_reelles") val repsReelles: Int? = null,
    /** Toutes les reps faites proprement. */
    val propre: Boolean = false,
    val sets: List<LoggedSet> = emptyList(),
) {
    /** Séries réelles de cet exercice, quelle que soit l'époque du log.
     *
     *  Si [sets] est renseigné, c'est lui qui fait foi. Sinon on reconstruit
     *  [series] séries identiques à partir des champs agrégés — une
     *  approximation assumée de l'historique pré-Lot 0, marquée comme telle
     *  par [setsSontDerives]. La dernière série reconstruite est marquée AMRAP
     *  faute de mieux : c'est la convention la plus proche de la réalité d'une
     *  séance où seule la charge finale était loguée. */
    val effectiveSets: List<LoggedSet>
        get() {
            if (sets.isNotEmpty()) return sets
            if (series < 1) return emptyList()
            val charge = chargeReelleKg ?: chargeCibleKg ?: return emptyList()
            val repsParSerie = repsReelles ?: reps
            return (1..series).map { i ->
                LoggedSet(
                    index = i,
                    reps = repsParSerie,
                    chargeKg = charge,
                    estAmrap = i == series,
                )
            }
        }

    /** True quand les séries affichées viennent de la dérivation historique et
     *  non d'une saisie réelle — l'UI doit alors éviter de présenter le RIR ou
     *  le flag poigne comme des données mesurées. */
    val setsSontDerives: Boolean get() = sets.isEmpty()

    /** Volume de l'exercice (Σ reps × charge sur les séries réelles). */
    val volumeTotal: Double get() = effectiveSets.sumOf { it.volume }

    /** Charge de travail la plus lourde effectivement soulevée. */
    val chargeMaxKg: Double? get() = effectiveSets.maxOfOrNull { it.chargeKg }
}

/** Remplace les séries et resynchronise les champs agrégés.
 *
 *  Les agrégats ne sont plus la source de vérité, mais ils restent lus par le
 *  code antérieur au logging par série (et par toute requête SQL existante) :
 *  les laisser dériver produirait des volumes faux. Toujours passer par ici
 *  plutôt que par `copy(sets = ...)`. */
fun LoggedExercise.withSets(nouvelles: List<LoggedSet>): LoggedExercise {
    val renumerotees = nouvelles.mapIndexed { i, s -> s.copy(index = i + 1) }
    return copy(
        sets = renumerotees,
        series = renumerotees.size.coerceAtLeast(1),
        reps = renumerotees.firstOrNull()?.reps ?: reps,
        repsReelles = renumerotees.maxOfOrNull { it.reps } ?: repsReelles,
        chargeReelleKg = renumerotees.maxOfOrNull { it.chargeKg } ?: chargeReelleKg,
    )
}

fun PlannedExercise.toLogged() = LoggedExercise(
    nom = nom,
    series = series,
    reps = reps,
    chargeCibleKg = chargeCibleKg,
    sets = (1..series).map { i ->
        LoggedSet(
            index = i,
            reps = reps,
            chargeKg = chargeCibleKg ?: 0.0,
            estAmrap = i == series,
        )
    },
)
