package com.example.healthcheckin.data.backup

import com.example.healthcheckin.data.local.entity.AnalyticsEventEntity
import com.example.healthcheckin.data.local.entity.DailyBudgetEntity
import com.example.healthcheckin.data.local.entity.FoodEntity
import com.example.healthcheckin.data.local.entity.GoalEntity
import com.example.healthcheckin.data.local.entity.MealEntryEntity
import com.example.healthcheckin.data.local.entity.ProfileEntity
import com.example.healthcheckin.data.local.entity.WeightRecordEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object SupabaseRowMapper {

    private val isoFormatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    fun encode(table: BackupTable, rows: List<Any>): String {
        val array = buildJsonArray {
            rows.forEach { row ->
                add(encodeRow(table, row))
            }
        }
        return array.toString()
    }

    fun decode(table: BackupTable, payload: String, json: Json): List<Any> {
        val array = json.parseToJsonElement(payload).jsonArray
        return array.map { element ->
            when (table) {
                BackupTable.PROFILES -> decodeProfile(element.jsonObject)
                BackupTable.GOALS -> decodeGoal(element.jsonObject)
                BackupTable.DAILY_BUDGETS -> decodeDailyBudget(element.jsonObject)
                BackupTable.FOODS -> decodeFood(element.jsonObject)
                BackupTable.MEAL_ENTRIES -> decodeMealEntry(element.jsonObject)
                BackupTable.WEIGHT_RECORDS -> decodeWeightRecord(element.jsonObject)
                BackupTable.ANALYTICS_EVENTS -> decodeAnalyticsEvent(element.jsonObject)
            }
        }
    }

    private fun encodeRow(table: BackupTable, row: Any): JsonObject = when (table) {
        BackupTable.PROFILES -> encodeProfile(row as ProfileEntity)
        BackupTable.GOALS -> encodeGoal(row as GoalEntity)
        BackupTable.DAILY_BUDGETS -> encodeDailyBudget(row as DailyBudgetEntity)
        BackupTable.FOODS -> encodeFood(row as FoodEntity)
        BackupTable.MEAL_ENTRIES -> encodeMealEntry(row as MealEntryEntity)
        BackupTable.WEIGHT_RECORDS -> encodeWeightRecord(row as WeightRecordEntity)
        BackupTable.ANALYTICS_EVENTS -> encodeAnalyticsEvent(row as AnalyticsEventEntity)
    }

    private fun encodeProfile(entity: ProfileEntity) = buildJsonObject {
        put("id", entity.id)
        put("user_id", entity.userId)
        put("email", entity.email)
        putNullable("sex", entity.sex)
        putNullable("birth_year_month", entity.birthYearMonth)
        putNullableNumber("height_cm", entity.heightCm)
        putNullableNumber("initial_weight_kg", entity.initialWeightKg)
        putNullableTimestamp("onboarding_completed_at", entity.onboardingCompletedAt)
        put("registered_local_date", entity.registeredLocalDate)
        put("device_id", entity.deviceId)
        putTimestamp("created_at", entity.createdAt)
        putTimestamp("updated_at", entity.updatedAt)
        putNullableTimestamp("deleted_at", entity.deletedAt)
    }

    private fun decodeProfile(obj: JsonObject) = ProfileEntity(
        id = obj.string("id"),
        userId = obj.string("user_id"),
        email = obj.string("email"),
        sex = obj.stringOrNull("sex"),
        birthYearMonth = obj.stringOrNull("birth_year_month"),
        heightCm = obj.doubleOrNull("height_cm"),
        initialWeightKg = obj.doubleOrNull("initial_weight_kg"),
        onboardingCompletedAt = obj.timestampOrNull("onboarding_completed_at"),
        registeredLocalDate = obj.string("registered_local_date"),
        deviceId = obj.string("device_id"),
        createdAt = obj.timestamp("created_at"),
        updatedAt = obj.timestamp("updated_at"),
        deletedAt = obj.timestampOrNull("deleted_at"),
        syncState = "SYNCED",
    )

    private fun encodeGoal(entity: GoalEntity) = buildJsonObject {
        put("id", entity.id)
        put("user_id", entity.userId)
        putNumber("current_weight_kg", entity.currentWeightKg)
        putNumber("target_weight_kg", entity.targetWeightKg)
        put("target_weeks", entity.targetWeeks)
        put("activity_level", entity.activityLevel)
        put("goal_type", entity.goalType)
        put("bmr_kcal", entity.bmrKcal)
        put("tdee_kcal", entity.tdeeKcal)
        put("daily_delta_kcal", entity.dailyDeltaKcal)
        put("budget_kcal", entity.budgetKcal)
        putNumber("protein_g", entity.proteinG)
        putNumber("carb_g", entity.carbG)
        putNumber("fat_g", entity.fatG)
        put("clamped", entity.clamped)
        putNullable("est_weeks", entity.estWeeks)
        put("effective_from", entity.effectiveFrom)
        put("is_active", entity.isActive)
        put("device_id", entity.deviceId)
        putTimestamp("created_at", entity.createdAt)
        putTimestamp("updated_at", entity.updatedAt)
        putNullableTimestamp("deleted_at", entity.deletedAt)
    }

    private fun decodeGoal(obj: JsonObject) = GoalEntity(
        id = obj.string("id"),
        userId = obj.string("user_id"),
        currentWeightKg = obj.double("current_weight_kg"),
        targetWeightKg = obj.double("target_weight_kg"),
        targetWeeks = obj.int("target_weeks"),
        activityLevel = obj.string("activity_level"),
        goalType = obj.string("goal_type"),
        bmrKcal = obj.int("bmr_kcal"),
        tdeeKcal = obj.int("tdee_kcal"),
        dailyDeltaKcal = obj.int("daily_delta_kcal"),
        budgetKcal = obj.int("budget_kcal"),
        proteinG = obj.double("protein_g"),
        carbG = obj.double("carb_g"),
        fatG = obj.double("fat_g"),
        clamped = obj.boolean("clamped"),
        estWeeks = obj.intOrNull("est_weeks"),
        effectiveFrom = obj.string("effective_from"),
        isActive = obj.boolean("is_active"),
        deviceId = obj.string("device_id"),
        createdAt = obj.timestamp("created_at"),
        updatedAt = obj.timestamp("updated_at"),
        deletedAt = obj.timestampOrNull("deleted_at"),
        syncState = "SYNCED",
    )

    private fun encodeDailyBudget(entity: DailyBudgetEntity) = buildJsonObject {
        put("id", entity.id)
        put("user_id", entity.userId)
        put("local_date", entity.localDate)
        put("goal_id", entity.goalId)
        put("budget_kcal", entity.budgetKcal)
        putNumber("protein_g", entity.proteinG)
        putNumber("carb_g", entity.carbG)
        putNumber("fat_g", entity.fatG)
        put("device_id", entity.deviceId)
        putTimestamp("created_at", entity.createdAt)
        putTimestamp("updated_at", entity.updatedAt)
        putNullableTimestamp("deleted_at", entity.deletedAt)
    }

    private fun decodeDailyBudget(obj: JsonObject) = DailyBudgetEntity(
        id = obj.string("id"),
        userId = obj.string("user_id"),
        localDate = obj.string("local_date"),
        goalId = obj.string("goal_id"),
        budgetKcal = obj.int("budget_kcal"),
        proteinG = obj.double("protein_g"),
        carbG = obj.double("carb_g"),
        fatG = obj.double("fat_g"),
        deviceId = obj.string("device_id"),
        createdAt = obj.timestamp("created_at"),
        updatedAt = obj.timestamp("updated_at"),
        deletedAt = obj.timestampOrNull("deleted_at"),
        syncState = "SYNCED",
    )

    private fun encodeFood(entity: FoodEntity) = buildJsonObject {
        put("id", entity.id)
        putNullable("user_id", entity.userId)
        put("source", entity.source)
        putNullable("external_id", entity.externalId)
        put("name", entity.name)
        put("name_normalized", entity.nameNormalized)
        putNullable("brand", entity.brand)
        put("basis_unit", entity.basisUnit)
        putNumber("kcal_per_100", entity.kcalPer100)
        putNullableNumber("protein_per_100", entity.proteinPer100)
        putNullableNumber("carb_per_100", entity.carbPer100)
        putNullableNumber("fat_per_100", entity.fatPer100)
        putNullable("serving_name", entity.servingName)
        putNullableNumber("serving_grams", entity.servingGrams)
        put("data_incomplete", entity.dataIncomplete)
        put("nutrition_warning", entity.nutritionWarning)
        putNullable("ingredient_key", entity.ingredientKey)
        putNullableTimestamp("last_used_at", entity.lastUsedAt)
        put("use_count_30d", entity.useCount30d)
        putNullableNumber("last_quantity", entity.lastQuantity)
        putNullable("last_unit", entity.lastUnit)
        putNullable("last_meal_slot", entity.lastMealSlot)
        put("device_id", entity.deviceId)
        putTimestamp("created_at", entity.createdAt)
        putTimestamp("updated_at", entity.updatedAt)
        putNullableTimestamp("deleted_at", entity.deletedAt)
    }

    private fun decodeFood(obj: JsonObject) = FoodEntity(
        id = obj.string("id"),
        userId = obj.stringOrNull("user_id"),
        source = obj.string("source"),
        externalId = obj.stringOrNull("external_id"),
        name = obj.string("name"),
        nameNormalized = obj.string("name_normalized"),
        brand = obj.stringOrNull("brand"),
        basisUnit = obj.string("basis_unit"),
        kcalPer100 = obj.double("kcal_per_100"),
        proteinPer100 = obj.doubleOrNull("protein_per_100"),
        carbPer100 = obj.doubleOrNull("carb_per_100"),
        fatPer100 = obj.doubleOrNull("fat_per_100"),
        servingName = obj.stringOrNull("serving_name"),
        servingGrams = obj.doubleOrNull("serving_grams"),
        dataIncomplete = obj.boolean("data_incomplete"),
        nutritionWarning = obj.boolean("nutrition_warning"),
        ingredientKey = obj.stringOrNull("ingredient_key"),
        lastUsedAt = obj.timestampOrNull("last_used_at"),
        useCount30d = obj.intOrNull("use_count_30d") ?: 0,
        lastQuantity = obj.doubleOrNull("last_quantity"),
        lastUnit = obj.stringOrNull("last_unit"),
        lastMealSlot = obj.stringOrNull("last_meal_slot"),
        deviceId = obj.string("device_id"),
        createdAt = obj.timestamp("created_at"),
        updatedAt = obj.timestamp("updated_at"),
        deletedAt = obj.timestampOrNull("deleted_at"),
        syncState = "SYNCED",
    )

    private fun encodeMealEntry(entity: MealEntryEntity) = buildJsonObject {
        put("id", entity.id)
        put("user_id", entity.userId)
        put("local_date", entity.localDate)
        put("tz_offset_minutes", entity.tzOffsetMinutes)
        putTimestamp("consumed_at", entity.consumedAt)
        put("meal_slot", entity.mealSlot)
        putNullable("food_id", entity.foodId)
        putNumber("quantity", entity.quantity)
        put("unit", entity.unit)
        putNumber("basis_amount", entity.basisAmount)
        put("snap_food_name", entity.snapFoodName)
        putNullable("snap_brand", entity.snapBrand)
        put("snap_source", entity.snapSource)
        put("snap_basis_unit", entity.snapBasisUnit)
        putNumber("snap_kcal_per_100", entity.snapKcalPer100)
        putNullableNumber("snap_protein_per_100", entity.snapProteinPer100)
        putNullableNumber("snap_carb_per_100", entity.snapCarbPer100)
        putNullableNumber("snap_fat_per_100", entity.snapFatPer100)
        putNullable("snap_serving_name", entity.snapServingName)
        putNullableNumber("snap_serving_grams", entity.snapServingGrams)
        putNumber("kcal", entity.kcal)
        putNullableNumber("protein_g", entity.proteinG)
        putNullableNumber("carb_g", entity.carbG)
        putNullableNumber("fat_g", entity.fatG)
        put("from_inventory", entity.fromInventory)
        putNullable("inventory_item_id", entity.inventoryItemId)
        putNullableNumber("inventory_deducted_amount", entity.inventoryDeductedAmount)
        put("entry_source", entity.entrySource)
        put("device_id", entity.deviceId)
        putTimestamp("created_at", entity.createdAt)
        putTimestamp("updated_at", entity.updatedAt)
        putNullableTimestamp("deleted_at", entity.deletedAt)
    }

    private fun decodeMealEntry(obj: JsonObject) = MealEntryEntity(
        id = obj.string("id"),
        userId = obj.string("user_id"),
        localDate = obj.string("local_date"),
        tzOffsetMinutes = obj.int("tz_offset_minutes"),
        consumedAt = obj.timestamp("consumed_at"),
        mealSlot = obj.string("meal_slot"),
        foodId = obj.stringOrNull("food_id"),
        quantity = obj.double("quantity"),
        unit = obj.string("unit"),
        basisAmount = obj.double("basis_amount"),
        snapFoodName = obj.string("snap_food_name"),
        snapBrand = obj.stringOrNull("snap_brand"),
        snapSource = obj.string("snap_source"),
        snapBasisUnit = obj.string("snap_basis_unit"),
        snapKcalPer100 = obj.double("snap_kcal_per_100"),
        snapProteinPer100 = obj.doubleOrNull("snap_protein_per_100"),
        snapCarbPer100 = obj.doubleOrNull("snap_carb_per_100"),
        snapFatPer100 = obj.doubleOrNull("snap_fat_per_100"),
        snapServingName = obj.stringOrNull("snap_serving_name"),
        snapServingGrams = obj.doubleOrNull("snap_serving_grams"),
        kcal = obj.double("kcal"),
        proteinG = obj.doubleOrNull("protein_g"),
        carbG = obj.doubleOrNull("carb_g"),
        fatG = obj.doubleOrNull("fat_g"),
        fromInventory = obj.boolean("from_inventory"),
        inventoryItemId = obj.stringOrNull("inventory_item_id"),
        inventoryDeductedAmount = obj.doubleOrNull("inventory_deducted_amount"),
        entrySource = obj.string("entry_source"),
        deviceId = obj.string("device_id"),
        createdAt = obj.timestamp("created_at"),
        updatedAt = obj.timestamp("updated_at"),
        deletedAt = obj.timestampOrNull("deleted_at"),
        syncState = "SYNCED",
    )

    private fun encodeWeightRecord(entity: WeightRecordEntity) = buildJsonObject {
        put("id", entity.id)
        put("user_id", entity.userId)
        put("local_date", entity.localDate)
        put("tz_offset_minutes", entity.tzOffsetMinutes)
        putNumber("weight_kg", entity.weightKg)
        putNullable("note", entity.note)
        put("device_id", entity.deviceId)
        putTimestamp("created_at", entity.createdAt)
        putTimestamp("updated_at", entity.updatedAt)
        putNullableTimestamp("deleted_at", entity.deletedAt)
    }

    private fun decodeWeightRecord(obj: JsonObject) = WeightRecordEntity(
        id = obj.string("id"),
        userId = obj.string("user_id"),
        localDate = obj.string("local_date"),
        tzOffsetMinutes = obj.int("tz_offset_minutes"),
        weightKg = obj.double("weight_kg"),
        note = obj.stringOrNull("note"),
        deviceId = obj.string("device_id"),
        createdAt = obj.timestamp("created_at"),
        updatedAt = obj.timestamp("updated_at"),
        deletedAt = obj.timestampOrNull("deleted_at"),
        syncState = "SYNCED",
    )

    private fun encodeAnalyticsEvent(entity: AnalyticsEventEntity) = buildJsonObject {
        put("id", entity.id)
        putNullable("user_id", entity.userId)
        put("event_name", entity.eventName)
        putTimestamp("event_at", entity.eventAt)
        put("local_date", entity.localDate)
        put("tz_offset_minutes", entity.tzOffsetMinutes)
        put("session_id", entity.sessionId)
        put("app_version", entity.appVersion)
        put("os_version", entity.osVersion)
        put("device_model", entity.deviceModel)
        put("params", kotlinx.serialization.json.Json.parseToJsonElement(entity.paramsJson))
        put("device_id", entity.deviceId)
        putTimestamp("created_at", entity.createdAt)
        putTimestamp("updated_at", entity.updatedAt)
        putNullableTimestamp("deleted_at", entity.deletedAt)
    }

    private fun decodeAnalyticsEvent(obj: JsonObject) = AnalyticsEventEntity(
        id = obj.string("id"),
        userId = obj.stringOrNull("user_id"),
        eventName = obj.string("event_name"),
        eventAt = obj.timestamp("event_at"),
        localDate = obj.string("local_date"),
        tzOffsetMinutes = obj.int("tz_offset_minutes"),
        sessionId = obj.string("session_id"),
        appVersion = obj.string("app_version"),
        osVersion = obj.string("os_version"),
        deviceModel = obj.string("device_model"),
        paramsJson = obj["params"]?.toString() ?: "{}",
        deviceId = obj.string("device_id"),
        createdAt = obj.timestamp("created_at"),
        updatedAt = obj.timestamp("updated_at"),
        deletedAt = obj.timestampOrNull("deleted_at"),
        syncState = "SYNCED",
    )

    private fun JsonObjectBuilder.put(key: String, value: String) {
        put(key, JsonPrimitive(value))
    }

    private fun JsonObjectBuilder.put(key: String, value: Int) {
        put(key, JsonPrimitive(value))
    }

    private fun JsonObjectBuilder.put(key: String, value: Boolean) {
        put(key, JsonPrimitive(value))
    }

    private fun JsonObjectBuilder.putNullable(key: String, value: String?) {
        put(key, value?.let { JsonPrimitive(it) } ?: JsonNull)
    }

    private fun JsonObjectBuilder.putNullable(key: String, value: Int?) {
        put(key, value?.let { JsonPrimitive(it) } ?: JsonNull)
    }

    private fun JsonObjectBuilder.putNumber(key: String, value: Double) {
        put(key, JsonPrimitive(value))
    }

    private fun JsonObjectBuilder.putNullableNumber(key: String, value: Double?) {
        put(key, value?.let { JsonPrimitive(it) } ?: JsonNull)
    }

    private fun JsonObjectBuilder.putTimestamp(key: String, epochMillis: Long) {
        put(key, JsonPrimitive(epochMillis.toIso8601()))
    }

    private fun JsonObjectBuilder.putNullableTimestamp(key: String, epochMillis: Long?) {
        put(key, epochMillis?.let { JsonPrimitive(it.toIso8601()) } ?: JsonNull)
    }

    private fun Long.toIso8601(): String =
        Instant.ofEpochMilli(this).atOffset(ZoneOffset.UTC).format(isoFormatter)

    private fun parseIso8601(value: String): Long =
        OffsetDateTime.parse(value).toInstant().toEpochMilli()

    private fun JsonObject.string(key: String): String =
        getValue(key).jsonPrimitive.content

    private fun JsonObject.stringOrNull(key: String): String? =
        this[key]?.takeIf { it !is JsonNull }?.jsonPrimitive?.content

    private fun JsonObject.int(key: String): Int = string(key).toDouble().toInt()

    private fun JsonObject.intOrNull(key: String): Int? =
        stringOrNull(key)?.toDouble()?.toInt()

    private fun JsonObject.double(key: String): Double = string(key).toDouble()

    private fun JsonObject.doubleOrNull(key: String): Double? =
        stringOrNull(key)?.toDouble()

    private fun JsonObject.boolean(key: String): Boolean =
        getValue(key).jsonPrimitive.content.toBooleanStrictOrNull() ?: false

    private fun JsonObject.timestamp(key: String): Long = parseIso8601(string(key))

    private fun JsonObject.timestampOrNull(key: String): Long? =
        stringOrNull(key)?.let { parseIso8601(it) }
}

private typealias JsonObjectBuilder = kotlinx.serialization.json.JsonObjectBuilder
