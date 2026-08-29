package com.example.mmarecomp.util

import com.example.mmarecomp.model.LoggedExercise
import com.example.mmarecomp.model.MuscleZone
import java.text.Normalizer

/** Classe un exercice par zone à partir de son nom.
 *
 *  Les noms d'exercices sont des chaînes libres saisies par l'utilisateur :
 *  la classification se fait donc par mots-clés, avec accents et casse
 *  normalisés. Elle est faillible par construction — d'où [override], qui
 *  laisse le dernier mot à l'utilisateur et persiste son choix. */
object MuscleZoneClassifier {

    private val overrides = mutableMapOf<String, MuscleZone>()

    /** Ordre significatif : le premier motif qui matche gagne. Les motifs les
     *  plus spécifiques doivent précéder les plus génériques (« soulevé de
     *  terre roumain » avant « soulevé de terre »). */
    private val motifs: List<Pair<List<String>, MuscleZone>> = listOf(
        listOf(
            "cou", "neck", "pont", "bridge", "dead hang", "suspension", "hang",
            "farmer", "poigne", "grip", "avant-bras", "pince",
        ) to MuscleZone.COU_POIGNE,

        listOf(
            "rowing", "row", "traction", "pull-up", "pull up", "tirage", "chin",
            "tractions", "lat", "dorsaux", "face pull", "shrug", "haussement",
            "trapeze", "pendlay",
        ) to MuscleZone.TIRAGE,

        listOf(
            "souleve de terre", "deadlift", "romanian", "roumain", "rdl",
            "hip thrust", "good morning", "ischio", "fessier", "glute",
            "leg curl", "hyperextension", "lombaire",
        ) to MuscleZone.CHAINE_POSTERIEURE,

        listOf(
            "developpe", "bench", "dips", "pompe", "push-up", "push up",
            "militaire", "overhead press", "ohp", "pec", "pectoraux", "triceps",
        ) to MuscleZone.POUSSEE,

        listOf(
            "squat", "zercher", "front squat", "presse", "leg press", "fente",
            "lunge", "leg extension", "quadriceps", "mollet", "calf",
            "curl", "biceps",
        ) to MuscleZone.QUADS_BRAS,
    )

    /** Exercices sollicitant plusieurs articulations — cibles RIR plus
     *  conservatrices (cf. RirTargets). */
    private val polyarticulaires = listOf(
        "squat", "zercher", "souleve de terre", "deadlift", "developpe", "bench",
        "rowing", "row", "traction", "pull-up", "pull up", "tirage", "dips",
        "presse", "leg press", "fente", "lunge", "militaire", "overhead press",
        "ohp", "hip thrust", "pendlay", "good morning", "pompe", "push-up",
    )

    fun classifier(nom: String): MuscleZone {
        val clef = normaliser(nom)
        if (clef.isBlank()) return MuscleZone.QUADS_BRAS
        overrides[clef]?.let { return it }
        motifs.forEach { (mots, zone) ->
            if (mots.any { correspond(clef, it) }) return zone
        }
        // Défaut volontairement neutre : mieux vaut une zone secondaire
        // sous-estimée qu'un tirage fantôme qui fausserait le ratio.
        return MuscleZone.QUADS_BRAS
    }

    fun estPolyarticulaire(nom: String): Boolean {
        val clef = normaliser(nom)
        return polyarticulaires.any { correspond(clef, it) }
    }

    /** Correspondance motif / nom d'exercice.
     *
     *  Un simple `contains` classait « développé couché » en zone cou, parce
     *  que « couché » contient « cou ». On compare donc mot à mot, avec la
     *  seule tolérance du pluriel (« tractions » doit matcher « traction »).
     *  Les motifs de plusieurs mots restent cherchés comme une expression. */
    private fun correspond(clef: String, motif: String): Boolean {
        val m = normaliser(motif)
        if (m.isBlank()) return false
        if (m.contains(' ')) return clef.contains(m)
        return clef.split(' ').any { mot -> mot == m || mot == m + "s" }
    }

    /** Force la zone d'un nom d'exercice. Persistant pour la session ;
     *  à brancher sur SharedPreferences si le besoin se confirme. */
    fun override(nom: String, zone: MuscleZone) {
        overrides[normaliser(nom)] = zone
    }

    fun effacerOverrides() = overrides.clear()

    /** Volume par zone sur une liste d'exercices. */
    fun volumeParZone(exercices: List<LoggedExercise>): Map<MuscleZone, Double> =
        exercices
            .groupBy { classifier(it.nom) }
            .mapValues { (_, liste) -> liste.sumOf { it.volumeTotal } }

    /** Part relative de chaque zone dans le volume total. Vide si aucun
     *  volume n'a été loggué — surtout pas une répartition inventée. */
    fun repartition(exercices: List<LoggedExercise>): Map<MuscleZone, Double> {
        val parZone = volumeParZone(exercices)
        val total = parZone.values.sum()
        if (total <= 0.0) return emptyMap()
        return parZone.mapValues { (_, v) -> v / total }
    }

    /** Ratio tirage:poussée réel. Null si aucune poussée loguée — un ratio
     *  « infini » n'informe sur rien. */
    fun ratioTiragePoussee(exercices: List<LoggedExercise>): Double? {
        val parZone = volumeParZone(exercices)
        val tirage = parZone[MuscleZone.TIRAGE] ?: 0.0
        val poussee = parZone[MuscleZone.POUSSEE] ?: 0.0
        if (poussee <= 0.0) return null
        return tirage / poussee
    }

    /** Minuscules sans accents ni ponctuation : « Développé couché » et
     *  « developpe couche » doivent tomber sur la même clef. */
    private fun normaliser(texte: String): String =
        Normalizer.normalize(texte.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace(Regex("[^a-z0-9 -]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
}
