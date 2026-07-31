package com.example.healthcheckin.ui.screens.customfood

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healthcheckin.R
import com.example.healthcheckin.ui.screens.meal.basisUnitLabel
import com.example.healthcheckin.util.BasisUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CustomFoodFormScreen(
    onBack: () -> Unit,
    onSaved: (foodId: String) -> Unit,
    viewModel: CustomFoodFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isEditMode = uiState.foodId != null

    LaunchedEffect(uiState.savedFoodId) {
        uiState.savedFoodId?.let { foodId ->
            onSaved(foodId)
            viewModel.clearSavedFoodId()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { key ->
            val resId = when (key) {
                "custom_food_error_name" -> R.string.custom_food_error_name
                "custom_food_error_kcal" -> R.string.custom_food_error_kcal
                "custom_food_error_macro" -> R.string.custom_food_error_macro
                "custom_food_error_macro_min" -> R.string.custom_food_error_macro_min
                "custom_food_error_serving" -> R.string.custom_food_error_serving
                else -> R.string.meal_save_failed
            }
            Toast.makeText(context, context.getString(resId), Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    if (uiState.duplicateName != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDuplicateDialog,
            title = { Text(stringResource(R.string.custom_food_duplicate_title)) },
            text = {
                Text(stringResource(R.string.custom_food_duplicate_message, uiState.duplicateName!!))
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissDuplicateDialog()
                    viewModel.save(overwrite = true)
                }) {
                    Text(stringResource(R.string.custom_food_duplicate_overwrite))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDuplicateDialog) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (isEditMode) R.string.custom_food_edit_title else R.string.custom_food_create_title,
                        ),
                    )
                },
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
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::updateName,
                label = { Text(stringResource(R.string.custom_food_name_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(stringResource(R.string.custom_food_basis_label))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = uiState.basisUnit == BasisUnit.G,
                    onClick = { viewModel.updateBasisUnit(BasisUnit.G) },
                    label = { Text(stringResource(R.string.custom_food_basis_g)) },
                )
                FilterChip(
                    selected = uiState.basisUnit == BasisUnit.ML,
                    onClick = { viewModel.updateBasisUnit(BasisUnit.ML) },
                    label = { Text(stringResource(R.string.custom_food_basis_ml)) },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val basisLabel = basisUnitLabel(uiState.basisUnit.name)
            OutlinedTextField(
                value = uiState.kcalText,
                onValueChange = viewModel::updateKcal,
                label = { Text(stringResource(R.string.custom_food_kcal_label, basisLabel)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.proteinText,
                onValueChange = viewModel::updateProtein,
                label = { Text(stringResource(R.string.custom_food_protein_label, basisLabel)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.carbText,
                onValueChange = viewModel::updateCarb,
                label = { Text(stringResource(R.string.custom_food_carb_label, basisLabel)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.fatText,
                onValueChange = viewModel::updateFat,
                label = { Text(stringResource(R.string.custom_food_fat_label, basisLabel)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = uiState.servingGramsText,
                onValueChange = viewModel::updateServingGrams,
                label = { Text(stringResource(R.string.custom_food_serving_label, basisLabel)) },
                supportingText = { Text(stringResource(R.string.custom_food_serving_hint)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.save() },
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                } else {
                    Text(stringResource(R.string.meal_save))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
