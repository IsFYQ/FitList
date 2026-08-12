package com.example.healthcheckin.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.healthcheckin.ui.screens.auth.ForgotPasswordScreen
import com.example.healthcheckin.ui.screens.auth.LoginScreen
import com.example.healthcheckin.ui.screens.auth.RegisterScreen
import com.example.healthcheckin.ui.screens.auth.ResetPasswordScreen
import com.example.healthcheckin.ui.screens.customfood.CustomFoodFormScreen
import com.example.healthcheckin.ui.screens.customfood.CustomFoodListScreen
import com.example.healthcheckin.ui.screens.dashboard.DashboardScreen
import com.example.healthcheckin.ui.screens.meal.MealEditScreen
import com.example.healthcheckin.ui.screens.meal.MealSearchScreen
import com.example.healthcheckin.ui.screens.onboarding.OnboardingScreen
import com.example.healthcheckin.ui.screens.export.ExportScreen
import com.example.healthcheckin.ui.screens.settings.AboutScreen
import com.example.healthcheckin.ui.screens.settings.ChangePasswordScreen
import com.example.healthcheckin.ui.screens.settings.DiagnosticsScreen
import com.example.healthcheckin.ui.screens.settings.SettingsScreen
import com.example.healthcheckin.ui.screens.weight.WeightChartScreen
import com.example.healthcheckin.ui.screens.splash.SplashDestination
import com.example.healthcheckin.ui.screens.splash.SplashScreen
import com.example.healthcheckin.ui.screens.splash.SplashViewModel

