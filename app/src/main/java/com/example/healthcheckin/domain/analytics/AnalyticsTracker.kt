package com.example.healthcheckin.domain.analytics

import kotlinx.coroutines.flow.Flow

interface AnalyticsTracker {
    fun track(eventName: String, params: Map<String, Any?> = emptyMap())

    fun isEnabled(): Flow<Boolean>

    suspend fun setEnabled(enabled: Boolean)

    suspend fun onUserLoggedIn(userId: String)

    suspend fun runMaintenance()
}

object AnalyticsEvents {
    const val APP_SESSION_START = "app_session_start"
    const val APP_SESSION_END = "app_session_end"
    const val SIGN_UP_SUCCEEDED = "sign_up_succeeded"
    const val SIGN_UP_FAILED = "sign_up_failed"
    const val SIGN_IN_SUCCEEDED = "sign_in_succeeded"
    const val SIGN_IN_FAILED = "sign_in_failed"
    const val ONBOARDING_COMPLETE = "onboarding_complete"
    const val GOAL_UPDATED = "goal_updated"
    const val DASHBOARD_VIEWED = "dashboard_viewed"
    const val MEAL_LOGGED = "meal_logged"
    const val MEAL_EDITED = "meal_edited"
    const val MEAL_DELETED = "meal_deleted"
    const val FOOD_SEARCH_PERFORMED = "food_search_performed"
    const val CUSTOM_FOOD_CREATED = "custom_food_created"
    const val WEIGHT_RECORDED = "weight_recorded"
    const val DATA_EXPORT_STARTED = "data_export_started"
    const val DATA_EXPORT_COMPLETED = "data_export_completed"
    const val DATA_EXPORT_FAILED = "data_export_failed"
    const val SYNC_BATCH_COMPLETED = "sync_batch_completed"
    const val SYNC_FAILED = "sync_failed"
    const val ANALYTICS_TOGGLE_CHANGED = "analytics_toggle_changed"
    const val SIGN_OUT = "sign_out"
    const val PASSWORD_CHANGED = "password_changed"
    const val ACCOUNT_DELETED = "account_deleted"
    const val SETTINGS_OPENED = "settings_opened"
    const val ABOUT_OPENED = "about_opened"
    const val THEME_CHANGED = "theme_changed"
    const val PASSWORD_RESET_REQUESTED = "password_reset_requested"
    const val PASSWORD_RESET_COMPLETED = "password_reset_completed"
    const val HEALTH_TIP_SHOWN = "health_tip_shown"
    const val HEALTH_TIP_DISMISSED = "health_tip_dismissed"
    const val HEALTH_TIP_ACTION_CLICKED = "health_tip_action_clicked"
    const val EXTERNAL_LINK_CLICKED = "external_link_clicked"
}

object AnalyticsSettingsKeys {
    const val ANALYTICS_ENABLED = "analytics_enabled"
}
