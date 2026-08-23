package com.example.mmarecomp.data

import com.example.mmarecomp.model.NewWorkout
import com.example.mmarecomp.model.Workout
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.eq
import io.github.jan.supabase.postgrest.query.filter.gte

class WorkoutRepository {
    private val client = SupabaseProvider.client

    suspend fun fetchRecent(limit: Long = 30): List<Workout> =
        client.postgrest.from("workouts")
            .select {
                order("date", Order.DESCENDING)
                limit(limit)
            }
            .decodeList()

    suspend fun fetchWeek(fromDate: String): List<Workout> =
        client.postgrest.from("workouts")
            .select {
                filter { gte("date", fromDate) }
                order("date", Order.ASCENDING)
            }
            .decodeList()

    /** Tout l'historique, sans filtre de date — sert aux records personnels
     *  qui doivent rester valables même hors de la fenêtre de progression. */
    suspend fun fetchAll(): List<Workout> =
        client.postgrest.from("workouts")
            .select { order("date", Order.ASCENDING) }
            .decodeList()

    suspend fun log(workout: NewWorkout): Workout =
        client.postgrest.from("workouts")
            .insert(workout) { select() }
            .decodeSingle()

    suspend fun delete(id: String) {
        client.postgrest.from("workouts").delete { filter { eq("id", id) } }
    }
}
