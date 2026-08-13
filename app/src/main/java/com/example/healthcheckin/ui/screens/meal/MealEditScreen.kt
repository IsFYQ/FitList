package com.example.healthcheckin.ui.screens.meal

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healthcheckin.R
import com.example.healthcheckin.util.DateTimeUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealEditScreen(
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: MealEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onBack()
    }

    LaunchedEffect(uiState.deleted) {
        if (uiState.deleted) onDeleted()
    }

    LaunchedEffect(uiState.notFound) {
        if (uiState.notFound) {
            Toast.makeText(context, context.getString(R.string.meal_entry_not_found), Toast.LENGTH_SHORT).show()
            onBack()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        when (uiState.errorMessage) {
            "save_failed" -> Toast.makeText(context, context.getString(R.string.meal_save_failed), Toast.LENGTH_SHORT).show()
            "delete_failed" -> Toast.makeText(context, context.getString(R.string.meal_delete_failed), Toast.LENGTH_SHORT).show()
            "load_failed" -> Toast.makeText(context, context.getString(R.string.meal_entry_not_found), Toast.LENGTH_SHORT).show()
        }
        if (uiState.errorMessage != null) viewModel.clearError()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.meal_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.confirmState != null && uiState.minDate.isNotBlank() -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    MealConfirmSheet(
                        state = uiState.confirmState!!,
                        minDate = DateTimeUtil.parseLocalDate(uiState.minDate),
                        onDismiss = onBack,
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
                        onDelete = viewModel::delete,
                    )
                }
            }
        }
    }
}
