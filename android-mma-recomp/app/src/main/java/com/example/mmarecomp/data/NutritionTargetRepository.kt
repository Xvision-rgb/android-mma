package com.example.mmarecomp.data

import com.example.mmarecomp.model.NewNutritionTarget
import com.example.mmarecomp.model.NutritionTarget
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.eq

class NutritionTargetRepository {
    private val client = SupabaseProvider.client

    suspend fun fetch(forDate: String): NutritionTarget? =
        client.postgrest.from("nutrition_targets")
            .select { filter { eq("date", forDate) } }
            .decodeList<NutritionTarget>()
            .firstOrNull()

    suspend fun set(target: NewNutritionTarget): NutritionTarget =
        client.postgrest.from("nutrition_targets")
            .upsert(target, onConflict = "user_id,date") { select() }
            .decodeSingle()
}
