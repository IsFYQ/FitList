package com.example.healthcheckin.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_search_cache")
data class FoodSearchCacheEntity(
    @PrimaryKey val queryNormalized: String,
    val payloadJson: String,
    val fetchedAt: Long,
    val expiresAt: Long,
)

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey val id: String,
    val tableName: String,
    val rowId: String,
    val operation: String,
    val retryCount: Int = 0,
    val nextRetryAt: Long? = null,
    val lastErrorCode: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey val key: String,
    val valueJson: String,
    val updatedAt: Long,
)

@Entity(tableName = "backup_state")
data class BackupStateEntity(
    @PrimaryKey val id: String,
    val tableName: String,
    val rowId: String,
    val lastBackupAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)
