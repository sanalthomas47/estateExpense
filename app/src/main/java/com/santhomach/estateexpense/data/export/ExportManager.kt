package com.santhomach.estateexpense.data.export

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import androidx.room.withTransaction
import com.santhomach.estateexpense.data.AppDatabase
import com.santhomach.estateexpense.data.model.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File
import java.time.LocalDateTime

/**
 * Data export manager for JSON backup
 */
@Serializable
data class ExportReceipt(
    val filename: String,
    val base64Data: String
)

@Serializable
data class ExportData(
    @SerialName("export_date")
    val exportDate: String,
    @SerialName("app_version")
    val appVersion: String = "1.1",
    @SerialName("daily_expenses")
    val dailyExpenses: List<DailyExpense>,
    @SerialName("weekly_settlements")
    val weeklySettlements: List<WeeklySettlement>,
    @SerialName("excess_balances")
    val excessBalances: List<ExcessBalance>,
    @SerialName("expense_types")
    val expenseTypes: List<ExpenseType>,
    @SerialName("expense_subtypes")
    val expenseSubtypes: List<ExpenseSubtype>,
    @SerialName("income_types")
    val incomeTypes: List<IncomeType>,
    @SerialName("worker_types")
    val workerTypes: List<WorkerType>,
    @SerialName("permanent_workers")
    val permanentWorkers: List<PermanentWorker>,
    @SerialName("work_tasks")
    val workTasks: List<WorkTask>,
    @SerialName("worker_payments")
    val workerPayments: List<WorkerPayment>,
    @SerialName("weekly_funds")
    val weeklyFunds: List<WeeklyFunds>,
    @SerialName("receipts")
    val receipts: List<ExportReceipt> = emptyList()
)

class ExportManager(private val context: Context) {

    private val database = AppDatabase.getInstance(context)
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    suspend fun exportToJson(): String {
        return try {
            val dailyExpenses = database.dailyExpenseDao().getRecent(Int.MAX_VALUE)
            val weeklySettlements = database.weeklySettlementDao().getRecent(Int.MAX_VALUE)
            val excessBalances = database.excessBalanceDao().getByDateRange("1900-01-01", "2100-12-31")
            val expenseTypes = database.expenseTypeDao().getAll()
            val expenseSubtypes = database.expenseSubtypeDao().getAll()
            val incomeTypes = database.incomeTypeDao().getAll()
            val workerTypes = database.workerTypeDao().getAll()
            val permanentWorkers = database.permanentWorkerDao().getAll()
            val workTasks = database.workTaskDao().getAll()
            val workerPayments = database.workerPaymentDao().getAll()
            val weeklyFunds = database.weeklyFundsDao().getAll()

            // Export receipts
            val receiptsDir = File(context.filesDir, "receipts")
            val receipts = if (receiptsDir.exists()) {
                receiptsDir.listFiles()?.mapNotNull { file ->
                    try {
                        val bytes = file.readBytes()
                        ExportReceipt(file.name, Base64.encodeToString(bytes, Base64.NO_WRAP))
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
            } else emptyList()

            val exportData = ExportData(
                exportDate = LocalDateTime.now().toString(),
                dailyExpenses = dailyExpenses,
                weeklySettlements = weeklySettlements,
                excessBalances = excessBalances,
                expenseTypes = expenseTypes,
                expenseSubtypes = expenseSubtypes,
                incomeTypes = incomeTypes,
                workerTypes = workerTypes,
                permanentWorkers = permanentWorkers,
                workTasks = workTasks,
                workerPayments = workerPayments,
                weeklyFunds = weeklyFunds,
                receipts = receipts
            )

            json.encodeToString(exportData)
        } catch (e: Exception) {
            throw ExportException("Failed to export data: ${e.message}", e)
        }
    }

    suspend fun exportToFile(filename: String = "estate_expense_backup_${System.currentTimeMillis()}.json"): String {
        var uri: android.net.Uri? = null
        return try {
            val jsonData = exportToJson()
            
            // Save to Downloads folder using MediaStore for easy access
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val resolver = context.contentResolver
            uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw Exception("Could not create MediaStore entry")

            resolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonData.toByteArray())
            } ?: throw Exception("Could not open output stream")

            "Downloads/$filename"
        } catch (e: Exception) {
            // Cleanup MediaStore entry if creation failed partially
            uri?.let { context.contentResolver.delete(it, null, null) }
            throw ExportException("Failed to save export file to Downloads: ${e.message}", e)
        }
    }

