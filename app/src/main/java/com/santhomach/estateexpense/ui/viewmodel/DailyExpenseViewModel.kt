package com.santhomach.estateexpense.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.santhomach.estateexpense.data.model.*
import com.santhomach.estateexpense.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.serialization.builtins.ListSerializer

@HiltViewModel
class DailyExpenseViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    private val application: Application
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

    val expenseSubtypes = repository.getAllActiveExpenseSubtypesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val incomeTypes = repository.getAllActiveIncomeTypesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workerTypes = repository.getAllActiveWorkerTypesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val permanentWorkers = repository.getAllActivePermanentWorkersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workTasks = repository.getAllActiveWorkTasksFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Previous excess balance calculated from all history BEFORE the current week
    val previousExcessBalance = _currentExpense.flatMapLatest { expense ->
        if (expense == null) return@flatMapLatest flowOf(BigDecimal.ZERO)
        
        val currentDate = try { LocalDate.parse(expense.date) } catch(e: Exception) { LocalDate.now() }
        val currentWeekStart = currentDate.with(java.time.DayOfWeek.MONDAY)
        val startStr = currentWeekStart.format(DateTimeFormatter.ISO_LOCAL_DATE)
        
        flow {
            val previousRecords = repository.getDailyExpensesBeforeDate(startStr)
            val totalPaid = previousRecords.fold(BigDecimal.ZERO) { acc, r -> acc + r.advanceAmount + r.weeklyPaymentDone }
            val totalExpenses = previousRecords.fold(BigDecimal.ZERO) { acc, r -> acc + r.totalLaborCost + r.totalOvertimeCost + r.totalOtherExpensesCost }
            emit(totalPaid - totalExpenses)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BigDecimal.ZERO)

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

    fun loadOrCreateExpense(date: LocalDate) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val existingExpenses = repository.getDailyExpensesByDate(dateString)

                if (existingExpenses.isNotEmpty()) {
                    // Return the first one if multiple exist for some reason
                    val expense = existingExpenses.first()
                    _currentExpense.value = expense
                    _uiState.update { it.copy(isLoading = false, isEditing = true) }
                } else {
                    val newExpense = DailyExpense(
                        date = dateString,
                        createdBy = "user"
                    )
                    _currentExpense.value = newExpense
                    _uiState.update { it.copy(isLoading = false, isEditing = true, error = null) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
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

    fun updateOvertime(hours: Int) {
        _currentExpense.update { expense ->
            expense?.copy(overtimeHours = hours)
        }
        recalculateTotals()
    }

    fun addWorkerGroup(
        workerTypeId: Int,
        count: Int,
        wage: BigDecimal,
        overtimeHours: Int,
        overtimeWagePerHour: BigDecimal,
        task: String,
        comments: String = ""
    ) {
        _currentExpense.update { expense ->
            expense?.let {
                val currentGroups = parseWorkerGroups(it.workerGroups)
                val newEntry = WorkerGroupEntry(
                    workerTypeId = workerTypeId,
                    workerTypeName = workerTypes.value.find { type -> type.id == workerTypeId }?.workerTypeName ?: "Unknown",
                    count = count,
                    wagePerDay = wage,
                    overtimeHours = overtimeHours,
                    overtimeWagePerHour = overtimeWagePerHour,
                    taskPerformed = task,
                    comments = comments
                )
                val updatedGroups = currentGroups + newEntry

                it.copy(
                    workerGroups = kotlinx.serialization.json.Json.encodeToString(ListSerializer(WorkerGroupEntry.serializer()), updatedGroups)
                )
            }
        }
        recalculateTotals()
    }

    fun removeWorkerGroup(index: Int) {
        _currentExpense.update { expense ->
            expense?.let {
                val currentGroups = parseWorkerGroups(it.workerGroups)
                if (index in currentGroups.indices) {
                    val updatedGroups = currentGroups.toMutableList().apply { removeAt(index) }
                    it.copy(
                        workerGroups = kotlinx.serialization.json.Json.encodeToString(ListSerializer(WorkerGroupEntry.serializer()), updatedGroups)
                    )
                } else it
            }
        }
        recalculateTotals()
    }

    fun updateWorkerGroup(
        index: Int,
        workerTypeId: Int,
        count: Int,
        wage: BigDecimal,
        overtimeHours: Int,
        overtimeWagePerHour: BigDecimal,
        task: String,
        comments: String = ""
    ) {
        _currentExpense.update { expense ->
            expense?.let {
                val currentGroups = parseWorkerGroups(it.workerGroups).toMutableList()
                if (index in currentGroups.indices) {
                    val newEntry = WorkerGroupEntry(
                        workerTypeId = workerTypeId,
                        workerTypeName = workerTypes.value.find { type -> type.id == workerTypeId }?.workerTypeName ?: "Unknown",
                        count = count,
                        wagePerDay = wage,
                        overtimeHours = overtimeHours,
                        overtimeWagePerHour = overtimeWagePerHour,
                        taskPerformed = task,
                        comments = comments
                    )
                    currentGroups[index] = newEntry
                    it.copy(
                        workerGroups = kotlinx.serialization.json.Json.encodeToString(ListSerializer(WorkerGroupEntry.serializer()), currentGroups)
                    )
                } else it
            }
        }
        recalculateTotals()
    }

    fun addNewWorkTask(taskName: String) {
        viewModelScope.launch {
            repository.insertWorkTask(WorkTask(taskName = taskName))
        }
    }

    fun addOtherExpense(expenseTypeId: Int, amount: BigDecimal, quantity: Double = 1.0, notes: String = "", customTypeName: String? = null,customSubtypeName: String? = null, receiptImagePath: String? = null) {
        viewModelScope.launch {
            val finalTypeId: Int
            val finalTypeName: String
            
            if (customTypeName != null) {
                finalTypeId = repository.insertExpenseType(ExpenseType(typeName = customTypeName)).toInt()
                finalTypeName = customTypeName
            } else {
                finalTypeId = expenseTypeId
                finalTypeName = expenseTypes.value.find { type -> type.id == expenseTypeId }?.typeName ?: "Unknown"
            }

            if(customSubtypeName != null){
                val existing = repository.getExpenseSubtypeByName(customSubtypeName)
                // Only insert if it doesn't exist OR if it exists but belongs to a different parent type
                if (existing == null || existing.parentTypeName.id != finalTypeId) {
                    val parentType = expenseTypes.value.find { it.id == finalTypeId }
                        ?: ExpenseType(id = finalTypeId, typeName = finalTypeName)
                    repository.insertExpenseSubtype(ExpenseSubtype(typeName = customSubtypeName, parentTypeName = parentType))
                }
            }

            _currentExpense.update { expense ->
                expense?.let {
                    val currentExpenses = parseOtherExpenses(it.otherExpenses)
                    val newEntry = OtherExpenseEntry(
                        expenseTypeId = finalTypeId,
                        typeName = finalTypeName,
                        subtypeName = customSubtypeName,
                        amount = amount,
                        quantity = quantity,
                        notes = notes,
                        receiptImagePath = receiptImagePath
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

    fun updateOtherExpense(index: Int, expenseTypeId: Int, amount: BigDecimal, quantity: Double = 1.0, notes: String = "", customTypeName: String? = null,customSubtypeName: String? = null, receiptImagePath: String? = null) {
        viewModelScope.launch {
            val finalTypeId: Int
            val finalTypeName: String
            
            if (customTypeName != null) {
                finalTypeId = repository.insertExpenseType(ExpenseType(typeName = customTypeName)).toInt()
                finalTypeName = customTypeName
            } else {
                finalTypeId = expenseTypeId
                finalTypeName = expenseTypes.value.find { type -> type.id == expenseTypeId }?.typeName ?: "Unknown"
            }
            if(customSubtypeName != null){
                val existing = repository.getExpenseSubtypeByName(customSubtypeName)
                // Only insert if it doesn't exist OR if it exists but belongs to a different parent type
                if (existing == null || existing.parentTypeName.id != finalTypeId) {
                    val parentType = expenseTypes.value.find { it.id == finalTypeId }
                        ?: ExpenseType(id = finalTypeId, typeName = finalTypeName)
                    repository.insertExpenseSubtype(ExpenseSubtype(typeName = customSubtypeName, parentTypeName = parentType))
                }
            }

            _currentExpense.update { expense ->
                expense?.let {
                    val currentExpenses = parseOtherExpenses(it.otherExpenses).toMutableList()
                    if (index in currentExpenses.indices) {
                        val newEntry = OtherExpenseEntry(
                            expenseTypeId = finalTypeId,
                            typeName = finalTypeName,
                            subtypeName = customSubtypeName,
                            amount = amount,
                            quantity = quantity,
                            notes = notes,
                            receiptImagePath = receiptImagePath ?: currentExpenses[index].receiptImagePath
                        )
                        currentExpenses[index] = newEntry
                        val totalAmount = currentExpenses.sumOf { entry -> entry.amount }

                        it.copy(
                            otherExpenses = kotlinx.serialization.json.Json.encodeToString(ListSerializer(OtherExpenseEntry.serializer()), currentExpenses),
                            totalOtherExpensesCost = totalAmount
                        )
                    } else it
                }
            }
            recalculateTotals()
        }
    }

    fun addIncome(
        incomeTypeId: Int,
        amount: BigDecimal,
        weight: Double = 0.0,
        pricePerKilo: BigDecimal = BigDecimal.ZERO,
        transportationCharge: BigDecimal = BigDecimal.ZERO,
        notes: String = "",
        customTypeName: String? = null
    ) {
        viewModelScope.launch {
            val finalTypeId: Int
            val finalTypeName: String
            
            if (customTypeName != null) {
                finalTypeId = repository.insertIncomeType(IncomeType(typeName = customTypeName)).toInt()
                finalTypeName = customTypeName
            } else {
                finalTypeId = incomeTypeId
                finalTypeName = incomeTypes.value.find { type -> type.id == incomeTypeId }?.typeName ?: "Unknown"
            }

            _currentExpense.update { expense ->
                expense?.let {
                    val currentIncomes = parseIncomeEntries(it.incomeEntries)
                    val newEntry = IncomeEntry(
                        incomeTypeId = finalTypeId,
                        typeName = finalTypeName,
                        amount = amount,
                        weight = weight,
                        pricePerKilo = pricePerKilo,
                        transportationCharge = transportationCharge,
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

    fun updateIncome(
        index: Int,
        incomeTypeId: Int,
        amount: BigDecimal,
        weight: Double = 0.0,
        pricePerKilo: BigDecimal = BigDecimal.ZERO,
        transportationCharge: BigDecimal = BigDecimal.ZERO,
        notes: String = "",
        customTypeName: String? = null
    ) {
        viewModelScope.launch {
            val finalTypeId: Int
            val finalTypeName: String
            
            if (customTypeName != null) {
                finalTypeId = repository.insertIncomeType(IncomeType(typeName = customTypeName)).toInt()
                finalTypeName = customTypeName
            } else {
                finalTypeId = incomeTypeId
                finalTypeName = incomeTypes.value.find { type -> type.id == incomeTypeId }?.typeName ?: "Unknown"
            }

            _currentExpense.update { expense ->
                expense?.let {
                    val currentIncomes = parseIncomeEntries(it.incomeEntries).toMutableList()
                    if (index in currentIncomes.indices) {
                        val newEntry = IncomeEntry(
                            incomeTypeId = finalTypeId,
                            typeName = finalTypeName,
                            amount = amount,
                            weight = weight,
                            pricePerKilo = pricePerKilo,
                            transportationCharge = transportationCharge,
                            notes = notes
                        )
                        currentIncomes[index] = newEntry
                        val totalAmount = currentIncomes.sumOf { entry -> entry.amount }

                        it.copy(
                            incomeEntries = kotlinx.serialization.json.Json.encodeToString(ListSerializer(IncomeEntry.serializer()), currentIncomes),
                            totalIncome = totalAmount
                        )
                    } else it
                }
            }
            recalculateTotals()
        }
    }

    fun updateExcessBalance(amount: BigDecimal) {
        _currentExpense.update { expense ->
            expense?.copy(excessBalance = amount)
        }
    }

    fun updateAdvanceAmount(amount: BigDecimal) {
        _currentExpense.update { expense ->
            expense?.copy(advanceAmount = amount)
        }
    }

    fun updateAdvanceReason(reason: String) {
        _currentExpense.update { expense ->
            expense?.copy(advanceReason = reason)
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
                
                val currentAdvances = parseAdvanceEntries(it.advanceEntries)
                val totalAdvance = currentAdvances.sumOf { entry -> entry.amount }

                it.copy(
                    totalLaborCost = laborCost,
                    totalOvertimeCost = overtimeCost,
                    advanceAmount = totalAdvance
                )
            }
        }
    }

    private fun calculateTotalLaborFromGroups(groups: List<WorkerGroupEntry>): BigDecimal {
        return groups.sumOf { it.wagePerDay * it.count.toBigDecimal() }
    }

    private fun calculateTotalOvertimeFromGroups(groups: List<WorkerGroupEntry>): BigDecimal {
        return groups.sumOf { (it.overtimeWagePerHour * it.overtimeHours.toBigDecimal()) * it.count.toBigDecimal() }
    }

    private fun calculateLaborCost(expense: DailyExpense): BigDecimal {
        val legacyCost = (expense.malayaliMaleWagePerDay * expense.malayaliMaleCount.toBigDecimal()) +
                (expense.bengaliMaleWagePerDay * expense.bengaliMaleCount.toBigDecimal()) +
                (expense.malayaliFemaleWagePerDay * expense.malayaliFemaleCount.toBigDecimal()) +
                (expense.bengaliFemaleWagePerDay * expense.bengaliFemaleCount.toBigDecimal())

        val groupsCost = calculateTotalLaborFromGroups(parseWorkerGroups(expense.workerGroups))

        return legacyCost + groupsCost
    }

    private fun calculateOvertimeCost(expense: DailyExpense): BigDecimal {
        val groups = parseWorkerGroups(expense.workerGroups)
        val groupsOvertime = calculateTotalOvertimeFromGroups(groups)

        val legacyWorkersCount = (expense.malayaliMaleCount + expense.bengaliMaleCount +
                expense.malayaliFemaleCount + expense.bengaliFemaleCount).toBigDecimal()

        val legacyAverageRate = if (legacyWorkersCount > BigDecimal.ZERO) {
            (expense.malayaliMaleWagePerDay + expense.bengaliMaleWagePerDay +
                    expense.malayaliFemaleWagePerDay + expense.bengaliFemaleWagePerDay) / BigDecimal("4")
        } else BigDecimal.ZERO

        val legacyOvertime = legacyAverageRate * BigDecimal("1.5") * expense.overtimeHours.toBigDecimal()

        return legacyOvertime + groupsOvertime
    }

    fun saveExpense() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSaving = true, error = null) }
                val expense = _currentExpense.value ?: throw IllegalStateException("No expense to save")

                val id = if (expense.id == 0) {
                    repository.insertDailyExpense(expense)
                } else {
                    repository.updateDailyExpense(expense)
                    expense.id.toLong()
                }

                // Check and auto-create weekly funds if this is the first expense of the week
                try {
                    val expenseDate = LocalDate.parse(expense.date)
                    val monday = expenseDate.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                    val mondayStr = monday.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                    
                    val allWeeklyFunds = repository.getAllWeeklyFundsFlow().first()
                    if (allWeeklyFunds.none { it.weekStartDate == mondayStr }) {
                        repository.insertWeeklyFunds(
                            com.santhomach.estateexpense.data.model.WeeklyFunds(
                                weekStartDate = mondayStr,
                                amountReceived = BigDecimal.ZERO,
                                notes = "Auto-created from first expense of the week"
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Log but don't fail saving expense if weekly funds creation fails
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

    private fun parseWorkerGroups(json: String): List<WorkerGroupEntry> {
        return try {
            if (json.isBlank() || json == "[]") emptyList()
            else kotlinx.serialization.json.Json.decodeFromString(ListSerializer(WorkerGroupEntry.serializer()), json)
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

    fun getWorkerGroups(): List<WorkerGroupEntry> {
        return _currentExpense.value?.let { parseWorkerGroups(it.workerGroups) } ?: emptyList()
    }

    fun updateWeeklyPaymentDone(amount: BigDecimal) {
        _currentExpense.update { expense ->
            expense?.copy(weeklyPaymentDone = amount)
        }
    }

    fun addAdvanceEntry(amount: BigDecimal, reason: String, recipient: String) {
        _currentExpense.update { expense ->
            expense?.let {
                val current = parseAdvanceEntries(it.advanceEntries)
                val newEntry = AdvanceEntry(amount = amount, reason = reason, recipientName = recipient)
                val updated = current + newEntry
                val totalAmount = updated.sumOf { entry -> entry.amount }
                it.copy(
                    advanceEntries = kotlinx.serialization.json.Json.encodeToString(ListSerializer(AdvanceEntry.serializer()), updated),
                    advanceAmount = totalAmount
                )
            }
        }
    }

    fun removeAdvanceEntry(index: Int) {
        _currentExpense.update { expense ->
            expense?.let {
                val current = parseAdvanceEntries(it.advanceEntries)
                if (index in current.indices) {
                    val updated = current.toMutableList().apply { removeAt(index) }
                    val totalAmount = updated.sumOf { entry -> entry.amount }
                    it.copy(
                        advanceEntries = kotlinx.serialization.json.Json.encodeToString(ListSerializer(AdvanceEntry.serializer()), updated),
                        advanceAmount = totalAmount
                    )
                } else it
            }
        }
    }

    fun updateAdvanceEntry(index: Int, amount: BigDecimal, reason: String, recipient: String) {
        _currentExpense.update { expense ->
            expense?.let {
                val current = parseAdvanceEntries(it.advanceEntries).toMutableList()
                if (index in current.indices) {
                    val newEntry = AdvanceEntry(amount = amount, reason = reason, recipientName = recipient)
                    current[index] = newEntry
                    val totalAmount = current.sumOf { entry -> entry.amount }
                    it.copy(
                        advanceEntries = kotlinx.serialization.json.Json.encodeToString(ListSerializer(AdvanceEntry.serializer()), current),
                        advanceAmount = totalAmount
                    )
                } else it
            }
        }
    }

    private fun parseAdvanceEntries(json: String): List<AdvanceEntry> {
        return try {
            if (json.isBlank() || json == "[]") emptyList()
            else kotlinx.serialization.json.Json.decodeFromString(ListSerializer(AdvanceEntry.serializer()), json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getAdvanceEntries(): List<AdvanceEntry> {
        return _currentExpense.value?.let { parseAdvanceEntries(it.advanceEntries) } ?: emptyList()
    }

    fun getTotalActualExpenses(): BigDecimal {
        return _currentExpense.value?.let {
            it.totalLaborCost + it.totalOvertimeCost + it.totalOtherExpensesCost
        } ?: BigDecimal.ZERO
    }

    fun getTotalPaymentsMade(): BigDecimal {
        return _currentExpense.value?.let {
            it.advanceAmount + it.weeklyPaymentDone
        } ?: BigDecimal.ZERO
    }

    fun getNetAmount(): BigDecimal {
        val expenses = getTotalActualExpenses()
        val payments = getTotalPaymentsMade()
        val previousExcess = previousExcessBalance.value ?: BigDecimal.ZERO
        // Offset logic: (Current Payments + Carry-over) - Actual Costs
        return (payments + previousExcess) - expenses
    }

    fun saveReceiptImage(uri: Uri): String? {
        return try {
            val inputStream = application.contentResolver.openInputStream(uri) ?: return null
            val fileName = "receipt_${System.currentTimeMillis()}.jpg"
            val file = File(application.filesDir, "receipts").apply {
                if (!exists()) mkdirs()
            }
            val targetFile = File(file, fileName)

            inputStream.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            targetFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}

data class DailyExpenseUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val isEditing: Boolean = false,
    val error: String? = null
)
