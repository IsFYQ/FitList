package com.example.healthcheckin.data.analytics

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.healthcheckin.domain.analytics.AnalyticsEvents
import com.example.healthcheckin.domain.analytics.AnalyticsTracker
import com.example.healthcheckin.util.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsLifecycleObserver @Inject constructor(
    private val analyticsTracker: AnalyticsTracker,
    private val sessionTracker: AnalyticsSessionTracker,
    private val networkMonitor: NetworkMonitor,
    private val analyticsManager: AnalyticsManager,
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
    private var isForeground = false

    fun register() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        scope.launch {
            analyticsManager.runMaintenance()
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        if (isForeground) return
        scope.launch {
            val isCold = sessionTracker.isColdStartPending()
            val newSession = sessionTracker.onAppForeground()
            if (isCold || newSession) {
                analyticsTracker.track(
                    AnalyticsEvents.APP_SESSION_START,
                    mapOf(
                        "is_cold_start" to isCold,
                        "is_offline" to !networkMonitor.isOnline(),
                    ),
                )
                sessionTracker.markColdStartHandled()
            }
            isForeground = true
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        if (!isForeground) return
        isForeground = false
        val durationMs = sessionTracker.onAppBackground()
        analyticsTracker.track(
            AnalyticsEvents.APP_SESSION_END,
            mapOf("duration_ms" to durationMs),
        )
        scope.launch {
            analyticsManager.flushNow()
        }
    }
}
