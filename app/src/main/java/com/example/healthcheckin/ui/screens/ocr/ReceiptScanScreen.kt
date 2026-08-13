package com.example.healthcheckin.ui.screens.ocr

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.healthcheckin.R
import com.example.healthcheckin.domain.model.OcrConfirmLine
import com.example.healthcheckin.ui.theme.HealthCheckInDimens
import com.example.healthcheckin.ui.theme.HealthCheckInRadius
import com.example.healthcheckin.util.InventoryCategory
import com.example.healthcheckin.util.InventoryUnit
import com.example.healthcheckin.util.PrecisionUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScanScreen(
    onBack: () -> Unit,
    onImportSuccess: (List<String>) -> Unit = {},
    viewModel: ReceiptScanViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        uri?.let { selected ->
            context.contentResolver.openInputStream(selected)?.use { stream ->
                viewModel.processImageBytes(stream.readBytes(), "GALLERY")
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
    ) { bitmap ->
        bitmap?.let { viewModel.processBitmap(it, "CAMERA") }
    }

    LaunchedEffect(uiState.importSuccess) {
        if (uiState.importSuccess) {
            onImportSuccess(uiState.importedIds)
            viewModel.clearImportSuccess()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { key ->
            val resId = when (key) {
                "ocr_error_no_items" -> R.string.ocr_error_no_items
                "ocr_error_model_prepare" -> R.string.ocr_error_model_prepare
                "ocr_error_image_too_large" -> R.string.ocr_error_image_too_large
                "ocr_error_recognition" -> R.string.ocr_error_recognition
                "ocr_error_import" -> R.string.ocr_error_import
                else -> R.string.ocr_error_recognition
            }
            Toast.makeText(context, context.getString(resId), Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ocr_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        bottomBar = {
            if (uiState.phase == ReceiptScanPhase.CONFIRM) {
                val selectedCount = uiState.lines.count { it.selected }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(HealthCheckInDimens.PagePadding),
                ) {
                    Button(
                        onClick = viewModel::importSelected,
                        enabled = selectedCount > 0 && !uiState.isImporting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (uiState.isImporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(stringResource(R.string.ocr_import_selected, selectedCount))
                        }
                    }
                    OutlinedButton(
                        onClick = viewModel::retake,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.ocr_retake))
                    }
                }
            }
        },
    ) { padding ->
        when (uiState.phase) {
            ReceiptScanPhase.CAPTURE -> CapturePhase(
                modifier = Modifier.fillMaxSize().padding(padding),
                onTakePhoto = { cameraLauncher.launch(null) },
                onPickGallery = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
            )
            ReceiptScanPhase.PROCESSING -> ProcessingPhase(
                modifier = Modifier.fillMaxSize().padding(padding),
                isPreparingModel = uiState.isPreparingModel,
                onCancel = viewModel::cancelProcessing,
            )
            ReceiptScanPhase.CONFIRM -> ConfirmPhase(
                modifier = Modifier.fillMaxSize().padding(padding),
                uiState = uiState,
                viewModel = viewModel,
            )
        }
    }
}

@Composable
private fun CapturePhase(
    modifier: Modifier = Modifier,
    onTakePhoto: () -> Unit,
    onPickGallery: () -> Unit,
) {
    Column(
        modifier = modifier.padding(HealthCheckInDimens.PagePadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(HealthCheckInRadius.Card))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.ocr_capture_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onTakePhoto,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text(stringResource(R.string.ocr_take_photo))
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onPickGallery) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text(stringResource(R.string.ocr_pick_gallery))
        }
    }
}

