package com.example.mmarecomp.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode(val label: String) {
    Systeme("Système"),
    Clair("Clair"),
    Sombre("Sombre"),
}

private const val PREFS_NAME = "theme_prefs"
private const val KEY_MODE = "mode"

/** Préférence de thème persistée localement (pas de compte requis) — la
 *  valeur par défaut suit le thème système, comme avant l'ajout du réglage. */
object ThemePreference {
    var mode by mutableStateOf(ThemeMode.Systeme)
        private set

    private var prefs: android.content.SharedPreferences? = null

    fun init(context: Context) {
        val stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = stored
        mode = stored.getString(KEY_MODE, ThemeMode.Systeme.name)
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.Systeme
    }

    fun updateMode(newMode: ThemeMode) {
        mode = newMode
        prefs?.edit()?.putString(KEY_MODE, newMode.name)?.apply()
    }
}
