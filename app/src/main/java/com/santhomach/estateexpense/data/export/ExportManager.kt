package com.santhomach.estateexpense.data.export

import android.content.Context
import com.santhomach.estateexpense.data.AppDatabase
import com.santhomach.estateexpense.data.model.DailyExpense
import com.santhomach.estateexpense.data.model.WeeklySettlement
import com.santhomach.estateexpense.data.model.ExcessBalance
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
data class ExportData(
    @SerialName("export_date")
    val exportDate: String,
    @SerialName("app_version")
    val appVersion: String = "1.0",
    @SerialName("daily_expenses")
    val dailyExpenses: List<DailyExpense>,
    @SerialName("weekly_settlements")
    val weeklySettlements: List<WeeklySettlement>,
    @SerialName("excess_balances")
    val excessBalances: List<ExcessBalance>
)

class ExportManager(private val context: Context) {

    private val database = AppDatabase.getInstance(context)
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    suspend fun exportToJson(): String {
        return try {
            val dailyExpenses = database.dailyExpenseDao().getRecent(Int.MAX_VALUE)
            val weeklySettlements = database.weeklySettlementDao().getRecent(Int.MAX_VALUE)
            val excessBalances = database.excessBalanceDao().getByDateRange("1900-01-01", "2100-12-31")

            val exportData = ExportData(
                exportDate = LocalDateTime.now().toString(),
                dailyExpenses = dailyExpenses,
                weeklySettlements = weeklySettlements,
                excessBalances = excessBalances
            )

            json.encodeToString(exportData)
        } catch (e: Exception) {
            throw ExportException("Failed to export data: ${e.message}", e)
        }
    }

    suspend fun exportToFile(filename: String = "estate_expense_backup_${System.currentTimeMillis()}.json"): File {
        return try {
            val jsonData = exportToJson()
            val file = File(context.getExternalFilesDir(null), filename)
            file.writeText(jsonData)
            file
        } catch (e: Exception) {
            throw ExportException("Failed to save export file: ${e.message}", e)
        }
    }

    suspend fun importFromJson(jsonString: String) {
        return try {
            val exportData = json.decodeFromString<ExportData>(jsonString)

            // Import data into database
            val expenseDao = database.dailyExpenseDao()
            val settlementDao = database.weeklySettlementDao()
            val balanceDao = database.excessBalanceDao()

            exportData.dailyExpenses.forEach { expenseDao.insert(it) }
            exportData.weeklySettlements.forEach { settlementDao.insert(it) }
            exportData.excessBalances.forEach { balanceDao.insert(it) }
        } catch (e: Exception) {
            throw ExportException("Failed to import data: ${e.message}", e)
        }
    }
}

class ExportException(message: String, cause: Throwable? = null) : Exception(message, cause)
