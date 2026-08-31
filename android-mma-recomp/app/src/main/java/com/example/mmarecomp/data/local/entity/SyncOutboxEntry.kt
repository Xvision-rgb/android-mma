package com.example.mmarecomp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_outbox")
data class SyncOutboxEntry(
    @PrimaryKey val id: String,
    val entityType: String,
    val operation: String,
    val payloadJson: String,
    val createdAtMs: Long,
    val retryCount: Int = 0,
)
