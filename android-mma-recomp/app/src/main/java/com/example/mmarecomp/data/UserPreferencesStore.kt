package com.example.mmarecomp.data

import android.content.Context
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Stockage local (SharedPreferences, JSON) de [UserPreferences] — même
 *  approche que [ThemePreferenceStore] et [WodTemplateStore], en un seul
 *  objet plutôt qu'une clé par champ pour rester simple à faire évoluer. */
class UserPreferencesStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): UserPreferences {
        val raw = prefs.getString(KEY_PREFS, null) ?: return UserPreferences()
        return runCatching { json.decodeFromString<UserPreferences>(raw) }.getOrDefault(UserPreferences())
    }

    fun save(preferences: UserPreferences) {
        prefs.edit().putString(KEY_PREFS, json.encodeToString(preferences)).apply()
    }

    companion object {
        private const val PREFS_NAME = "user_preferences"
        private const val KEY_PREFS = "preferences"
    }
}
