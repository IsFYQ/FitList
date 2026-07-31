package com.example.healthcheckin.util

object ValidationConstants {
    const val EMAIL_MAX_LENGTH = 254
    const val PASSWORD_MIN_LENGTH = 8
    const val PASSWORD_MAX_LENGTH = 64
    const val FOOD_NAME_MIN_LENGTH = 1
    const val FOOD_NAME_MAX_LENGTH = 50
    const val DESCRIPTION_MAX_LENGTH = 200
    const val NOTE_MAX_LENGTH = 100
    const val WEIGHT_MIN_KG = 25.0
    const val WEIGHT_MAX_KG = 300.0
    const val HEIGHT_MIN_CM = 100.0
    const val HEIGHT_MAX_CM = 250.0
    const val TARGET_WEEKS_MIN = 4
    const val TARGET_WEEKS_MAX = 52
    const val BMR_MIN_KCAL = 800
    const val BUDGET_MIN_KCAL = 1000
    const val BUDGET_MAX_KCAL = 6000
    const val SAFETY_FLOOR_MALE = 1500
    const val SAFETY_FLOOR_FEMALE = 1200
    const val LOSE_DELTA_CAP = 1000
    const val GAIN_DELTA_CAP = 500
    const val ENERGY_PER_KG = 7700.0
    const val GOAL_MAINTAIN_THRESHOLD = 0.5
    const val CARB_ABSOLUTE_MIN_G = 50.0
    const val MACRO_TOLERANCE_KCAL = 20
    const val LOGIN_FAIL_LOCK_COUNT = 5
    const val LOGIN_LOCK_SECONDS = 60
    const val BACKFILL_MAX_DAYS = 365
    const val AGE_MIN = 14
    const val AGE_MAX = 100
    const val WEIGHT_DIFF_CONFIRM_KG = 50.0
    const val BMR_AGE_UPDATE_THRESHOLD = 10
    const val DEFAULT_BIRTH_YEAR_MONTH = "1995-01"
    const val DEFAULT_TARGET_WEEKS = 12
}

object SyncState {
    const val PENDING = "PENDING"
    const val SYNCING = "SYNCING"
    const val SYNCED = "SYNCED"
    const val FAILED = "FAILED"
}

enum class Sex { MALE, FEMALE }

enum class ActivityLevel(val pal: Double) {
    SEDENTARY(1.200),
    LIGHT(1.375),
    MODERATE(1.550),
    ACTIVE(1.725),
    ATHLETE(1.900),
}

enum class GoalType { LOSE, MAINTAIN, GAIN }

enum class MealSlot { BREAKFAST, LUNCH, DINNER, SNACK }

enum class BasisUnit { G, ML }

enum class MealUnit { G, ML, SERVING }

enum class FoodSource { CUSTOM, PUBLIC, FATSECRET, OFF }

enum class MealEntrySource { SEARCH, RECENT, CUSTOM, RECOMMEND, OCR }

enum class CalorieState { NORMAL, WARN, OVER }
