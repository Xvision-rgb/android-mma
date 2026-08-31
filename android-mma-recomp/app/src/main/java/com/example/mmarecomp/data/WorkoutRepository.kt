package com.example.mmarecomp.data

import com.example.mmarecomp.data.offline.OfflineCoordinator
import com.example.mmarecomp.data.offline.SyncEntityType
import com.example.mmarecomp.data.offline.SyncOperation
import com.example.mmarecomp.data.offline.WorkoutListCache
import com.example.mmarecomp.data.offline.DeletePayload
import com.example.mmarecomp.model.NewWorkout
import com.example.mmarecomp.model.Workout
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.util.UUID

class WorkoutRepository(
    private val offline: OfflineCoordinator? = null,
) {
    private val client = SupabaseProvider.client
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchRecent(limit: Long = 30): List<Workout> {
        val cacheKey = "workouts_recent_$limit"
        return if (offline != null) {
            offline.fetchWithCache(
                cacheKey = cacheKey,
                fetchRemote = { fetchRecentRemote(limit) },
                readCache = { payload -> json.decodeFromString<WorkoutListCache>(payload).items },
                writeCache = { workouts -> json.encodeToString(WorkoutListCache(workouts)) },
            )
        } else {
            fetchRecentRemote(limit)
        }
    }

    suspend fun fetchWeek(fromDate: String): List<Workout> {
        val cacheKey = "workouts_week_$fromDate"
        return if (offline != null) {
            offline.fetchWithCache(
                cacheKey = cacheKey,
                fetchRemote = { fetchWeekRemote(fromDate) },
                readCache = { payload -> json.decodeFromString<WorkoutListCache>(payload).items },
                writeCache = { workouts -> json.encodeToString(WorkoutListCache(workouts)) },
            )
        } else {
            fetchWeekRemote(fromDate)
        }
    }

    suspend fun log(workout: NewWorkout): Workout =
        try {
            logRemote(workout)
        } catch (e: java.io.IOException) {
            if (offline == null) throw e
            offline.enqueue(
                entityType = SyncEntityType.WORKOUT,
                operation = SyncOperation.INSERT,
                payloadJson = json.encodeToString(workout),
            )
            Workout(
                id = "local-${UUID.randomUUID()}",
                userId = "",
                date = workout.date,
                type = workout.type,
                exercices = workout.exercices,
                dureeMin = workout.dureeMin,
                rpe = workout.rpe,
                notes = workout.notes,
            )
        }

    suspend fun delete(id: String) {
        try {
            deleteRemote(id)
        } catch (e: java.io.IOException) {
            if (offline == null) throw e
            if (!id.startsWith("local-")) {
                offline.enqueue(
                    entityType = SyncEntityType.WORKOUT,
                    operation = SyncOperation.DELETE,
                    payloadJson = json.encodeToString(DeletePayload(id)),
                )
            }
        }
    }

    suspend fun fetchRecentRemote(limit: Long): List<Workout> =
        client.postgrest.from("workouts")
            .select {
                order("date", Order.DESCENDING)
                limit(limit)
            }
            .decodeList()

    suspend fun fetchWeekRemote(fromDate: String): List<Workout> =
        client.postgrest.from("workouts")
            .select {
                filter { gte("date", fromDate) }
                order("date", Order.ASCENDING)
            }
            .decodeList()

    suspend fun logRemote(workout: NewWorkout): Workout =
        client.postgrest.from("workouts")
            .insert(workout) { select() }
            .decodeSingle()

    suspend fun deleteRemote(id: String) {
        client.postgrest.from("workouts").delete { filter { eq("id", id) } }
    }
}