@Composable
fun HealthCheckInNavHost(
    pendingPasswordReset: Boolean = false,
    pendingRecoveryToken: String? = null,
) {
    val navController = rememberNavController()
    val splashViewModel: SplashViewModel = hiltViewModel()
    val destination by splashViewModel.destination.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(pendingPasswordReset) {
        if (pendingPasswordReset) {
            navController.navigate(ResetPasswordRoute) {
                launchSingleTop = true
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        NavHost(
            navController = navController,
            startDestination = SplashRoute,
            modifier = Modifier.padding(padding),
        ) {
            composable<SplashRoute> {
                SplashScreen()
                LaunchedEffect(destination) {
                    when (destination) {
                        SplashDestination.Loading -> Unit
                        SplashDestination.Login -> {
                            navController.navigate(LoginRoute) {
                                popUpTo(SplashRoute) { inclusive = true }
                            }
                        }
                        SplashDestination.Onboarding -> {
                            navController.navigate(OnboardingRoute(isEditMode = false)) {
                                popUpTo(SplashRoute) { inclusive = true }
                            }
                        }
                        SplashDestination.Dashboard -> {
                            navController.navigate(MainRoute) {
                                popUpTo(SplashRoute) { inclusive = true }
                            }
                        }
                    }
                }
            }

            composable<LoginRoute> { entry ->
                val prefillEmail = entry.savedStateHandle.get<String>("prefillEmail")
                LoginScreen(
                    prefillEmail = prefillEmail,
                    onNavigateRegister = { navController.navigate(RegisterRoute) },
                    onNavigateForgotPassword = { navController.navigate(ForgotPasswordRoute) },
                    onLoginSuccess = { needsOnboarding ->
                        val route = if (needsOnboarding) {
                            OnboardingRoute(isEditMode = false)
                        } else {
                            MainRoute
                        }
                        navController.navigate(route) {
                            popUpTo(LoginRoute) { inclusive = true }
                        }
                    },
                )
                LaunchedEffect(prefillEmail) {
                    prefillEmail?.let {
                        entry.savedStateHandle.remove<String>("prefillEmail")
                    }
                }
            }

            composable<RegisterRoute> {
                RegisterScreen(
                    onNavigateLogin = { navController.popBackStack() },
                    onRegisterSuccess = {
                        navController.navigate(OnboardingRoute(isEditMode = false)) {
                            popUpTo(RegisterRoute) { inclusive = true }
                        }
                    },
                    onRegisterNeedsLogin = { email ->
                        navController.getBackStackEntry<LoginRoute>()
                            .savedStateHandle["prefillEmail"] = email
                        navController.popBackStack()
                    },
                )
            }

            composable<ForgotPasswordRoute> {
                ForgotPasswordScreen(onBack = { navController.popBackStack() })
            }

            composable<ResetPasswordRoute> {
                ResetPasswordScreen(
                    onBackToLogin = {
                        navController.navigate(LoginRoute) {
                            popUpTo(LoginRoute) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateForgotPassword = {
                        navController.navigate(ForgotPasswordRoute) {
                            popUpTo(LoginRoute) { inclusive = false }
                        }
                    },
                    recoveryToken = pendingRecoveryToken,
                )
            }

            composable<OnboardingRoute> { entry ->
                val route = entry.toRoute<OnboardingRoute>()
                OnboardingScreen(
                    isEditMode = route.isEditMode,
                    onComplete = { _, _ ->
                        val dest = if (route.isEditMode) SettingsRoute else MainRoute
                        navController.navigate(dest) {
                            popUpTo<OnboardingRoute> { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable<MainRoute> {
                MainTabScaffold(
                    onNavigateSettings = { navController.navigate(SettingsRoute) },
                    onNavigateOnboarding = {
                        navController.navigate(OnboardingRoute(isEditMode = true))
                    },
                    onNavigateAddMeal = { localDate ->
                        navController.navigate(MealSearchRoute(localDate = localDate))
                    },
                    onNavigateMealEdit = { entryId ->
                        navController.navigate(MealEditRoute(entryId = entryId))
                    },
                    onNavigateBodyMeasurements = { navController.navigate(BodyMeasurementsRoute) },
                    onNavigateMilestones = { navController.navigate(MilestonesRoute) },
                    onNavigateInventoryForm = { itemId ->
                        navController.navigate(InventoryFormRoute(itemId = itemId))
                    },
                )
            }

            composable<BodyMeasurementsRoute> {
                com.example.healthcheckin.ui.screens.body.BodyMeasurementsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenMetric = { metric ->
                        navController.navigate(BodyMetricDetailRoute(metric = metric))
                    },
                )
            }

            composable<BodyMetricDetailRoute> { entry ->
                val route = entry.toRoute<BodyMetricDetailRoute>()
                com.example.healthcheckin.ui.screens.body.BodyMetricDetailScreen(
                    metricName = route.metric,
                    onBack = { navController.popBackStack() },
                )
            }

            composable<MilestonesRoute> {
                com.example.healthcheckin.ui.screens.milestone.MilestonesScreen(
                    onBack = { navController.popBackStack() },
                )
            }

            composable<InventoryFormRoute> { entry ->
                val route = entry.toRoute<InventoryFormRoute>()
                com.example.healthcheckin.ui.screens.inventory.InventoryFormScreen(
                    onBack = { navController.popBackStack() },
                )
            }

            composable<IngredientBindingsRoute> {
                com.example.healthcheckin.ui.screens.inventory.IngredientBindingsScreen(
                    onBack = { navController.popBackStack() },
                )
            }

            composable<MealSearchRoute> { entry ->
                val route = entry.toRoute<MealSearchRoute>()
                val pendingFoodId = navController.currentBackStackEntry
                    ?.savedStateHandle
                    ?.get<String>("selectFoodId")
                MealSearchScreen(
                    onBack = { navController.popBackStack() },
                    onMealSaved = { entryId ->
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("highlightEntryId", entryId)
                        navController.popBackStack()
                    },
                    onCreateCustomFood = { prefilledName ->
                        navController.navigate(
                            CustomFoodFormRoute(
                                prefilledName = prefilledName,
                                localDate = route.localDate,
                                returnToMealSearch = true,
                            ),
                        )
                    },
                    pendingSelectFoodId = pendingFoodId,
                    onPendingFoodConsumed = {
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.remove<String>("selectFoodId")
                    },
                )
            }

            composable<CustomFoodFormRoute> { entry ->
                val route = entry.toRoute<CustomFoodFormRoute>()
                CustomFoodFormScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { foodId ->
                        if (route.returnToMealSearch) {
                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("selectFoodId", foodId)
                        }
                        navController.popBackStack()
                    },
                )
            }

            composable<CustomFoodListRoute> {
                CustomFoodListScreen(
                    onBack = { navController.popBackStack() },
                    onCreateFood = {
                        navController.navigate(CustomFoodFormRoute(returnToMealSearch = false))
                    },
                    onEditFood = { foodId ->
                        navController.navigate(
                            CustomFoodFormRoute(foodId = foodId, returnToMealSearch = false),
                        )
                    },
                )
            }

            composable<MealEditRoute> {
                MealEditScreen(
                    onBack = { navController.popBackStack() },
                    onDeleted = { navController.popBackStack() },
                )
            }

            composable<WeightChartRoute> {
                WeightChartScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateMilestones = { navController.navigate(MilestonesRoute) },
                )
            }

            composable<ExportRoute> {
                ExportScreen(onBack = { navController.popBackStack() })
            }

            composable<SettingsRoute> {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onEditGoal = {
                        navController.navigate(OnboardingRoute(isEditMode = true))
                    },
                    onManageCustomFoods = {
                        navController.navigate(CustomFoodListRoute)
                    },
                    onExportData = {
                        navController.navigate(ExportRoute)
                    },
                    onNavigateAbout = { navController.navigate(AboutRoute) },
                    onNavigateDiagnostics = { navController.navigate(DiagnosticsRoute) },
                    onNavigateChangePassword = { navController.navigate(ChangePasswordRoute) },
                    onLogoutComplete = {
                        navController.navigate(LoginRoute) {
                            popUpTo(MainRoute) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onAccountDeleted = {
                        navController.navigate(RegisterRoute) {
                            popUpTo(MainRoute) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNavigateMilestones = { navController.navigate(MilestonesRoute) },
                    onNavigateBindings = { navController.navigate(IngredientBindingsRoute) },
                    onNavigateBodyMeasurements = { navController.navigate(BodyMeasurementsRoute) },
                )
            }

            composable<AboutRoute> {
                AboutScreen(onBack = { navController.popBackStack() })
            }

            composable<DiagnosticsRoute> {
                DiagnosticsScreen(onBack = { navController.popBackStack() })
            }

            composable<ChangePasswordRoute> {
                ChangePasswordScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
