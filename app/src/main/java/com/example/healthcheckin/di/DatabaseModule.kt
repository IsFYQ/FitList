package com.example.healthcheckin.di

import android.content.Context
import androidx.room.Room
import com.example.healthcheckin.data.local.HealthDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HealthDatabase =
        Room.databaseBuilder(
            context,
            HealthDatabase::class.java,
            "health_checkin.db"
        ).fallbackToDestructiveMigration()
            .build()

    @Provides fun provideProfileDao(db: HealthDatabase) = db.profileDao()
    @Provides fun provideGoalDao(db: HealthDatabase) = db.goalDao()
    @Provides fun provideDailyBudgetDao(db: HealthDatabase) = db.dailyBudgetDao()
    @Provides fun provideFoodDao(db: HealthDatabase) = db.foodDao()
    @Provides fun providePublicFoodDao(db: HealthDatabase) = db.publicFoodDao()
    @Provides fun provideMealEntryDao(db: HealthDatabase) = db.mealEntryDao()
    @Provides fun provideWeightRecordDao(db: HealthDatabase) = db.weightRecordDao()
    @Provides fun provideAnalyticsEventDao(db: HealthDatabase) = db.analyticsEventDao()
    @Provides fun provideFoodSearchCacheDao(db: HealthDatabase) = db.foodSearchCacheDao()
    @Provides fun provideSyncQueueDao(db: HealthDatabase) = db.syncQueueDao()
    @Provides fun provideSyncStatusDao(db: HealthDatabase) = db.syncStatusDao()
    @Provides fun provideAppSettingDao(db: HealthDatabase) = db.appSettingDao()
    @Provides fun provideBackupStateDao(db: HealthDatabase) = db.backupStateDao()
    @Provides fun provideBodyMeasurementDao(db: HealthDatabase) = db.bodyMeasurementDao()
    @Provides fun provideMilestoneDao(db: HealthDatabase) = db.milestoneDao()
    @Provides fun provideInventoryItemDao(db: HealthDatabase) = db.inventoryItemDao()
    @Provides fun provideInventoryLedgerDao(db: HealthDatabase) = db.inventoryLedgerDao()
    @Provides fun provideIngredientAliasDao(db: HealthDatabase) = db.ingredientAliasDao()
    @Provides fun provideIngredientBindingDao(db: HealthDatabase) = db.ingredientBindingDao()
    @Provides fun provideExerciseRecordDao(db: HealthDatabase) = db.exerciseRecordDao()
}
