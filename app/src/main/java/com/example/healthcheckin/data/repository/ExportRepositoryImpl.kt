package com.example.healthcheckin.data.repository

import android.content.Context
import android.util.JsonWriter
import com.example.healthcheckin.BuildConfig
import com.example.healthcheckin.data.auth.SessionManager
import com.example.healthcheckin.data.export.ExportRowEncoder
import com.example.healthcheckin.data.local.dao.DailyBudgetDao
import com.example.healthcheckin.data.local.dao.FoodDao
import com.example.healthcheckin.data.local.dao.GoalDao
import com.example.healthcheckin.data.local.dao.MealEntryDao
import com.example.healthcheckin.data.local.dao.ProfileDao
import com.example.healthcheckin.data.local.dao.WeightRecordDao
import com.example.healthcheckin.data.local.entity.DailyBudgetEntity
import com.example.healthcheckin.data.local.entity.FoodEntity
import com.example.healthcheckin.data.local.entity.GoalEntity
import com.example.healthcheckin.data.local.entity.MealEntryEntity
import com.example.healthcheckin.data.local.entity.WeightRecordEntity
import com.example.healthcheckin.domain.model.ExportFormat
import com.example.healthcheckin.domain.model.ExportProgress
import com.example.healthcheckin.domain.model.ExportResult
import com.example.healthcheckin.domain.model.ExportTable
import com.example.healthcheckin.domain.repository.ExportRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

