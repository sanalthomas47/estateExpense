package com.santhomach.estateexpense.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.santhomach.estateexpense.data.model.*
import com.santhomach.estateexpense.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class CsvImportViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val progress: Int = 0,
        val total: Int = 0,
        val importedCount: Int = 0,
        val skippedCount: Int = 0,
        val errors: List<String> = emptyList(),
        val isDone: Boolean = false,
        val templateSaved: Boolean = false,
        val templatePath: String = ""
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private data class CsvRow(
        val date: String,
        val type: String,
        val category: String,
        val count: Int,
        val rate: BigDecimal,
        val amount: BigDecimal,
        val notes: String,
        val lineNumber: Int
    )

    fun reset() {
        _uiState.value = UiState()
    }

    fun saveTemplate() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val csv = buildString {
                    appendLine("date,type,category,count,rate,amount,notes")
                    appendLine("2023-01-15,LABOR,Malayali Male,5,500,,Weeding")
                    appendLine("2023-01-15,LABOR,Bengali Male,3,500,,Weeding")
                    appendLine("2023-01-15,EXPENSE,Fertilizers,,,2500,NPK 50kg")
                    appendLine("2023-01-15,INCOME,Cardamom Sales,,,15000,30kg at 500/kg")
                    appendLine("2023-01-15,ADVANCE,Rajan,,,500,weekly advance")
                }
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: context.filesDir
                val file = File(dir, "estate_expense_template.csv")
                file.writeText(csv)
                _uiState.update { it.copy(templateSaved = true, templatePath = file.absolutePath) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errors = listOf("Could not save template: ${e.message}")) }
            }
        }
    }

    fun importFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, errors = emptyList(), isDone = false, importedCount = 0, skippedCount = 0) }
            try {
                val lines = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.readLines()

                if (lines == null || lines.size < 2) {
                    _uiState.update { it.copy(isLoading = false, errors = listOf("File is empty or missing header row")) }
                    return@launch
                }

                val dataLines = lines.drop(1).filter { it.isNotBlank() && !it.trimStart().startsWith("#") }
                _uiState.update { it.copy(total = dataLines.size) }

                // Load lookup tables once
                val expenseTypes = repository.getAllActiveExpenseTypes().associateBy { it.typeName }.toMutableMap()
                val incomeTypes = repository.getAllActiveIncomeTypes().associateBy { it.typeName }.toMutableMap()
                val workerTypes = repository.getAllActiveWorkerTypes().associateBy { it.workerTypeName }

                val parseErrors = mutableListOf<String>()
                val validRows = mutableListOf<CsvRow>()

                dataLines.forEachIndexed { idx, line ->
                    val lineNum = idx + 2
                    val parts = parseCsvLine(line)
                    if (parts.size < 3) {
                        if (line.isNotBlank()) parseErrors.add("Line $lineNum: not enough columns, skipped")
                        return@forEachIndexed
                    }

                    val date = parts[0].trim()
                    val type = parts[1].trim().uppercase()
                    val category = parts.getOrElse(2) { "" }.trim()
                    val count = parts.getOrElse(3) { "0" }.trim().toIntOrNull() ?: 0
                    val rate = parts.getOrElse(4) { "0" }.trim().replace(",", "").toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val amount = parts.getOrElse(5) { "0" }.trim().replace(",", "").toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val notes = parts.getOrElse(6) { "" }.trim()

                    try {
                        LocalDate.parse(date)
                    } catch (e: Exception) {
                        parseErrors.add("Line $lineNum: invalid date '$date' — use YYYY-MM-DD, skipped")
                        return@forEachIndexed
                    }

                    if (type !in setOf("LABOR", "EXPENSE", "INCOME", "ADVANCE")) {
                        parseErrors.add("Line $lineNum: unknown type '$type' — use LABOR, EXPENSE, INCOME, or ADVANCE, skipped")
                        return@forEachIndexed
                    }

                    validRows.add(CsvRow(date, type, category, count, rate, amount, notes, lineNum))
                }

                val byDate = validRows.groupBy { it.date }
                var imported = 0
                var skipped = 0
                var processed = 0

                byDate.forEach { (dateStr, rows) ->
                    try {
                        var totalLaborCost = BigDecimal.ZERO
                        var totalOtherExpenses = BigDecimal.ZERO
                        var totalIncome = BigDecimal.ZERO
                        var advanceAmount = BigDecimal.ZERO

                        val workerGroupsList = mutableListOf<WorkerGroupEntry>()
                        val otherExpensesList = mutableListOf<OtherExpenseEntry>()
                        val incomeEntriesList = mutableListOf<IncomeEntry>()
                        val advanceEntriesList = mutableListOf<AdvanceEntry>()

                        var malayaliMaleCount = 0; var malayaliMaleWage = BigDecimal.ZERO
                        var bengaliMaleCount = 0; var bengaliMaleWage = BigDecimal.ZERO
                        var malayaliFemaleCount = 0; var malayaliFemaleWage = BigDecimal.ZERO
                        var bengaliFemaleCount = 0; var bengaliFemaleWage = BigDecimal.ZERO

                        rows.forEach { row ->
                            when (row.type) {
                                "LABOR" -> {
                                    val wt = workerTypes[row.category]
                                    val effectiveRate = when {
                                        row.rate > BigDecimal.ZERO -> row.rate
                                        row.count > 0 && row.amount > BigDecimal.ZERO -> row.amount / row.count.toBigDecimal()
                                        else -> wt?.dailyBasicWage ?: BigDecimal.ZERO
                                    }
                                    totalLaborCost += effectiveRate * row.count.toBigDecimal()
                                    workerGroupsList.add(WorkerGroupEntry(
                                        workerTypeId = wt?.id ?: 0,
                                        workerTypeName = row.category,
                                        count = row.count,
                                        wagePerDay = effectiveRate,
                                        taskPerformed = row.notes
                                    ))
                                    when (row.category.lowercase().trim()) {
                                        "malayali male" -> { malayaliMaleCount += row.count; if (effectiveRate > BigDecimal.ZERO) malayaliMaleWage = effectiveRate }
                                        "bengali male" -> { bengaliMaleCount += row.count; if (effectiveRate > BigDecimal.ZERO) bengaliMaleWage = effectiveRate }
                                        "malayali female" -> { malayaliFemaleCount += row.count; if (effectiveRate > BigDecimal.ZERO) malayaliFemaleWage = effectiveRate }
                                        "bengali female" -> { bengaliFemaleCount += row.count; if (effectiveRate > BigDecimal.ZERO) bengaliFemaleWage = effectiveRate }
                                    }
                                }
                                "EXPENSE" -> {
                                    val expenseType = expenseTypes[row.category] ?: run {
                                        val newId = repository.insertExpenseType(ExpenseType(typeName = row.category)).toInt()
                                        val created = ExpenseType(id = newId, typeName = row.category)
                                        expenseTypes[row.category] = created
                                        created
                                    }
                                    totalOtherExpenses += row.amount
                                    otherExpensesList.add(OtherExpenseEntry(
                                        expenseTypeId = expenseType.id,
                                        typeName = expenseType.typeName,
                                        amount = row.amount,
                                        notes = row.notes
                                    ))
                                }
                                "INCOME" -> {
                                    val incomeType = incomeTypes[row.category] ?: run {
                                        val newId = repository.insertIncomeType(IncomeType(typeName = row.category)).toInt()
                                        val created = IncomeType(id = newId, typeName = row.category)
                                        incomeTypes[row.category] = created
                                        created
                                    }
                                    totalIncome += row.amount
                                    incomeEntriesList.add(IncomeEntry(
                                        incomeTypeId = incomeType.id,
                                        typeName = incomeType.typeName,
                                        amount = row.amount,
                                        notes = row.notes
                                    ))
                                }
                                "ADVANCE" -> {
                                    advanceAmount += row.amount
                                    advanceEntriesList.add(AdvanceEntry(
                                        amount = row.amount,
                                        reason = row.notes,
                                        recipientName = row.category
                                    ))
                                }
                            }
                        }

                        repository.insertDailyExpense(DailyExpense(
                            date = dateStr,
                            malayaliMaleCount = malayaliMaleCount,
                            bengaliMaleCount = bengaliMaleCount,
                            malayaliFemaleCount = malayaliFemaleCount,
                            bengaliFemaleCount = bengaliFemaleCount,
                            malayaliMaleWagePerDay = malayaliMaleWage,
                            bengaliMaleWagePerDay = bengaliMaleWage,
                            malayaliFemaleWagePerDay = malayaliFemaleWage,
                            bengaliFemaleWagePerDay = bengaliFemaleWage,
                            totalLaborCost = totalLaborCost,
                            workerGroups = Json.encodeToString(workerGroupsList),
                            otherExpenses = Json.encodeToString(otherExpensesList),
                            totalOtherExpensesCost = totalOtherExpenses,
                            incomeEntries = Json.encodeToString(incomeEntriesList),
                            totalIncome = totalIncome,
                            advanceAmount = advanceAmount,
                            advanceEntries = Json.encodeToString(advanceEntriesList),
                            expenseAdditionType = "import"
                        ))
                        imported++
                    } catch (e: Exception) {
                        parseErrors.add("Date $dateStr: ${e.message}")
                        skipped++
                    }

                    processed += rows.size
                    _uiState.update { it.copy(progress = processed, importedCount = imported, skippedCount = skipped) }
                }

                _uiState.update { it.copy(
                    isLoading = false,
                    isDone = true,
                    importedCount = imported,
                    skippedCount = skipped,
                    errors = parseErrors
                ) }

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errors = listOf("Import failed: ${e.message}")) }
            }
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        for (c in line) {
            when {
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> { result.add(sb.toString()); sb.clear() }
                else -> sb.append(c)
            }
        }
        result.add(sb.toString())
        return result
    }
}
