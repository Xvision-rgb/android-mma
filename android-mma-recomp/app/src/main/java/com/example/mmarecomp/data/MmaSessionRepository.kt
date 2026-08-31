package com.example.mmarecomp.data

import com.example.mmarecomp.data.offline.DeletePayload
import com.example.mmarecomp.data.offline.MmaSessionListCache
import com.example.mmarecomp.data.offline.OfflineCoordinator
import com.example.mmarecomp.data.offline.SyncEntityType
import com.example.mmarecomp.data.offline.SyncOperation
import com.example.mmarecomp.model.MmaSession
import com.example.mmarecomp.model.NewMmaSession
import com.example.mmarecomp.util.isOfflineEnqueueable
import com.example.mmarecomp.util.rethrowCancellation
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class MmaSessionRepository(
    private val offline: OfflineCoordinator? = null,
) {
    private val client = SupabaseProvider.client
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchRecent(limit: Long = 20): List<MmaSession> {
        val cacheKey = "mma_recent_$limit"
        return if (offline != null) {
            offline.fetchWithCache(
                cacheKey = cacheKey,
                fetchRemote = { fetchRecentRemote(limit) },
                readCache = { payload -> json.decodeFromString<MmaSessionListCache>(payload).items },
                writeCache = { items -> json.encodeToString(MmaSessionListCache(items)) },
            )
        } else {
            fetchRecentRemote(limit)
        }
    }

    /** Séances MMA depuis une date — alimente la charge interne (l'ACWR
     *  compterait faux en ignorant le sparring) et la détection de conflit
     *  avec la musculation. */
    suspend fun fetchSince(sinceDate: String): List<MmaSession> {
        val cacheKey = "mma_since_$sinceDate"
        return if (offline != null) {
            offline.fetchWithCache(
                cacheKey = cacheKey,
                fetchRemote = { fetchSinceRemote(sinceDate) },
                readCache = { payload -> json.decodeFromString<MmaSessionListCache>(payload).items },
                writeCache = { items -> json.encodeToString(MmaSessionListCache(items)) },
            )
        } else {
            fetchSinceRemote(sinceDate)
        }
    }

    suspend fun log(session: NewMmaSession): MmaSession =
        try {
            logRemote(session)
        } catch (e: Throwable) {
            rethrowCancellation(e)
            if (offline == null || !e.isOfflineEnqueueable()) throw e
            val localId = "local-${UUID.randomUUID()}"
            offline.enqueue(
                entityType = SyncEntityType.MMA_SESSION,
                operation = SyncOperation.INSERT,
                payloadJson = json.encodeToString(session),
                id = localId,
            )
            MmaSession(
                id = localId,
                userId = "",
                date = session.date,
                wodContent = session.wodContent,
                roundsSets = session.roundsSets,
                ressenti = session.ressenti,
                notesTechnique = session.notesTechnique,
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
                entityType = SyncEntityType.MMA_SESSION,
                operation = SyncOperation.DELETE,
                payloadJson = json.encodeToString(DeletePayload(id)),
            )
        }
    }

    suspend fun fetchRecentRemote(limit: Long): List<MmaSession> =
        client.postgrest.from("mma_sessions")
            .select {
                order("date", Order.DESCENDING)
                limit(limit)
            }
            .decodeList()

    suspend fun fetchSinceRemote(sinceDate: String): List<MmaSession> =
        client.postgrest.from("mma_sessions")
            .select {
                filter { gte("date", sinceDate) }
                order("date", Order.ASCENDING)
            }
            .decodeList()

    suspend fun logRemote(session: NewMmaSession): MmaSession =
        client.postgrest.from("mma_sessions")
            .insert(session) { select() }
            .decodeSingle()

    suspend fun deleteRemote(id: String) {
        client.postgrest.from("mma_sessions").delete { filter { eq("id", id) } }
    }
}
