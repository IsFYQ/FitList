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

enum class BodyMetric(val labelZh: String) {
    WAIST("\u8170\u56F4"),
    HIP("\u81C0\u56F4"),
    THIGH("\u5927\u817F\u56F4"),
    UPPER_ARM("\u4E0A\u81C2\u56F4"),
    CHEST("\u80F8\u56F4"),
}

enum class InventoryCategory(val labelZh: String) {
    VEGETABLE("\u852C\u83DC"),
    MEAT("\u8089\u7C7B"),
    STAPLE("\u4E3B\u98DF"),
    DAIRY("\u4E73\u5236\u54C1"),
    SEASONING("\u8C03\u5473\u54C1"),
    OTHER("\u5176\u4ED6"),
}

enum class InventoryUnit(val labelZh: String) {
    G("g"),
    KG("kg"),
    ML("ml"),
    L("L"),
    PIECE("\u4E2A"),
}

enum class InventoryExpiryStatus {
    NORMAL,
    NEAR_EXPIRY,
    EXPIRED,
}

enum class InventoryChangeType {
    CREATE,
    MEAL_DEDUCT,
    MEAL_REVERT,
    MANUAL_ADJUST,
    DISCARD,
}

enum class InventorySortMode {
    BY_CATEGORY,
    BY_EXPIRY,
    BY_RECENT,
}

enum class InventoryMatchLevel {
    L1,
    L2,
    L3,
    NONE,
}

enum class InventoryDeductResolution {
    DEDUCT_REMAINING,
    MANUAL,
    SKIP,
}

enum class ExerciseType(val labelZh: String, val defaultMet: Double) {
    RUNNING("跑步", 8.0),
    BRISK_WALKING("快走", 4.3),
    CYCLING("骑行", 6.8),
    SWIMMING("游泳", 7.0),
    STRENGTH("力量训练", 5.0),
    YOGA("瑜伽", 2.5),
    CUSTOM("自定义", 4.0),
}

object P2ValidationConstants {
    const val EXERCISE_DURATION_MIN = 1
    const val EXERCISE_DURATION_MAX = 600
    const val EXERCISE_MET_MIN = 1.0
    const val EXERCISE_MET_MAX = 20.0
    const val EXERCISE_CUSTOM_NAME_MAX = 20
    const val RECOMMEND_SWAP_MAX = 5
    const val RECOMMEND_GAP_MIN_KCAL = 100
}

object P1ValidationConstants {
    const val BODY_METRIC_MIN_CM = 20.0
    const val BODY_METRIC_MAX_CM = 200.0
    const val BODY_METRIC_DIFF_CONFIRM_CM = 10.0
    const val MILESTONE_TITLE_MAX = 30
    const val MILESTONE_REWARD_MAX = 100
    const val MILESTONE_ACTIVE_MAX = 10
    const val INVENTORY_NAME_MAX = 50
    const val INVENTORY_AMOUNT_MAX = 100_000.0
    const val INVENTORY_PIECE_GRAMS_MAX = 10_000.0
    const val INVENTORY_PRICE_MAX = 100_000.0
    const val INVENTORY_PURCHASE_MAX_DAYS_BACK = 365
    const val NEAR_EXPIRY_DAYS = 3
}
