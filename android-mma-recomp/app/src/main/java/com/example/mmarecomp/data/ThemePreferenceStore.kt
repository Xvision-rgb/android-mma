package com.example.mmarecomp.data

import android.content.Context

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Préférence d'affichage clair/sombre/système, stockée en local — un
 *  réglage propre à cet appareil, pas une donnée de suivi à synchroniser. */
class ThemePreferenceStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): ThemeMode =
        runCatching { ThemeMode.valueOf(prefs.getString(KEY_MODE, null) ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)

    fun save(mode: ThemeMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
    }

    companion object {
        private const val PREFS_NAME = "theme_preference"
        private const val KEY_MODE = "mode"
    }
}
