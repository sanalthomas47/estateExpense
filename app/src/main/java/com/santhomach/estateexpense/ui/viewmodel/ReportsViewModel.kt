package com.santhomach.estateexpense.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.santhomach.estateexpense.data.model.AdvanceEntry
import com.santhomach.estateexpense.data.model.DailyExpense
import com.santhomach.estateexpense.data.model.OtherExpenseEntry
import com.santhomach.estateexpense.data.model.VendorPayment
import com.santhomach.estateexpense.data.model.WorkerGroupEntry
import com.santhomach.estateexpense.data.repository.ExpenseRepository
import com.santhomach.estateexpense.data.repository.ExpenseSummary
import com.santhomach.estateexpense.data.repository.WeeklyExpenseSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    private val _dateRange = MutableStateFlow<DateRange>(DateRange.CurrentWeek)
    val dateRange: StateFlow<DateRange> = _dateRange.asStateFlow()

    // Summary data flows
    val dailySummary: StateFlow<ExpenseSummary> = _dateRange.flatMapLatest { range ->
        repository.getDailyExpenseSummaryFlow(
            getStartDate(range).format(DateTimeFormatter.ISO_LOCAL_DATE),
            getEndDate(range).format(DateTimeFormatter.ISO_LOCAL_DATE)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExpenseSummary())

    val weeklySummary: StateFlow<WeeklyExpenseSummary> = _dateRange.flatMapLatest { range ->
        repository.getWeeklyExpenseSummaryFlow(
            getStartDate(range).format(DateTimeFormatter.ISO_LOCAL_DATE),
            getEndDate(range).format(DateTimeFormatter.ISO_LOCAL_DATE)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeeklyExpenseSummary())

    val yearSummary: StateFlow<ExpenseSummary> = repository.getDailyExpenseSummaryFlow(
        LocalDate.now().withDayOfYear(1).format(DateTimeFormatter.ISO_LOCAL_DATE),
        LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExpenseSummary())

    val allTimeSummary: StateFlow<ExpenseSummary> = repository.getDailyExpenseSummaryFlow(
        "1900-01-01",
        "2100-12-31"
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExpenseSummary())

    val weekSummary: StateFlow<ExpenseSummary> = repository.getDailyExpenseSummaryFlow(
        LocalDate.now().with(java.time.DayOfWeek.MONDAY).format(DateTimeFormatter.ISO_LOCAL_DATE),
        LocalDate.now().with(java.time.DayOfWeek.SATURDAY).format(DateTimeFormatter.ISO_LOCAL_DATE)
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExpenseSummary())

    // Cumulative balance carried over from all weeks before the current one.
    // Includes advances, weekly payments, and manual excess-balance entries so that
    // nothing is lost until the running balance reaches zero.
    val previousWeekCarryover: StateFlow<java.math.BigDecimal> = repository.getDailyExpensesByDateRangeFlow(
        "1900-01-01",
        LocalDate.now().with(java.time.DayOfWeek.MONDAY).minusDays(1)
            .format(DateTimeFormatter.ISO_LOCAL_DATE)
    ).map { expenses ->
        val totalPaid = expenses.fold(java.math.BigDecimal.ZERO) { acc, r ->
            acc + r.advanceAmount + r.weeklyPaymentDone + r.excessBalance
        }
        val totalExpense = expenses.fold(java.math.BigDecimal.ZERO) { acc, r ->
            acc + r.totalLaborCost + r.totalOvertimeCost + r.totalOtherExpensesCost
        }
        totalPaid - totalExpense
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), java.math.BigDecimal.ZERO)

    // Outstanding vendor balance: pesticide + fertilizer expenses minus recorded vendor payments
    val vendorOutstanding: StateFlow<java.math.BigDecimal> = combine(
        repository.getDailyExpensesByDateRangeFlow("1900-01-01", "2100-12-31"),
        repository.getAllVendorPaymentsFlow()
    ) { expenses, payments ->
        val totalExpenses = expenses.fold(java.math.BigDecimal.ZERO) { acc, expense ->
            acc + try {
                Json.decodeFromString(ListSerializer(OtherExpenseEntry.serializer()), expense.otherExpenses)
                    .filter {
                        it.typeName.contains("Pesticide", ignoreCase = true) ||
                        it.typeName.contains("Fertilizer", ignoreCase = true)
                    }
                    .fold(java.math.BigDecimal.ZERO) { a, e -> a + e.amount }
            } catch (_: Exception) { java.math.BigDecimal.ZERO }
        }
        val totalPaid = payments.fold(java.math.BigDecimal.ZERO) { acc, p -> acc + p.amount }
        (totalExpenses - totalPaid).coerceAtLeast(java.math.BigDecimal.ZERO)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), java.math.BigDecimal.ZERO)

    fun recordVendorPayment(amount: java.math.BigDecimal, vendorName: String, notes: String, date: LocalDate) {
        viewModelScope.launch {
            repository.insertVendorPayment(
                VendorPayment(
                    date = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    amount = amount,
                    vendorName = vendorName,
                    notes = notes
                )
            )
        }
    }

    // Recent expenses for detailed view
    val recentExpenses = repository.getRecentExpensesFlow(100)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All expenses from the start of the current year
    val yearlyExpenses: StateFlow<List<DailyExpense>> = repository.getDailyExpensesByDateRangeFlow(
        LocalDate.now().withDayOfYear(1).format(DateTimeFormatter.ISO_LOCAL_DATE),
        LocalDate.now().plusYears(10).format(DateTimeFormatter.ISO_LOCAL_DATE)
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Expenses filtered by current date range
    val filteredExpenses = _dateRange.flatMapLatest { range ->
        repository.getDailyExpensesByDateRangeFlow(
            getStartDate(range).format(DateTimeFormatter.ISO_LOCAL_DATE),
            getEndDate(range).format(DateTimeFormatter.ISO_LOCAL_DATE)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    // ---------------------------------------------------------------------------
    // Clone
    // ---------------------------------------------------------------------------

    fun cloneExpense(source: DailyExpense, targetDate: LocalDate) {
        viewModelScope.launch {
            try {
                // Strip overtime from each worker group; recompute labor cost from base wages only
                val strippedGroups = try {
                    Json.decodeFromString(
                        ListSerializer(WorkerGroupEntry.serializer()), source.workerGroups
                    ).map { it.copy(overtimeHours = 0, overtimeWagePerHour = java.math.BigDecimal.ZERO) }
                } catch (_: Exception) { emptyList() }
                val strippedGroupsJson = Json.encodeToString(ListSerializer(WorkerGroupEntry.serializer()), strippedGroups)
                val newLaborCost = strippedGroups.fold(java.math.BigDecimal.ZERO) { acc, g -> acc + g.calculateTotalGroupCost() }

                val cloned = source.copy(
                    id = 0,
                    date = targetDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    workerGroups = strippedGroupsJson,
                    totalLaborCost = newLaborCost,
                    totalOvertimeCost = java.math.BigDecimal.ZERO,
                    // Other expenses are day-specific — do not clone
                    otherExpenses = "[]",
                    totalOtherExpensesCost = java.math.BigDecimal.ZERO,
                    // Income, advances, settlements are day-specific — do not clone
                    incomeEntries = "[]",
                    totalIncome = java.math.BigDecimal.ZERO,
                    advanceEntries = "[]",
                    advanceAmount = java.math.BigDecimal.ZERO,
                    advanceReason = "",
                    weeklyPaymentDone = java.math.BigDecimal.ZERO,
                    excessBalance = java.math.BigDecimal.ZERO,
                    expenseAdditionType = "clone",
                    createdAt = LocalDateTime.now().toString(),
                    updatedAt = LocalDateTime.now().toString()
                )
                repository.insertDailyExpense(cloned)
                _uiState.update { it.copy(cloneSuccessDate = targetDate) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Clone failed: ${e.message}") }
            }
        }
    }

    fun clearCloneSuccess() {
        _uiState.update { it.copy(cloneSuccessDate = null) }
    }

    fun nextWorkday(from: LocalDate): LocalDate {
        var next = from.plusDays(1)
        if (next.dayOfWeek == DayOfWeek.SUNDAY) next = next.plusDays(1)
        return next
    }

    // Helper functions for date calculations
    private fun getStartDate(range: DateRange): LocalDate {
        val today = LocalDate.now()
        return when (range) {
            is DateRange.CurrentWeek -> today.with(DayOfWeek.MONDAY)
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
            is DateRange.CurrentWeek -> today.with(DayOfWeek.SATURDAY)
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

    fun getWeeklySpecificWorkers(expenses: List<DailyExpense>): List<WorkerTypeSpecific> {
        val map = mutableMapOf<String, WorkerTypeSpecific>()
        expenses.forEach { expense ->
            try {
                val groups = Json.decodeFromString(ListSerializer(WorkerGroupEntry.serializer()), expense.workerGroups)
                groups.forEach { group ->
                    val current = map.getOrDefault(group.workerTypeName, WorkerTypeSpecific(group.workerTypeName))
                    val baseCost = group.wagePerDay * group.count.toBigDecimal()
                    val otCost = group.overtimeWagePerHour * group.overtimeHours.toBigDecimal() * group.count.toBigDecimal()
                    map[group.workerTypeName] = current.copy(
                        totalCount = current.totalCount + group.count,
                        totalBaseCost = current.totalBaseCost + baseCost,
                        totalOvertimeCost = current.totalOvertimeCost + otCost
                    )
                }
            } catch (_: Exception) {}
        }
        return map.values.sortedBy { it.workerTypeName }
    }

    fun getWeeklySpecificOtherExpenses(expenses: List<DailyExpense>): List<OtherExpenseSpecific> {
        val map = mutableMapOf<String, OtherExpenseSpecific>()
        expenses.forEach { expense ->
            try {
                val entries = Json.decodeFromString(ListSerializer(OtherExpenseEntry.serializer()), expense.otherExpenses)
                entries.forEach { entry ->
                    val key = "${entry.typeName}||${entry.subtypeName}"
                    val current = map.getOrDefault(key, OtherExpenseSpecific(entry.typeName, entry.subtypeName))
                    map[key] = current.copy(
                        totalQuantity = current.totalQuantity + entry.quantity,
                        totalAmount = current.totalAmount + entry.amount
                    )
                }
            } catch (_: Exception) {}
        }
        return map.values.sortedBy { it.typeName }
    }

    fun getIndividualOtherExpenses(expenses: List<DailyExpense>): List<Pair<String, OtherExpenseEntry>> {
        return expenses
            .sortedBy { it.date }
            .flatMap { expense ->
                try {
                    Json.decodeFromString(ListSerializer(OtherExpenseEntry.serializer()), expense.otherExpenses)
                        .map { entry -> Pair(expense.date, entry) }
                } catch (_: Exception) { emptyList() }
            }
    }

    fun getWeeklySpecificSettlement(expenses: List<DailyExpense>): java.math.BigDecimal =
        expenses.fold(java.math.BigDecimal.ZERO) { acc, e -> acc + e.weeklyPaymentDone }

    fun getWeeklySpecificAdvances(expenses: List<DailyExpense>): List<AdvanceSpecific> {
        val map = mutableMapOf<Pair<String, String>, AdvanceSpecific>()
        expenses.forEach { expense ->
            try {
                val entries = Json.decodeFromString(ListSerializer(AdvanceEntry.serializer()), expense.advanceEntries)
                entries.forEach { entry ->
                    val recipient = entry.recipientName.ifBlank { "General" }
                    val reason = entry.reason
                    val key = Pair(recipient, reason)
                    val current = map.getOrDefault(key, AdvanceSpecific(recipient, reason))
                    map[key] = current.copy(totalAmount = current.totalAmount + entry.amount)
                }
            } catch (_: Exception) {}
        }
        return map.values.sortedWith(compareBy({ it.recipientName }, { it.reason }))
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
    val error: String? = null,
    val cloneSuccessDate: LocalDate? = null
)

sealed class DateRange {
    object CurrentWeek : DateRange()
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

data class WorkerTypeSpecific(
    val workerTypeName: String,
    val totalCount: Int = 0,
    val totalBaseCost: java.math.BigDecimal = java.math.BigDecimal.ZERO,
    val totalOvertimeCost: java.math.BigDecimal = java.math.BigDecimal.ZERO
)

data class OtherExpenseSpecific(
    val typeName: String,
    val subtypeName: String?,
    val totalQuantity: Double = 0.0,
    val totalAmount: java.math.BigDecimal = java.math.BigDecimal.ZERO
)

data class AdvanceSpecific(
    val recipientName: String,
    val reason: String,
    val totalAmount: java.math.BigDecimal = java.math.BigDecimal.ZERO
)
