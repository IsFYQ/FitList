package com.example.healthcheckin.di

import com.example.healthcheckin.data.repository.AuthRepositoryImpl
import com.example.healthcheckin.data.repository.BackupRepositoryImpl
import com.example.healthcheckin.data.repository.BodyMeasurementRepositoryImpl
import com.example.healthcheckin.data.repository.DashboardRepositoryImpl
import com.example.healthcheckin.data.repository.ExportRepositoryImpl
import com.example.healthcheckin.data.repository.FoodRepositoryImpl
import com.example.healthcheckin.data.repository.GoalRepositoryImpl
import com.example.healthcheckin.data.repository.MealRepositoryImpl
import com.example.healthcheckin.data.repository.MilestoneRepositoryImpl
import com.example.healthcheckin.data.repository.InventoryRepositoryImpl
import com.example.healthcheckin.data.repository.IngredientBindingRepositoryImpl
import com.example.healthcheckin.data.repository.WeightRepositoryImpl
import com.example.healthcheckin.domain.repository.AuthRepository
import com.example.healthcheckin.domain.repository.BackupRepository
import com.example.healthcheckin.domain.repository.BodyMeasurementRepository
import com.example.healthcheckin.domain.repository.DashboardRepository
import com.example.healthcheckin.domain.repository.ExportRepository
import com.example.healthcheckin.domain.repository.FoodRepository
import com.example.healthcheckin.domain.repository.GoalRepository
import com.example.healthcheckin.domain.repository.MealRepository
import com.example.healthcheckin.domain.repository.MilestoneRepository
import com.example.healthcheckin.domain.repository.InventoryRepository
import com.example.healthcheckin.domain.repository.IngredientBindingRepository
import com.example.healthcheckin.domain.repository.WeightRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindGoalRepository(impl: GoalRepositoryImpl): GoalRepository

    @Binds
    @Singleton
    abstract fun bindDashboardRepository(impl: DashboardRepositoryImpl): DashboardRepository

    @Binds
    @Singleton
    abstract fun bindMealRepository(impl: MealRepositoryImpl): MealRepository

    @Binds
    @Singleton
    abstract fun bindFoodRepository(impl: FoodRepositoryImpl): FoodRepository

    @Binds
    @Singleton
    abstract fun bindWeightRepository(impl: WeightRepositoryImpl): WeightRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository

    @Binds
    @Singleton
    abstract fun bindExportRepository(impl: ExportRepositoryImpl): ExportRepository

    @Binds @Singleton abstract fun bindBodyMeasurementRepository(impl: BodyMeasurementRepositoryImpl): BodyMeasurementRepository
    @Binds @Singleton abstract fun bindMilestoneRepository(impl: MilestoneRepositoryImpl): MilestoneRepository
    @Binds @Singleton abstract fun bindInventoryRepository(impl: InventoryRepositoryImpl): InventoryRepository
    @Binds @Singleton abstract fun bindIngredientBindingRepository(impl: IngredientBindingRepositoryImpl): IngredientBindingRepository
}
