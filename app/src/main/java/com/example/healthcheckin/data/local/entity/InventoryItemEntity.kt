package com.example.healthcheckin.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventory_items",
    indices = [
        Index(value = ["userId", "category", "expiryDate"]),
        Index(value = ["userId", "nameNormalized"]),
        Index(value = ["userId", "ingredientKey"]),
    ],
)
data class InventoryItemEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val nameNormalized: String,
    val ingredientKey: String? = null,
    val category: String,
    val initialAmount: Double,
    val remainingAmount: Double,
    val unit: String,
    val pieceGrams: Double? = null,
    val purchaseDate: String,
    val expiryDate: String? = null,
    val unitPrice: Double? = null,
    val version: Int = 0,
    val entrySource: String = "MANUAL",
    val rawText: String? = null,
    val deviceId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncState: String,
)
