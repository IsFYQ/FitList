package com.example.healthcheckin

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class HealthCheckInApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var analyticsLifecycleObserver: com.example.healthcheckin.data.analytics.AnalyticsLifecycleObserver

    override fun onCreate() {
        super.onCreate()
        analyticsLifecycleObserver.register()
    }

    fun prepareMlKit() {
        // Called from MainActivity after Hilt injection
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
