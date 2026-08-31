package com.example.mmarecomp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cache_entries")
data class CacheEntry(
    @PrimaryKey val cacheKey: String,
    val jsonPayload: String,
    val updatedAtMs: Long,
)
