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
    private val incomeTypeDao = database.incomeTypeDao()
    private val workerTypeDao = database.workerTypeDao()
    private val permanentWorkerDao = database.permanentWorkerDao()
    private val settlementDao = database.weeklySettlementDao()
    private val balanceDao = database.excessBalanceDao()

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

    fun getRecentExpensesFlow(limit: Int = 50): Flow<List<DailyExpense>> {
        return expenseDao.getRecentFlow(limit)
    }

    // Expense Type Operations
    suspend fun insertExpenseType(expenseType: ExpenseType): Long {
        return expenseTypeDao.insert(expenseType)
    }

    suspend fun getAllActiveExpenseTypes(): List<ExpenseType> {
        return expenseTypeDao.getAllActive()
    }

    fun getAllActiveExpenseTypesFlow(): Flow<List<ExpenseType>> {
        return expenseTypeDao.getAllActiveFlow()
    }

    // Income Type Operations
    suspend fun insertIncomeType(incomeType: IncomeType): Long {
        return incomeTypeDao.insert(incomeType)
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

    suspend fun getAllActivePermanentWorkers(): List<PermanentWorker> {
        return permanentWorkerDao.getAllActive()
    }

    fun getAllActivePermanentWorkersFlow(): Flow<List<PermanentWorker>> {
        return permanentWorkerDao.getAllActiveFlow()
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

    // Analytics and Reports
    fun getDailyExpenseSummaryFlow(startDate: String, endDate: String): Flow<ExpenseSummary> {
        return getDailyExpensesByDateRangeFlow(startDate, endDate).map { expenses ->
            ExpenseSummary(
                totalIncome = expenses.sumOf { it.totalIncome },
                totalLaborCost = expenses.sumOf { it.totalLaborCost },
                totalOvertimeCost = expenses.sumOf { it.totalOvertimeCost },
                totalOtherExpenses = expenses.sumOf { it.totalOtherExpensesCost },
                totalExcessBalance = expenses.sumOf { it.excessBalance },
                netAmount = expenses.sumOf { it.calculateNetAmount() },
                totalDays = expenses.size,
                averageDailyIncome = if (expenses.isNotEmpty()) expenses.sumOf { it.totalIncome } / expenses.size.toBigDecimal() else BigDecimal.ZERO,
                averageDailyExpense = if (expenses.isNotEmpty()) expenses.sumOf { it.totalLaborCost + it.totalOvertimeCost + it.totalOtherExpensesCost } / expenses.size.toBigDecimal() else BigDecimal.ZERO
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
                totalExcessBalance = dailySummary.totalExcessBalance,
                netAmount = dailySummary.netAmount,
                totalWeeks = (dailySummary.totalDays / 7).coerceAtLeast(1),
                averageWeeklyIncome = dailySummary.totalIncome / (dailySummary.totalDays / 7).coerceAtLeast(1).toBigDecimal(),
                averageWeeklyExpense = (dailySummary.totalLaborCost + dailySummary.totalOvertimeCost + dailySummary.totalOtherExpenses) / (dailySummary.totalDays / 7).coerceAtLeast(1).toBigDecimal()
            )
        }
    }

    // Data initialization
    suspend fun initializeDefaultData() {
        // Initialize default expense types
        val defaultExpenseTypes = listOf(
            ExpenseType(typeName = "Pesticides", description = "Pesticide purchases"),
            ExpenseType(typeName = "Fertilizers", description = "Fertilizer purchases"),
            ExpenseType(typeName = "Seeds", description = "Seed purchases"),
            ExpenseType(typeName = "Equipment", description = "Equipment maintenance and purchases"),
            ExpenseType(typeName = "Transportation", description = "Transport costs"),
            ExpenseType(typeName = "Utilities", description = "Electricity, water, etc."),
            ExpenseType(typeName = "Maintenance", description = "General maintenance"),
            ExpenseType(typeName = "Other", description = "Miscellaneous expenses")
        )

        for (type in defaultExpenseTypes) {
            if (expenseTypeDao.getAllActive().none { it.typeName == type.typeName }) {
                expenseTypeDao.insert(type)
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
            WorkerType(workerTypeName = "Malayalam Male", dailyBasicWage = BigDecimal("500")),
            WorkerType(workerTypeName = "Bengali Male", dailyBasicWage = BigDecimal("500")),
            WorkerType(workerTypeName = "Malayalam Female", dailyBasicWage = BigDecimal("450")),
            WorkerType(workerTypeName = "Bengali Female", dailyBasicWage = BigDecimal("450"))
        )

        for (type in defaultWorkerTypes) {
            if (workerTypeDao.getAllActive().none { it.workerTypeName == type.workerTypeName }) {
                workerTypeDao.insert(type)
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
    val totalExcessBalance: BigDecimal = BigDecimal.ZERO,
    val netAmount: BigDecimal = BigDecimal.ZERO,
    val totalWeeks: Int = 0,
    val averageWeeklyIncome: BigDecimal = BigDecimal.ZERO,
    val averageWeeklyExpense: BigDecimal = BigDecimal.ZERO
)
