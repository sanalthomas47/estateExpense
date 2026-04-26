package com.santhomach.estateexpense.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.santhomach.estateexpense.data.model.*
import com.santhomach.estateexpense.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.serialization.builtins.ListSerializer

@HiltViewModel
class DailyExpenseViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(DailyExpenseUiState())
    val uiState: StateFlow<DailyExpenseUiState> = _uiState.asStateFlow()

    // Current expense being edited
    private val _currentExpense = MutableStateFlow<DailyExpense?>(null)
    val currentExpense: StateFlow<DailyExpense?> = _currentExpense.asStateFlow()

    // Available types
    val expenseTypes = repository.getAllActiveExpenseTypesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val incomeTypes = repository.getAllActiveIncomeTypesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workerTypes = repository.getAllActiveWorkerTypesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val permanentWorkers = repository.getAllActivePermanentWorkersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Initialize default data on first launch
    init {
        viewModelScope.launch {
            try {
                repository.initializeDefaultData()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun createNewExpense(date: LocalDate = LocalDate.now()) {
        val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val newExpense = DailyExpense(
            date = dateString,
            createdBy = "user"
        )
        _currentExpense.value = newExpense
        _uiState.update { it.copy(isEditing = true, error = null) }
    }

    fun loadExpense(id: Int) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val expense = repository.getDailyExpenseById(id)
                _currentExpense.value = expense
                _uiState.update { it.copy(isLoading = false, isEditing = expense != null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun updateWorkerCount(workerType: String, count: Int) {
        _currentExpense.update { expense ->
            expense?.copy(
                malayaliMaleCount = if (workerType == "Malayalam Male") count else expense.malayaliMaleCount,
                bengaliMaleCount = if (workerType == "Bengali Male") count else expense.bengaliMaleCount,
                malayaliFemaleCount = if (workerType == "Malayalam Female") count else expense.malayaliFemaleCount,
                bengaliFemaleCount = if (workerType == "Bengali Female") count else expense.bengaliFemaleCount
            )
        }
        recalculateTotals()
    }

    fun updateWage(workerType: String, wage: BigDecimal) {
        _currentExpense.update { expense ->
            expense?.copy(
                malayaliMaleWagePerDay = if (workerType == "Malayalam Male") wage else expense.malayaliMaleWagePerDay,
                bengaliMaleWagePerDay = if (workerType == "Bengali Male") wage else expense.bengaliMaleWagePerDay,
                malayaliFemaleWagePerDay = if (workerType == "Malayalam Female") wage else expense.malayaliFemaleWagePerDay,
                bengaliFemaleWagePerDay = if (workerType == "Bengali Female") wage else expense.bengaliFemaleWagePerDay
            )
        }
        recalculateTotals()
    }

    fun updateOvertime(hours: Int) {
        _currentExpense.update { expense ->
            expense?.copy(overtimeHours = hours)
        }
        recalculateTotals()
    }

    fun addOtherExpense(expenseTypeId: Int, amount: BigDecimal, notes: String = "") {
        _currentExpense.update { expense ->
            expense?.let {
                val currentExpenses = parseOtherExpenses(it.otherExpenses)
                val newEntry = OtherExpenseEntry(
                    expenseTypeId = expenseTypeId,
                    typeName = expenseTypes.value.find { type -> type.id == expenseTypeId }?.typeName ?: "Unknown",
                    amount = amount,
                    notes = notes
                )
                val updatedExpenses = currentExpenses + newEntry
                val totalAmount = updatedExpenses.sumOf { entry -> entry.amount }

                it.copy(
                    otherExpenses = kotlinx.serialization.json.Json.encodeToString(ListSerializer(OtherExpenseEntry.serializer()), updatedExpenses),
                    totalOtherExpensesCost = totalAmount
                )
            }
        }
        recalculateTotals()
    }

    fun removeOtherExpense(index: Int) {
        _currentExpense.update { expense ->
            expense?.let {
                val currentExpenses = parseOtherExpenses(it.otherExpenses)
                if (index in currentExpenses.indices) {
                    val updatedExpenses = currentExpenses.toMutableList().apply { removeAt(index) }
                    val totalAmount = updatedExpenses.sumOf { entry -> entry.amount }

                    it.copy(
                        otherExpenses = kotlinx.serialization.json.Json.encodeToString(ListSerializer(OtherExpenseEntry.serializer()), updatedExpenses),
                        totalOtherExpensesCost = totalAmount
                    )
                } else it
            }
        }
        recalculateTotals()
    }

    fun addIncome(incomeTypeId: Int, amount: BigDecimal, notes: String = "") {
        _currentExpense.update { expense ->
            expense?.let {
                val currentIncomes = parseIncomeEntries(it.incomeEntries)
                val newEntry = IncomeEntry(
                    incomeTypeId = incomeTypeId,
                    typeName = incomeTypes.value.find { type -> type.id == incomeTypeId }?.typeName ?: "Unknown",
                    amount = amount,
                    notes = notes
                )
                val updatedIncomes = currentIncomes + newEntry
                val totalAmount = updatedIncomes.sumOf { entry -> entry.amount }

                it.copy(
                    incomeEntries = kotlinx.serialization.json.Json.encodeToString(ListSerializer(IncomeEntry.serializer()), updatedIncomes),
                    totalIncome = totalAmount
                )
            }
        }
        recalculateTotals()
    }

    fun removeIncome(index: Int) {
        _currentExpense.update { expense ->
            expense?.let {
                val currentIncomes = parseIncomeEntries(it.incomeEntries)
                if (index in currentIncomes.indices) {
                    val updatedIncomes = currentIncomes.toMutableList().apply { removeAt(index) }
                    val totalAmount = updatedIncomes.sumOf { entry -> entry.amount }

                    it.copy(
                        incomeEntries = kotlinx.serialization.json.Json.encodeToString(ListSerializer(IncomeEntry.serializer()), updatedIncomes),
                        totalIncome = totalAmount
                    )
                } else it
            }
        }
        recalculateTotals()
    }

    fun updateExcessBalance(amount: BigDecimal) {
        _currentExpense.update { expense ->
            expense?.copy(excessBalance = amount)
        }
    }

    fun updateComments(comments: String) {
        _currentExpense.update { expense ->
            expense?.copy(comments = comments)
        }
    }

    fun updateManager(managerId: Int?) {
        _currentExpense.update { expense ->
            expense?.copy(managerId = managerId)
        }
    }

    private fun recalculateTotals() {
        _currentExpense.update { expense ->
            expense?.let {
                val laborCost = calculateLaborCost(it)
                val overtimeCost = calculateOvertimeCost(it)

                it.copy(
                    totalLaborCost = laborCost,
                    totalOvertimeCost = overtimeCost
                )
            }
        }
    }

    private fun calculateLaborCost(expense: DailyExpense): BigDecimal {
        val malayaliMaleCost = expense.malayaliMaleWagePerDay * expense.malayaliMaleCount.toBigDecimal()
        val bengaliMaleCost = expense.bengaliMaleWagePerDay * expense.bengaliMaleCount.toBigDecimal()
        val malayaliFemaleCost = expense.malayaliFemaleWagePerDay * expense.malayaliFemaleCount.toBigDecimal()
        val bengaliFemaleCost = expense.bengaliFemaleWagePerDay * expense.bengaliFemaleCount.toBigDecimal()

        return malayaliMaleCost + bengaliMaleCost + malayaliFemaleCost + bengaliFemaleCost
    }

    private fun calculateOvertimeCost(expense: DailyExpense): BigDecimal {
        // Assuming overtime rate is 1.5x base rate for simplicity
        // In a real app, you'd have separate overtime rates
        val totalWorkers = (expense.malayaliMaleCount + expense.bengaliMaleCount +
                          expense.malayaliFemaleCount + expense.bengaliFemaleCount).toBigDecimal()
        val averageRate = if (totalWorkers > BigDecimal.ZERO) {
            (expense.malayaliMaleWagePerDay + expense.bengaliMaleWagePerDay +
             expense.malayaliFemaleWagePerDay + expense.bengaliFemaleWagePerDay) / BigDecimal("4")
        } else BigDecimal.ZERO

        return averageRate * BigDecimal("1.5") * expense.overtimeHours.toBigDecimal()
    }

    fun saveExpense() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSaving = true, error = null) }
                val expense = _currentExpense.value ?: throw IllegalStateException("No expense to save")

                if (expense.id == 0) {
                    repository.insertDailyExpense(expense)
                } else {
                    repository.updateDailyExpense(expense)
                }

                _uiState.update { it.copy(isSaving = false, isEditing = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun deleteExpense() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isDeleting = true, error = null) }
                val expense = _currentExpense.value ?: throw IllegalStateException("No expense to delete")

                repository.deleteDailyExpense(expense)
                _currentExpense.value = null
                _uiState.update { it.copy(isDeleting = false, isEditing = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isDeleting = false, error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun cancelEditing() {
        _currentExpense.value = null
        _uiState.update { it.copy(isEditing = false, error = null) }
    }

    private fun parseOtherExpenses(json: String): List<OtherExpenseEntry> {
        return try {
            if (json.isBlank() || json == "[]") emptyList()
            else kotlinx.serialization.json.Json.decodeFromString(ListSerializer(OtherExpenseEntry.serializer()), json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseIncomeEntries(json: String): List<IncomeEntry> {
        return try {
            if (json.isBlank() || json == "[]") emptyList()
            else kotlinx.serialization.json.Json.decodeFromString(ListSerializer(IncomeEntry.serializer()), json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Getters for current expense data
    fun getOtherExpenses(): List<OtherExpenseEntry> {
        return _currentExpense.value?.let { parseOtherExpenses(it.otherExpenses) } ?: emptyList()
    }

    fun getIncomeEntries(): List<IncomeEntry> {
        return _currentExpense.value?.let { parseIncomeEntries(it.incomeEntries) } ?: emptyList()
    }

    fun getTotalCost(): BigDecimal {
        return _currentExpense.value?.let {
            it.totalLaborCost + it.totalOvertimeCost + it.totalOtherExpensesCost
        } ?: BigDecimal.ZERO
    }

    fun getNetAmount(): BigDecimal {
        return _currentExpense.value?.calculateNetAmount() ?: BigDecimal.ZERO
    }
}

data class DailyExpenseUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val isEditing: Boolean = false,
    val error: String? = null
)
