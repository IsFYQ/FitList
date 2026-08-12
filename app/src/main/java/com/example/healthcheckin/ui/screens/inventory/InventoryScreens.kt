package com.example.healthcheckin.ui.screens.inventory

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healthcheckin.R
import com.example.healthcheckin.domain.model.InventoryItem
import com.example.healthcheckin.ui.components.AppEmptyState
import com.example.healthcheckin.ui.theme.HealthCheckInDimens
import com.example.healthcheckin.util.InventoryCategory
import com.example.healthcheckin.util.InventoryExpiryStatus
import com.example.healthcheckin.util.InventorySortMode
import com.example.healthcheckin.util.InventoryUnit
import com.example.healthcheckin.util.PrecisionUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryListScreen(
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: InventoryListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    uiState.deleteConfirm?.let { (item, count) ->
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            title = { Text(stringResource(R.string.inventory_delete_linked_title)) },
            text = {
                Text(
                    if (count > 0) stringResource(R.string.inventory_delete_linked_message, count)
                    else stringResource(R.string.inventory_delete),
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) { Text(stringResource(R.string.inventory_delete)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDelete) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.inventory_title)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) { Icon(Icons.Default.Add, contentDescription = null) }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = HealthCheckInDimens.PagePadding)) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::updateQuery,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.inventory_search_hint)) },
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SortChip(R.string.inventory_sort_category, uiState.sortMode == InventorySortMode.BY_CATEGORY) {
                    viewModel.setSort(InventorySortMode.BY_CATEGORY)
                }
                SortChip(R.string.inventory_sort_expiry, uiState.sortMode == InventorySortMode.BY_EXPIRY) {
                    viewModel.setSort(InventorySortMode.BY_EXPIRY)
                }
                SortChip(R.string.inventory_sort_recent, uiState.sortMode == InventorySortMode.BY_RECENT) {
                    viewModel.setSort(InventorySortMode.BY_RECENT)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (uiState.items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AppEmptyState(title = stringResource(R.string.inventory_empty))
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (uiState.sortMode == InventorySortMode.BY_CATEGORY) {
                        val groups = uiState.items.groupBy { it.category }
                        groups.forEach { (category, items) ->
                            item {
                                Text(
                                    text = category.labelZh + " (" + items.size + ")",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                            }
                            items(items, key = { it.id }) { item ->
                                InventoryRow(
                                    item = item,
                                    onClick = { onEdit(item.id) },
                                    onUsedUp = { viewModel.markUsedUp(item) },
                                    onDelete = { viewModel.requestDelete(item) },
                                )
                            }
                        }
                    } else {
                        items(uiState.items, key = { it.id }) { item ->
                            InventoryRow(
                                item = item,
                                onClick = { onEdit(item.id) },
                                onUsedUp = { viewModel.markUsedUp(item) },
                                onDelete = { viewModel.requestDelete(item) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SortChip(labelRes: Int, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(stringResource(labelRes)) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InventoryRow(
    item: InventoryItem,
    onClick: () -> Unit,
    onUsedUp: () -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> { onDelete(); false }
                SwipeToDismissBoxValue.StartToEnd -> { onUsedUp(); false }
                else -> false
            }
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.inventory_used_up), color = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.inventory_delete), color = MaterialTheme.colorScheme.error)
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (item.expiryStatus == InventoryExpiryStatus.EXPIRED || item.remainingAmount <= 0) 0.6f else 1f)
                .clickable(onClick = onClick)
                .padding(vertical = 10.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(
                        R.string.inventory_remaining,
                        PrecisionUtil.roundStorage(item.remainingAmount).toString(),
                        item.unit.labelZh,
                    ),
                )
            }
            Text(stringResource(R.string.inventory_stored_days, item.daysStored), style = MaterialTheme.typography.bodySmall)
            item.expiryLabel?.let {
                Text(
                    it,
                    color = if (item.expiryStatus == InventoryExpiryStatus.EXPIRED) Color.Red else Color(0xFFFF9800),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            if (!item.canDeduct && item.unit == InventoryUnit.PIECE) {
                Text(stringResource(R.string.inventory_no_piece_grams), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            }
            item.lastDeductLabel?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun InventoryFormScreen(
    onBack: () -> Unit,
    viewModel: InventoryFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onBack()
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { key ->
            val msg = when (key) {
                "amount" -> context.getString(R.string.inventory_amount_error)
                "expiry" -> context.getString(R.string.inventory_expiry_before_purchase)
                else -> context.getString(R.string.inventory_save_failed)
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (uiState.itemId == null) R.string.inventory_form_add else R.string.inventory_form_edit,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::save, enabled = !uiState.isSaving) {
                        Text(stringResource(R.string.common_save))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(HealthCheckInDimens.PagePadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::updateName,
                label = { Text(stringResource(R.string.inventory_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            uiState.suggestions.take(5).forEach { suggestion ->
                Text(
                    suggestion,
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.updateName(suggestion) }.padding(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(stringResource(R.string.inventory_category))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InventoryCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = uiState.category == cat,
                        onClick = { viewModel.updateCategory(cat) },
                        label = { Text(cat.labelZh) },
                    )
                }
            }
            if (uiState.itemId == null) {
                OutlinedTextField(
                    value = uiState.amountText,
                    onValueChange = viewModel::updateAmount,
                    label = { Text(stringResource(R.string.inventory_amount)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            } else {
                OutlinedTextField(
                    value = uiState.remainingText,
                    onValueChange = viewModel::updateRemaining,
                    label = { Text(stringResource(R.string.inventory_remaining_amount)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            Text(stringResource(R.string.inventory_unit))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InventoryUnit.entries.forEach { unit ->
                    FilterChip(
                        selected = uiState.unit == unit,
                        onClick = { viewModel.updateUnit(unit) },
                        enabled = !uiState.unitLocked,
                        label = { Text(unit.labelZh) },
                    )
                }
            }
            if (uiState.unit == InventoryUnit.PIECE) {
                OutlinedTextField(
                    value = uiState.pieceGramsText,
                    onValueChange = viewModel::updatePieceGrams,
                    label = { Text(stringResource(R.string.inventory_piece_grams)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            OutlinedTextField(
                value = uiState.purchaseDate,
                onValueChange = viewModel::updatePurchaseDate,
                label = { Text(stringResource(R.string.inventory_purchase_date)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = uiState.expiryDate,
                onValueChange = viewModel::updateExpiryDate,
                label = { Text(stringResource(R.string.inventory_expiry_date)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = uiState.unitPriceText,
                onValueChange = viewModel::updateUnitPrice,
                label = { Text(stringResource(R.string.inventory_unit_price)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
    }
}
