package com.santhomach.estateexpense.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.santhomach.estateexpense.data.model.DailyExpense
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class DailyExpenseDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: com.santhomach.estateexpense.data.dao.DailyExpenseDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).build()
        dao = database.dailyExpenseDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testInsertAndRetrieveExpense() = runBlocking {
        val expense = DailyExpense(
            date = LocalDate.now().toString(),
            malayaliMaleCount = 5,
            malayaliMaleWagePerDay = BigDecimal("500"),
            totalLaborCost = BigDecimal("2500")
        )

        val id = dao.insert(expense)
        val retrieved = dao.getById(id.toInt())

        assertNotNull(retrieved)
        assertEquals(5, retrieved?.malayaliMaleCount)
        assertEquals(BigDecimal("2500"), retrieved?.totalLaborCost)
    }

    @Test
    fun testUpdateExpense() = runBlocking {
        val expense = DailyExpense(
            date = LocalDate.now().toString(),
            malayaliMaleCount = 5,
            totalLaborCost = BigDecimal("2500")
        )

        val id = dao.insert(expense).toInt()
        val updated = expense.copy(id = id, malayaliMaleCount = 10)
        dao.update(updated)

        val retrieved = dao.getById(id)
        assertEquals(10, retrieved?.malayaliMaleCount)
    }

    @Test
    fun testDeleteExpense() = runBlocking {
        val expense = DailyExpense(
            date = LocalDate.now().toString(),
            malayaliMaleCount = 5
        )

        val id = dao.insert(expense).toInt()
        val inserted = dao.getById(id)
        assertNotNull(inserted)

        dao.delete(inserted!!)
        val deleted = dao.getById(id)
        assertNull(deleted)
    }

    @Test
    fun testGetByDateRange() = runBlocking {
        val today = LocalDate.now().toString()
        val tomorrow = LocalDate.now().plusDays(1).toString()

        val expense1 = DailyExpense(date = today, malayaliMaleCount = 5)
        val expense2 = DailyExpense(date = tomorrow, malayaliMaleCount = 3)

        dao.insert(expense1)
        dao.insert(expense2)

        val results = dao.getByDateRange(today, tomorrow)
        assertEquals(2, results.size)
    }

    @Test
    fun testGetCount() = runBlocking {
        dao.insert(DailyExpense(date = LocalDate.now().toString(), malayaliMaleCount = 1))
        dao.insert(DailyExpense(date = LocalDate.now().toString(), malayaliMaleCount = 2))

        val count = dao.getCount()
        assertEquals(2, count)
    }
}
