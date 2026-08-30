package com.example.mmarecomp.util

import com.example.mmarecomp.model.DailyCheckIn
import com.example.mmarecomp.model.MmaSession
import com.example.mmarecomp.model.Workout
import java.time.LocalDate
import kotlin.math.sqrt

/** Décision de modulation de la séance du jour. */
enum class ReadinessAction(val label: String) {
    NOMINALE("Séance nominale"),
    VOLUME_REDUIT("Volume allégé"),
    ALLEGEE("Séance allégée"),
    DELOAD("Semaine de deload"),
}

/** Modulation prescrite — agit sur le VOLUME en priorité, la charge en
 *  dernier recours. On ne propose jamais un repos complet : la dose minimale
 *  maintient l'adaptation, le repos total la perd. */
data class ModulationSeance(
    val action: ReadinessAction,
    val facteurVolume: Double,
    val facteurCharge: Double,
    val rirSupplementaire: Int,
    val explication: String,
)

/** Charge interne et ratio charge aiguë / charge chronique.
 *
 *  La charge de séance (session-RPE) est le produit du RPE global par la
 *  durée. L'ACWR compare la charge des 7 derniers jours à la moyenne des 28 :
 *  la zone 0,8–1,3 correspond au risque de blessure minimal, au-delà de 1,5 le
 *  risque augmente nettement. */
object TrainingLoad {

    const val ACWR_MIN = 0.8
    const val ACWR_MAX = 1.3
    const val ACWR_ALERTE = 1.5

    /** Durée par défaut d'une séance MMA quand elle n'est pas chronométrée —
     *  ignorer ces séances sous-estimerait la charge réelle de moitié. */
    private const val DUREE_MMA_DEFAUT_MIN = 90

    fun chargeSeance(workout: Workout): Double {
        val rpe = workout.rpe ?: return 0.0
        val duree = workout.dureeMin ?: return 0.0
        return rpe.toDouble() * duree
    }

    fun chargeSeance(session: MmaSession): Double {
        val ressenti = session.ressenti ?: return 0.0
        return ressenti.toDouble() * DUREE_MMA_DEFAUT_MIN
    }

    /** Charges quotidiennes cumulées, musculation et MMA confondues. */
    fun chargesParJour(
        workouts: List<Workout>,
        mmaSessions: List<MmaSession> = emptyList(),
    ): Map<String, Double> {
        val map = mutableMapOf<String, Double>()
        workouts.forEach { map[it.date] = (map[it.date] ?: 0.0) + chargeSeance(it) }
        mmaSessions.forEach { map[it.date] = (map[it.date] ?: 0.0) + chargeSeance(it) }
        return map
    }

    /** ACWR = charge moyenne sur 7 jours / charge moyenne sur 28 jours.
     *
     *  Les jours sans séance comptent comme zéro et non comme absents : c'est
     *  ce qui rend le ratio robuste aux trous et lui donne son sens de
     *  « montée en charge ». Null tant que la fenêtre chronique est vide. */
    fun acwr(
        chargesParJour: Map<String, Double>,
        aujourdhui: LocalDate = LocalDate.now(),
    ): Double? {
        fun moyenneSur(jours: Long): Double {
            val debut = aujourdhui.minusDays(jours - 1)
            val total = (0 until jours).sumOf { offset ->
                chargesParJour[DateUtils.string(debut.plusDays(offset))] ?: 0.0
            }
            return total / jours
        }
        val chronique = moyenneSur(28)
        if (chronique <= 0.0) return null
        return moyenneSur(7) / chronique
    }

