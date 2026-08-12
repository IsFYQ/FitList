package com.example.healthcheckin.ui.screens.meal

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healthcheckin.R
import com.example.healthcheckin.domain.model.FoodSearchItem
import com.example.healthcheckin.domain.model.SearchBanner
import com.example.healthcheckin.ui.components.AppEmptyState
import com.example.healthcheckin.util.DateTimeUtil
import com.example.healthcheckin.util.PrecisionUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealSearchScreen(
    onBack: () -> Unit,
    onMealSaved: (entryId: String) -> Unit,
    onCreateCustomFood: (prefilledName: String) -> Unit,
    pendingSelectFoodId: String? = null,
    onPendingFoodConsumed: () -> Unit = {},
    viewModel: MealSearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusRequester = FocusRequester()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(pendingSelectFoodId) {
        pendingSelectFoodId?.let { foodId ->
            viewModel.onPendingFoodSelected(foodId)
            onPendingFoodConsumed()
        }
    }

    LaunchedEffect(uiState.savedEntryId) {
        uiState.savedEntryId?.let { entryId ->
            onMealSaved(entryId)
            viewModel.clearSavedEntry()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage == "save_failed") {
            Toast.makeText(context, context.getString(R.string.meal_save_failed), Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.meal_search_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .focusRequester(focusRequester),
                placeholder = { Text(stringResource(R.string.meal_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            )

            searchBanner(uiState.banner)?.let { banner ->
                Text(
                    text = banner,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            if (uiState.isSearching) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            val showRecent = uiState.query.isBlank()
            val recent = uiState.recentFrequent.recent
            val frequent = uiState.recentFrequent.frequent

            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.weight(1f),
            ) {
                if (showRecent) {
                    if (recent.isEmpty() && frequent.isEmpty()) {
                        item {
                            AppEmptyState(
                                title = stringResource(R.string.meal_recent_empty),
                                icon = Icons.Outlined.Restaurant,
                            )
                        }
                    } else {
                        if (recent.isNotEmpty()) {
                            item {
                                SectionTitle(stringResource(R.string.meal_recent_section))
                            }
                            items(recent, key = { "recent-${it.foodId ?: it.name}" }) { food ->
                                FoodSearchRow(
                                    food = food,
                                    showLastPortion = true,
                                    onClick = { viewModel.selectFood(food, fromRecent = true) },
                                )
                            }
                        }
                        if (frequent.isNotEmpty()) {
                            item {
                                SectionTitle(stringResource(R.string.meal_frequent_section))
                            }
                            items(frequent, key = { "frequent-${it.foodId ?: it.name}" }) { food ->
                                FoodSearchRow(
                                    food = food,
                                    showLastPortion = true,
                                    onClick = { viewModel.selectFood(food, fromRecent = true) },
                                )
                            }
                        }
                    }
                } else {
                    items(uiState.searchResults, key = { "${it.foodId ?: it.publicFoodId ?: it.name}-${it.source}" }) { food ->
                        FoodSearchRow(
                            food = food,
                            showLastPortion = false,
                            onClick = { viewModel.selectFood(food, fromRecent = false) },
                        )
                    }

                    if (!uiState.isSearching && uiState.query.isNotBlank() && uiState.searchResults.isEmpty()) {
                        item {
                            AppEmptyState(
                                title = stringResource(R.string.meal_no_results, uiState.query),
                                icon = Icons.Outlined.SearchOff,
                            )
                        }
                    }
                }
            }

            TextButton(
                onClick = {
                    val name = uiState.query.trim()
                    onCreateCustomFood(name)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
            ) {
                Text(stringResource(R.string.meal_create_custom))
            }
        }
    }

    uiState.confirmState?.let { confirm ->
        MealConfirmSheet(
            state = confirm,
            minDate = DateTimeUtil.parseLocalDate(uiState.minDate),
            onDismiss = viewModel::dismissConfirm,
            onQuantityChange = viewModel::updateQuantity,
            onAdjustQuantity = viewModel::adjustQuantity,
            onUnitChange = viewModel::selectUnit,
            onQuickQuantity = viewModel::selectQuickQuantity,
            onServingGramsChange = viewModel::updateServingGrams,
            onMealSlotChange = viewModel::selectMealSlot,
            onDateChange = viewModel::updateDate,
            onTimeChange = viewModel::updateTime,
            onSubmit = viewModel::submit,
            onDismissZeroKcal = viewModel::dismissZeroKcalDialog,
            onConfirmZeroKcal = viewModel::confirmZeroKcal,
            onToggleDeduct = viewModel::toggleDeduct,
            onOpenInventoryPicker = viewModel::openInventoryPicker,
            onSelectInventoryItem = viewModel::selectInventoryItem,
            onDismissInventoryPicker = viewModel::dismissInventoryPicker,
            onConfirmL3 = viewModel::confirmL3Deduct,
            onDismissL3 = viewModel::dismissL3Confirm,
            onResolveInsufficient = { resolution -> viewModel.resolveInsufficient(resolution) },
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun FoodSearchRow(
    food: FoodSearchItem,
    showLastPortion: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = food.name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val portionLabel = if (showLastPortion) lastPortionLabel(food) else null
        Text(
            text = buildString {
                portionLabel?.let {
                    append(it)
                    append(" · ")
                }
                food.brand?.let {
                    append("$it · ")
                }
                append("${PrecisionUtil.roundCaloriesDisplay(food.kcalPer100)} 大卡/100${basisUnitLabel(food.basisUnit.name)}")
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        Text(
            text = foodSourceLabel(food.source.name),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun searchBanner(banner: SearchBanner): String? = when (banner) {
    SearchBanner.OFFLINE -> stringResource(R.string.meal_banner_offline)
    SearchBanner.REMOTE_LOADING -> stringResource(R.string.meal_banner_remote_loading)
    SearchBanner.REMOTE_TIMEOUT -> stringResource(R.string.meal_banner_remote_timeout)
    SearchBanner.REMOTE_UNAVAILABLE -> stringResource(R.string.meal_banner_remote_unavailable)
    SearchBanner.QUOTA_EXHAUSTED -> stringResource(R.string.meal_banner_quota)
    SearchBanner.FROM_CACHE -> stringResource(R.string.meal_banner_cache)
    SearchBanner.NONE -> null
}
