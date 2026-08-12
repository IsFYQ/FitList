package com.example.healthcheckin.data.export

import android.util.JsonWriter
import com.example.healthcheckin.data.local.entity.BodyMeasurementEntity
import com.example.healthcheckin.data.local.entity.DailyBudgetEntity
import com.example.healthcheckin.data.local.entity.FoodEntity
import com.example.healthcheckin.data.local.entity.GoalEntity
import com.example.healthcheckin.data.local.entity.IngredientBindingEntity
import com.example.healthcheckin.data.local.entity.InventoryItemEntity
import com.example.healthcheckin.data.local.entity.InventoryLedgerEntity
import com.example.healthcheckin.data.local.entity.MealEntryEntity
import com.example.healthcheckin.data.local.entity.MilestoneEntity
import com.example.healthcheckin.data.local.entity.ProfileEntity
import com.example.healthcheckin.data.local.entity.WeightRecordEntity
import com.example.healthcheckin.util.DateTimeUtil
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipOutputStream

object ExportRowEncoder {

    private val csvDateTimeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val isoOffsetFormatter: DateTimeFormatter =
        DateTimeFormatter.ISO_OFFSET_DATE_TIME

    fun writeCsvBom(writer: BufferedWriter) {
        writer.write("\uFEFF")
    }

