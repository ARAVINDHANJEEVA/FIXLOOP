package com.fixloop.mobile.data.local.dao

import androidx.room.*
import com.fixloop.mobile.data.local.entity.CachedCategoryEntity
import com.fixloop.mobile.data.local.entity.CachedComplaintEntity
import com.fixloop.mobile.data.local.entity.CachedNotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ComplaintDao {

    @Query("SELECT * FROM cached_complaints ORDER BY updatedAt DESC")
    fun getAllComplaintsFlow(): Flow<List<CachedComplaintEntity>>

    @Query("SELECT * FROM cached_complaints WHERE id = :id LIMIT 1")
    suspend fun getComplaintById(id: String): CachedComplaintEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(complaint: CachedComplaintEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(complaints: List<CachedComplaintEntity>)

    @Query("UPDATE cached_complaints SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM cached_complaints WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM cached_complaints")
    suspend fun clearAll()
}

@Dao
interface NotificationDao {

    @Query("SELECT * FROM cached_notifications ORDER BY createdAtMillis ASC")
    fun getAllNotificationsFlow(): Flow<List<CachedNotificationEntity>>

    @Query("SELECT COUNT(*) FROM cached_notifications WHERE isUnread = 1")
    fun getUnreadCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(notification: CachedNotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifications: List<CachedNotificationEntity>)

    @Query("UPDATE cached_notifications SET isUnread = 0 WHERE id = :id OR complaintId = :id")
    suspend fun markAsRead(id: String)

    @Query("UPDATE cached_notifications SET isUnread = 0")
    suspend fun markAllAsRead()

    @Query("DELETE FROM cached_notifications")
    suspend fun clearAll()
}

@Dao
interface CategoryDao {

    @Query("SELECT * FROM cached_categories")
    fun getAllCategoriesFlow(): Flow<List<CachedCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CachedCategoryEntity>)

    @Query("DELETE FROM cached_categories")
    suspend fun clearAll()
}
