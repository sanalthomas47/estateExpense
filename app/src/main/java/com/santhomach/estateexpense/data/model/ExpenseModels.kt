package com.santhomach.estateexpense.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import com.santhomach.estateexpense.data.serialization.BigDecimalSerializer

/**
 * Predefined expense types (e.g., Pesticide, Fertilizer, Seeds)
 * This table is populated during app initialization
 */
@Entity(tableName = "expense_types")
@Serializable
data class ExpenseType(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val typeName: String,
    val description: String = "",
    val isActive: Boolean = true,
    val createdAt: String = LocalDateTime.now().toString()
)

/**
 * Predefined income types (e.g., Cardamom Sales, Pepper Sales, Other)
 */
@Entity(tableName = "income_types")
@Serializable
data class IncomeType(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val typeName: String,
    val description: String = "",
    val isActive: Boolean = true,
    val createdAt: String = LocalDateTime.now().toString()
)

/**
 * Permanent workers (stable employees)
 */
@Entity(tableName = "permanent_workers")
@Serializable
data class PermanentWorker(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val role: String, // "Manager", "Permanent Labor", "Specialist"
    @Serializable(with = BigDecimalSerializer::class)
    val dailyBasicWage: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val dailyOvertimeRate: BigDecimal = BigDecimal.ZERO,
    val isActive: Boolean = true,
    val hireDate: String = LocalDate.now().toString(),
    val notes: String = "",
    val createdAt: String = LocalDateTime.now().toString()
)

/**
 * Worker types and their daily wages (for rotating workers)
 * E.g., Malayalam Male, Bengali Male, Malayalam Female, Bengali Female
 */
@Entity(tableName = "worker_types")
@Serializable
data class WorkerType(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val workerTypeName: String, // E.g., "Malayalam Male", "Bengali Female"
    @Serializable(with = BigDecimalSerializer::class)
    val dailyBasicWage: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val dailyOvertimeRate: BigDecimal = BigDecimal.ZERO,
    val isActive: Boolean = true,
    val createdAt: String = LocalDateTime.now().toString()
)

/**
 * Predefined work tasks (e.g., Spraying, Weeding, Harvesting)
 */
@Entity(tableName = "work_tasks")
@Serializable
data class WorkTask(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val taskName: String,
    val description: String = "",
    val isActive: Boolean = true,
    val createdAt: String = LocalDateTime.now().toString()
)

/**
 * Main daily expense/income record
 */
