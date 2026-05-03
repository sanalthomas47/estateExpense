package com.santhomach.estateexpense.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.santhomach.estateexpense.data.model.DailyExpense
import com.santhomach.estateexpense.data.model.OtherExpenseEntry
import com.santhomach.estateexpense.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.serialization.builtins.ListSerializer
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class TrendDirection { UP, DOWN, NEUTRAL }

data class SubtypeRow(
    val subtypeName: String,
    val thisWeekAmount: BigDecimal,
    val thisYearAmount: BigDecimal,
    val allTimeAmount: BigDecimal
)

data class ExpenseTypeRow(
    val typeName: String,
    val thisWeekAmount: BigDecimal,
    val thisYearAmount: BigDecimal,
    val allTimeAmount: BigDecimal,
    val thisWeekPercent: Float,
    val thisYearPercent: Float,
    val allTimePercent: Float,
    val weekTrend: TrendDirection,
    val yearTrend: TrendDirection,
    val subtypes: List<SubtypeRow>,
    val isExpanded: Boolean = false
)

data class ExpenseTypeSummaryUiState(
    val rows: List<ExpenseTypeRow> = emptyList(),
    val thisWeekTotal: BigDecimal = BigDecimal.ZERO,
    val thisYearTotal: BigDecimal = BigDecimal.ZERO,
    val allTimeTotal: BigDecimal = BigDecimal.ZERO,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class ExpenseTypeSummaryViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _expandedTypes = MutableStateFlow<Set<String>>(emptySet())

    private val iso = DateTimeFormatter.ISO_LOCAL_DATE

    // Date ranges — computed once per collection cycle
    private fun thisWeekStart(): String = LocalDate.now().with(DayOfWeek.MONDAY).format(iso)
    private fun thisWeekEnd(): String = LocalDate.now().with(DayOfWeek.SATURDAY).format(iso)
    private fun prevWeekStart(): String = LocalDate.now().with(DayOfWeek.MONDAY).minusWeeks(1).format(iso)
    private fun prevWeekEnd(): String = LocalDate.now().with(DayOfWeek.SATURDAY).minusWeeks(1).format(iso)
    private fun thisYearStart(): String = LocalDate.now().withDayOfYear(1).format(iso)
    private fun thisYearEnd(): String = LocalDate.now().format(iso)
    private fun prevYearStart(): String = LocalDate.now().minusYears(1).withDayOfYear(1).format(iso)
    private fun prevYearEnd(): String = LocalDate.now().minusYears(1).with(java.time.temporal.TemporalAdjusters.lastDayOfYear()).format(iso)

    private val allExpenses: Flow<List<DailyExpense>> =
        repository.getDailyExpensesByDateRangeFlow("1900-01-01", "2100-12-31")

    val uiState: StateFlow<ExpenseTypeSummaryUiState> = combine(
        allExpenses,
        _expandedTypes
    ) { expenses, expandedTypes ->
        buildUiState(expenses, expandedTypes)
    }.catch { e ->
        emit(ExpenseTypeSummaryUiState(isLoading = false, error = e.message))
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ExpenseTypeSummaryUiState(isLoading = true)
    )

    fun toggleExpanded(typeName: String) {
        _expandedTypes.update { current ->
            if (typeName in current) current - typeName else current + typeName
        }
    }

    private fun buildUiState(
        expenses: List<DailyExpense>,
        expandedTypes: Set<String>
    ): ExpenseTypeSummaryUiState {
        val weekStart = thisWeekStart()
        val weekEnd = thisWeekEnd()
        val prevWkStart = prevWeekStart()
        val prevWkEnd = prevWeekEnd()
        val yearStart = thisYearStart()
        val yearEnd = thisYearEnd()
        val prevYrStart = prevYearStart()
        val prevYrEnd = prevYearEnd()

        val weekExpenses = expenses.filter { it.date in weekStart..weekEnd }
        val prevWeekExpenses = expenses.filter { it.date in prevWkStart..prevWkEnd }
        val yearExpenses = expenses.filter { it.date in yearStart..yearEnd }
        val prevYearExpenses = expenses.filter { it.date in prevYrStart..prevYrEnd }

        // type → subtype → amount for each period
        val weekMap = buildTypeMap(weekExpenses)
        val prevWeekMap = buildTypeMap(prevWeekExpenses)
        val yearMap = buildTypeMap(yearExpenses)
        val prevYearMap = buildTypeMap(prevYearExpenses)
        val allMap = buildTypeMap(expenses)

        val weekGrandTotal = weekMap.values.sumOf { it.values.fold(BigDecimal.ZERO, BigDecimal::add) }
        val yearGrandTotal = yearMap.values.sumOf { it.values.fold(BigDecimal.ZERO, BigDecimal::add) }
        val allGrandTotal = allMap.values.sumOf { it.values.fold(BigDecimal.ZERO, BigDecimal::add) }

        val allTypeNames = allMap.keys.sorted()

        val rows = allTypeNames.map { typeName ->
            val subtypeNames = allMap[typeName]?.keys.orEmpty().sorted()

            val weekTypeTotal = weekMap[typeName]?.values?.fold(BigDecimal.ZERO, BigDecimal::add) ?: BigDecimal.ZERO
            val prevWeekTypeTotal = prevWeekMap[typeName]?.values?.fold(BigDecimal.ZERO, BigDecimal::add) ?: BigDecimal.ZERO
            val yearTypeTotal = yearMap[typeName]?.values?.fold(BigDecimal.ZERO, BigDecimal::add) ?: BigDecimal.ZERO
            val prevYearTypeTotal = prevYearMap[typeName]?.values?.fold(BigDecimal.ZERO, BigDecimal::add) ?: BigDecimal.ZERO
            val allTypeTotal = allMap[typeName]?.values?.fold(BigDecimal.ZERO, BigDecimal::add) ?: BigDecimal.ZERO

            val subtypes = subtypeNames.map { sub ->
                SubtypeRow(
                    subtypeName = sub,
                    thisWeekAmount = weekMap[typeName]?.get(sub) ?: BigDecimal.ZERO,
                    thisYearAmount = yearMap[typeName]?.get(sub) ?: BigDecimal.ZERO,
                    allTimeAmount = allMap[typeName]?.get(sub) ?: BigDecimal.ZERO
                )
            }

            ExpenseTypeRow(
                typeName = typeName,
                thisWeekAmount = weekTypeTotal,
                thisYearAmount = yearTypeTotal,
                allTimeAmount = allTypeTotal,
                thisWeekPercent = percent(weekTypeTotal, weekGrandTotal),
                thisYearPercent = percent(yearTypeTotal, yearGrandTotal),
                allTimePercent = percent(allTypeTotal, allGrandTotal),
                weekTrend = trend(weekTypeTotal, prevWeekTypeTotal),
                yearTrend = trend(yearTypeTotal, prevYearTypeTotal),
                subtypes = subtypes,
                isExpanded = typeName in expandedTypes
            )
        }

        return ExpenseTypeSummaryUiState(
            rows = rows,
            thisWeekTotal = weekGrandTotal,
            thisYearTotal = yearGrandTotal,
            allTimeTotal = allGrandTotal,
            isLoading = false,
            error = null
        )
    }

    // Returns type → subtype → total amount map
    private fun buildTypeMap(expenses: List<DailyExpense>): Map<String, MutableMap<String, BigDecimal>> {
        val result = mutableMapOf<String, MutableMap<String, BigDecimal>>()
        expenses.forEach { expense ->
            parseOtherExpenses(expense.otherExpenses).forEach { entry ->
                val subtypeKey = entry.subtypeName?.takeIf { it.isNotBlank() } ?: ""
                result
                    .getOrPut(entry.typeName) { mutableMapOf() }
                    .merge(subtypeKey, entry.amount, BigDecimal::add)
            }
        }
        return result
    }

    private fun parseOtherExpenses(json: String): List<OtherExpenseEntry> {
        return try {
            if (json.isBlank() || json == "[]") emptyList()
            else kotlinx.serialization.json.Json.decodeFromString(
                ListSerializer(OtherExpenseEntry.serializer()), json
            )
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun percent(part: BigDecimal, total: BigDecimal): Float {
        if (total == BigDecimal.ZERO) return 0f
        return part.multiply(BigDecimal("100"))
            .divide(total, 2, RoundingMode.HALF_UP)
            .toFloat()
    }

    private fun trend(current: BigDecimal, previous: BigDecimal): TrendDirection = when {
        previous == BigDecimal.ZERO -> TrendDirection.NEUTRAL
        current > previous -> TrendDirection.UP
        current < previous -> TrendDirection.DOWN
        else -> TrendDirection.NEUTRAL
    }
}
