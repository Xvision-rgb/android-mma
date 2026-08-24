package com.example.mmarecomp.data

import com.example.mmarecomp.model.Meal
import com.example.mmarecomp.model.NewMeal
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.eq
import io.github.jan.supabase.postgrest.query.filter.gte

class MealRepository {
    private val client = SupabaseProvider.client

    suspend fun fetch(forDate: String): List<Meal> =
        client.postgrest.from("meals")
            .select {
                filter { eq("date", forDate) }
                order("repas", Order.ASCENDING)
            }
            .decodeList()

    suspend fun fetch(sinceDate: String): List<Meal> =
        client.postgrest.from("meals")
            .select {
                filter { gte("date", sinceDate) }
                order("date", Order.ASCENDING)
            }
            .decodeList()

    /** Upsert sur (user_id, date, repas) : re-loguer un créneau remplace
     *  l'entrée existante plutôt que d'en créer une deuxième. */
    suspend fun log(meal: NewMeal): Meal =
        client.postgrest.from("meals")
            .upsert(meal, onConflict = "user_id,date,repas") { select() }
            .decodeSingle()

    suspend fun delete(id: String) {
        client.postgrest.from("meals").delete { filter { eq("id", id) } }
    }

    suspend fun deleteAll(userId: String) {
        client.postgrest.from("meals").delete { filter { eq("user_id", userId) } }
    }
}
