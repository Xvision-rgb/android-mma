package com.example.mmarecomp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.mmarecomp.data.local.entity.CacheEntry

@Dao
interface CacheDao {
    @Query("SELECT * FROM cache_entries WHERE cacheKey = :key LIMIT 1")
    suspend fun get(key: String): CacheEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entry: CacheEntry)

    @Query("DELETE FROM cache_entries WHERE cacheKey = :key")
    suspend fun delete(key: String)
}
