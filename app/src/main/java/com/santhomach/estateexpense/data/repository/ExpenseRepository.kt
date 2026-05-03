package com.santhomach.estateexpense.data.repository

import com.santhomach.estateexpense.data.AppDatabase
import com.santhomach.estateexpense.data.model.*
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepository @Inject constructor(
    private val database: AppDatabase
) {
    private val expenseDao = database.dailyExpenseDao()
    private val expenseTypeDao = database.expenseTypeDao()

    private val expenseSubtypeDao = database.expenseSubtypeDao()
    private val incomeTypeDao = database.incomeTypeDao()
    private val workerTypeDao = database.workerTypeDao()
    private val permanentWorkerDao = database.permanentWorkerDao()
    private val settlementDao = database.weeklySettlementDao()
    private val balanceDao = database.excessBalanceDao()
    private val taskDao = database.workTaskDao()
    private val paymentDao = database.workerPaymentDao()
    private val fundsDao = database.weeklyFundsDao()

    // Daily Expense Operations
    suspend fun insertDailyExpense(expense: DailyExpense): Long {
        return expenseDao.insert(expense.copy(updatedAt = LocalDateTime.now().toString()))
    }

    suspend fun updateDailyExpense(expense: DailyExpense) {
        expenseDao.update(expense.copy(updatedAt = LocalDateTime.now().toString()))
    }

    suspend fun deleteDailyExpense(expense: DailyExpense) {
        expenseDao.delete(expense)
    }

    suspend fun getDailyExpenseById(id: Int): DailyExpense? {
        return expenseDao.getById(id)
    }

    suspend fun getDailyExpensesByDate(date: String): List<DailyExpense> {
        return expenseDao.getByDate(date)
    }

    fun getDailyExpensesByDateFlow(date: String): Flow<List<DailyExpense>> {
        return expenseDao.getByDateFlow(date)
    }

    suspend fun getDailyExpensesByDateRange(startDate: String, endDate: String): List<DailyExpense> {
        return expenseDao.getByDateRange(startDate, endDate)
    }

    fun getDailyExpensesByDateRangeFlow(startDate: String, endDate: String): Flow<List<DailyExpense>> {
        return expenseDao.getByDateRangeFlow(startDate, endDate)
    }

    suspend fun getRecentExpenses(limit: Int = 50): List<DailyExpense> {
        return expenseDao.getRecent(limit)
    }

    suspend fun getDailyExpensesBeforeDate(date: String): List<DailyExpense> {
        return expenseDao.getBeforeDate(date)
    }

    suspend fun searchDailyExpenses(query: String): List<DailyExpense> {
        return expenseDao.searchExpenses(query)
    }

    fun getRecentExpensesFlow(limit: Int = 50): Flow<List<DailyExpense>> {
        return expenseDao.getRecentFlow(limit)
    }

    // Expense Type Operations
    suspend fun insertExpenseType(expenseType: ExpenseType): Long {
        return expenseTypeDao.insert(expenseType)
    }

    suspend fun updateExpenseType(expenseType: ExpenseType) {
        expenseTypeDao.update(expenseType)
    }

    suspend fun deleteExpenseType(expenseType: ExpenseType) {
        expenseTypeDao.delete(expenseType)
    }

    suspend fun getAllActiveExpenseTypes(): List<ExpenseType> {
        return expenseTypeDao.getAllActive()
    }

    fun getAllActiveExpenseTypesFlow(): Flow<List<ExpenseType>> {
        return expenseTypeDao.getAllActiveFlow()
    }

    //Expense Subtype Operations
    suspend fun insertExpenseSubtype(expenseSubtype: ExpenseSubtype): Long {
        return expenseSubtypeDao.insert(expenseSubtype)
    }

    suspend fun updateExpenseSubtype(expenseSubtype: ExpenseSubtype) {
        expenseSubtypeDao.update(expenseSubtype)
    }

    suspend fun deleteExpenseSubtype(expenseSubtype: ExpenseSubtype) {
        expenseSubtypeDao.delete(expenseSubtype)
    }

    suspend fun getAllActiveExpenseSubtypes(): List<ExpenseSubtype> {
        return expenseSubtypeDao.getAllActive()
    }

    suspend fun getExpenseSubtypeByName(name: String): ExpenseSubtype? {
        return expenseSubtypeDao.getByName(name)
    }

    fun getAllActiveExpenseSubtypesFlow(): Flow<List<ExpenseSubtype>> {
        return expenseSubtypeDao.getAllActiveFlow()
    }

    // Income Type Operations
    suspend fun insertIncomeType(incomeType: IncomeType): Long {
        return incomeTypeDao.insert(incomeType)
    }

    suspend fun updateIncomeType(incomeType: IncomeType) {
        incomeTypeDao.update(incomeType)
    }

    suspend fun deleteIncomeType(incomeType: IncomeType) {
        incomeTypeDao.delete(incomeType)
    }

    suspend fun getAllActiveIncomeTypes(): List<IncomeType> {
        return incomeTypeDao.getAllActive()
    }

    fun getAllActiveIncomeTypesFlow(): Flow<List<IncomeType>> {
        return incomeTypeDao.getAllActiveFlow()
    }

    // Worker Type Operations
    suspend fun insertWorkerType(workerType: WorkerType): Long {
        return workerTypeDao.insert(workerType)
    }

    suspend fun updateWorkerType(workerType: WorkerType) {
        workerTypeDao.update(workerType)
    }

    suspend fun deleteWorkerType(workerType: WorkerType) {
        workerTypeDao.delete(workerType)
    }

    suspend fun getAllActiveWorkerTypes(): List<WorkerType> {
        return workerTypeDao.getAllActive()
    }

    fun getAllActiveWorkerTypesFlow(): Flow<List<WorkerType>> {
        return workerTypeDao.getAllActiveFlow()
    }

    // Permanent Worker Operations
    suspend fun insertPermanentWorker(worker: PermanentWorker): Long {
        return permanentWorkerDao.insert(worker)
    }

    suspend fun updatePermanentWorker(worker: PermanentWorker) {
        permanentWorkerDao.update(worker)
    }

    suspend fun deletePermanentWorker(worker: PermanentWorker) {
        permanentWorkerDao.delete(worker)
    }

    suspend fun getAllActivePermanentWorkers(): List<PermanentWorker> {
        return permanentWorkerDao.getAllActive()
    }

    fun getAllActivePermanentWorkersFlow(): Flow<List<PermanentWorker>> {
        return permanentWorkerDao.getAllActiveFlow()
    }

    // Worker Payment Operations
    suspend fun insertWorkerPayment(payment: WorkerPayment): Long {
        return paymentDao.insert(payment)
    }

    suspend fun updateWorkerPayment(payment: WorkerPayment) {
        paymentDao.update(payment)
    }

    suspend fun deleteWorkerPayment(payment: WorkerPayment) {
        paymentDao.delete(payment)
    }

    fun getAllPaymentsFlow(): Flow<List<WorkerPayment>> {
        return paymentDao.getAllPaymentsFlow()
    }

    suspend fun getPaymentByWorkerAndDate(workerId: Int, date: String): WorkerPayment? {
        return paymentDao.getByWorkerAndDate(workerId, date)
    }

    // Weekly Settlement Operations
    suspend fun insertWeeklySettlement(settlement: WeeklySettlement): Long {
        return settlementDao.insert(settlement.copy(updatedAt = LocalDateTime.now().toString()))
    }

    suspend fun updateWeeklySettlement(settlement: WeeklySettlement) {
        settlementDao.update(settlement.copy(updatedAt = LocalDateTime.now().toString()))
    }

    suspend fun getWeeklySettlementById(id: Int): WeeklySettlement? {
        return settlementDao.getById(id)
    }

    suspend fun getWeeklySettlementByDate(date: String): WeeklySettlement? {
        return settlementDao.getBySettlementDate(date)
    }

    suspend fun getWeeklySettlementsByDateRange(startDate: String, endDate: String): List<WeeklySettlement> {
        return settlementDao.getByDateRange(startDate, endDate)
    }

    fun getAllWeeklySettlementsFlow(): Flow<List<WeeklySettlement>> {
        return settlementDao.getAllFlow()
    }

    // Excess Balance Operations
    suspend fun insertExcessBalance(balance: ExcessBalance): Long {
        return balanceDao.insert(balance)
    }

    suspend fun updateExcessBalance(balance: ExcessBalance) {
        balanceDao.update(balance)
    }

    suspend fun getUnsettledBalances(): List<ExcessBalance> {
        return balanceDao.getUnsettled()
    }

    fun getUnsettledBalancesFlow(): Flow<List<ExcessBalance>> {
        return balanceDao.getUnsettledFlow()
    }

    suspend fun getTotalUnsettledBalance(): BigDecimal {
        val totalStr = balanceDao.getTotalUnsettledBalance()
        return totalStr?.toBigDecimalOrNull() ?: BigDecimal.ZERO
    }

    // Work Task Operations
    suspend fun insertWorkTask(task: WorkTask): Long {
        return taskDao.insert(task)
    }

    suspend fun updateWorkTask(task: WorkTask) {
        taskDao.update(task)
    }

    suspend fun deleteWorkTask(task: WorkTask) {
        taskDao.delete(task)
    }

    suspend fun getAllActiveWorkTasks(): List<WorkTask> {
        return taskDao.getAllActive()
    }

    fun getAllActiveWorkTasksFlow(): Flow<List<WorkTask>> {
        return taskDao.getAllActiveFlow()
    }

    // Weekly Funds Operations
    suspend fun insertWeeklyFunds(funds: WeeklyFunds): Long {
        return fundsDao.insert(funds)
    }

    suspend fun updateWeeklyFunds(funds: WeeklyFunds) {
        fundsDao.update(funds)
    }

    suspend fun deleteWeeklyFunds(funds: WeeklyFunds) {
        fundsDao.delete(funds)
    }

    fun getAllWeeklyFundsFlow(): Flow<List<WeeklyFunds>> {
        return fundsDao.getAllFlow()
    }

    // Analytics and Reports
    fun getDailyExpenseSummaryFlow(startDate: String, endDate: String): Flow<ExpenseSummary> {
        return getDailyExpensesByDateRangeFlow(startDate, endDate).map { expenses ->
            ExpenseSummary(
                totalIncome = expenses.sumOf { it.totalIncome },
                totalLaborCost = expenses.sumOf { it.totalLaborCost },
                totalOvertimeCost = expenses.sumOf { it.totalOvertimeCost },
                totalOtherExpenses = expenses.sumOf { it.totalOtherExpensesCost },
                totalAdvanceAmount = expenses.sumOf { it.advanceAmount },
                totalExcessBalance = expenses.sumOf { it.excessBalance },
                totalWeeklyPayment = expenses.sumOf { it.weeklyPaymentDone },
                netAmount = expenses.sumOf { it.calculateNetAmount() },
                totalDays = expenses.size,
                averageDailyIncome = if (expenses.isNotEmpty()) expenses.sumOf { it.totalIncome } / expenses.size.toBigDecimal() else BigDecimal.ZERO,
                averageDailyExpense = if (expenses.isNotEmpty()) expenses.sumOf { (it.totalLaborCost + it.totalOvertimeCost + it.totalOtherExpensesCost + it.advanceAmount + it.weeklyPaymentDone - it.excessBalance).coerceAtLeast(BigDecimal.ZERO) } / expenses.size.toBigDecimal() else BigDecimal.ZERO
            )
        }
    }

    fun getWeeklyExpenseSummaryFlow(startDate: String, endDate: String): Flow<WeeklyExpenseSummary> {
        return getDailyExpenseSummaryFlow(startDate, endDate).map { dailySummary ->
            WeeklyExpenseSummary(
                totalIncome = dailySummary.totalIncome,
                totalLaborCost = dailySummary.totalLaborCost,
                totalOvertimeCost = dailySummary.totalOvertimeCost,
                totalOtherExpenses = dailySummary.totalOtherExpenses,
                totalAdvanceAmount = dailySummary.totalAdvanceAmount,
                totalWeeklyPayment = dailySummary.totalWeeklyPayment,
                totalExcessBalance = dailySummary.totalExcessBalance,
                netAmount = dailySummary.netAmount,
                totalWeeks = (dailySummary.totalDays / 7).coerceAtLeast(1),
                averageWeeklyIncome = dailySummary.totalIncome / (dailySummary.totalDays / 7).coerceAtLeast(1).toBigDecimal(),
                averageWeeklyExpense = (dailySummary.totalLaborCost + dailySummary.totalOvertimeCost + dailySummary.totalOtherExpenses + dailySummary.totalAdvanceAmount + dailySummary.totalWeeklyPayment - dailySummary.totalExcessBalance).coerceAtLeast(BigDecimal.ZERO) / (dailySummary.totalDays / 7).coerceAtLeast(1).toBigDecimal()
            )
        }
    }

    // Data initialization
    suspend fun initializeDefaultData() {
        // Initialize default expense types
        val defaultExpenseTypes = listOf(
            ExpenseType(typeName = "Pesticides", description = "Pesticide purchases"),
            ExpenseType(typeName = "Fuel", description = "Fuel purchases"),
            ExpenseType(typeName = "Fertilizers", description = "Fertilizer purchases"),
            ExpenseType(typeName = "Capital Expenses", description = "Capital Expenses"),
            ExpenseType(typeName = "Cardamom Drying", description = "Drying Cost"),
            ExpenseType(typeName = "Equipment", description = "Equipment maintenance and purchases"),
            ExpenseType(typeName = "Transportation", description = "Transport costs"),
            ExpenseType(typeName = "Utilities", description = "Electricity, water, etc."),
            ExpenseType(typeName = "Maintenance", description = "General maintenance"),
            ExpenseType(typeName = "PropertyTax", description = "Property Tax"),
            ExpenseType(typeName = "Other", description = "Miscellaneous expenses")
        )

        val defaultExpenseSubtypes = listOf(
            ExpenseSubtype(typeName = "Diesel", parentTypeName = ExpenseType(typeName = "Fuel", description = "Fuel purchases") ,description = "Pesticide purchases"),
            ExpenseSubtype(typeName = "Petrol",parentTypeName = ExpenseType(typeName = "Fuel", description = "Fuel purchases"), description = "Fertilizer purchases"),
            /*ExpenseSubtype(typeName = "Seeds", description = "Seed purchases"),
            ExpenseSubtype(typeName = "Equipment", description = "Equipment maintenance and purchases"),
            ExpenseSubtype(typeName = "Transportation", description = "Transport costs"),
            ExpenseSubtype(typeName = "Utilities", description = "Electricity, water, etc."),
            ExpenseSubtype(typeName = "Maintenance", description = "General maintenance"),
            ExpenseSubtype(typeName = "Other", description = "Miscellaneous expenses")*/
        )

        for (type in defaultExpenseTypes) {
            if (expenseTypeDao.getAllActive().none { it.typeName == type.typeName }) {
                expenseTypeDao.insert(type)
            }
        }

        for (subtype in defaultExpenseSubtypes) {
            if (expenseSubtypeDao.getAllActive().none { it.typeName == subtype.typeName }) {
                expenseSubtypeDao.insert(subtype)
            }
        }

        // Initialize default income types
        val defaultIncomeTypes = listOf(
            IncomeType(typeName = "Cardamom Sales", description = "Cardamom harvest sales"),
            IncomeType(typeName = "Pepper Sales", description = "Pepper harvest sales"),
            IncomeType(typeName = "Other Income", description = "Miscellaneous income")
        )

        for (type in defaultIncomeTypes) {
            if (incomeTypeDao.getAllActive().none { it.typeName == type.typeName }) {
                incomeTypeDao.insert(type)
            }
        }

        // Initialize default worker types
        val defaultWorkerTypes = listOf(
            WorkerType(workerTypeName = "Malayali Male", dailyBasicWage = BigDecimal("500")),
            WorkerType(workerTypeName = "Bengali Male", dailyBasicWage = BigDecimal("500")),
            WorkerType(workerTypeName = "Malayali Female", dailyBasicWage = BigDecimal("450")),
            WorkerType(workerTypeName = "Bengali Female", dailyBasicWage = BigDecimal("450"))
        )

        for (type in defaultWorkerTypes) {
            if (workerTypeDao.getAllActive().none { it.workerTypeName == type.workerTypeName }) {
                workerTypeDao.insert(type)
            }
        }

        // Initialize default work tasks
        val defaultTasks = listOf(
            WorkTask(taskName = "Spraying", description = "Spraying pesticides/fertilizers"),
            WorkTask(taskName = "Weeding", description = "Removing weeds"),
            WorkTask(taskName = "Harvesting", description = "Harvesting crops"),
            WorkTask(taskName = "Pruning", description = "Pruning plants"),
            WorkTask(taskName = "Planting", description = "Planting new saplings"),
            WorkTask(taskName = "Drying", description = "Drying cardamom/pepper"),
            WorkTask(taskName = "Other", description = "Other farm work")
        )

        for (task in defaultTasks) {
            if (taskDao.getAllActive().none { it.taskName == task.taskName }) {
                taskDao.insert(task)
            }
        }
    }
}

