package com.example.healthcheckin.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ingredient_bindings",
    indices = [
        Index(value = ["userId", "foodId"], unique = true),
        Index(value = ["inventoryItemId"]),
    ],
)
data class IngredientBindingEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val foodId: String,
    val inventoryItemId: String,
    val deviceId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val syncState: String,
)
