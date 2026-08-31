package com.example.mmarecomp.data.offline

import android.content.Context
import com.example.mmarecomp.data.local.AppDatabase
import com.example.mmarecomp.data.local.entity.CacheEntry
import com.example.mmarecomp.data.local.entity.SyncOutboxEntry
import com.example.mmarecomp.util.isOfflineEnqueueable
import com.example.mmarecomp.util.rethrowCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.util.UUID

object SyncEntityType {
    const val WORKOUT = "workout"
    const val MEAL = "meal"
    const val WEIGH_IN = "weigh_in"
    const val CHECK_IN = "check_in"
    const val MMA_SESSION = "mma_session"
}

object SyncOperation {
    const val INSERT = "insert"
    const val DELETE = "delete"
}

/** Cache local + file d'attente pour les écritures hors-ligne. */
class OfflineCoordinator private constructor(context: Context) {
    private val cacheDao = AppDatabase.get(context).cacheDao()
    private val outboxDao = AppDatabase.get(context).syncOutboxDao()
    private val json = Json { ignoreUnknownKeys = true }

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    suspend fun refreshPendingCount() {
        _pendingCount.value = outboxDao.count()
    }

    suspend fun <T> fetchWithCache(
        cacheKey: String,
        fetchRemote: suspend () -> T,
        readCache: (String) -> T,
        writeCache: (T) -> String,
    ): T {
        return try {
            val remote = fetchRemote()
            cacheDao.put(
                CacheEntry(
                    cacheKey = cacheKey,
                    jsonPayload = writeCache(remote),
                    updatedAtMs = System.currentTimeMillis(),
                ),
            )
            remote
        } catch (e: Throwable) {
            rethrowCancellation(e)
            if (!e.isOfflineEnqueueable()) throw e
            val cached = cacheDao.get(cacheKey)?.jsonPayload
            if (cached != null) readCache(cached) else throw e
        }
    }

    /**
     * @param id identifiant stable (idéalement `local-…`) pour pouvoir annuler
     *           un INSERT offline via [removeOutboxEntry] au delete UI.
     */
    suspend fun enqueue(
        entityType: String,
        operation: String,
        payloadJson: String,
        id: String = UUID.randomUUID().toString(),
    ) {
        outboxDao.insert(
            SyncOutboxEntry(
                id = id,
                entityType = entityType,
                operation = operation,
                payloadJson = payloadJson,
                createdAtMs = System.currentTimeMillis(),
            ),
        )
        refreshPendingCount()
    }

    suspend fun pendingEntries(): List<SyncOutboxEntry> = outboxDao.all()

    suspend fun removeOutboxEntry(id: String) {
        outboxDao.delete(id)
        refreshPendingCount()
    }

    suspend fun incrementRetry(id: String) {
        outboxDao.incrementRetry(id)
    }

    fun <T> decodeModel(serializer: kotlinx.serialization.KSerializer<T>, payload: String): T =
        json.decodeFromString(serializer, payload)

    fun <T> encodeModel(serializer: kotlinx.serialization.KSerializer<T>, value: T): String =
        json.encodeToString(serializer, value)

    companion object {
        const val MAX_RETRY = 5

        @Volatile
        private var instance: OfflineCoordinator? = null

        fun get(context: Context): OfflineCoordinator =
            instance ?: synchronized(this) {
                instance ?: OfflineCoordinator(context.applicationContext).also { instance = it }
            }
    }
}
