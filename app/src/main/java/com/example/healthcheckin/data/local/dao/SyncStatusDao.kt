package com.example.healthcheckin.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncStatusDao {
    @Query(
        """
        SELECT
          (SELECT COUNT(*) FROM meal_entries WHERE syncState = 'FAILED' AND deletedAt IS NULL) +
          (SELECT COUNT(*) FROM goals WHERE syncState = 'FAILED' AND deletedAt IS NULL) +
          (SELECT COUNT(*) FROM daily_budgets WHERE syncState = 'FAILED' AND deletedAt IS NULL) +
          (SELECT COUNT(*) FROM weight_records WHERE syncState = 'FAILED' AND deletedAt IS NULL) +
          (SELECT COUNT(*) FROM profiles WHERE syncState = 'FAILED' AND deletedAt IS NULL)
          + (SELECT COUNT(*) FROM body_measurements WHERE syncState = 'FAILED' AND deletedAt IS NULL)
          + (SELECT COUNT(*) FROM milestones WHERE syncState = 'FAILED' AND deletedAt IS NULL)
          + (SELECT COUNT(*) FROM inventory_items WHERE syncState = 'FAILED' AND deletedAt IS NULL)
          + (SELECT COUNT(*) FROM inventory_ledger WHERE syncState = 'FAILED' AND deletedAt IS NULL)
          + (SELECT COUNT(*) FROM ingredient_bindings WHERE syncState = 'FAILED' AND deletedAt IS NULL)
        """
    )
    fun observeFailedSyncCount(): Flow<Int>
}
