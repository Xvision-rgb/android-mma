package com.example.mmarecomp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.mmarecomp.data.local.entity.SyncOutboxEntry

@Dao
interface SyncOutboxDao {
    @Query("SELECT * FROM sync_outbox ORDER BY createdAtMs ASC")
    suspend fun all(): List<SyncOutboxEntry>

    @Query("SELECT COUNT(*) FROM sync_outbox")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: SyncOutboxEntry)

    @Query("DELETE FROM sync_outbox WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE sync_outbox SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetry(id: String)
}
