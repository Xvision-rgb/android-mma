package com.example.mmarecomp.data

import com.example.mmarecomp.data.offline.DeletePayload
import com.example.mmarecomp.data.offline.OfflineCoordinator
import com.example.mmarecomp.data.offline.SyncEntityType
import com.example.mmarecomp.data.offline.SyncOperation
import com.example.mmarecomp.data.offline.WeighInListCache
import com.example.mmarecomp.model.NewWeighIn
import com.example.mmarecomp.model.WeighIn
import com.example.mmarecomp.util.isOfflineEnqueueable
import com.example.mmarecomp.util.rethrowCancellation
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class WeighInRepository(
    private val offline: OfflineCoordinator? = null,
) {
    private val client = SupabaseProvider.client
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetch(sinceDate: String): List<WeighIn> {
        val cacheKey = "weighins_since_$sinceDate"
        return if (offline != null) {
            offline.fetchWithCache(
                cacheKey = cacheKey,
                fetchRemote = { fetchRemote(sinceDate) },
                readCache = { payload -> json.decodeFromString<WeighInListCache>(payload).items },
                writeCache = { items -> json.encodeToString(WeighInListCache(items)) },
            )
        } else {
            fetchRemote(sinceDate)
        }
    }

    suspend fun log(weighIn: NewWeighIn): WeighIn =
        try {
            logRemote(weighIn)
        } catch (e: Throwable) {
            rethrowCancellation(e)
            if (offline == null || !e.isOfflineEnqueueable()) throw e
            val localId = "local-${UUID.randomUUID()}"
            offline.enqueue(
                entityType = SyncEntityType.WEIGH_IN,
                operation = SyncOperation.INSERT,
                payloadJson = json.encodeToString(weighIn),
                id = localId,
            )
            WeighIn(
                id = localId,
                userId = "",
                date = weighIn.date,
                heure = weighIn.heure,
                type = weighIn.type,
                poidsKg = weighIn.poidsKg,
                bfPct = weighIn.bfPct,
                contexte = weighIn.contexte,
            )
        }

    suspend fun delete(id: String) {
        if (id.startsWith("local-")) {
            offline?.removeOutboxEntry(id)
            return
        }
        try {
            deleteRemote(id)
        } catch (e: Throwable) {
            rethrowCancellation(e)
            if (offline == null || !e.isOfflineEnqueueable()) throw e
            offline.enqueue(
                entityType = SyncEntityType.WEIGH_IN,
                operation = SyncOperation.DELETE,
                payloadJson = json.encodeToString(DeletePayload(id)),
            )
        }
    }

    suspend fun fetchRemote(sinceDate: String): List<WeighIn> =
        client.postgrest.from("weigh_ins")
            .select {
                filter { gte("date", sinceDate) }
                order("date", Order.ASCENDING)
            }
            .decodeList()

    suspend fun logRemote(weighIn: NewWeighIn): WeighIn =
        client.postgrest.from("weigh_ins")
            .upsert(weighIn) {
                onConflict = "user_id,date,type"
                select()
            }
            .decodeSingle()

    suspend fun deleteRemote(id: String) {
        client.postgrest.from("weigh_ins").delete { filter { eq("id", id) } }
    }
}
