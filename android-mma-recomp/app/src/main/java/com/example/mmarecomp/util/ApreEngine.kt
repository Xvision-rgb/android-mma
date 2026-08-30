package com.example.mmarecomp.util

import com.example.mmarecomp.model.LoggedExercise
import com.example.mmarecomp.model.LoggedSet
import kotlin.math.abs
import kotlin.math.roundToInt

/** Protocole d'autorégulation progressive (APRE).
 *
 *  Le nombre de reps cible de la série AMRAP définit la qualité travaillée :
 *  moins de reps = plus proche de la force maximale. */
enum class ApreProtocol(val label: String, val repsCible: Int) {
    APRE_3("Force max", 3),
    APRE_6("Force base", 6),
    APRE_10("Hypertrophie", 10),
}

/** Prescription de charge pour la prochaine occurrence d'un exercice. */
data class ApreProchaineSeance(
    val chargeKg: Double,
    val deltaKg: Double,
    /** Phrase affichée telle quelle à l'utilisateur — factuelle, jamais un
     *  jugement sur la performance. */
    val justification: String,
)

/** Autorégulation de la charge à partir de la série AMRAP réellement réalisée.
 *
 *  Remplace l'heuristique « +2,5 kg si la séance était propre », qui ne
 *  regardait ni l'écart aux reps cibles ni la raison d'un échec. La charge
 *  monte quand la performance dépasse la cible et descend quand elle est en
 *  dessous, sans attendre la fin d'un cycle. */
object ApreEngine {

    /** Incrément de charge par défaut, en kg. Réglable : tous les gymnases
     *  n'ont pas de disques de 1,25 kg. */
    const val INCREMENT_DEFAUT = 2.5

    /** Série de référence pour l'autorégulation : la série explicitement
     *  marquée AMRAP, sinon la dernière série travaillée. */
    fun serieDeReference(exercice: LoggedExercise): LoggedSet? {
        val sets = exercice.effectiveSets
        return sets.lastOrNull { it.estAmrap } ?: sets.lastOrNull()
    }

    /** Prescription pour la prochaine séance, ou null si l'exercice n'a pas
     *  assez de données pour décider (aucune série, charge nulle). */
    fun prescrire(
        exercice: LoggedExercise,
        protocole: ApreProtocol,
        incrementKg: Double = INCREMENT_DEFAUT,
        /** Biais RIR personnel (cf. RirCalibration) : un athlète qui
         *  surestime sa marge travaille en réalité plus près de l'échec que
         *  déclaré, donc la charge ne doit pas monter aussi vite. */
        biaisRir: Double = 0.0,
    ): ApreProchaineSeance? {
        val serie = serieDeReference(exercice) ?: return null
        if (serie.chargeKg <= 0.0) return null

        // Une série coupée par la poigne n'a pas mesuré le muscle cible :
        // baisser la charge corrigerait le mauvais maillon et ferait reculer
        // le dos alors que c'est lui qu'on cherche à charger.
        if (serie.limitePoigne) {
            return ApreProchaineSeance(
                chargeKg = serie.chargeKg,
                deltaKg = 0.0,
                justification = "Série limitée par la poigne — charge maintenue, sangles recommandées.",
            )
        }

        val ecart = serie.reps - protocole.repsCible
        val pourcentage = pourcentageAjustement(ecart)

        // Le biais RIR ne s'applique qu'à la hausse : il corrige une
        // surestimation de la marge, qui ne peut que rendre une progression
        // trop optimiste. Il n'a aucune raison d'accélérer une baisse.
        val pourcentageCorrige = when {
            pourcentage > 0 && biaisRir > 0 -> pourcentage * facteurBiais(biaisRir)
            pourcentage > 0 && biaisRir < 0 ->
                pourcentage * (1.0 - biaisRir / 4.0).coerceIn(1.0, 1.25)
            else -> pourcentage
        }

        val brut = serie.chargeKg * (1 + pourcentageCorrige)
        val arrondie = arrondir(brut, incrementKg)
        val chargeFinale = if (ecart <= 0) {
            minOf(arrondie, serie.chargeKg)
        } else {
            arrondie
        }
        val delta = chargeFinale - serie.chargeKg

        return ApreProchaineSeance(
            chargeKg = chargeFinale,
            deltaKg = delta,
            justification = justifier(ecart, delta, protocole),
        )
    }

    /** Table d'ajustement APRE : l'écart aux reps cibles de la série AMRAP
     *  détermine la charge suivante. */
    internal fun pourcentageAjustement(ecart: Int): Double = when {
        ecart <= -4 -> -0.06
        ecart <= -1 -> -0.025
        ecart == 0 -> 0.0
        ecart <= 3 -> 0.025
        else -> 0.05
    }

    /** Amortit la progression proportionnellement au biais d'estimation :
     *  +2 RIR de biais réduit la hausse de moitié. Plafonné pour ne jamais
     *  inverser le signe. */
    private fun facteurBiais(biaisRir: Double): Double =
        (1.0 - (biaisRir.coerceAtLeast(0.0) / 4.0)).coerceIn(0.25, 1.0)

    /** Arrondit à l'incrément réellement chargeable — une prescription de
     *  63,7 kg n'est pas exécutable en salle. */
    internal fun arrondir(chargeKg: Double, incrementKg: Double): Double {
        if (incrementKg <= 0.0) return chargeKg
        return (chargeKg / incrementKg).roundToInt() * incrementKg
    }

    private fun justifier(ecart: Int, deltaKg: Double, protocole: ApreProtocol): String {
        val cible = protocole.repsCible
        val delta = Formatting.oneDecimal(abs(deltaKg))
        return when {
            deltaKg > 0 -> "+$ecart reps au-dessus de la cible ($cible) — charge +${delta}kg."
            deltaKg < 0 -> "${abs(ecart)} reps sous la cible ($cible) — charge −${delta}kg."
            ecart == 0 -> "Pile sur la cible ($cible reps) — charge inchangée."
            else -> "Charge inchangée (incrément disponible trop grossier pour l'ajustement)."
        }
    }
}
