package com.example.healthcheckin.data.analytics

import com.example.healthcheckin.util.UuidV7
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsSessionTracker @Inject constructor() {

    private var sessionId: String = UuidV7.generate()
    private var sessionStartAt: Long = System.currentTimeMillis()
    private var lastStoppedAt: Long? = null
    private var coldStartPending: Boolean = true

    fun currentSessionId(): String = sessionId

    fun markColdStartHandled() {
        coldStartPending = false
    }

    fun isColdStartPending(): Boolean = coldStartPending

    fun onNewSession() {
        sessionId = UuidV7.generate()
        sessionStartAt = System.currentTimeMillis()
        coldStartPending = false
    }

    fun onAppForeground(now: Long = System.currentTimeMillis()): Boolean {
        val gap = lastStoppedAt?.let { now - it }
        val newSession = gap != null && gap >= SESSION_GAP_MS
        if (newSession) {
            onNewSession()
        }
        lastStoppedAt = null
        return newSession || coldStartPending
    }

    fun onAppBackground(now: Long = System.currentTimeMillis()): Long {
        lastStoppedAt = now
        return (now - sessionStartAt).coerceAtLeast(0)
    }

    fun sessionDurationMs(now: Long = System.currentTimeMillis()): Long =
        (now - sessionStartAt).coerceAtLeast(0)

    companion object {
        private const val SESSION_GAP_MS = 30L * 60 * 1000
    }
}