    fun escapeCsv(value: String?): String {
        if (value.isNullOrEmpty()) return ""
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuotes) return value
        return "\"${value.replace("\"", "\"\"")}\""
    }

    fun csvLine(vararg values: String?): String =
        values.joinToString(",") { escapeCsv(it) } + "\r\n"

    fun formatCsvTimestamp(epochMillis: Long?): String {
        if (epochMillis == null) return ""
        return DateTimeUtil.toLocalDateTime(epochMillis).format(csvDateTimeFormatter)
    }

    fun formatIsoTimestamp(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .format(isoOffsetFormatter)

    fun formatIsoNow(): String =
        OffsetDateTime.now(ZoneId.systemDefault()).format(isoOffsetFormatter)

    fun csvDouble(value: Double?): String = value?.toString() ?: ""

    fun csvInt(value: Int?): String = value?.toString() ?: ""

    fun csvBoolean(value: Boolean): String = if (value) "true" else "false"

    fun writeProfileCsvHeader(writer: BufferedWriter) {
        writer.write(
            csvLine(
                "id", "user_id", "email", "sex", "birth_year_month", "height_cm",
                "initial_weight_kg", "onboarding_completed_at", "registered_local_date",
                "device_id", "created_at", "updated_at", "tz_offset_minutes",
            ),
        )
    }

    fun writeProfileCsvRow(writer: BufferedWriter, entity: ProfileEntity) {
        val tz = DateTimeUtil.tzOffsetMinutes()
        writer.write(
            csvLine(
                entity.id,
                entity.userId,
                entity.email,
                entity.sex,
                entity.birthYearMonth,
                csvDouble(entity.heightCm),
                csvDouble(entity.initialWeightKg),
                formatCsvTimestamp(entity.onboardingCompletedAt),
                entity.registeredLocalDate,
                entity.deviceId,
                formatCsvTimestamp(entity.createdAt),
                formatCsvTimestamp(entity.updatedAt),
                tz.toString(),
            ),
        )
    }

    fun writeGoalsCsvHeader(writer: BufferedWriter) {
        writer.write(
            csvLine(
                "id", "user_id", "current_weight_kg", "target_weight_kg", "target_weeks",
                "activity_level", "goal_type", "bmr_kcal", "tdee_kcal", "daily_delta_kcal",
                "budget_kcal", "protein_g", "carb_g", "fat_g", "clamped", "est_weeks",
                "effective_from", "is_active", "device_id", "created_at", "updated_at",
                "tz_offset_minutes",
            ),
        )
    }

    fun writeGoalCsvRow(writer: BufferedWriter, entity: GoalEntity) {
        val tz = DateTimeUtil.tzOffsetMinutes()
        writer.write(
            csvLine(
                entity.id,
                entity.userId,
                csvDouble(entity.currentWeightKg),
                csvDouble(entity.targetWeightKg),
                entity.targetWeeks.toString(),
                entity.activityLevel,
                entity.goalType,
                entity.bmrKcal.toString(),
                entity.tdeeKcal.toString(),
                entity.dailyDeltaKcal.toString(),
                entity.budgetKcal.toString(),
                csvDouble(entity.proteinG),
                csvDouble(entity.carbG),
                csvDouble(entity.fatG),
                csvBoolean(entity.clamped),
                csvInt(entity.estWeeks),
                entity.effectiveFrom,
                csvBoolean(entity.isActive),
                entity.deviceId,
                formatCsvTimestamp(entity.createdAt),
                formatCsvTimestamp(entity.updatedAt),
                tz.toString(),
            ),
        )
    }

    fun writeDailyBudgetsCsvHeader(writer: BufferedWriter) {
        writer.write(
            csvLine(
                "id", "user_id", "local_date", "goal_id", "budget_kcal", "protein_g",
                "carb_g", "fat_g", "device_id", "created_at", "updated_at", "tz_offset_minutes",
            ),
        )
    }

    fun writeDailyBudgetCsvRow(writer: BufferedWriter, entity: DailyBudgetEntity) {
        val tz = DateTimeUtil.tzOffsetMinutes()
        writer.write(
            csvLine(
                entity.id,
                entity.userId,
                entity.localDate,
                entity.goalId,
                entity.budgetKcal.toString(),
                csvDouble(entity.proteinG),
                csvDouble(entity.carbG),
                csvDouble(entity.fatG),
                entity.deviceId,
                formatCsvTimestamp(entity.createdAt),
                formatCsvTimestamp(entity.updatedAt),
                tz.toString(),
            ),
        )
    }

    fun writeFoodsCsvHeader(writer: BufferedWriter) {
        writer.write(
            csvLine(
                "id", "user_id", "source", "external_id", "name", "name_normalized", "brand",
                "basis_unit", "kcal_per_100", "protein_per_100", "carb_per_100", "fat_per_100",
                "serving_name", "serving_grams", "data_incomplete", "nutrition_warning",
                "ingredient_key", "last_used_at", "use_count_30d", "last_quantity", "last_unit",
                "last_meal_slot", "device_id", "created_at", "updated_at", "tz_offset_minutes",
            ),
        )
    }

    fun writeFoodCsvRow(writer: BufferedWriter, entity: FoodEntity) {
        val tz = DateTimeUtil.tzOffsetMinutes()
        writer.write(
            csvLine(
                entity.id,
                entity.userId,
                entity.source,
                entity.externalId,
                entity.name,
                entity.nameNormalized,
                entity.brand,
                entity.basisUnit,
                csvDouble(entity.kcalPer100),
                csvDouble(entity.proteinPer100),
                csvDouble(entity.carbPer100),
                csvDouble(entity.fatPer100),
                entity.servingName,
                csvDouble(entity.servingGrams),
                csvBoolean(entity.dataIncomplete),
                csvBoolean(entity.nutritionWarning),
                entity.ingredientKey,
                formatCsvTimestamp(entity.lastUsedAt),
                entity.useCount30d.toString(),
                csvDouble(entity.lastQuantity),
                entity.lastUnit,
                entity.lastMealSlot,
                entity.deviceId,
                formatCsvTimestamp(entity.createdAt),
                formatCsvTimestamp(entity.updatedAt),
                tz.toString(),
            ),
        )
    }

    fun writeMealEntriesCsvHeader(writer: BufferedWriter) {
        writer.write(
            csvLine(
                "id", "user_id", "local_date", "tz_offset_minutes", "consumed_at", "meal_slot",
                "food_id", "quantity", "unit", "basis_amount", "snap_food_name", "snap_brand",
                "snap_source", "snap_basis_unit", "snap_kcal_per_100", "snap_protein_per_100",
                "snap_carb_per_100", "snap_fat_per_100", "snap_serving_name", "snap_serving_grams",
                "kcal", "protein_g", "carb_g", "fat_g", "from_inventory", "inventory_item_id",
                "inventory_deducted_amount", "entry_source", "device_id", "created_at", "updated_at",
            ),
        )
    }

    fun writeMealEntryCsvRow(writer: BufferedWriter, entity: MealEntryEntity) {
        writer.write(
            csvLine(
                entity.id,
                entity.userId,
                entity.localDate,
                entity.tzOffsetMinutes.toString(),
                formatCsvTimestamp(entity.consumedAt),
                entity.mealSlot,
                entity.foodId,
                csvDouble(entity.quantity),
                entity.unit,
                csvDouble(entity.basisAmount),
                entity.snapFoodName,
                entity.snapBrand,
                entity.snapSource,
                entity.snapBasisUnit,
                csvDouble(entity.snapKcalPer100),
                csvDouble(entity.snapProteinPer100),
                csvDouble(entity.snapCarbPer100),
                csvDouble(entity.snapFatPer100),
                entity.snapServingName,
                csvDouble(entity.snapServingGrams),
                csvDouble(entity.kcal),
                csvDouble(entity.proteinG),
                csvDouble(entity.carbG),
                csvDouble(entity.fatG),
                csvBoolean(entity.fromInventory),
                entity.inventoryItemId,
                csvDouble(entity.inventoryDeductedAmount),
                entity.entrySource,
                entity.deviceId,
                formatCsvTimestamp(entity.createdAt),
                formatCsvTimestamp(entity.updatedAt),
            ),
        )
    }

    fun writeWeightRecordsCsvHeader(writer: BufferedWriter) {
        writer.write(
            csvLine(
                "id", "user_id", "local_date", "tz_offset_minutes", "weight_kg", "note",
                "device_id", "created_at", "updated_at",
            ),
        )
    }

    fun writeWeightRecordCsvRow(writer: BufferedWriter, entity: WeightRecordEntity) {
        writer.write(
            csvLine(
                entity.id,
                entity.userId,
                entity.localDate,
                entity.tzOffsetMinutes.toString(),
                csvDouble(entity.weightKg),
                entity.note,
                entity.deviceId,
                formatCsvTimestamp(entity.createdAt),
                formatCsvTimestamp(entity.updatedAt),
            ),
        )
    }

    fun writeBodyMeasurementsCsvHeader(writer: BufferedWriter) {
        writer.write(
            csvLine(
                "id", "user_id", "metric", "local_date", "tz_offset_minutes", "value_cm",
                "device_id", "created_at", "updated_at",
            ),
        )
    }

    fun writeBodyMeasurementCsvRow(writer: BufferedWriter, entity: BodyMeasurementEntity) {
        writer.write(
            csvLine(
                entity.id, entity.userId, entity.metric, entity.localDate, entity.tzOffsetMinutes.toString(),
                csvDouble(entity.valueCm), entity.deviceId, formatCsvTimestamp(entity.createdAt),
                formatCsvTimestamp(entity.updatedAt),
            ),
        )
    }

    fun writeMilestonesCsvHeader(writer: BufferedWriter) {
        writer.write(
            csvLine(
                "id", "user_id", "title", "target_weight_kg", "reward_text", "achieved_at",
                "achieved_weight_kg", "days_elapsed", "shared_count", "device_id", "created_at", "updated_at",
            ),
        )
    }

    fun writeMilestoneCsvRow(writer: BufferedWriter, entity: MilestoneEntity) {
        writer.write(
            csvLine(
                entity.id, entity.userId, entity.title, csvDouble(entity.targetWeightKg), entity.rewardText,
                formatCsvTimestamp(entity.achievedAt), csvDouble(entity.achievedWeightKg),
                csvInt(entity.daysElapsed), entity.sharedCount.toString(), entity.deviceId,
                formatCsvTimestamp(entity.createdAt), formatCsvTimestamp(entity.updatedAt),
            ),
        )
    }

    fun writeInventoryItemsCsvHeader(writer: BufferedWriter) {
        writer.write(
            csvLine(
                "id", "user_id", "name", "name_normalized", "ingredient_key", "category",
                "initial_amount", "remaining_amount", "unit", "piece_grams", "purchase_date",
                "expiry_date", "unit_price", "version", "entry_source", "raw_text", "device_id",
                "created_at", "updated_at",
            ),
        )
    }

    fun writeInventoryItemCsvRow(writer: BufferedWriter, entity: InventoryItemEntity) {
        writer.write(
            csvLine(
                entity.id, entity.userId, entity.name, entity.nameNormalized, entity.ingredientKey,
                entity.category, csvDouble(entity.initialAmount), csvDouble(entity.remainingAmount), entity.unit,
                csvDouble(entity.pieceGrams), entity.purchaseDate, entity.expiryDate, csvDouble(entity.unitPrice),
                entity.version.toString(), entity.entrySource, entity.rawText, entity.deviceId,
                formatCsvTimestamp(entity.createdAt), formatCsvTimestamp(entity.updatedAt),
            ),
        )
    }

    fun writeInventoryLedgerCsvHeader(writer: BufferedWriter) {
        writer.write(
            csvLine(
                "id", "user_id", "inventory_item_id", "change_type", "delta_amount",
                "balance_after", "ref_meal_entry_id", "note", "device_id", "created_at", "updated_at",
            ),
        )
    }

    fun writeInventoryLedgerCsvRow(writer: BufferedWriter, entity: InventoryLedgerEntity) {
        writer.write(
            csvLine(
                entity.id, entity.userId, entity.inventoryItemId, entity.changeType,
                csvDouble(entity.deltaAmount), csvDouble(entity.balanceAfter), entity.refMealEntryId,
                entity.note, entity.deviceId, formatCsvTimestamp(entity.createdAt),
                formatCsvTimestamp(entity.updatedAt),
            ),
        )
    }

    fun writeIngredientBindingsCsvHeader(writer: BufferedWriter) {
        writer.write(
            csvLine(
                "id", "user_id", "food_id", "inventory_item_id", "device_id", "created_at", "updated_at",
            ),
        )
    }

    fun writeIngredientBindingCsvRow(writer: BufferedWriter, entity: IngredientBindingEntity) {
        writer.write(
            csvLine(
                entity.id, entity.userId, entity.foodId, entity.inventoryItemId, entity.deviceId,
                formatCsvTimestamp(entity.createdAt), formatCsvTimestamp(entity.updatedAt),
            ),
        )
    }

    fun writeProfileJson(writer: JsonWriter, entity: ProfileEntity?) {
        if (entity == null) {
            writer.nullValue()
            return
        }
        writer.beginObject()
        writer.name("id").value(entity.id)
        writer.name("user_id").value(entity.userId)
        writer.name("email").value(entity.email)
        writeNullableString(writer, "sex", entity.sex)
        writeNullableString(writer, "birth_year_month", entity.birthYearMonth)
        writeNullableNumber(writer, "height_cm", entity.heightCm)
        writeNullableNumber(writer, "initial_weight_kg", entity.initialWeightKg)
        writeNullableTimestamp(writer, "onboarding_completed_at", entity.onboardingCompletedAt)
        writer.name("registered_local_date").value(entity.registeredLocalDate)
        writer.name("device_id").value(entity.deviceId)
        writeTimestamp(writer, "created_at", entity.createdAt)
        writeTimestamp(writer, "updated_at", entity.updatedAt)
        writer.endObject()
    }

    fun writeGoalJson(writer: JsonWriter, entity: GoalEntity) {
        writer.beginObject()
        writer.name("id").value(entity.id)
        writer.name("user_id").value(entity.userId)
        writer.name("current_weight_kg").value(entity.currentWeightKg)
        writer.name("target_weight_kg").value(entity.targetWeightKg)
        writer.name("target_weeks").value(entity.targetWeeks)
        writer.name("activity_level").value(entity.activityLevel)
        writer.name("goal_type").value(entity.goalType)
        writer.name("bmr_kcal").value(entity.bmrKcal)
        writer.name("tdee_kcal").value(entity.tdeeKcal)
        writer.name("daily_delta_kcal").value(entity.dailyDeltaKcal)
        writer.name("budget_kcal").value(entity.budgetKcal)
        writer.name("protein_g").value(entity.proteinG)
        writer.name("carb_g").value(entity.carbG)
        writer.name("fat_g").value(entity.fatG)
        writer.name("clamped").value(entity.clamped)
        writeNullableInt(writer, "est_weeks", entity.estWeeks)
        writer.name("effective_from").value(entity.effectiveFrom)
        writer.name("is_active").value(entity.isActive)
        writer.name("device_id").value(entity.deviceId)
        writeTimestamp(writer, "created_at", entity.createdAt)
        writeTimestamp(writer, "updated_at", entity.updatedAt)
        writer.endObject()
    }

    fun writeDailyBudgetJson(writer: JsonWriter, entity: DailyBudgetEntity) {
        writer.beginObject()
        writer.name("id").value(entity.id)
        writer.name("user_id").value(entity.userId)
        writer.name("local_date").value(entity.localDate)
        writer.name("goal_id").value(entity.goalId)
        writer.name("budget_kcal").value(entity.budgetKcal)
        writer.name("protein_g").value(entity.proteinG)
        writer.name("carb_g").value(entity.carbG)
        writer.name("fat_g").value(entity.fatG)
        writer.name("device_id").value(entity.deviceId)
        writeTimestamp(writer, "created_at", entity.createdAt)
        writeTimestamp(writer, "updated_at", entity.updatedAt)
        writer.endObject()
    }

    fun writeFoodJson(writer: JsonWriter, entity: FoodEntity) {
        writer.beginObject()
        writer.name("id").value(entity.id)
        writeNullableString(writer, "user_id", entity.userId)
        writer.name("source").value(entity.source)
        writeNullableString(writer, "external_id", entity.externalId)
        writer.name("name").value(entity.name)
        writer.name("name_normalized").value(entity.nameNormalized)
        writeNullableString(writer, "brand", entity.brand)
        writer.name("basis_unit").value(entity.basisUnit)
        writer.name("kcal_per_100").value(entity.kcalPer100)
        writeNullableNumber(writer, "protein_per_100", entity.proteinPer100)
        writeNullableNumber(writer, "carb_per_100", entity.carbPer100)
        writeNullableNumber(writer, "fat_per_100", entity.fatPer100)
        writeNullableString(writer, "serving_name", entity.servingName)
        writeNullableNumber(writer, "serving_grams", entity.servingGrams)
        writer.name("data_incomplete").value(entity.dataIncomplete)
        writer.name("nutrition_warning").value(entity.nutritionWarning)
        writeNullableString(writer, "ingredient_key", entity.ingredientKey)
        writeNullableTimestamp(writer, "last_used_at", entity.lastUsedAt)
        writer.name("use_count_30d").value(entity.useCount30d)
        writeNullableNumber(writer, "last_quantity", entity.lastQuantity)
        writeNullableString(writer, "last_unit", entity.lastUnit)
        writeNullableString(writer, "last_meal_slot", entity.lastMealSlot)
        writer.name("device_id").value(entity.deviceId)
        writeTimestamp(writer, "created_at", entity.createdAt)
        writeTimestamp(writer, "updated_at", entity.updatedAt)
        writer.endObject()
    }

    fun writeMealEntryJson(writer: JsonWriter, entity: MealEntryEntity) {
        writer.beginObject()
        writer.name("id").value(entity.id)
        writer.name("user_id").value(entity.userId)
        writer.name("local_date").value(entity.localDate)
        writer.name("tz_offset_minutes").value(entity.tzOffsetMinutes)
        writeTimestamp(writer, "consumed_at", entity.consumedAt)
        writer.name("meal_slot").value(entity.mealSlot)
        writeNullableString(writer, "food_id", entity.foodId)
        writer.name("quantity").value(entity.quantity)
        writer.name("unit").value(entity.unit)
        writer.name("basis_amount").value(entity.basisAmount)
        writer.name("snap_food_name").value(entity.snapFoodName)
        writeNullableString(writer, "snap_brand", entity.snapBrand)
        writer.name("snap_source").value(entity.snapSource)
        writer.name("snap_basis_unit").value(entity.snapBasisUnit)
        writer.name("snap_kcal_per_100").value(entity.snapKcalPer100)
        writeNullableNumber(writer, "snap_protein_per_100", entity.snapProteinPer100)
        writeNullableNumber(writer, "snap_carb_per_100", entity.snapCarbPer100)
        writeNullableNumber(writer, "snap_fat_per_100", entity.snapFatPer100)
        writeNullableString(writer, "snap_serving_name", entity.snapServingName)
        writeNullableNumber(writer, "snap_serving_grams", entity.snapServingGrams)
        writer.name("kcal").value(entity.kcal)
        writeNullableNumber(writer, "protein_g", entity.proteinG)
        writeNullableNumber(writer, "carb_g", entity.carbG)
        writeNullableNumber(writer, "fat_g", entity.fatG)
        writer.name("from_inventory").value(entity.fromInventory)
        writeNullableString(writer, "inventory_item_id", entity.inventoryItemId)
        writeNullableNumber(writer, "inventory_deducted_amount", entity.inventoryDeductedAmount)
        writer.name("entry_source").value(entity.entrySource)
        writer.name("device_id").value(entity.deviceId)
        writeTimestamp(writer, "created_at", entity.createdAt)
        writeTimestamp(writer, "updated_at", entity.updatedAt)
        writer.endObject()
    }

    fun writeWeightRecordJson(writer: JsonWriter, entity: WeightRecordEntity) {
        writer.beginObject()
        writer.name("id").value(entity.id)
        writer.name("user_id").value(entity.userId)
        writer.name("local_date").value(entity.localDate)
        writer.name("tz_offset_minutes").value(entity.tzOffsetMinutes)
        writer.name("weight_kg").value(entity.weightKg)
        writeNullableString(writer, "note", entity.note)
        writer.name("device_id").value(entity.deviceId)
        writeTimestamp(writer, "created_at", entity.createdAt)
        writeTimestamp(writer, "updated_at", entity.updatedAt)
        writer.endObject()
    }

    fun writeBodyMeasurementJson(writer: JsonWriter, entity: BodyMeasurementEntity) {
        writer.beginObject()
        writer.name("id").value(entity.id)
        writer.name("user_id").value(entity.userId)
        writer.name("metric").value(entity.metric)
        writer.name("local_date").value(entity.localDate)
        writer.name("tz_offset_minutes").value(entity.tzOffsetMinutes)
        writer.name("value_cm").value(entity.valueCm)
        writer.name("device_id").value(entity.deviceId)
        writeTimestamp(writer, "created_at", entity.createdAt)
        writeTimestamp(writer, "updated_at", entity.updatedAt)
        writer.endObject()
    }

    fun writeMilestoneJson(writer: JsonWriter, entity: MilestoneEntity) {
        writer.beginObject()
        writer.name("id").value(entity.id)
        writer.name("user_id").value(entity.userId)
        writer.name("title").value(entity.title)
        writer.name("target_weight_kg").value(entity.targetWeightKg)
        writeNullableString(writer, "reward_text", entity.rewardText)
        writeNullableTimestamp(writer, "achieved_at", entity.achievedAt)
        writeNullableNumber(writer, "achieved_weight_kg", entity.achievedWeightKg)
        writeNullableInt(writer, "days_elapsed", entity.daysElapsed)
        writer.name("shared_count").value(entity.sharedCount)
        writer.name("device_id").value(entity.deviceId)
        writeTimestamp(writer, "created_at", entity.createdAt)
        writeTimestamp(writer, "updated_at", entity.updatedAt)
        writer.endObject()
    }

    fun writeInventoryItemJson(writer: JsonWriter, entity: InventoryItemEntity) {
        writer.beginObject()
        writer.name("id").value(entity.id)
        writer.name("user_id").value(entity.userId)
        writer.name("name").value(entity.name)
        writer.name("name_normalized").value(entity.nameNormalized)
        writeNullableString(writer, "ingredient_key", entity.ingredientKey)
        writer.name("category").value(entity.category)
        writer.name("initial_amount").value(entity.initialAmount)
        writer.name("remaining_amount").value(entity.remainingAmount)
        writer.name("unit").value(entity.unit)
        writeNullableNumber(writer, "piece_grams", entity.pieceGrams)
        writer.name("purchase_date").value(entity.purchaseDate)
        writeNullableString(writer, "expiry_date", entity.expiryDate)
        writeNullableNumber(writer, "unit_price", entity.unitPrice)
        writer.name("version").value(entity.version)
        writer.name("entry_source").value(entity.entrySource)
        writeNullableString(writer, "raw_text", entity.rawText)
        writer.name("device_id").value(entity.deviceId)
        writeTimestamp(writer, "created_at", entity.createdAt)
        writeTimestamp(writer, "updated_at", entity.updatedAt)
        writer.endObject()
    }

    fun writeInventoryLedgerJson(writer: JsonWriter, entity: InventoryLedgerEntity) {
        writer.beginObject()
        writer.name("id").value(entity.id)
        writer.name("user_id").value(entity.userId)
        writer.name("inventory_item_id").value(entity.inventoryItemId)
        writer.name("change_type").value(entity.changeType)
        writer.name("delta_amount").value(entity.deltaAmount)
        writer.name("balance_after").value(entity.balanceAfter)
        writeNullableString(writer, "ref_meal_entry_id", entity.refMealEntryId)
        writeNullableString(writer, "note", entity.note)
        writer.name("device_id").value(entity.deviceId)
        writeTimestamp(writer, "created_at", entity.createdAt)
        writeTimestamp(writer, "updated_at", entity.updatedAt)
        writer.endObject()
    }

    fun writeIngredientBindingJson(writer: JsonWriter, entity: IngredientBindingEntity) {
        writer.beginObject()
        writer.name("id").value(entity.id)
        writer.name("user_id").value(entity.userId)
        writer.name("food_id").value(entity.foodId)
        writer.name("inventory_item_id").value(entity.inventoryItemId)
        writer.name("device_id").value(entity.deviceId)
        writeTimestamp(writer, "created_at", entity.createdAt)
        writeTimestamp(writer, "updated_at", entity.updatedAt)
        writer.endObject()
    }

    fun openCsvWriter(zipOut: ZipOutputStream, entryName: String): BufferedWriter {
        zipOut.putNextEntry(java.util.zip.ZipEntry(entryName))
        val writer = BufferedWriter(
            OutputStreamWriter(zipOut, StandardCharsets.UTF_8),
        )
        writeCsvBom(writer)
        return writer
    }

    private fun writeNullableString(writer: JsonWriter, name: String, value: String?) {
        writer.name(name)
        if (value == null) writer.nullValue() else writer.value(value)
    }

    private fun writeNullableNumber(writer: JsonWriter, name: String, value: Double?) {
        writer.name(name)
        if (value == null) writer.nullValue() else writer.value(value)
    }

    private fun writeNullableInt(writer: JsonWriter, name: String, value: Int?) {
        writer.name(name)
        if (value == null) writer.nullValue() else writer.value(value)
    }

    private fun writeTimestamp(writer: JsonWriter, name: String, epochMillis: Long) {
        writer.name(name).value(formatIsoTimestamp(epochMillis))
    }

    private fun writeNullableTimestamp(writer: JsonWriter, name: String, epochMillis: Long?) {
        writer.name(name)
        if (epochMillis == null) writer.nullValue() else writer.value(formatIsoTimestamp(epochMillis))
    }
}
