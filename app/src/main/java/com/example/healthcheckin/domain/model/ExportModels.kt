package com.example.healthcheckin.domain.model

import java.io.File

enum class ExportTable(val fileName: String, val progressLabel: String) {
    PROFILE("profile.csv", "档案"),
    GOALS("goals.csv", "目标"),
    DAILY_BUDGETS("daily_budgets.csv", "每日预算"),
    FOODS("foods.csv", "食物"),
    MEAL_ENTRIES("meal_entries.csv", "饮食记录"),
    WEIGHT_RECORDS("weight_records.csv", "体重记录"),
    BODY_MEASUREMENTS("body_measurements.csv", "围度记录"),
    MILESTONES("milestones.csv", "里程碑"),
    INVENTORY_ITEMS("inventory_items.csv", "食材库存"),
    INVENTORY_LEDGER("inventory_ledger.csv", "库存流水"),
    INGREDIENT_BINDINGS("ingredient_bindings.csv", "食材绑定"),
}

data class ExportProgress(
    val table: ExportTable? = null,
    val message: String = "",
)

data class ExportResult(
    val success: Boolean,
    val file: File? = null,
    val totalRows: Int = 0,
    val fileSizeKb: Int = 0,
    val elapsedMs: Long = 0,
    val isEmptyData: Boolean = false,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val cancelled: Boolean = false,
)