@Singleton
class ExportRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager,
    private val profileDao: ProfileDao,
    private val goalDao: GoalDao,
    private val dailyBudgetDao: DailyBudgetDao,
    private val foodDao: FoodDao,
    private val mealEntryDao: MealEntryDao,
    private val weightRecordDao: WeightRecordDao,
) : ExportRepository {

    private val progressFlow = MutableStateFlow(ExportProgress())
    private var exportJob: Job? = null

    override fun observeProgress(): Flow<ExportProgress> = progressFlow.asStateFlow()

    override suspend fun cleanupTempFiles() {
        withContext(Dispatchers.IO) {
            exportsDir().listFiles()
                ?.filter { it.name.endsWith(".tmp") }
                ?.forEach { it.delete() }
        }
    }

    override suspend fun export(format: ExportFormat): ExportResult {
        val userId = sessionManager.getUserId()
            ?: return ExportResult(success = false, errorCode = "E6014", errorMessage = "no_session")

        exportJob?.cancel()
        val job = Job()
        exportJob = job

        return try {
            withContext(Dispatchers.IO + job) {
                performExport(userId, format)
            }
        } catch (_: CancellationException) {
            ExportResult(success = false, cancelled = true)
        } finally {
            if (exportJob == job) {
                exportJob = null
            }
            progressFlow.value = ExportProgress()
        }
    }

    override fun cancelExport() {
        exportJob?.cancel()
    }

    private suspend fun performExport(
        userId: String,
        format: ExportFormat,
    ): ExportResult {
        coroutineContext.ensureActive()
        val startedAt = System.currentTimeMillis()

        val rowCounts = loadRowCounts(userId)
        val totalRows = rowCounts.values.sum()
        val isEmptyData = totalRows == 0

        val dir = exportsDir()
        dir.mkdirs()
        cleanupTempFiles()

        val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val zipName = "healthcheckin-export-$timestamp.zip"
        val tmpFile = File(dir, "$zipName.tmp")
        val finalFile = File(dir, zipName)

        if (tmpFile.exists()) tmpFile.delete()

        val includeJson = format == ExportFormat.JSON || format == ExportFormat.BOTH
        val includeCsv = format == ExportFormat.CSV || format == ExportFormat.BOTH

        try {
            if (context.filesDir.freeSpace < MIN_FREE_BYTES) {
                return ExportResult(
                    success = false,
                    errorCode = "E6011",
                    errorMessage = "insufficient_storage",
                )
            }

            FileOutputStream(tmpFile).use { fileOut ->
                ZipOutputStream(fileOut).use { zipOut ->
                    writeMetaJson(zipOut, userId, rowCounts)
                    if (includeJson) {
                        emitProgress(ExportTable.PROFILE, "data.json")
                        writeDataJson(zipOut, userId, rowCounts)
                    }
                    if (includeCsv) {
                        writeCsvFiles(zipOut, userId, rowCounts)
                    }
                }
            }

            coroutineContext.ensureActive()

            if (finalFile.exists()) finalFile.delete()
            if (!tmpFile.renameTo(finalFile)) {
                tmpFile.copyTo(finalFile, overwrite = true)
                tmpFile.delete()
            }

            pruneOldExports(dir)

            val elapsed = System.currentTimeMillis() - startedAt
            val sizeKb = ((finalFile.length() + 1023) / 1024).toInt()
            return ExportResult(
                success = true,
                file = finalFile,
                totalRows = totalRows,
                fileSizeKb = sizeKb,
                elapsedMs = elapsed,
                isEmptyData = isEmptyData,
            )
        } catch (e: Exception) {
            tmpFile.delete()
            if (e is CancellationException) throw e
            return ExportResult(
                success = false,
                errorCode = "E6014",
                errorMessage = e.message,
            )
        }
    }

    private suspend fun loadRowCounts(userId: String): Map<String, Int> {
        val profileCount = profileDao.countActiveForUser(userId)
        return mapOf(
            "profile" to profileCount,
            "goals" to goalDao.countActiveForUser(userId),
            "daily_budgets" to dailyBudgetDao.countActiveForUser(userId),
            "foods" to foodDao.countActiveForUser(userId),
            "meal_entries" to mealEntryDao.countActiveForUser(userId),
            "weight_records" to weightRecordDao.countActiveForUser(userId),
        )
    }

    private suspend fun writeMetaJson(
        zipOut: ZipOutputStream,
        userId: String,
        rowCounts: Map<String, Int>,
    ) {
        zipOut.putNextEntry(ZipEntry("meta.json"))
        val writer = JsonWriter(OutputStreamWriter(zipOut, StandardCharsets.UTF_8))
        writer.beginObject()
        writeMetaFields(writer, userId, rowCounts)
        writer.endObject()
        writer.flush()
        zipOut.closeEntry()
    }

    private suspend fun writeDataJson(
        zipOut: ZipOutputStream,
        userId: String,
        rowCounts: Map<String, Int>,
    ) {
        zipOut.putNextEntry(ZipEntry("data.json"))
        val writer = JsonWriter(OutputStreamWriter(zipOut, StandardCharsets.UTF_8))
        writer.beginObject()
        writer.name("meta").beginObject()
        writeMetaFields(writer, userId, rowCounts)
        writer.endObject()

        val profile = profileDao.getActiveByUserId(userId)
        writer.name("profile")
        ExportRowEncoder.writeProfileJson(writer, profile)

        writeJsonArray(writer, ExportTable.GOALS) {
            paginateGoals(userId) { ExportRowEncoder.writeGoalJson(writer, it) }
        }
        writeJsonArray(writer, ExportTable.DAILY_BUDGETS) {
            paginateDailyBudgets(userId) { ExportRowEncoder.writeDailyBudgetJson(writer, it) }
        }
        writeJsonArray(writer, ExportTable.FOODS) {
            paginateFoods(userId) { ExportRowEncoder.writeFoodJson(writer, it) }
        }
        writeJsonArray(writer, ExportTable.MEAL_ENTRIES) {
            paginateMealEntries(userId) { ExportRowEncoder.writeMealEntryJson(writer, it) }
        }
        writeJsonArray(writer, ExportTable.WEIGHT_RECORDS) {
            paginateWeightRecords(userId) { ExportRowEncoder.writeWeightRecordJson(writer, it) }
        }

        writer.endObject()
        writer.flush()
        zipOut.closeEntry()
    }

    private suspend fun writeJsonArray(
        writer: JsonWriter,
        table: ExportTable,
        writePage: suspend () -> Unit,
    ) {
        emitProgress(table, "data.json")
        coroutineContext.ensureActive()
        val fieldName = jsonFieldName(table)
        writer.name(fieldName).beginArray()
        writePage()
        writer.endArray()
    }

    private suspend fun writeCsvFiles(
        zipOut: ZipOutputStream,
        userId: String,
        rowCounts: Map<String, Int>,
    ) {
        writeProfileCsv(zipOut, userId)
        writePagedCsv(zipOut, ExportTable.GOALS, rowCounts["goals"] ?: 0) { writer ->
            paginateGoals(userId) { ExportRowEncoder.writeGoalCsvRow(writer, it) }
        }
        writePagedCsv(zipOut, ExportTable.DAILY_BUDGETS, rowCounts["daily_budgets"] ?: 0) { writer ->
            paginateDailyBudgets(userId) { ExportRowEncoder.writeDailyBudgetCsvRow(writer, it) }
        }
        writePagedCsv(zipOut, ExportTable.FOODS, rowCounts["foods"] ?: 0) { writer ->
            paginateFoods(userId) { ExportRowEncoder.writeFoodCsvRow(writer, it) }
        }
        writePagedCsv(zipOut, ExportTable.MEAL_ENTRIES, rowCounts["meal_entries"] ?: 0) { writer ->
            paginateMealEntries(userId) { ExportRowEncoder.writeMealEntryCsvRow(writer, it) }
        }
        writePagedCsv(zipOut, ExportTable.WEIGHT_RECORDS, rowCounts["weight_records"] ?: 0) { writer ->
            paginateWeightRecords(userId) { ExportRowEncoder.writeWeightRecordCsvRow(writer, it) }
        }
    }

    private suspend fun writeProfileCsv(zipOut: ZipOutputStream, userId: String) {
        emitProgress(ExportTable.PROFILE, ExportTable.PROFILE.fileName)
        coroutineContext.ensureActive()
        val writer = ExportRowEncoder.openCsvWriter(zipOut, ExportTable.PROFILE.fileName)
        ExportRowEncoder.writeProfileCsvHeader(writer)
        profileDao.getActiveByUserId(userId)?.let {
            ExportRowEncoder.writeProfileCsvRow(writer, it)
        }
        writer.flush()
        zipOut.closeEntry()
    }

    private suspend fun writePagedCsv(
        zipOut: ZipOutputStream,
        table: ExportTable,
        total: Int,
        writeRows: suspend (BufferedWriter) -> Unit,
    ) {
        emitProgress(table, table.fileName)
        coroutineContext.ensureActive()
        val writer = ExportRowEncoder.openCsvWriter(zipOut, table.fileName)
        when (table) {
            ExportTable.GOALS -> ExportRowEncoder.writeGoalsCsvHeader(writer)
            ExportTable.DAILY_BUDGETS -> ExportRowEncoder.writeDailyBudgetsCsvHeader(writer)
            ExportTable.FOODS -> ExportRowEncoder.writeFoodsCsvHeader(writer)
            ExportTable.MEAL_ENTRIES -> ExportRowEncoder.writeMealEntriesCsvHeader(writer)
            ExportTable.WEIGHT_RECORDS -> ExportRowEncoder.writeWeightRecordsCsvHeader(writer)
            ExportTable.PROFILE -> Unit
        }
        if (total > 0) {
            writeRows(writer)
        }
        writer.flush()
        zipOut.closeEntry()
    }

    private suspend fun paginateGoals(userId: String, block: suspend (GoalEntity) -> Unit) {
        forEachPage(goalDao.countActiveForUser(userId), { offset, limit ->
            goalDao.getActivePageForUser(userId, limit, offset)
        }, block)
    }

    private suspend fun paginateDailyBudgets(
        userId: String,
        block: suspend (DailyBudgetEntity) -> Unit,
    ) {
        forEachPage(dailyBudgetDao.countActiveForUser(userId), { offset, limit ->
            dailyBudgetDao.getActivePageForUser(userId, limit, offset)
        }, block)
    }

    private suspend fun paginateFoods(
        userId: String,
        block: suspend (FoodEntity) -> Unit,
    ) {
        forEachPage(foodDao.countActiveForUser(userId), { offset, limit ->
            foodDao.getActivePageForUser(userId, limit, offset)
        }, block)
    }

    private suspend fun paginateMealEntries(
        userId: String,
        block: suspend (MealEntryEntity) -> Unit,
    ) {
        forEachPage(mealEntryDao.countActiveForUser(userId), { offset, limit ->
            mealEntryDao.getActivePageForUser(userId, limit, offset)
        }, block)
    }

    private suspend fun paginateWeightRecords(
        userId: String,
        block: suspend (WeightRecordEntity) -> Unit,
    ) {
        forEachPage(weightRecordDao.countActiveForUser(userId), { offset, limit ->
            weightRecordDao.getActivePageForUser(userId, limit, offset)
        }, block)
    }

    private suspend fun <T> forEachPage(
        total: Int,
        loadPage: suspend (offset: Int, limit: Int) -> List<T>,
        block: suspend (T) -> Unit,
    ) {
        var offset = 0
        while (offset < total) {
            coroutineContext.ensureActive()
            val page = loadPage(offset, PAGE_SIZE)
            if (page.isEmpty()) break
            page.forEach { block(it) }
            offset += PAGE_SIZE
        }
    }

    private fun writeMetaFields(
        writer: JsonWriter,
        userId: String,
        rowCounts: Map<String, Int>,
    ) {
        writer.name("exported_at").value(ExportRowEncoder.formatIsoNow())
        writer.name("app_version").value(BuildConfig.VERSION_NAME)
        writer.name("schema_version").value(SCHEMA_VERSION)
        writer.name("user_id").value(userId)
        writer.name("row_counts").beginObject()
        rowCounts.forEach { (key, count) ->
            writer.name(key).value(count)
        }
        writer.endObject()
    }

    private fun jsonFieldName(table: ExportTable): String = when (table) {
        ExportTable.GOALS -> "goals"
        ExportTable.DAILY_BUDGETS -> "daily_budgets"
        ExportTable.FOODS -> "foods"
        ExportTable.MEAL_ENTRIES -> "meal_entries"
        ExportTable.WEIGHT_RECORDS -> "weight_records"
        ExportTable.PROFILE -> "profile"
    }

    private fun emitProgress(table: ExportTable, target: String) {
        progressFlow.value = ExportProgress(
            table = table,
            message = table.progressLabel,
        )
    }

    private fun exportsDir(): File = File(context.filesDir, "exports")

    private fun pruneOldExports(dir: File) {
        dir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".zip") && !it.name.endsWith(".tmp") }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(MAX_EXPORT_FILES)
            ?.forEach { it.delete() }
    }

    companion object {
        private const val PAGE_SIZE = 500
        private const val SCHEMA_VERSION = 1
        private const val MAX_EXPORT_FILES = 3
        private const val MIN_FREE_BYTES = 5L * 1024 * 1024
    }
}
