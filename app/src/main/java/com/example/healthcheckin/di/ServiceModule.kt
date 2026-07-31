package com.example.healthcheckin.di

import com.example.healthcheckin.data.service.FoodSearchServiceImpl
import com.example.healthcheckin.domain.service.FoodSearchService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {
    @Binds
    @Singleton
    abstract fun bindFoodSearchService(impl: FoodSearchServiceImpl): FoodSearchService
}
