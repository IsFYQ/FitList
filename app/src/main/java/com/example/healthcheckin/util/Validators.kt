package com.example.healthcheckin.util

import java.text.Normalizer

object Validators {

    private val EMAIL_REGEX = Regex(
        "^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$"
    )

    fun normalizeText(input: String): String {
        val trimmed = input.trim()
        return trimmed.replace(Regex("\\s+"), " ")
    }

    fun normalizeFoodName(input: String): String {
        val halfWidth = toHalfWidth(input.lowercase())
        return halfWidth
            .replace(Regex("[\\s\\p{Punct}\\p{IsPunctuation}]"), "")
    }

    fun toHalfWidth(input: String): String {
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFKC)
        val builder = StringBuilder(normalized.length)
        for (ch in normalized) {
            builder.append(
                when {
                    ch in '\uFF01'..'\uFF5E' -> (ch.code - 0xFEE0).toChar()
                    ch == '\u3000' -> ' '
                    else -> ch
                }
            )
        }
        return builder.toString()
    }

    fun validateEmail(email: String): ValidationResult {
        val normalized = normalizeText(email)
        if (normalized.length > ValidationConstants.EMAIL_MAX_LENGTH) {
            return ValidationResult.Error(ValidationError.EMAIL_INVALID)
        }
        if (!EMAIL_REGEX.matches(normalized)) {
            return ValidationResult.Error(ValidationError.EMAIL_INVALID)
        }
        return ValidationResult.Valid(normalized)
    }

    fun validatePassword(password: String): ValidationResult {
        if (password.length !in ValidationConstants.PASSWORD_MIN_LENGTH..ValidationConstants.PASSWORD_MAX_LENGTH) {
            return ValidationResult.Error(ValidationError.PASSWORD_INVALID)
        }
        if (password.any { it.isWhitespace() }) {
            return ValidationResult.Error(ValidationError.PASSWORD_INVALID)
        }
        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }
        if (!hasLetter || !hasDigit) {
            return ValidationResult.Error(ValidationError.PASSWORD_INVALID)
        }
        return ValidationResult.Valid(password)
    }

    fun validateConfirmPassword(password: String, confirm: String): ValidationResult {
        return if (password == confirm) {
            ValidationResult.Valid(confirm)
        } else {
            ValidationResult.Error(ValidationError.PASSWORD_MISMATCH)
        }
    }

    fun validateFoodName(name: String): ValidationResult {
        val normalized = normalizeText(name)
        if (normalized.length !in ValidationConstants.FOOD_NAME_MIN_LENGTH..ValidationConstants.FOOD_NAME_MAX_LENGTH) {
            return ValidationResult.Error(ValidationError.FOOD_NAME_INVALID)
        }
        return ValidationResult.Valid(normalized)
    }

    fun validateWeightKg(weight: Double): ValidationResult {
        return if (weight in ValidationConstants.WEIGHT_MIN_KG..ValidationConstants.WEIGHT_MAX_KG) {
            ValidationResult.Valid(weight)
        } else {
            ValidationResult.Error(ValidationError.WEIGHT_OUT_OF_RANGE)
        }
    }

    fun validateHeightCm(height: Double): ValidationResult {
        return if (height in ValidationConstants.HEIGHT_MIN_CM..ValidationConstants.HEIGHT_MAX_CM) {
            ValidationResult.Valid(height)
        } else {
            ValidationResult.Error(ValidationError.HEIGHT_OUT_OF_RANGE)
        }
    }

    fun validateBirthYearMonth(birthYearMonth: String): ValidationResult {
        val pattern = Regex("^\\d{4}-\\d{2}$")
        if (!pattern.matches(birthYearMonth)) {
            return ValidationResult.Error(ValidationError.AGE_OUT_OF_RANGE)
        }
        return try {
            val age = DateTimeUtil.ageYears(birthYearMonth)
            if (age in ValidationConstants.AGE_MIN..ValidationConstants.AGE_MAX) {
                ValidationResult.Valid(birthYearMonth)
            } else {
                ValidationResult.Error(ValidationError.AGE_OUT_OF_RANGE)
            }
        } catch (_: Exception) {
            ValidationResult.Error(ValidationError.AGE_OUT_OF_RANGE)
        }
    }

    fun parseDecimalInput(input: String): Double? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        return trimmed.toDoubleOrNull()
    }

    fun filterDecimalInput(input: String, maxDecimals: Int = 1): String {
        val filtered = buildString {
            var dotUsed = false
            var decimals = 0
            for (ch in input) {
                when {
                    ch.isDigit() -> {
                        if (dotUsed) {
                            if (decimals < maxDecimals) {
                                append(ch)
                                decimals++
                            }
                        } else {
                            append(ch)
                        }
                    }
                    ch == '.' && !dotUsed -> {
                        dotUsed = true
                        append(ch)
                    }
                }
            }
        }
        return filtered
    }
}

sealed class ValidationResult {
    data class Valid<T>(val value: T) : ValidationResult()
    data class Error(val error: ValidationError) : ValidationResult()

    val isValid: Boolean get() = this is Valid<*>
}

enum class ValidationError {
    EMAIL_INVALID,
    PASSWORD_INVALID,
    PASSWORD_MISMATCH,
    FOOD_NAME_INVALID,
    WEIGHT_OUT_OF_RANGE,
    HEIGHT_OUT_OF_RANGE,
    AGE_OUT_OF_RANGE,
    SEX_REQUIRED,
    ACTIVITY_REQUIRED,
}
