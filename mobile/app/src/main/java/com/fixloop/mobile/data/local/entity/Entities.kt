package com.fixloop.mobile.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_complaints")
data class CachedComplaintEntity(
    @PrimaryKey
    val id: String, // e.g. "SNT-2026-000482"
    val backendId: Int,
    val categoryIdString: String,
    val categoryName: String,
    val title: String,
    val description: String,
    val severity: String,
    val location: String,
    val status: String,
    val createdAt: String,
    val timestamp: String,
    val anonymous: Boolean,
    val photosJson: String, // serialized list of image URLs/paths
    val timelineJson: String, // serialized list of TimelineNode objects
    val isAtRisk: Boolean = false,
    val isBreached: Boolean = false,
    val priorityScore: Double? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_notifications")
data class CachedNotificationEntity(
    @PrimaryKey
    val id: String, // e.g. "notif-1" or backend id
    val backendId: Int?,
    val complaintId: String,
    val dateGroup: String, // "Today", "Earlier"
    val text: String,
    val time: String,
    val status: String,
    val categoryIdString: String,
    val isUnread: Boolean,
    val type: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_categories")
data class CachedCategoryEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val icon: String?,
    val color: String?,
    val sensitive: Boolean,
    val defaultSlaHours: Int?,
    val filterGroup: String?,
    val formSchemaJson: String?
)
