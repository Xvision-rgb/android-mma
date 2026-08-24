package com.example.mmarecomp.data

import android.content.Context

/** Compteur d'hydratation simple, jour courant uniquement — pas d'historique,
 *  pas de cible imposée, juste un tally personnel qui se réinitialise chaque
 *  jour. Stockage local, rien à synchroniser. */
class HydrationStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun countForToday(today: String): Int {
        val storedDate = prefs.getString(KEY_DATE, null)
        return if (storedDate == today) prefs.getInt(KEY_COUNT, 0) else 0
    }

    fun setCountForToday(today: String, count: Int) {
        prefs.edit().putString(KEY_DATE, today).putInt(KEY_COUNT, count.coerceAtLeast(0)).apply()
    }

    companion object {
        private const val PREFS_NAME = "hydration"
        private const val KEY_DATE = "date"
        private const val KEY_COUNT = "count"
    }
}
