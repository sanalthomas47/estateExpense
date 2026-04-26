package com.santhomach.estateexpense.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class WeeklySettlementTest {

    @Test
    fun testWeeklySettlementCreation() {
        val settlement = WeeklySettlement(
            settlementDate = LocalDate.now().toString(),
            weekStartDate = LocalDate.now().minusDays(3).toString(),
            weekEndDate = LocalDate.now().toString(),
            totalWorkersMonThur = 50,
            totalLaborCostMonThur = BigDecimal("25000")
        )

        assertEquals(50, settlement.totalWorkersMonThur)
        assertEquals(BigDecimal("25000"), settlement.totalLaborCostMonThur)
    }

    @Test
    fun testPaymentCalculationWithCarryover() {
        val settlement = WeeklySettlement(
            settlementDate = LocalDate.now().toString(),
            weekStartDate = LocalDate.now().toString(),
            weekEndDate = LocalDate.now().toString(),
            totalLaborCostMonThur = BigDecimal("22500"),
            carryoverFromPreviousWeek = BigDecimal("-1500"), // Manager owed 1500
            amountToPay = BigDecimal("21000") // 22500 - 1500
        )

        assertEquals(BigDecimal("21000"), settlement.amountToPay)
    }

    @Test
    fun testExcessBalanceCarryover() {
        val settlement = WeeklySettlement(
            settlementDate = LocalDate.now().toString(),
            weekStartDate = LocalDate.now().toString(),
            weekEndDate = LocalDate.now().toString(),
            totalLaborCostMonThur = BigDecimal("22500"),
            differenceAmount = BigDecimal("3000"), // Fri-Sat excess
            carryoverToNextWeek = BigDecimal("3000")
        )

        assertEquals(BigDecimal("3000"), settlement.differenceAmount)
        assertEquals(BigDecimal("3000"), settlement.carryoverToNextWeek)
    }

    @Test
    fun testPaymentRecordingWithMethod() {
        val settlement = WeeklySettlement(
            settlementDate = LocalDate.now().toString(),
            weekStartDate = LocalDate.now().toString(),
            weekEndDate = LocalDate.now().toString(),
            amountToPay = BigDecimal("20000"),
            amountPaid = BigDecimal("20000"),
            paymentDate = LocalDate.now().toString(),
            paymentMethod = "Cash"
        )

        assertEquals(settlement.amountToPay, settlement.amountPaid)
        assertTrue(settlement.paymentDate != null)
        assertEquals("Cash", settlement.paymentMethod)
    }

    @Test
    fun testNullSafetyForOptionalFields() {
        val settlement = WeeklySettlement(
            settlementDate = LocalDate.now().toString(),
            weekStartDate = LocalDate.now().toString(),
            weekEndDate = LocalDate.now().toString()
        )

        // Optional fields should have default values
        assertEquals(BigDecimal.ZERO, settlement.totalLaborCostFriSat)
        assertTrue(settlement.paymentDate == null)
        assertEquals(0, settlement.totalWorkersFriSat)
    }
}
