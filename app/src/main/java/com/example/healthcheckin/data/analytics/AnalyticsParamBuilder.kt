package com.example.healthcheckin.data.analytics

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

object AnalyticsParamBuilder {

    fun toJson(params: Map<String, Any?>): String {
        val obj = buildJsonObject {
            params.forEach { (key, value) ->
                put(key, toElement(value))
            }
        }
        return obj.toString()
    }

    fun weightDeltaBucket(deltaKg: Double?, isFirst: Boolean): String {
        if (isFirst || deltaKg == null) return "FIRST"
        return when {
            deltaKg <= -1.0 -> "DECREASE_LARGE"
            deltaKg <= -0.1 -> "DECREASE_SMALL"
            deltaKg <= 0.1 -> "FLAT"
            deltaKg <= 1.0 -> "INCREASE_SMALL"
            else -> "INCREASE_LARGE"
        }
    }

    private fun toElement(value: Any?): JsonElement = when (value) {
        null -> JsonNull
        is Boolean -> JsonPrimitive(value)
        is Int -> JsonPrimitive(value)
        is Long -> JsonPrimitive(value)
        is Double -> JsonPrimitive(value)
        is Float -> JsonPrimitive(value.toDouble())
        is String -> JsonPrimitive(value)
        is List<*> -> JsonArray(value.map { toElement(it) })
        else -> JsonPrimitive(value.toString())
    }
}
