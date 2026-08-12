package com.example.healthcheckin.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventory_ledger",
    indices = [
        Index(value = ["userId", "inventoryItemId"]),
        Index(value = ["refMealEntryId"]),
    ],
)
data class InventoryLedgerEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val inventoryItemId: String,
    val changeType: String,
    val deltaAmount: Double,
    val balanceAfter: Double,
    val refMealEntryId: String? = null,
    val note: String? = null,
    val deviceId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncState: String,
)
