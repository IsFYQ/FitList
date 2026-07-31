package com.example.healthcheckin.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.healthcheckin.data.local.dao.AnalyticsEventDao
import com.example.healthcheckin.data.local.dao.AppSettingDao
import com.example.healthcheckin.data.local.dao.BackupStateDao
import com.example.healthcheckin.data.local.dao.DailyBudgetDao
import com.example.healthcheckin.data.local.dao.FoodDao
import com.example.healthcheckin.data.local.dao.FoodSearchCacheDao
import com.example.healthcheckin.data.local.dao.GoalDao
import com.example.healthcheckin.data.local.dao.MealEntryDao
import com.example.healthcheckin.data.local.dao.ProfileDao
import com.example.healthcheckin.data.local.dao.PublicFoodDao
import com.example.healthcheckin.data.local.dao.SyncQueueDao
import com.example.healthcheckin.data.local.dao.SyncStatusDao
import com.example.healthcheckin.data.local.dao.WeightRecordDao
import com.example.healthcheckin.data.local.entity.AnalyticsEventEntity
import com.example.healthcheckin.data.local.entity.AppSettingEntity
import com.example.healthcheckin.data.local.entity.BackupStateEntity
import com.example.healthcheckin.data.local.entity.DailyBudgetEntity
import com.example.healthcheckin.data.local.entity.FoodEntity
import com.example.healthcheckin.data.local.entity.FoodSearchCacheEntity
import com.example.healthcheckin.data.local.entity.GoalEntity
import com.example.healthcheckin.data.local.entity.MealEntryEntity
import com.example.healthcheckin.data.local.entity.ProfileEntity
import com.example.healthcheckin.data.local.entity.PublicFoodEntity
import com.example.healthcheckin.data.local.entity.SyncQueueEntity
import com.example.healthcheckin.data.local.entity.WeightRecordEntity

@Database(
    entities = [
        ProfileEntity::class,
        GoalEntity::class,
        DailyBudgetEntity::class,
        FoodEntity::class,
        PublicFoodEntity::class,
        MealEntryEntity::class,
        WeightRecordEntity::class,
        AnalyticsEventEntity::class,
        FoodSearchCacheEntity::class,
        SyncQueueEntity::class,
        AppSettingEntity::class,
        BackupStateEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class HealthDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun goalDao(): GoalDao
    abstract fun dailyBudgetDao(): DailyBudgetDao
    abstract fun foodDao(): FoodDao
    abstract fun publicFoodDao(): PublicFoodDao
    abstract fun mealEntryDao(): MealEntryDao
    abstract fun weightRecordDao(): WeightRecordDao
    abstract fun analyticsEventDao(): AnalyticsEventDao
    abstract fun foodSearchCacheDao(): FoodSearchCacheDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun syncStatusDao(): SyncStatusDao
    abstract fun appSettingDao(): AppSettingDao
    abstract fun backupStateDao(): BackupStateDao
}
