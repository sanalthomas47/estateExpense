package com.santhomach.estateexpense.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.santhomach.estateexpense.data.model.WeeklyFunds
import com.santhomach.estateexpense.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

@HiltViewModel
class WeeklyFundsViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeeklyFundsUiState())
    val uiState: StateFlow<WeeklyFundsUiState> = _uiState.asStateFlow()

    val weeklyFunds = repository.getAllWeeklyFundsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Comparison for each week
    fun getComparisonFlow(startDate: LocalDate): Flow<WeekComparison> {
        val endDate = startDate.plusDays(6)
        val startStr = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val endStr = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

        return repository.getDailyExpenseSummaryFlow(startStr, endStr).map { summary ->
            WeekComparison(
                totalExpenses = summary.totalLaborCost + summary.totalOvertimeCost + summary.totalOtherExpenses,
                totalIncome = summary.totalIncome
            )
        }
    }

    fun addFunds(amount: BigDecimal, startDate: LocalDate, notes: String) {
        viewModelScope.launch {
            try {
                // Adjust to Monday
                val monday = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val funds = WeeklyFunds(
                    weekStartDate = monday.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    amountReceived = amount,
                    notes = notes
                )
                repository.insertWeeklyFunds(funds)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun addFunds(amount: BigDecimal, paymentMade: BigDecimal, startDate: LocalDate, notes: String) {
        viewModelScope.launch {
            try {
                // Adjust to Monday
                val monday = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val funds = WeeklyFunds(
                    weekStartDate = monday.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    amountReceived = amount,
                    paymentMade = paymentMade,
                    notes = notes
                )
                repository.insertWeeklyFunds(funds)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateFunds(id: Int, amount: BigDecimal, paymentMade: BigDecimal, notes: String) {
        viewModelScope.launch {
            try {
                val existingFunds = weeklyFunds.value.find { it.id == id }
                existingFunds?.let {
                    val updatedFunds = it.copy(
                        amountReceived = amount,
                        paymentMade = paymentMade,
                        notes = notes
                    )
                    repository.updateWeeklyFunds(updatedFunds)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updatePaymentMade(id: Int, paymentMade: BigDecimal) {
        viewModelScope.launch {
            try {
                val existingFunds = weeklyFunds.value.find { it.id == id }
                existingFunds?.let {
                    val updatedFunds = it.copy(paymentMade = paymentMade)
                    repository.updateWeeklyFunds(updatedFunds)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteFunds(funds: WeeklyFunds) {
        viewModelScope.launch {
            try {
                repository.deleteWeeklyFunds(funds)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    // Calculate excess balance that carries over to next week
    fun getExcessBalanceFlow(startDate: LocalDate): Flow<BigDecimal> {
        val endDate = startDate.plusDays(6)
        val startStr = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val endStr = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

        return repository.getDailyExpenseSummaryFlow(startStr, endStr).map { summary ->
            val totalExpenses = summary.totalLaborCost + summary.totalOvertimeCost + summary.totalOtherExpenses
            val currentFunds = weeklyFunds.value.find {
                LocalDate.parse(it.weekStartDate) == startDate
            }
            val paymentMade = currentFunds?.paymentMade ?: BigDecimal.ZERO
            paymentMade - totalExpenses // Positive = excess, Negative = shortfall
        }
    }
}

data class WeeklyFundsUiState(
    val error: String? = null
)

data class WeekComparison(
    val totalExpenses: BigDecimal = BigDecimal.ZERO,
    val totalIncome: BigDecimal = BigDecimal.ZERO
)
