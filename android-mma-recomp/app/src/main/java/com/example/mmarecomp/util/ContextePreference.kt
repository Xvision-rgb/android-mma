package com.example.mmarecomp.util

import android.content.Context
import com.example.mmarecomp.model.ContexteSportif

/** Contexte sportif courant, stocké localement.
 *
 *  Volontairement en SharedPreferences et non en base : c'est un réglage
 *  d'appareil qui change quand la vie change (un club qui ferme, une reprise),
 *  pas une donnée d'historique. Le défaut est « salle uniquement » — c'est le
 *  cas le plus fréquent, et il vaut mieux sous-estimer la dépense que
 *  l'inverse quand l'objectif est de rester sec. */
class ContextePreference(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var contexte: ContexteSportif
        get() {
            val value = prefs.getString(KEY, null) ?: return ContexteSportif.SalleUniquement
            return ContexteSportif.entries.firstOrNull { it.value == value }
                ?: ContexteSportif.SalleUniquement
        }
        set(nouveau) {
            prefs.edit().putString(KEY, nouveau.value).apply()
        }

    private companion object {
        const val PREFS = "contexte_sportif"
        const val KEY = "contexte"
    }
}
