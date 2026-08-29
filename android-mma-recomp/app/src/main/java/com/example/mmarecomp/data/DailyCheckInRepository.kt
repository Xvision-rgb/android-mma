package com.example.mmarecomp.data

import com.example.mmarecomp.model.DailyCheckIn
import com.example.mmarecomp.model.NewDailyCheckIn
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order

class DailyCheckInRepository {
    private val client = SupabaseProvider.client

    suspend fun fetchSince(sinceDate: String): List<DailyCheckIn> =
        client.postgrest.from("daily_checkins")
            .select {
                filter { gte("date", sinceDate) }
                order("date", Order.ASCENDING)
            }
            .decodeList()

    suspend fun fetch(date: String): DailyCheckIn? =
        client.postgrest.from("daily_checkins")
            .select { filter { eq("date", date) } }
            .decodeList<DailyCheckIn>()
            .firstOrNull()

    /** Upsert sur (user_id, date) : refaire son check-in le même jour corrige
     *  celui du matin au lieu d'en empiler un second. */
    suspend fun log(checkIn: NewDailyCheckIn): DailyCheckIn =
        client.postgrest.from("daily_checkins")
            .upsert(checkIn) {
                onConflict = "user_id,date"
                select()
            }
            .decodeSingle()
}
