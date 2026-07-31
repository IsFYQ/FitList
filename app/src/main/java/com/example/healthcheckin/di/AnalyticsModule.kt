package com.example.healthcheckin.di

import com.example.healthcheckin.data.analytics.AnalyticsManager
import com.example.healthcheckin.domain.analytics.AnalyticsTracker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {
    @Binds
    @Singleton
    abstract fun bindAnalyticsTracker(impl: AnalyticsManager): AnalyticsTracker
}