// Summary data classes
data class ExpenseSummary(
    val totalIncome: BigDecimal = BigDecimal.ZERO,
    val totalLaborCost: BigDecimal = BigDecimal.ZERO,
    val totalOvertimeCost: BigDecimal = BigDecimal.ZERO,
    val totalOtherExpenses: BigDecimal = BigDecimal.ZERO,
    val totalAdvanceAmount: BigDecimal = BigDecimal.ZERO,
    val totalWeeklyPayment: BigDecimal = BigDecimal.ZERO,
    val totalExcessBalance: BigDecimal = BigDecimal.ZERO,
    val netAmount: BigDecimal = BigDecimal.ZERO,
    val totalDays: Int = 0,
    val averageDailyIncome: BigDecimal = BigDecimal.ZERO,
    val averageDailyExpense: BigDecimal = BigDecimal.ZERO
)

data class WeeklyExpenseSummary(
    val totalIncome: BigDecimal = BigDecimal.ZERO,
    val totalLaborCost: BigDecimal = BigDecimal.ZERO,
    val totalOvertimeCost: BigDecimal = BigDecimal.ZERO,
    val totalOtherExpenses: BigDecimal = BigDecimal.ZERO,
    val totalAdvanceAmount: BigDecimal = BigDecimal.ZERO,
    val totalWeeklyPayment: BigDecimal = BigDecimal.ZERO,
    val totalExcessBalance: BigDecimal = BigDecimal.ZERO,
    val netAmount: BigDecimal = BigDecimal.ZERO,
    val totalWeeks: Int = 0,
    val averageWeeklyIncome: BigDecimal = BigDecimal.ZERO,
    val averageWeeklyExpense: BigDecimal = BigDecimal.ZERO
)
