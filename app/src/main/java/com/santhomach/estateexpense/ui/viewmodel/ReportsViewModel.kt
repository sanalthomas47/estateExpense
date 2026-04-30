package com.santhomach.estateexpense.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.santhomach.estateexpense.data.model.DailyExpense
import com.santhomach.estateexpense.data.repository.ExpenseRepository
import com.santhomach.estateexpense.data.repository.ExpenseSummary
import com.santhomach.estateexpense.data.repository.WeeklyExpenseSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    private val _dateRange = MutableStateFlow<DateRange>(DateRange.Last30Days)
    val dateRange: StateFlow<DateRange> = _dateRange.asStateFlow()

    // Summary data flows
    val dailySummary: StateFlow<ExpenseSummary> = combine(
        _dateRange,
        repository.getDailyExpenseSummaryFlow(
            getStartDate(_dateRange.value).format(DateTimeFormatter.ISO_LOCAL_DATE),
            getEndDate(_dateRange.value).format(DateTimeFormatter.ISO_LOCAL_DATE)
        )
    ) { _, summary -> summary }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExpenseSummary())

    val weeklySummary: StateFlow<WeeklyExpenseSummary> = combine(
        _dateRange,
        repository.getWeeklyExpenseSummaryFlow(
            getStartDate(_dateRange.value).format(DateTimeFormatter.ISO_LOCAL_DATE),
            getEndDate(_dateRange.value).format(DateTimeFormatter.ISO_LOCAL_DATE)
        )
    ) { _, summary -> summary }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeeklyExpenseSummary())

    // Recent expenses for detailed view
    val recentExpenses = repository.getRecentExpensesFlow(100)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Expenses filtered by current date range
    val filteredExpenses = combine(
        _dateRange,
        repository.getDailyExpensesByDateRangeFlow(
            getStartDate(_dateRange.value).format(DateTimeFormatter.ISO_LOCAL_DATE),
            getEndDate(_dateRange.value).format(DateTimeFormatter.ISO_LOCAL_DATE)
        )
    ) { _, expenses -> expenses }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setDateRange(range: DateRange) {
        _dateRange.value = range
    }

    fun setCustomDateRange(startDate: LocalDate, endDate: LocalDate) {
        _dateRange.value = DateRange.Custom(startDate, endDate)
    }

    fun refreshData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isRefreshing = true) }
                // Data will automatically refresh through flows
                _uiState.update { it.copy(isRefreshing = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isRefreshing = false, error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // Helper functions for date calculations
    private fun getStartDate(range: DateRange): LocalDate {
        val today = LocalDate.now()
        return when (range) {
            is DateRange.Last7Days -> today.minusDays(7)
            is DateRange.Last30Days -> today.minusDays(30)
            is DateRange.Last90Days -> today.minusDays(90)
            is DateRange.LastYear -> today.minusYears(1)
            is DateRange.ThisMonth -> today.withDayOfMonth(1)
            is DateRange.LastMonth -> today.minusMonths(1).withDayOfMonth(1)
            is DateRange.ThisYear -> today.withDayOfYear(1)
            is DateRange.Custom -> range.startDate
        }
    }

    private fun getEndDate(range: DateRange): LocalDate {
        val today = LocalDate.now()
        return when (range) {
            is DateRange.Last7Days -> today
            is DateRange.Last30Days -> today
            is DateRange.Last90Days -> today
            is DateRange.LastYear -> today
            is DateRange.ThisMonth -> today
            is DateRange.LastMonth -> {
                val lastMonth = today.minusMonths(1)
                lastMonth.withDayOfMonth(lastMonth.lengthOfMonth())
            }
            is DateRange.ThisYear -> today
            is DateRange.Custom -> range.endDate
        }
    }

    // Analytics functions
    fun getTopExpenseCategories(expenses: List<DailyExpense>): List<CategoryExpense> {
        val categoryMap = mutableMapOf<String, java.math.BigDecimal>()

        expenses.forEach { expense ->
            // Parse other expenses JSON and aggregate by category
            try {
                val otherExpenses = kotlinx.serialization.json.Json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(com.santhomach.estateexpense.data.model.OtherExpenseEntry.serializer()), expense.otherExpenses)
                otherExpenses.forEach { entry ->
                    categoryMap[entry.typeName] = categoryMap.getOrDefault(entry.typeName, java.math.BigDecimal.ZERO) + entry.amount
                }
            } catch (e: Exception) {
                // Skip malformed JSON
            }
        }

        return categoryMap.entries
            .sortedByDescending { it.value }
            .take(10)
            .map { CategoryExpense(it.key, it.value) }
    }

    fun getIncomeTrend(expenses: List<DailyExpense>): List<DailyTrend> {
        return expenses
            .sortedBy { it.date }
            .map { DailyTrend(it.date, it.totalIncome) }
    }

    fun getExpenseTrend(expenses: List<DailyExpense>): List<DailyTrend> {
        return expenses
            .sortedBy { it.date }
            .map { DailyTrend(it.date, it.totalLaborCost + it.totalOvertimeCost + it.totalOtherExpensesCost + it.advanceAmount) }
    }

    fun getProfitLossTrend(expenses: List<DailyExpense>): List<DailyTrend> {
        return expenses
            .sortedBy { it.date }
            .map { DailyTrend(it.date, it.calculateNetAmount()) }
    }

    fun getIncomeBreakdown(expenses: List<DailyExpense>): List<IncomeBreakdown> {
        val breakdownMap = mutableMapOf<String, IncomeBreakdown>()

        expenses.forEach { expense ->
            try {
                val incomeEntries = kotlinx.serialization.json.Json.decodeFromString(
                    kotlinx.serialization.builtins.ListSerializer(com.santhomach.estateexpense.data.model.IncomeEntry.serializer()), 
                    expense.incomeEntries
                )
                incomeEntries.forEach { entry ->
                    val current = breakdownMap.getOrDefault(entry.typeName, IncomeBreakdown(entry.typeName))
                    breakdownMap[entry.typeName] = current.copy(
                        totalAmount = current.totalAmount + entry.amount,
                        totalWeight = current.totalWeight + entry.weight
                    )
                }
            } catch (e: Exception) {
                // Skip
            }
        }

        return breakdownMap.values.sortedByDescending { it.totalAmount }
    }

    fun getWeeklyWorkerSummary(expenses: List<DailyExpense>): List<WorkerTypeSummary> {
        val summaryMap = mutableMapOf<Pair<String, String>, WorkerTypeSummary>()

        expenses.forEach { expense ->
            try {
                val groups = kotlinx.serialization.json.Json.decodeFromString(
                    kotlinx.serialization.builtins.ListSerializer(com.santhomach.estateexpense.data.model.WorkerGroupEntry.serializer()),
                    expense.workerGroups
                )
                groups.forEach { group ->
                    val key = Pair(group.workerTypeName, group.comments)
                    val current = summaryMap.getOrDefault(key, WorkerTypeSummary(group.workerTypeName, group.comments))
                    summaryMap[key] = current.copy(
                        totalCount = current.totalCount + group.count,
                        totalCost = current.totalCost + group.calculateTotalGroupCost()
                    )
                }
            } catch (e: Exception) {
                // Skip
            }
        }

        return summaryMap.values.sortedWith(compareBy({ it.workerType }, { it.comment }))
    }
}

// UI State and Data Classes
data class ReportsUiState(
    val isRefreshing: Boolean = false,
    val error: String? = null
)

sealed class DateRange {
    object Last7Days : DateRange()
    object Last30Days : DateRange()
    object Last90Days : DateRange()
    object LastYear : DateRange()
    object ThisMonth : DateRange()
    object LastMonth : DateRange()
    object ThisYear : DateRange()
    data class Custom(val startDate: LocalDate, val endDate: LocalDate) : DateRange()
}

data class CategoryExpense(
    val categoryName: String,
    val totalAmount: java.math.BigDecimal
)

data class DailyTrend(
    val date: String,
    val amount: java.math.BigDecimal
)

data class IncomeBreakdown(
    val commodityName: String,
    val totalAmount: java.math.BigDecimal = java.math.BigDecimal.ZERO,
    val totalWeight: Double = 0.0
)

data class WorkerTypeSummary(
    val workerType: String,
    val comment: String,
    val totalCount: Int = 0,
    val totalCost: java.math.BigDecimal = java.math.BigDecimal.ZERO
)
