package com.example.mmarecomp.data

import com.example.mmarecomp.model.Food
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order

class FoodRepository {
    private val client = SupabaseProvider.client

    suspend fun fetchAll(): List<Food> =
        client.postgrest.from("foods")
            .select {
                order("nom", Order.ASCENDING)
            }
            .decodeList()
}
