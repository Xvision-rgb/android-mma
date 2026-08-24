package com.example.mmarecomp.data

import com.example.mmarecomp.model.NewTrainingPlanDay
import com.example.mmarecomp.model.Phase
import com.example.mmarecomp.model.TrainingPlanDay
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.eq

class TrainingPlanRepository {
    private val client = SupabaseProvider.client

    suspend fun fetchWeek(phase: Phase): List<TrainingPlanDay> =
        client.postgrest.from("training_plan")
            .select {
                filter {
                    eq("phase", phase.value)
                    eq("actif", true)
                }
                order("jour_semaine", Order.ASCENDING)
            }
            .decodeList()

    suspend fun upsert(day: NewTrainingPlanDay) {
        client.postgrest.from("training_plan")
            .upsert(day, onConflict = "user_id,jour_semaine,phase")
    }

    suspend fun deleteAll(userId: String) {
        client.postgrest.from("training_plan").delete { filter { eq("user_id", userId) } }
    }
}
