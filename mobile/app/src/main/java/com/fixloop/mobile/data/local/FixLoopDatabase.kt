package com.fixloop.mobile.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.fixloop.mobile.data.local.dao.CategoryDao
import com.fixloop.mobile.data.local.dao.ComplaintDao
import com.fixloop.mobile.data.local.dao.NotificationDao
import com.fixloop.mobile.data.local.entity.CachedCategoryEntity
import com.fixloop.mobile.data.local.entity.CachedComplaintEntity
import com.fixloop.mobile.data.local.entity.CachedNotificationEntity

@Database(
    entities = [
        CachedComplaintEntity::class,
        CachedNotificationEntity::class,
        CachedCategoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FixLoopDatabase : RoomDatabase() {

    abstract fun complaintDao(): ComplaintDao
    abstract fun notificationDao(): NotificationDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: FixLoopDatabase? = null

        fun getInstance(context: Context): FixLoopDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FixLoopDatabase::class.java,
                    "fixloop_sentinel.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
