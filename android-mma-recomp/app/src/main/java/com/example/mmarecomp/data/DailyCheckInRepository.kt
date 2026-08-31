package com.example.mmarecomp.data

import com.example.mmarecomp.data.offline.CheckInListCache
import com.example.mmarecomp.data.offline.OfflineCoordinator
import com.example.mmarecomp.data.offline.SyncEntityType
import com.example.mmarecomp.data.offline.SyncOperation
import com.example.mmarecomp.model.DailyCheckIn
import com.example.mmarecomp.model.NewDailyCheckIn
import com.example.mmarecomp.util.isOfflineEnqueueable
import com.example.mmarecomp.util.rethrowCancellation
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class DailyCheckInRepository(
    private val offline: OfflineCoordinator? = null,
) {
    private val client = SupabaseProvider.client
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchSince(sinceDate: String): List<DailyCheckIn> {
        val cacheKey = "checkins_since_$sinceDate"
        return if (offline != null) {
            offline.fetchWithCache(
                cacheKey = cacheKey,
                fetchRemote = { fetchSinceRemote(sinceDate) },
                readCache = { payload -> json.decodeFromString<CheckInListCache>(payload).items },
                writeCache = { items -> json.encodeToString(CheckInListCache(items)) },
            )
        } else {
            fetchSinceRemote(sinceDate)
        }
    }

    suspend fun fetch(date: String): DailyCheckIn? =
        fetchForDateRemote(date)

    suspend fun log(checkIn: NewDailyCheckIn): DailyCheckIn =
        try {
            logRemote(checkIn)
        } catch (e: Throwable) {
            rethrowCancellation(e)
            if (offline == null || !e.isOfflineEnqueueable()) throw e
            val localId = "local-${UUID.randomUUID()}"
            offline.enqueue(
                entityType = SyncEntityType.CHECK_IN,
                operation = SyncOperation.INSERT,
                payloadJson = json.encodeToString(checkIn),
                id = localId,
            )
            DailyCheckIn(
                id = localId,
                userId = "",
                date = checkIn.date,
                sommeil = checkIn.sommeil,
                courbatures = checkIn.courbatures,
                fatigue = checkIn.fatigue,
                humeur = checkIn.humeur,
                stress = checkIn.stress,
                hrvRmssd = checkIn.hrvRmssd,
                deadHangSec = checkIn.deadHangSec,
            )
        }

    suspend fun fetchSinceRemote(sinceDate: String): List<DailyCheckIn> =
        client.postgrest.from("daily_checkins")
            .select {
                filter { gte("date", sinceDate) }
                order("date", Order.ASCENDING)
            }
            .decodeList()

    private suspend fun fetchForDateRemote(date: String): DailyCheckIn? =
        client.postgrest.from("daily_checkins")
            .select {
                filter { eq("date", date) }
                limit(1)
            }
            .decodeList<DailyCheckIn>()
            .firstOrNull()

    suspend fun logRemote(checkIn: NewDailyCheckIn): DailyCheckIn =
        client.postgrest.from("daily_checkins")
            .upsert(checkIn) {
                onConflict = "user_id,date"
                select()
            }
            .decodeSingle()
}