    /** Écart de la HRV du jour à la moyenne mobile 7 jours, en écarts-types.
     *
     *  Une valeur basse isolée ne veut rien dire — c'est l'écart à la
     *  tendance qui informe, cohérent avec la règle du poids en moyenne
     *  mobile. Null si l'historique est trop court pour avoir un sens. */
    fun ecartHrvEnSigma(checkIns: List<DailyCheckIn>): Double? {
        // Tri explicite : l'appelant peut avoir ajouté le check-in du jour en
        // fin de liste sans retrier, et « la dernière valeur » doit rester la
        // plus récente pour que la comparaison ait un sens.
        val valeurs = checkIns.sortedBy { it.date }.mapNotNull { it.hrvRmssd }
        if (valeurs.size < 5) return null
        val reference = valeurs.takeLast(8).dropLast(1)
        if (reference.size < 4) return null
        val moyenne = reference.average()
        val variance = reference.sumOf { (it - moyenne) * (it - moyenne) } / reference.size
        val ecartType = sqrt(variance)
        if (ecartType <= 0.0) return null
        return (valeurs.last() - moyenne) / ecartType
    }

    /** Nombre de jours consécutifs terminant aujourd'hui où le score est
     *  franchement bas — déclencheur du deload réactif. */
    fun joursConsecutifsEnRouge(
        checkIns: List<DailyCheckIn>,
        aujourdhui: LocalDate = LocalDate.now(),
    ): Int {
        val parDate = checkIns.associateBy { it.date }
        var cursor = aujourdhui
        var jours = 0
        while (true) {
            val checkIn = parDate[DateUtils.string(cursor)] ?: break
            if (checkIn.score >= 15) break
            jours++
            cursor = cursor.minusDays(1)
        }
        return jours
    }

    /** Modulation de la séance du jour.
     *
     *  Le volume encaisse l'ajustement avant la charge : baisser la charge
     *  fait reculer l'adaptation de force, alors que couper une série de
     *  finition ne coûte presque rien. */
    fun moduler(
        score: Int?,
        acwr: Double?,
        ecartHrvSigma: Double? = null,
        joursConsecutifsEnRouge: Int = 0,
    ): ModulationSeance {
        if (joursConsecutifsEnRouge >= 3) {
            return ModulationSeance(
                action = ReadinessAction.DELOAD,
                facteurVolume = 0.5,
                facteurCharge = 1.0,
                rirSupplementaire = 0,
                explication = "Trois jours de suite en dessous du seuil — " +
                    "semaine à ~50 % du volume, charges maintenues. Les charges " +
                    "reviennent plus vite que la fatigue ne part.",
            )
        }

        val acwrEleve = acwr != null && acwr > ACWR_ALERTE
        val scoreBas = score != null && score < 15
        if (scoreBas || acwrEleve) {
            val cause = if (acwrEleve) {
                // La valeur chiffrée de l'ACWR n'est pas répétée ici : la carte
                // l'affiche déjà en pied, avec la zone qui lui donne son sens
                // ("dans ta zone habituelle", "montée de charge marquée"…).
                "Charge des 7 derniers jours nettement au-dessus de ta moyenne"
            } else {
                "Score de forme bas ($score/25)"
            }
            return ModulationSeance(
                action = ReadinessAction.ALLEGEE,
                facteurVolume = 0.6,
                facteurCharge = 0.9,
                rirSupplementaire = 2,
                explication = "$cause — volume −40 %, charges −10 %, une rep de marge en plus. " +
                    "On garde la séance : la dose minimale entretient l'adaptation.",
            )
        }

        val scoreMoyen = score != null && score < 20
        val hrvBasse = ecartHrvSigma != null && ecartHrvSigma < -0.5
        if (scoreMoyen || hrvBasse) {
            val cause = if (hrvBasse) "HRV sous ta tendance 7 jours" else "Score de forme moyen ($score/25)"
            return ModulationSeance(
                action = ReadinessAction.VOLUME_REDUIT,
                facteurVolume = 0.78,
                facteurCharge = 1.0,
                rirSupplementaire = 0,
                explication = "$cause — coupe la dernière série des accessoires. Charges inchangées.",
            )
        }

        return ModulationSeance(
            action = ReadinessAction.NOMINALE,
            facteurVolume = 1.0,
            facteurCharge = 1.0,
            rirSupplementaire = 0,
            explication = "Rien à signaler — séance comme prévu.",
        )
    }
}
