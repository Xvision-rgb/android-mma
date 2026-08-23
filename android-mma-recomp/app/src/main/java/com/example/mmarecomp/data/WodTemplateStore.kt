package com.example.mmarecomp.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class WodTemplate(val name: String, val wodContent: String, val roundsSets: String? = null)

/**
 * Bibliothèque de WOD réutilisables pour les séances MMA récurrentes,
 * stockée en local (SharedPreferences, JSON) — volontairement pas dans
 * Supabase : c'est un raccourci de saisie personnel sur cet appareil, pas
 * une donnée de suivi à synchroniser.
 */
class WodTemplateStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): List<WodTemplate> {
        val raw = prefs.getString(KEY_TEMPLATES, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<WodTemplate>>(raw) }.getOrDefault(emptyList())
    }

    fun save(templates: List<WodTemplate>) {
        prefs.edit().putString(KEY_TEMPLATES, json.encodeToString(templates)).apply()
    }

    companion object {
        private const val PREFS_NAME = "wod_templates"
        private const val KEY_TEMPLATES = "templates"
    }
}
