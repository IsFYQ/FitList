package com.example.healthcheckin.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class DeviceTimeMonitor @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun recordServerTime(epochMs: Long) {
        prefs.edit()
            .putLong(KEY_LAST_SERVER_TIME_MS, epochMs)
            .putLong(KEY_RECORDED_AT_MS, System.currentTimeMillis())
            .apply()
    }

    fun recordServerTimeFromHttpDate(dateHeader: String) {
        parseHttpDate(dateHeader)?.let { recordServerTime(it) }
    }

    fun isDeviceTimeSuspicious(): Boolean {
        val lastServerTime = prefs.getLong(KEY_LAST_SERVER_TIME_MS, -1L)
        if (lastServerTime <= 0L) return false
        return abs(System.currentTimeMillis() - lastServerTime) > MAX_SKEW_MS
    }

    companion object {
        private const val PREFS_NAME = "device_time_prefs"
        private const val KEY_LAST_SERVER_TIME_MS = "last_server_time_ms"
        private const val KEY_RECORDED_AT_MS = "recorded_at_ms"
        private const val MAX_SKEW_MS = 24L * 60 * 60 * 1000

        private val HTTP_DATE_FORMATTER = DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.US)

        fun parseHttpDate(header: String): Long? = runCatching {
            ZonedDateTime.parse(header.trim(), HTTP_DATE_FORMATTER).toInstant().toEpochMilli()
        }.getOrNull()
    }
}
