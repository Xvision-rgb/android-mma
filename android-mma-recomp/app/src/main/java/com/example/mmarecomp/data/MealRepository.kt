package com.example.mmarecomp.data

import com.example.mmarecomp.data.offline.DeletePayload
import com.example.mmarecomp.data.offline.MealListCache
import com.example.mmarecomp.data.offline.OfflineCoordinator
import com.example.mmarecomp.data.offline.SyncEntityType
import com.example.mmarecomp.data.offline.SyncOperation
import com.example.mmarecomp.model.Meal
import com.example.mmarecomp.model.NewMeal
import com.example.mmarecomp.util.isOfflineEnqueueable
import com.example.mmarecomp.util.rethrowCancellation
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class MealRepository(
    private val offline: OfflineCoordinator? = null,
) {
    private val client = SupabaseProvider.client
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchForDate(forDate: String): List<Meal> {
        val cacheKey = "meals_date_$forDate"
        return if (offline != null) {
            offline.fetchWithCache(
                cacheKey = cacheKey,
                fetchRemote = { fetchForDateRemote(forDate) },
                readCache = { payload -> json.decodeFromString<MealListCache>(payload).items },
                writeCache = { meals -> json.encodeToString(MealListCache(meals)) },
            )
        } else {
            fetchForDateRemote(forDate)
        }
    }

    suspend fun fetchSince(sinceDate: String): List<Meal> {
        val cacheKey = "meals_since_$sinceDate"
        return if (offline != null) {
            offline.fetchWithCache(
                cacheKey = cacheKey,
                fetchRemote = { fetchSinceRemote(sinceDate) },
                readCache = { payload -> json.decodeFromString<MealListCache>(payload).items },
                writeCache = { meals -> json.encodeToString(MealListCache(meals)) },
            )
        } else {
            fetchSinceRemote(sinceDate)
        }
    }

    suspend fun log(meal: NewMeal): Meal =
        try {
            logRemote(meal)
        } catch (e: Throwable) {
            rethrowCancellation(e)
            if (offline == null || !e.isOfflineEnqueueable()) throw e
            val localId = "local-${UUID.randomUUID()}"
            offline.enqueue(
                entityType = SyncEntityType.MEAL,
                operation = SyncOperation.INSERT,
                payloadJson = json.encodeToString(meal),
                id = localId,
            )
            Meal(
                id = localId,
                userId = "",
                date = meal.date,
                repas = meal.repas,
                calories = meal.calories,
                proteinesG = meal.proteinesG,
                glucidesG = meal.glucidesG,
                lipidesG = meal.lipidesG,
                description = meal.description,
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
                entityType = SyncEntityType.MEAL,
                operation = SyncOperation.DELETE,
                payloadJson = json.encodeToString(DeletePayload(id)),
            )
        }
    }

    suspend fun fetchForDateRemote(forDate: String): List<Meal> =
        client.postgrest.from("meals")
            .select {
                filter { eq("date", forDate) }
                order("repas", Order.ASCENDING)
            }
            .decodeList()

    suspend fun fetchSinceRemote(sinceDate: String): List<Meal> =
        client.postgrest.from("meals")
            .select {
                filter { gte("date", sinceDate) }
                order("date", Order.ASCENDING)
            }
            .decodeList()

    suspend fun logRemote(meal: NewMeal): Meal =
        client.postgrest.from("meals")
            .upsert(meal) {
                onConflict = "user_id,date,repas"
                select()
            }
            .decodeSingle()

    suspend fun deleteRemote(id: String) {
        client.postgrest.from("meals").delete { filter { eq("id", id) } }
    }
}