    suspend fun importFromJson(jsonString: String) {
        try {
            if (jsonString.isBlank()) throw Exception("Import file is empty")
            
            val exportData = json.decodeFromString<ExportData>(jsonString)
            
            // Version check (optional but recommended)
            // if (exportData.appVersion != "1.1") { ... }

            // Import receipts first
            val receiptsDir = File(context.filesDir, "receipts").apply {
                if (!exists()) {
                    val created = mkdirs()
                    if (!created && !exists()) throw Exception("Could not create receipts directory")
                }
            }
            
            exportData.receipts.forEach { receipt ->
                try {
                    val bytes = Base64.decode(receipt.base64Data, Base64.DEFAULT)
                    val receiptFile = File(receiptsDir, receipt.filename)
                    receiptFile.writeBytes(bytes)
                } catch (e: Exception) {
                    // Log and continue - don't let one bad image fail the whole import
                }
            }

            importDataSequentially(exportData)

        } catch (e: Exception) {
            throw ExportException("Failed to import data: ${e.message}", e)
        }
    }

    private suspend fun importDataSequentially(exportData: ExportData) {
        val currentFilesPath = context.filesDir.absolutePath
        
        // Helper to fix paths in otherExpenses JSON
        val fixedDailyExpenses = exportData.dailyExpenses.map { expense ->
            try {
                if (expense.otherExpenses.isNullOrBlank() || expense.otherExpenses == "[]") return@map expense
                
                val otherExpenses = json.decodeFromString<List<OtherExpenseEntry>>(expense.otherExpenses)
                val fixedOther = otherExpenses.map { entry ->
                    if (!entry.receiptImagePath.isNullOrBlank()) {
                        val filename = File(entry.receiptImagePath).name
                        entry.copy(receiptImagePath = File(File(currentFilesPath, "receipts"), filename).absolutePath)
                    } else entry
                }
                expense.copy(otherExpenses = json.encodeToString(fixedOther))
            } catch (e: Exception) {
                // If parsing fails, we keep the original. This handles cases where 
                // otherExpenses might not be a valid JSON list or structure changed.
                expense
            }
        }

        // Use withTransaction for atomicity and to respect foreign key constraints
        database.withTransaction {
            // 1. Master/Reference Data first
            val expenseTypeDao = database.expenseTypeDao()
            exportData.expenseTypes.forEach { expenseTypeDao.insert(it) }

            val incomeTypeDao = database.incomeTypeDao()
            exportData.incomeTypes.forEach { incomeTypeDao.insert(it) }

            val workerTypeDao = database.workerTypeDao()
            exportData.workerTypes.forEach { workerTypeDao.insert(it) }

            val permanentWorkerDao = database.permanentWorkerDao()
            exportData.permanentWorkers.forEach { permanentWorkerDao.insert(it) }

            val workTaskDao = database.workTaskDao()
            exportData.workTasks.forEach { workTaskDao.insert(it) }

            val expenseSubtypeDao = database.expenseSubtypeDao()
            exportData.expenseSubtypes.forEach { expenseSubtypeDao.insert(it) }

            // 2. Transactional Data (depends on master data)
            val dailyExpenseDao = database.dailyExpenseDao()
            fixedDailyExpenses.forEach { dailyExpenseDao.insert(it) }

            val workerPaymentDao = database.workerPaymentDao()
            exportData.workerPayments.forEach { workerPaymentDao.insert(it) }

            val weeklySettlementDao = database.weeklySettlementDao()
            exportData.weeklySettlements.forEach { weeklySettlementDao.insert(it) }

            val excessBalanceDao = database.excessBalanceDao()
            exportData.excessBalances.forEach { excessBalanceDao.insert(it) }

            val weeklyFundsDao = database.weeklyFundsDao()
            exportData.weeklyFunds.forEach { weeklyFundsDao.insert(it) }
        }
    }
}

class ExportException(message: String, cause: Throwable? = null) : Exception(message, cause)
