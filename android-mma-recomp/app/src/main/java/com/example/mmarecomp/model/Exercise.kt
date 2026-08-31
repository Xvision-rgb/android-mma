package com.example.mmarecomp.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Mode de saisie d'un mouvement : force (séries/reps/kg) ou cardio (durée/distance). */
@Serializable
enum class ExerciseModality {
    @SerialName("strength") Strength,
    @SerialName("cardio") Cardio,
}

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
 *  quand `sets` est vide.
 *
 *  [modality] = Cardio bascule vers durée/distance : le volume force vaut alors 0
 *  et les outils APRE/RIR ne s'appliquent pas. */
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
    val modality: ExerciseModality = ExerciseModality.Strength,
    /** Durée du bloc cardio, en minutes. */
    @SerialName("duree_min") val dureeMin: Int? = null,
    /** Distance parcourue (km), optionnelle. */
    @SerialName("distance_km") val distanceKm: Double? = null,
    /** Intensité ressentie du bloc cardio (1–10). Défaut métier : 5. */
    val intensite: Int? = null,
) {
    val isCardio: Boolean get() = modality == ExerciseModality.Cardio

    val effectiveSets: List<LoggedSet>
        get() {
            if (isCardio) return emptyList()
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

    val setsSontDerives: Boolean get() = !isCardio && sets.isEmpty()

    /** Volume force uniquement — 0 pour un bloc cardio. */
    val volumeTotal: Double get() = if (isCardio) 0.0 else effectiveSets.sumOf { it.volume }

    val chargeMaxKg: Double? get() = if (isCardio) null else effectiveSets.maxOfOrNull { it.chargeKg }

    /** Allure min/km si distance et durée connues. */
    val allureMinParKm: Double? get() {
        val km = distanceKm ?: return null
        val min = dureeMin ?: return null
        if (km <= 0.0 || min <= 0) return null
        return min / km
    }
}

fun LoggedExercise.withSets(nouvelles: List<LoggedSet>): LoggedExercise {
    if (isCardio) return this
    val renumerotees = nouvelles.mapIndexed { i, s -> s.copy(index = i + 1) }
    return copy(
        sets = renumerotees,
        series = renumerotees.size.coerceAtLeast(1),
        reps = renumerotees.firstOrNull()?.reps ?: reps,
        repsReelles = renumerotees.maxOfOrNull { it.reps } ?: repsReelles,
        chargeReelleKg = renumerotees.maxOfOrNull { it.chargeKg } ?: chargeReelleKg,
    )
}

/** Passe en mode cardio en nettoyant les séries force. */
fun LoggedExercise.asCardio(
    dureeMin: Int? = this.dureeMin ?: 30,
    distanceKm: Double? = this.distanceKm,
    intensite: Int? = this.intensite ?: 5,
): LoggedExercise = copy(
    modality = ExerciseModality.Cardio,
    sets = emptyList(),
    series = 0,
    reps = 0,
    chargeCibleKg = null,
    chargeReelleKg = null,
    repsReelles = null,
    propre = false,
    dureeMin = dureeMin,
    distanceKm = distanceKm,
    intensite = intensite?.coerceIn(1, 10),
)

/** Passe en mode force avec 3 séries vides si besoin. */
fun LoggedExercise.asStrength(): LoggedExercise {
    if (!isCardio && sets.isNotEmpty()) return copy(modality = ExerciseModality.Strength)
    val baseSets = (1..3).map { i ->
        LoggedSet(index = i, reps = 10, chargeKg = chargeCibleKg ?: 0.0, estAmrap = i == 3)
    }
    return copy(
        modality = ExerciseModality.Strength,
        dureeMin = null,
        distanceKm = null,
        intensite = null,
        series = 3,
        reps = 10,
        sets = baseSets,
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
