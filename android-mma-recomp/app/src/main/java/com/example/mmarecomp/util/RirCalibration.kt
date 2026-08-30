package com.example.mmarecomp.util

import android.content.Context
import java.time.LocalDate

/** Calibration du biais d'estimation du RIR.
 *
 *  L'estimation des reps in reserve est nettement moins précise chez les
 *  pratiquants non experts, avec une tendance systématique à SURESTIMER la
 *  distance à l'échec : on croit qu'il reste 3 reps alors qu'il en reste 5.
 *  Un moteur d'autorégulation qui prend le RIR déclaré au pied de la lettre
 *  fait donc monter la charge trop vite.
 *
 *  Le test : une série d'isolation menée à l'échec réel, avec le RIR estimé
 *  saisi AVANT. Le biais est l'écart moyen glissant entre estimation et
 *  réalité. Positif = l'athlète surestime sa marge.
 *
 *  Le test est proposé toutes les deux semaines, jamais imposé, et cesse
 *  d'être proposé après deux refus consécutifs. */
class RirCalibration(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Biais moyen sur les six derniers tests, en reps. 0 tant qu'aucun test
     *  n'a été fait — neutre par défaut, jamais une correction inventée. */
    val biais: Double
        get() {
            val mesures = chargerMesures()
            return if (mesures.isEmpty()) 0.0 else mesures.average()
        }

    val nbMesures: Int get() = chargerMesures().size

    val aEteCalibre: Boolean get() = nbMesures > 0

    /** Enregistre un test : RIR estimé avant la série, reps réellement faites
     *  au-delà du point où l'athlète pensait s'arrêter. */
    fun enregistrerTest(rirEstime: Int, repsReellesEnPlus: Int) {
        val ecart = (rirEstime - repsReellesEnPlus).toDouble()
        val mesures = (chargerMesures() + ecart).takeLast(FENETRE)
        prefs.edit()
            .putString(KEY_MESURES, mesures.joinToString(","))
            .putString(KEY_DERNIER_TEST, LocalDate.now().toString())
            .putInt(KEY_REFUS, 0)
            .apply()
    }

    fun refuser() {
        prefs.edit()
            .putInt(KEY_REFUS, prefs.getInt(KEY_REFUS, 0) + 1)
            .putString(KEY_DERNIERE_PROPOSITION, LocalDate.now().toString())
            .apply()
    }

    /** True s'il est temps de reproposer un test de calibration. */
    fun doitProposerTest(aujourdhui: LocalDate = LocalDate.now()): Boolean {
        if (prefs.getInt(KEY_REFUS, 0) >= MAX_REFUS) return false
        val dernier = prefs.getString(KEY_DERNIER_TEST, null)
            ?: prefs.getString(KEY_DERNIERE_PROPOSITION, null)
            ?: return true
        val date = runCatching { LocalDate.parse(dernier) }.getOrNull() ?: return true
        return date.plusDays(INTERVALLE_JOURS) <= aujourdhui
    }

    private fun chargerMesures(): List<Double> =
        prefs.getString(KEY_MESURES, null)
            ?.split(",")
            ?.mapNotNull { it.toDoubleOrNull() }
            ?: emptyList()

    private companion object {
        const val PREFS = "rir_calibration"
        const val KEY_MESURES = "mesures"
        const val KEY_DERNIER_TEST = "dernier_test"
        const val KEY_DERNIERE_PROPOSITION = "derniere_proposition"
        const val KEY_REFUS = "refus_consecutifs"
        const val INTERVALLE_JOURS = 14L
        const val MAX_REFUS = 2
        const val FENETRE = 6
    }
}
