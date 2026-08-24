package com.example.mmarecomp.data

import android.content.Context
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Aliments personnels ajoutés par l'utilisateur (un plat maison que la base
 * intégrée [FoodDatabase] ne couvre pas) — même approche que
 * [MealPresetStore] : stockage local uniquement, propre à cet appareil.
 */
class CustomFoodStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): List<FoodItem> {
        val raw = prefs.getString(KEY_FOODS, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<FoodItem>>(raw) }.getOrDefault(emptyList())
    }

    fun save(foods: List<FoodItem>) {
        prefs.edit().putString(KEY_FOODS, json.encodeToString(foods)).apply()
    }

    companion object {
        private const val PREFS_NAME = "custom_foods"
        private const val KEY_FOODS = "foods"
    }
}
