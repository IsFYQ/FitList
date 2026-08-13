package com.example.healthcheckin.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.healthcheckin.R
import com.example.healthcheckin.ui.screens.dashboard.DashboardScreen
import com.example.healthcheckin.ui.screens.exercise.ExerciseScreen
import com.example.healthcheckin.ui.screens.inventory.InventoryListScreen
import com.example.healthcheckin.ui.screens.weight.WeightChartScreen

@Composable
fun MainTabScaffold(
    onNavigateSettings: () -> Unit,
    onNavigateOnboarding: () -> Unit,
    onNavigateAddMeal: (String) -> Unit,
    onNavigateMealEdit: (String) -> Unit,
    onNavigateBodyMeasurements: () -> Unit,
    onNavigateMilestones: () -> Unit,
    onNavigateInventoryForm: (String?) -> Unit,
    onNavigateRecommendation: () -> Unit,
    onNavigateReceiptScan: () -> Unit,
) {
    val tabNavController = rememberNavController()
    val backStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentDestination?.hasRoute<DashboardRoute>() == true,
                    onClick = { tabNavController.navigateToTab(DashboardRoute) },
                    icon = { Icon(Icons.Outlined.Home, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_home)) },
                )
                NavigationBarItem(
                    selected = currentDestination?.hasRoute<InventoryRoute>() == true,
                    onClick = { tabNavController.navigateToTab(InventoryRoute) },
                    icon = { Icon(Icons.Outlined.Kitchen, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_inventory)) },
                )
                NavigationBarItem(
                    selected = currentDestination?.hasRoute<WeightChartRoute>() == true,
                    onClick = { tabNavController.navigateToTab(WeightChartRoute) },
                    icon = { Icon(Icons.Outlined.MonitorWeight, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_weight)) },
                )
                NavigationBarItem(
                    selected = currentDestination?.hasRoute<ExerciseRoute>() == true,
                    onClick = { tabNavController.navigateToTab(ExerciseRoute) },
                    icon = { Icon(Icons.Outlined.DirectionsRun, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_exercise)) },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = tabNavController,
            startDestination = DashboardRoute,
            modifier = Modifier.padding(padding),
        ) {
            composable<DashboardRoute> {
                DashboardScreen(
                    onNavigateSettings = onNavigateSettings,
                    onNavigateOnboarding = onNavigateOnboarding,
                    onNavigateAddMeal = onNavigateAddMeal,
                    onNavigateMealEdit = onNavigateMealEdit,
                    onNavigateWeight = { tabNavController.navigateToTab(WeightChartRoute) },
                    onNavigateBodyMeasurements = onNavigateBodyMeasurements,
                    onNavigateRecommendation = onNavigateRecommendation,
                )
            }
            composable<InventoryRoute> {
                InventoryListScreen(
                    onAdd = { onNavigateInventoryForm(null) },
                    onEdit = { onNavigateInventoryForm(it) },
                    onScanReceipt = onNavigateReceiptScan,
                )
            }
            composable<WeightChartRoute> {
                WeightChartScreen(
                    onBack = null,
                    onNavigateMilestones = onNavigateMilestones,
                    embeddedInTabs = true,
                )
            }
            composable<ExerciseRoute> {
                ExerciseScreen(embeddedInTabs = true)
            }
        }
    }
}

private fun NavHostController.navigateToTab(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
