package com.example.healthcheckin.data.preferences

import com.example.healthcheckin.data.local.dao.AppSettingDao
import com.example.healthcheckin.data.local.entity.AppSettingEntity
import com.example.healthcheckin.domain.analytics.AnalyticsEvents
import com.example.healthcheckin.domain.analytics.AnalyticsTracker
import com.example.healthcheckin.domain.model.ThemeMode
import com.example.healthcheckin.domain.model.ThemeSettings
import com.example.healthcheckin.util.DateTimeUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemePreferences @Inject constructor(
    private val appSettingDao: AppSettingDao,
    private val analyticsTracker: AnalyticsTracker,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    init {
        scope.launch { load() }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        if (_themeMode.value == mode) return
        val now = DateTimeUtil.nowEpochMillis()
        appSettingDao.upsert(
            AppSettingEntity(
                key = ThemeSettings.THEME_MODE,
                valueJson = mode.name,
                updatedAt = now,
            ),
        )
        _themeMode.value = mode
        analyticsTracker.track(
            AnalyticsEvents.THEME_CHANGED,
            mapOf("theme_mode" to mode.name),
        )
    }

    private suspend fun load() {
        val raw = appSettingDao.get(ThemeSettings.THEME_MODE)?.valueJson
        _themeMode.value = runCatching { ThemeMode.valueOf(raw ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
    }
}
