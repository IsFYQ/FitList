package com.example.healthcheckin.ui.screens.ocr

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthcheckin.data.auth.SessionManager
import com.example.healthcheckin.data.ocr.MlKitInitializer
import com.example.healthcheckin.data.ocr.ReceiptOcrEngine
import com.example.healthcheckin.domain.analytics.AnalyticsTracker
import com.example.healthcheckin.domain.model.OcrConfirmLine
import com.example.healthcheckin.domain.model.OcrImportLineRequest
import com.example.healthcheckin.domain.repository.InventoryRepository
import com.example.healthcheckin.util.DateTimeUtil
import com.example.healthcheckin.util.InventoryCategory
import com.example.healthcheckin.util.InventoryUnit
import com.example.healthcheckin.util.Validators
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ReceiptScanPhase {
    CAPTURE,
    PROCESSING,
    CONFIRM,
}

data class ReceiptScanUiState(
    val phase: ReceiptScanPhase = ReceiptScanPhase.CAPTURE,
    val isPreparingModel: Boolean = false,
    val isProcessing: Boolean = false,
    val parseRate: Double = 0.0,
    val candidateCount: Int = 0,
    val parsedCount: Int = 0,
    val showParseRateWarning: Boolean = false,
    val lines: List<OcrConfirmLine> = emptyList(),
    val purchaseDate: String = DateTimeUtil.todayLocalDateString(),
    val isImporting: Boolean = false,
    val importSuccess: Boolean = false,
    val importedIds: List<String> = emptyList(),
    val errorMessage: String? = null,
)

