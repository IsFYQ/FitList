package com.example.healthcheckin.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object SplashRoute

@Serializable
object LoginRoute

@Serializable
object RegisterRoute

@Serializable
object ForgotPasswordRoute

@Serializable
object ResetPasswordRoute

@Serializable
data class OnboardingRoute(val isEditMode: Boolean = false)

@Serializable
object DashboardRoute

@Serializable
object SettingsRoute

@Serializable
data class MealSearchRoute(val localDate: String)

@Serializable
data class MealEditRoute(val entryId: String)

@Serializable
data class CustomFoodFormRoute(
    val prefilledName: String = "",
    val foodId: String? = null,
    val localDate: String = "",
    val returnToMealSearch: Boolean = false,
)

@Serializable
object CustomFoodListRoute

@Serializable
object WeightChartRoute

@Serializable
object ExportRoute

@Serializable
object AboutRoute

@Serializable
object DiagnosticsRoute

@Serializable
object ChangePasswordRoute

@Serializable
object MainRoute

@Serializable
object BodyMeasurementsRoute

@Serializable
data class BodyMetricDetailRoute(val metric: String)

@Serializable
object MilestonesRoute

@Serializable
object InventoryRoute

@Serializable
data class InventoryFormRoute(val itemId: String? = null)

@Serializable
object IngredientBindingsRoute

@Serializable
object ReceiptScanRoute

@Serializable
object RecommendationRoute

@Serializable
object ExerciseRoute
