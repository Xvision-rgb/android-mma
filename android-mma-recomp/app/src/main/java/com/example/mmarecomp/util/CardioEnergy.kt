package com.example.mmarecomp.util

import com.example.mmarecomp.model.ExerciseModality
import com.example.mmarecomp.model.LoggedExercise
import com.example.mmarecomp.model.asCardio
import kotlin.math.roundToInt

/** Preset cardio proposé en un tap depuis le log de séance. */
data class CardioPreset(
    val nom: String,
    val met: Double,
    val dureeMinDefaut: Int = 30,
    val distanceKmDefaut: Double? = null,
)

/**
 * Estimation énergétique et détection des blocs cardio.
 *
 * MET × poids (kg) × heures ≈ kcal. Valeurs Compendium approximatives,
 * volontairement conservatrices : l'objectif est de faire bouger l'EA et
 * la charge dans le bon sens, pas de remplacer une ceinture cardio.
 */
object CardioEnergy {

    val PRESETS: List<CardioPreset> = listOf(
        CardioPreset("Marche rapide", met = 4.3, dureeMinDefaut = 30),
        CardioPreset("Marche inclinée", met = 6.0, dureeMinDefaut = 25),
        CardioPreset("Course à pied", met = 9.0, dureeMinDefaut = 30, distanceKmDefaut = 5.0),
        CardioPreset("Vélo", met = 7.0, dureeMinDefaut = 40),
        CardioPreset("Elliptique", met = 6.5, dureeMinDefaut = 30),
        CardioPreset("Rameur", met = 7.0, dureeMinDefaut = 20),
    )

    private val MOTS_CLEFS_CARDIO = listOf(
        "marche", "walking", "walk",
        "course", "running", "run", "footing", "jogging",
        "velo", "vélo", "bike", "cycling", "cyclisme",
        "elliptique", "elliptical",
        "rameur", "rowing",
        "natation", "swim", "nage",
        "corde a sauter", "corde à sauter", "jump rope",
        "stair", "stepper", "assault bike", "air bike",
        "ski ergs", "skierg",
    )

    fun detectModality(nom: String): ExerciseModality =
        if (looksLikeCardio(nom)) ExerciseModality.Cardio else ExerciseModality.Strength

    fun looksLikeCardio(nom: String): Boolean {
        val cle = stripAccents(ExerciseName.cle(nom))
        if (cle.isBlank()) return false
        return MOTS_CLEFS_CARDIO
            .map { stripAccents(it.lowercase()) }
            .any { mot -> containsMot(cle, mot) }
    }

    /** Évite les faux positifs (« velo » dans « developpe »). */
    private fun containsMot(cle: String, mot: String): Boolean {
        if (mot.isBlank()) return false
        if (mot.contains(' ')) return cle.contains(mot)
        val regex = Regex("(^|[^a-z0-9])${Regex.escape(mot)}([^a-z0-9]|$)")
        return regex.containsMatchIn(cle)
    }

    private fun stripAccents(value: String): String {
        val normalized = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
        return normalized.replace("\\p{Mn}+".toRegex(), "")
    }

    fun metForName(nom: String): Double {
        val cle = ExerciseName.cle(nom)
        val preset = PRESETS.firstOrNull { ExerciseName.cle(it.nom) == cle }
            ?: PRESETS.firstOrNull { cle.contains(ExerciseName.cle(it.nom)) }
        if (preset != null) return preset.met
        return when {
            cle.contains("marche") && (cle.contains("inclin") || cle.contains("pente")) -> 6.0
            cle.contains("marche") -> 4.3
            cle.contains("course") || cle.contains("run") || cle.contains("footing") -> 9.0
            cle.contains("velo") || cle.contains("bike") || cle.contains("cycl") -> 7.0
            cle.contains("ellipt") -> 6.5
            cle.contains("rame") || cle.contains("row") -> 7.0
            cle.contains("nata") || cle.contains("swim") || cle.contains("nage") -> 8.0
            else -> 6.0
        }
    }

    /** kcal ≈ MET × kg × heures. Sans poids : estimation via intensité × durée. */
    fun kcalForExercise(exercice: LoggedExercise, poidsKg: Double?): Int {
        if (!exercice.isCardio) return 0
        val minutes = exercice.dureeMin?.takeIf { it > 0 } ?: return 0
        val heures = minutes / 60.0
        if (poidsKg != null && poidsKg > 0) {
            return (metForName(exercice.nom) * poidsKg * heures).roundToInt().coerceAtLeast(0)
        }
        val intensite = (exercice.intensite ?: 5).coerceIn(1, 10)
        // ~1,0 kcal / (intensité × minute) sans masse — ordre de grandeur prudent.
        return (intensite * minutes * 1.0).roundToInt().coerceAtLeast(0)
    }

    fun kcalForExercises(exercices: List<LoggedExercise>, poidsKg: Double?): Int =
        exercices.sumOf { kcalForExercise(it, poidsKg) }

    /** Charge interne approximative d'un bloc cardio (intensité × minutes). */
    fun chargeInterne(exercice: LoggedExercise): Double {
        if (!exercice.isCardio) return 0.0
        val minutes = exercice.dureeMin?.takeIf { it > 0 } ?: return 0.0
        val intensite = (exercice.intensite ?: 5).coerceIn(1, 10)
        return intensite.toDouble() * minutes
    }

    fun chargeInterneTotale(exercices: List<LoggedExercise>): Double =
        exercices.sumOf { chargeInterne(it) }

    fun fromPreset(preset: CardioPreset): LoggedExercise =
        LoggedExercise(
            nom = preset.nom,
            series = 0,
            reps = 0,
            modality = ExerciseModality.Cardio,
            dureeMin = preset.dureeMinDefaut,
            distanceKm = preset.distanceKmDefaut,
            intensite = 5,
        ).asCardio(
            dureeMin = preset.dureeMinDefaut,
            distanceKm = preset.distanceKmDefaut,
            intensite = 5,
        )

    /** Si le nom ressemble au cardio et que l'exo est encore en Strength vide,
     *  bascule automatiquement — sinon respecte le choix utilisateur. */
    fun maybeAutodetect(exercice: LoggedExercise): LoggedExercise {
        if (exercice.isCardio) return exercice
        if (!looksLikeCardio(exercice.nom)) return exercice
        val hasForceData = exercice.sets.any { it.chargeKg > 0 || it.reps > 0 } ||
            (exercice.chargeReelleKg != null && exercice.chargeReelleKg > 0)
        if (hasForceData) return exercice
        return exercice.asCardio()
    }
}
