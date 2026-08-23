package com.example.mmarecomp.data

import com.example.mmarecomp.model.Profile
import com.example.mmarecomp.model.ProfileUpdate
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.eq

class ProfileRepository {
    private val client = SupabaseProvider.client

    suspend fun fetch(userId: String): Profile =
        client.postgrest.from("profiles")
            .select { filter { eq("id", userId) } }
            .decodeSingle()

    suspend fun update(userId: String, patch: ProfileUpdate) {
        client.postgrest.from("profiles")
            .update(patch) { filter { eq("id", userId) } }
    }
}
