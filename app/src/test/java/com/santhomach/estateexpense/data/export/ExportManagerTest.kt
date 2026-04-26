package com.santhomach.estateexpense.data.export

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.santhomach.estateexpense.data.model.DailyExpense
import java.math.BigDecimal
import java.time.LocalDate

class ExportManagerTest {

    private val json = Json { prettyPrint = true }

    @Test
    fun testExportDataSerializationWithEmptyData() {
        val exportData = ExportData(
            exportDate = "2024-04-25T10:00:00",
            dailyExpenses = emptyList(),
            weeklySettlements = emptyList(),
            excessBalances = emptyList()
        )

        val jsonString = json.encodeToString(exportData)
        assertTrue(jsonString.contains("\"export_date\""))
        assertTrue(jsonString.contains("\"daily_expenses\""))
    }

    @Test
    fun testExportDataWithSampleData() {
        val expense = DailyExpense(
            date = LocalDate.now().toString(),
            malayaliMaleCount = 5,
            totalLaborCost = BigDecimal("2500")
        )

        val exportData = ExportData(
            exportDate = "2024-04-25T10:00:00",
            dailyExpenses = listOf(expense),
            weeklySettlements = emptyList(),
            excessBalances = emptyList()
        )

        val jsonString = json.encodeToString(exportData)
        assertTrue(jsonString.isNotEmpty())
        assertTrue(jsonString.contains("malayaliMaleCount"))
        assertTrue(jsonString.contains("2500"))
    }

    @Test
    fun testExportDataJsonValidFormat() {
        val exportData = ExportData(
            exportDate = "2024-04-25T10:00:00",
            dailyExpenses = emptyList(),
            weeklySettlements = emptyList(),
            excessBalances = emptyList()
        )

        val jsonString = json.encodeToString(exportData)

        // Verify JSON structure
        assertTrue(jsonString.startsWith("{"))
        assertTrue(jsonString.endsWith("}"))
    }
}
