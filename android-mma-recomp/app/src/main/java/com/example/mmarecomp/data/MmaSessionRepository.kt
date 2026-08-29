package com.example.mmarecomp.data

import com.example.mmarecomp.model.MmaSession
import com.example.mmarecomp.model.NewMmaSession
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order

class MmaSessionRepository {
    private val client = SupabaseProvider.client

    suspend fun fetchRecent(limit: Long = 20): List<MmaSession> =
        client.postgrest.from("mma_sessions")
            .select {
                order("date", Order.DESCENDING)
                limit(limit)
            }
            .decodeList()

    /** Séances MMA depuis une date — alimente la charge interne (l'ACWR
     *  compterait faux en ignorant le sparring) et la détection de conflit
     *  avec la musculation. */
    suspend fun fetchSince(sinceDate: String): List<MmaSession> =
        client.postgrest.from("mma_sessions")
            .select {
                filter { gte("date", sinceDate) }
                order("date", Order.ASCENDING)
            }
            .decodeList()

    suspend fun log(session: NewMmaSession): MmaSession =
        client.postgrest.from("mma_sessions")
            .insert(session) { select() }
            .decodeSingle()

    suspend fun delete(id: String) {
        client.postgrest.from("mma_sessions").delete { filter { eq("id", id) } }
    }
}
