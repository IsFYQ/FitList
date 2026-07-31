package com.example.healthcheckin.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "analytics_events",
    indices = [
        Index(value = ["userId", "eventName", "localDate"]),
        Index(value = ["syncState"])
    ]
)
data class AnalyticsEventEntity(
    @PrimaryKey val id: String,
    val userId: String?,
    val eventName: String,
    val eventAt: Long,
    val localDate: String,
    val tzOffsetMinutes: Int,
    val sessionId: String,
    val appVersion: String,
    val osVersion: String,
    val deviceModel: String,
    val paramsJson: String,
    val deviceId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncState: String,
)
