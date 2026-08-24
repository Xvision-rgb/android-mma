package com.example.mmarecomp.data

import com.example.mmarecomp.model.MmaSession
import com.example.mmarecomp.model.NewMmaSession
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.eq

class MmaSessionRepository {
    private val client = SupabaseProvider.client

    suspend fun fetchRecent(limit: Long = 20): List<MmaSession> =
        client.postgrest.from("mma_sessions")
            .select {
                order("date", Order.DESCENDING)
                limit(limit)
            }
            .decodeList()

    suspend fun log(session: NewMmaSession): MmaSession =
        client.postgrest.from("mma_sessions")
            .insert(session) { select() }
            .decodeSingle()

    suspend fun deleteAll(userId: String) {
        client.postgrest.from("mma_sessions").delete { filter { eq("user_id", userId) } }
    }
}
