package com.example.mmarecomp.data

import com.example.mmarecomp.model.NewWeighIn
import com.example.mmarecomp.model.WeighIn
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order

class WeighInRepository {
    private val client = SupabaseProvider.client

    suspend fun fetch(sinceDate: String): List<WeighIn> =
        client.postgrest.from("weigh_ins")
            .select {
                filter { gte("date", sinceDate) }
                order("date", Order.ASCENDING)
            }
            .decodeList()

    /** Upsert sur (user_id, date, type) : re-loguer la même pesée du jour
     *  remplace la précédente au lieu de dupliquer. */
    suspend fun log(weighIn: NewWeighIn): WeighIn =
        client.postgrest.from("weigh_ins")
            .upsert(weighIn) {
                onConflict = "user_id,date,type"
                select()
            }
            .decodeSingle()

    suspend fun delete(id: String) {
        client.postgrest.from("weigh_ins").delete { filter { eq("id", id) } }
    }
}
