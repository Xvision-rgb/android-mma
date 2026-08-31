package com.example.mmarecomp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.mmarecomp.data.local.dao.CacheDao
import com.example.mmarecomp.data.local.dao.SyncOutboxDao
import com.example.mmarecomp.data.local.entity.CacheEntry
import com.example.mmarecomp.data.local.entity.SyncOutboxEntry

@Database(
    entities = [CacheEntry::class, SyncOutboxEntry::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cacheDao(): CacheDao
    abstract fun syncOutboxDao(): SyncOutboxDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mma_recomp.db",
                ).build().also { instance = it }
            }
    }
}
