package com.santhomach.estateexpense.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class DailyExpenseTest {

    @Test
    fun testDailyExpenseCreation() {
        val expense = DailyExpense(
            id = 1,
            date = LocalDate.now().toString(),
            malayaliMaleCount = 5,
            bengaliMaleCount = 3,
            malayaliMaleWagePerDay = BigDecimal("500"),
            bengaliMaleWagePerDay = BigDecimal("500")
        )

        assertEquals(1, expense.id)
        assertEquals(5, expense.malayaliMaleCount)
        assertEquals(3, expense.bengaliMaleCount)
    }

    @Test
    fun testNetAmountCalculation() {
        val expense = DailyExpense(
            date = LocalDate.now().toString(),
            malayaliMaleCount = 5,
            malayaliMaleWagePerDay = BigDecimal("500"),
            totalLaborCost = BigDecimal("2500"),
            totalOtherExpensesCost = BigDecimal("1000"),
            totalIncome = BigDecimal("5000")
        )

        // Net = Income - Expenses = 5000 - (2500 + 0 + 1000) = 1500
        val net = expense.calculateNetAmount()
        assertEquals(BigDecimal("1500"), net)
    }

    @Test
    fun testNetAmountWithNegativeBalance() {
        val expense = DailyExpense(
            date = LocalDate.now().toString(),
            totalLaborCost = BigDecimal("1000"),
            totalOtherExpensesCost = BigDecimal("500"),
            totalIncome = BigDecimal("1200")
        )

        // Net = 1200 - 1500 = -300 (loss)
        val net = expense.calculateNetAmount()
        assertEquals(BigDecimal("-300"), net)
    }

    @Test
    fun testNullSafetyForWorkerCounts() {
        val expense = DailyExpense(
            date = LocalDate.now().toString()
            // Worker counts default to 0
        )

        assertEquals(0, expense.malayaliMaleCount)
        assertEquals(0, expense.bengaliMaleCount)
        assertEquals(0, expense.malayaliFemaleCount)
        assertEquals(0, expense.bengaliFemaleCount)
    }

    @Test
    fun testBigDecimalPrecisionForWages() {
        val wage1 = BigDecimal("500.50")
        val wage2 = BigDecimal("250.25")
        val total = wage1.add(wage2)

        assertEquals(BigDecimal("750.75"), total)
    }

    @Test
    fun testExcessBalanceIsZeroByDefault() {
        val expense = DailyExpense(date = LocalDate.now().toString())
        assertEquals(BigDecimal.ZERO, expense.excessBalance)
    }

    @Test
    fun testCommentsCanBeEmpty() {
        val expense = DailyExpense(
            date = LocalDate.now().toString(),
            comments = ""
        )
        assertTrue(expense.comments.isEmpty())
    }

    @Test
    fun testAuditFieldsPopulated() {
        val expense = DailyExpense(date = LocalDate.now().toString())
        assertTrue(expense.createdAt.isNotEmpty())
        assertTrue(expense.createdBy.isNotEmpty())
    }
}