@Composable
private fun ProcessingPhase(
    modifier: Modifier = Modifier,
    isPreparingModel: Boolean,
    onCancel: () -> Unit,
) {
    Column(
        modifier = modifier.padding(HealthCheckInDimens.PagePadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(
                if (isPreparingModel) R.string.ocr_preparing_model else R.string.ocr_processing,
            ),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = onCancel) {
            Text(stringResource(R.string.common_cancel))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConfirmPhase(
    modifier: Modifier = Modifier,
    uiState: ReceiptScanUiState,
    viewModel: ReceiptScanViewModel,
) {
    Column(modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = HealthCheckInDimens.PagePadding)) {
            Text(
                text = stringResource(R.string.ocr_parsed_count, uiState.parsedCount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (uiState.showParseRateWarning) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.ocr_parse_rate_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE65100),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFE0B2))
                        .padding(12.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            contentPadding = PaddingValues(
                start = HealthCheckInDimens.PagePadding,
                end = HealthCheckInDimens.PagePadding,
                bottom = 120.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(uiState.lines, key = { it.id }) { line ->
                OcrConfirmLineCard(
                    line = line,
                    onToggleSelected = { viewModel.toggleLineSelected(line.id) },
                    onRemove = { viewModel.removeLine(line.id) },
                    onNameChange = { viewModel.updateLineName(line.id, it) },
                    onQuantityChange = { viewModel.updateLineQuantity(line.id, it) },
                    onUnitChange = { viewModel.updateLineUnit(line.id, it) },
                    onCategoryChange = { viewModel.updateLineCategory(line.id, it) },
                    onPriceChange = { viewModel.updateLineUnitPrice(line.id, it) },
                    onToggleMerge = { viewModel.toggleMergeDuplicate(line.id) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OcrConfirmLineCard(
    line: OcrConfirmLine,
    onToggleSelected: () -> Unit,
    onRemove: () -> Unit,
    onNameChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onUnitChange: (InventoryUnit) -> Unit,
    onCategoryChange: (InventoryCategory) -> Unit,
    onPriceChange: (String) -> Unit,
    onToggleMerge: () -> Unit,
) {
    var expanded by remember(line.id) { mutableStateOf(false) }
    val duplicateBg = if (line.duplicateExistingId != null) Color(0xFFFFF8E1) else MaterialTheme.colorScheme.surface

    Card(
        colors = CardDefaults.cardColors(containerColor = duplicateBg),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = line.selected, onCheckedChange = { onToggleSelected() })
                OutlinedTextField(
                    value = line.name,
                    onValueChange = onNameChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(stringResource(R.string.inventory_name)) },
                )
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.inventory_delete))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = PrecisionUtil.roundStorage(line.quantity).toString(),
                    onValueChange = onQuantityChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(stringResource(R.string.inventory_amount)) },
                )
                OutlinedTextField(
                    value = line.unitPrice?.let { PrecisionUtil.roundStorage(it).toString() } ?: "",
                    onValueChange = onPriceChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(stringResource(R.string.inventory_unit_price)) },
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.inventory_unit),
                style = MaterialTheme.typography.labelSmall,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                InventoryUnit.entries.forEach { unit ->
                    FilterChip(
                        selected = line.unit == unit,
                        onClick = { onUnitChange(unit) },
                        label = { Text(unit.labelZh) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.inventory_category),
                style = MaterialTheme.typography.labelSmall,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                InventoryCategory.entries.forEach { category ->
                    FilterChip(
                        selected = line.category == category,
                        onClick = { onCategoryChange(category) },
                        label = { Text(category.labelZh) },
                    )
                }
            }
            if (line.duplicateExistingId != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.ocr_duplicate_hint, line.name),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE65100),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = line.mergeDuplicate, onCheckedChange = { onToggleMerge() })
                    Text(stringResource(R.string.ocr_merge_duplicate))
                }
            }
            if (line.needsReview) {
                Text(
                    text = stringResource(R.string.ocr_needs_review),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            TextButton(onClick = { expanded = !expanded }) {
                Text(
                    if (expanded) stringResource(R.string.ocr_hide_raw) else stringResource(R.string.ocr_show_raw),
                )
            }
            if (expanded) {
                Text(
                    text = line.rawText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