@Entity(
    tableName = "daily_expenses",
    indices = [
        Index(value = ["date"]),
        Index(value = ["managerId"]),
        Index(value = ["date", "managerId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = PermanentWorker::class,
            parentColumns = ["id"],
            childColumns = ["managerId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
@Serializable
data class DailyExpense(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Core date field
    val date: String, // ISO format: YYYY-MM-DD

    // Worker counts by type
    val malayaliMaleCount: Int = 0,
    val bengaliMaleCount: Int = 0,
    val malayaliFemaleCount: Int = 0,
    val bengaliFemaleCount: Int = 0,

    // Wage components
    @Serializable(with = BigDecimalSerializer::class)
    val malayaliMaleWagePerDay: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val bengaliMaleWagePerDay: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val malayaliFemaleWagePerDay: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val bengaliFemaleWagePerDay: BigDecimal = BigDecimal.ZERO,

    // Calculated labor cost
    @Serializable(with = BigDecimalSerializer::class)
    val totalLaborCost: BigDecimal = BigDecimal.ZERO,

    // Overtime
    val overtimeHours: Int = 0,
    @Serializable(with = BigDecimalSerializer::class)
    val totalOvertimeCost: BigDecimal = BigDecimal.ZERO,

    // Other expenses (can be multiple per day)
    // Stored as JSON string for flexibility
    val otherExpenses: String = "[]", // JSON array of {expenseTypeId, amount, notes}
    @Serializable(with = BigDecimalSerializer::class)
    val totalOtherExpensesCost: BigDecimal = BigDecimal.ZERO,

    // Income entries (can be multiple per day)
    val incomeEntries: String = "[]", // JSON array of {incomeTypeId, amount, notes}
    @Serializable(with = BigDecimalSerializer::class)
    val totalIncome: BigDecimal = BigDecimal.ZERO,

    // Excess balance (if workers brought on Fri-Sat exceed expectation)
    @Serializable(with = BigDecimalSerializer::class)
    val excessBalance: BigDecimal = BigDecimal.ZERO,

    // Advance amount paid on this day
    @Serializable(with = BigDecimalSerializer::class)
    val advanceAmount: BigDecimal = BigDecimal.ZERO,

    // Worker groups with tasks and comments (stored as JSON)
    val workerGroups: String = "[]", // JSON array of WorkerGroupEntry

    // Metadata
    val managerId: Int? = null,
    val comments: String = "",

    // Audit trail
    val createdAt: String = LocalDateTime.now().toString(),
    val updatedAt: String = LocalDateTime.now().toString(),
    val createdBy: String = "system",
    val expenseAdditionType: String = "manual" // "manual", "import", "bulk"
) {
    // Convenience function to calculate net (income - expenses)
    fun calculateNetAmount(): BigDecimal {
        val totalExpenses = totalLaborCost + totalOvertimeCost + totalOtherExpensesCost + advanceAmount
        return totalIncome - totalExpenses
    }
}

@Serializable
data class WorkerGroupEntry(
    val workerTypeId: Int,
    val workerTypeName: String,
    val count: Int,
    @Serializable(with = BigDecimalSerializer::class)
    val wagePerDay: BigDecimal,
    val overtimeHours: Int = 0,
    @Serializable(with = BigDecimalSerializer::class)
    val overtimeWagePerHour: BigDecimal = BigDecimal.ZERO,
    val taskPerformed: String,
    val comments: String = "",
    val addedAt: String = LocalDateTime.now().toString()
) {
    fun calculateTotalGroupCost(): BigDecimal {
        val baseWage = wagePerDay * count.toBigDecimal()
        val overtimeWage = (overtimeWagePerHour * overtimeHours.toBigDecimal()) * count.toBigDecimal()
        return baseWage + overtimeWage
    }
}

/**
 * Other expense entry (sub-record of DailyExpense)
 */
@Serializable
data class OtherExpenseEntry(
    val expenseTypeId: Int,
    val typeName: String,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    val notes: String = "",
    val addedAt: String = LocalDateTime.now().toString()
)

/**
 * Income entry (sub-record of DailyExpense)
 */
@Serializable
data class IncomeEntry(
    val incomeTypeId: Int,
    val typeName: String,
    val weight: Double = 0.0,
    @Serializable(with = BigDecimalSerializer::class)
    val pricePerKilo: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val transportationCharge: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal, // Total amount = (weight * pricePerKilo) - transportationCharge (usually)
    val notes: String = "",
    val addedAt: String = LocalDateTime.now().toString()
)

/**
 * Weekly settlement record (for payment calculations)
 */
@Entity(
    tableName = "weekly_settlements",
    indices = [
        Index(value = ["settlementDate"]),
        Index(value = ["weekStartDate", "weekEndDate"])
    ]
)
@Serializable
data class WeeklySettlement(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val settlementDate: String, // Thursday of the week (ISO format)
    val weekStartDate: String, // Monday (ISO format)
    val weekEndDate: String, // Thursday (ISO format)

    // Mon-Thu calculations
    val totalWorkersMonThur: Int = 0,
    @Serializable(with = BigDecimalSerializer::class)
    val totalLaborCostMonThur: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val totalTransportMonThur: BigDecimal = BigDecimal.ZERO,

    // Fri-Sat adjustments
    val totalWorkersFriSat: Int = 0,
    @Serializable(with = BigDecimalSerializer::class)
    val totalLaborCostFriSat: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val differenceAmount: BigDecimal = BigDecimal.ZERO,

    // Payment tracking
    @Serializable(with = BigDecimalSerializer::class)
    val carryoverFromPreviousWeek: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val amountToPay: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val amountPaid: BigDecimal = BigDecimal.ZERO,
    val paymentDate: String? = null,
    val paymentMethod: String = "", // "Cash", "Bank", "Check"
    val paymentNotes: String = "",

    // Carryover to next week
    @Serializable(with = BigDecimalSerializer::class)
    val carryoverToNextWeek: BigDecimal = BigDecimal.ZERO,

    val createdAt: String = LocalDateTime.now().toString(),
    val updatedAt: String = LocalDateTime.now().toString()
)

/**
 * Excess balance tracker (for Fri-Sat differences)
 */
@Entity(
    tableName = "excess_balances",
    indices = [
        Index(value = ["date"]),
        Index(value = ["managerId"])
    ]
)
@Serializable
data class ExcessBalance(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val date: String, // ISO format
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    val direction: String, // "CREDIT" (manager gets money) or "DEBIT" (manager gives money)
    val reason: String, // e.g., "Fri-Sat excess workers"
    val managerId: Int? = null,
    val notes: String = "",

    val createdAt: String = LocalDateTime.now().toString(),
    val settledAt: String? = null, // When balance was settled
    val isSettled: Boolean = false,

    val createdBy: String = "system"
)

/**
 * Weekly funds received from owner/office
 */
@Entity(
    tableName = "weekly_funds",
    indices = [Index(value = ["weekStartDate"])]
)
@Serializable
data class WeeklyFunds(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val weekStartDate: String, // ISO format (typically Monday)
    @Serializable(with = BigDecimalSerializer::class)
    val amountReceived: BigDecimal,
    val notes: String = "",
    val createdAt: String = LocalDateTime.now().toString()
)

/**
 * Worker payments (e.g., Monthly Manager Salary)
 */
@Entity(
    tableName = "worker_payments",
    indices = [Index(value = ["workerId"]), Index(value = ["paymentDate"])]
)
@Serializable
data class WorkerPayment(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val workerId: Int,
    val workerName: String,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    val paymentDate: String, // ISO format
    val periodStart: String? = null,
    val periodEnd: String? = null,
    val paymentType: String = "MONTHLY", // "MONTHLY", "WEEKLY", "ADVANCE"
    val notes: String = "",
    val createdAt: String = LocalDateTime.now().toString()
)
