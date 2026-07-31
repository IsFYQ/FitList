package com.example.healthcheckin.data.backup

enum class BackupTable(val tableName: String, val label: String) {
    PROFILES("profiles", "用户档案"),
    GOALS("goals", "目标设定"),
    DAILY_BUDGETS("daily_budgets", "每日预算"),
    FOODS("foods", "食物库"),
    MEAL_ENTRIES("meal_entries", "饮食记录"),
    WEIGHT_RECORDS("weight_records", "体重记录"),
    ANALYTICS_EVENTS("analytics_events", "埋点事件"),
    ;

    companion object {
        val uploadOrder = entries.toList()
    }
}

object BackupSettings {
    const val LAST_BACKUP_AT = "last_backup_at"
    const val LAST_RESTORE_AT = "last_restore_at"
}

object BackupRetryPolicy {
    private val BACKOFF_MS = listOf(5_000L, 15_000L, 60_000L, 300_000L, 900_000L)

    fun nextRetryAt(retryCount: Int, now: Long): Long? {
        if (retryCount >= BACKOFF_MS.size) return null
        return now + BACKOFF_MS[retryCount]
    }

    fun errorCode(httpCode: Int?): String = when (httpCode) {
        401 -> "E401"
        409 -> "E5001"
        in 400..499 -> "E5002"
        else -> "E5003"
    }
}