@HiltViewModel
class ReceiptScanViewModel @Inject constructor(
    private val receiptOcrEngine: ReceiptOcrEngine,
    private val mlKitInitializer: MlKitInitializer,
    private val inventoryRepository: InventoryRepository,
    private val sessionManager: SessionManager,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReceiptScanUiState())
    val uiState: StateFlow<ReceiptScanUiState> = _uiState.asStateFlow()

    private var processingJob: Job? = null
    private var scanStartedAt: Long = 0L
    private var editedCount: Int = 0

    fun processImageBytes(bytes: ByteArray, imageSource: String) {
        val bitmap = runCatching { receiptOcrEngine.decodeAndCompress(bytes) }
            .getOrElse {
                _uiState.update { state -> state.copy(errorMessage = "ocr_error_image_too_large") }
                return
            }
        processBitmap(bitmap, imageSource)
    }

    fun processBitmap(bitmap: Bitmap, imageSource: String) {
        processingJob?.cancel()
        processingJob = viewModelScope.launch {
            scanStartedAt = System.currentTimeMillis()
            analyticsTracker.track(
                "ocr_scan_started",
                mapOf("image_source" to imageSource),
            )
            _uiState.update {
                it.copy(
                    phase = ReceiptScanPhase.PROCESSING,
                    isProcessing = true,
                    isPreparingModel = true,
                    errorMessage = null,
                    importSuccess = false,
                )
            }

            val modelReady = mlKitInitializer.ensureChineseModelReady()
            if (modelReady.isFailure) {
                _uiState.update {
                    it.copy(
                        phase = ReceiptScanPhase.CAPTURE,
                        isProcessing = false,
                        isPreparingModel = false,
                        errorMessage = "ocr_error_model_prepare",
                    )
                }
                return@launch
            }
            _uiState.update { it.copy(isPreparingModel = false) }

            val parseResult = runCatching { receiptOcrEngine.recognizeReceipt(bitmap) }
                .getOrElse {
                    bitmap.recycle()
                    _uiState.update {
                        it.copy(
                            phase = ReceiptScanPhase.CAPTURE,
                            isProcessing = false,
                            errorMessage = "ocr_error_recognition",
                        )
                    }
                    return@launch
                }
            bitmap.recycle()

            val elapsed = System.currentTimeMillis() - scanStartedAt
            analyticsTracker.track(
                "ocr_scan_completed",
                mapOf(
                    "candidate_lines" to parseResult.candidateCount,
                    "parsed_lines" to parseResult.lines.size,
                    "parse_rate" to parseResult.parseRate,
                    "elapsed_ms" to elapsed,
                ),
            )

            if (parseResult.lines.isEmpty()) {
                _uiState.update {
                    it.copy(
                        phase = ReceiptScanPhase.CAPTURE,
                        isProcessing = false,
                        errorMessage = "ocr_error_no_items",
                    )
                }
                return@launch
            }

            val userId = sessionManager.getUserId()
            val confirmLines = if (userId != null) {
                buildConfirmLines(userId, _uiState.value.purchaseDate, parseResult.lines)
            } else {
                emptyList()
            }

            _uiState.update {
                it.copy(
                    phase = ReceiptScanPhase.CONFIRM,
                    isProcessing = false,
                    parseRate = parseResult.parseRate,
                    candidateCount = parseResult.candidateCount,
                    parsedCount = parseResult.lines.size,
                    showParseRateWarning = parseResult.parseRate < 0.7,
                    lines = confirmLines,
                )
            }
        }
    }

    private suspend fun buildConfirmLines(
        userId: String,
        purchaseDate: String,
        parsed: List<com.example.healthcheckin.domain.algorithm.ParsedReceiptLine>,
    ): List<OcrConfirmLine> = parsed.map { line ->
        val normalized = Validators.normalizeFoodName(line.name)
        val duplicate = inventoryRepository.findByNameOnDate(userId, normalized, purchaseDate)
        OcrConfirmLine(
            id = com.example.healthcheckin.util.UuidV7.generate(),
            name = line.name,
            quantity = if (line.quantity <= 0) 1.0 else line.quantity,
            unit = line.unit,
            unitPrice = line.unitPrice,
            category = line.category,
            rawText = line.rawText,
            selected = true,
            needsReview = line.needsReview || line.quantity <= 0,
            duplicateExistingId = duplicate?.id,
            mergeDuplicate = duplicate != null,
        )
    }

    fun cancelProcessing() {
        processingJob?.cancel()
        analyticsTracker.track("ocr_scan_abandoned")
        _uiState.update {
            it.copy(
                phase = ReceiptScanPhase.CAPTURE,
                isProcessing = false,
                isPreparingModel = false,
            )
        }
    }

    fun retake() {
        analyticsTracker.track("ocr_scan_abandoned")
        _uiState.update {
            ReceiptScanUiState(purchaseDate = it.purchaseDate)
        }
        editedCount = 0
    }

    fun toggleLineSelected(lineId: String) {
        _uiState.update { state ->
            state.copy(
                lines = state.lines.map { line ->
                    if (line.id == lineId) line.copy(selected = !line.selected) else line
                },
            )
        }
    }

    fun removeLine(lineId: String) {
        markEdited()
        _uiState.update { state ->
            state.copy(lines = state.lines.filterNot { it.id == lineId })
        }
    }

    fun updateLineName(lineId: String, name: String) {
        markEdited()
        _uiState.update { state ->
            state.copy(
                lines = state.lines.map { line ->
                    if (line.id == lineId) line.copy(name = name) else line
                },
            )
        }
    }

    fun updateLineQuantity(lineId: String, quantityText: String) {
        markEdited()
        val quantity = quantityText.toDoubleOrNull() ?: return
        _uiState.update { state ->
            state.copy(
                lines = state.lines.map { line ->
                    if (line.id == lineId) line.copy(quantity = quantity) else line
                },
            )
        }
    }

    fun updateLineUnit(lineId: String, unit: InventoryUnit) {
        markEdited()
        _uiState.update { state ->
            state.copy(
                lines = state.lines.map { line ->
                    if (line.id == lineId) line.copy(unit = unit) else line
                },
            )
        }
    }

    fun updateLineCategory(lineId: String, category: InventoryCategory) {
        markEdited()
        _uiState.update { state ->
            state.copy(
                lines = state.lines.map { line ->
                    if (line.id == lineId) line.copy(category = category) else line
                },
            )
        }
    }

    fun updateLineUnitPrice(lineId: String, priceText: String) {
        markEdited()
        val price = priceText.toDoubleOrNull()
        _uiState.update { state ->
            state.copy(
                lines = state.lines.map { line ->
                    if (line.id == lineId) line.copy(unitPrice = price) else line
                },
            )
        }
    }

    fun toggleMergeDuplicate(lineId: String) {
        markEdited()
        _uiState.update { state ->
            state.copy(
                lines = state.lines.map { line ->
                    if (line.id == lineId) line.copy(mergeDuplicate = !line.mergeDuplicate) else line
                },
            )
        }
    }

    fun importSelected() {
        val selected = _uiState.value.lines.filter { it.selected }
        if (selected.isEmpty()) return
        viewModelScope.launch {
            val userId = sessionManager.getUserId() ?: return@launch
            _uiState.update { it.copy(isImporting = true, errorMessage = null) }
            if (editedCount > 0) {
                analyticsTracker.track("ocr_items_edited", mapOf("edited_count" to editedCount))
            }
            val requests = selected.map { line ->
                OcrImportLineRequest(
                    name = line.name.trim(),
                    quantity = line.quantity,
                    unit = line.unit,
                    unitPrice = line.unitPrice,
                    category = line.category,
                    rawText = line.rawText,
                    mergeExistingId = if (line.mergeDuplicate) line.duplicateExistingId else null,
                )
            }
            val mergedCount = requests.count { it.mergeExistingId != null }
            inventoryRepository.importOcrBatch(userId, _uiState.value.purchaseDate, requests)
                .onSuccess { ids ->
                    analyticsTracker.track(
                        "ocr_items_imported",
                        mapOf(
                            "imported_count" to ids.size,
                            "merged_count" to mergedCount,
                        ),
                    )
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            importSuccess = true,
                            importedIds = ids,
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            errorMessage = "ocr_error_import",
                        )
                    }
                }
        }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    fun clearImportSuccess() = _uiState.update { it.copy(importSuccess = false, importedIds = emptyList()) }

    private fun markEdited() {
        editedCount++
    }
}
