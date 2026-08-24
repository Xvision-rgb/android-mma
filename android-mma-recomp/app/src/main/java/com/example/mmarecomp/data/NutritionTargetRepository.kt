package com.example.mmarecomp.data

import com.example.mmarecomp.model.NewNutritionTarget
import com.example.mmarecomp.model.NutritionTarget
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.eq
import io.github.jan.supabase.postgrest.query.filter.gte

class NutritionTargetRepository {
    private val client = SupabaseProvider.client

    suspend fun fetch(forDate: String): NutritionTarget? =
        client.postgrest.from("nutrition_targets")
            .select { filter { eq("date", forDate) } }
            .decodeList<NutritionTarget>()
            .firstOrNull()

    /** Historique des cibles nutrition passées, du plus récent au plus ancien. */
    suspend fun fetchHistory(sinceDate: String): List<NutritionTarget> =
        client.postgrest.from("nutrition_targets")
            .select {
                filter { gte("date", sinceDate) }
                order("date", Order.DESCENDING)
            }
            .decodeList()

    suspend fun set(target: NewNutritionTarget): NutritionTarget =
        client.postgrest.from("nutrition_targets")
            .upsert(target, onConflict = "user_id,date") { select() }
            .decodeSingle()
}
