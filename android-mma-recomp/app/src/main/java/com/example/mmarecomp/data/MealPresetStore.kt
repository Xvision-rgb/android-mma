package com.example.mmarecomp.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class MealPreset(
    val name: String,
    val calories: Int,
    val proteinesG: Double,
    val glucidesG: Double,
    val lipidesG: Double,
    val description: String? = null,
)

/**
 * Repas favoris réutilisables (petit-déj type, shake post-training…), même
 * approche que [WodTemplateStore] : stockage local uniquement, c'est un
 * raccourci de saisie personnel, pas une donnée de suivi à synchroniser.
 */
class MealPresetStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): List<MealPreset> {
        val raw = prefs.getString(KEY_PRESETS, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<MealPreset>>(raw) }.getOrDefault(emptyList())
    }

    fun save(presets: List<MealPreset>) {
        prefs.edit().putString(KEY_PRESETS, json.encodeToString(presets)).apply()
    }

    companion object {
        private const val PREFS_NAME = "meal_presets"
        private const val KEY_PRESETS = "presets"
    }
}
