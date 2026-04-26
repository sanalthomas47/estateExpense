package com.santhomach.estateexpense.data

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import com.santhomach.estateexpense.data.dao.DailyExpenseDao
import com.santhomach.estateexpense.data.dao.ExpenseTypeDao
import com.santhomach.estateexpense.data.dao.IncomeTypeDao
import com.santhomach.estateexpense.data.dao.WorkerTypeDao
import com.santhomach.estateexpense.data.dao.PermanentWorkerDao
import com.santhomach.estateexpense.data.dao.WeeklySettlementDao
import com.santhomach.estateexpense.data.dao.ExcessBalanceDao
import com.santhomach.estateexpense.data.model.DailyExpense
import com.santhomach.estateexpense.data.model.ExpenseType
import com.santhomach.estateexpense.data.model.IncomeType
import com.santhomach.estateexpense.data.model.WorkerType
import com.santhomach.estateexpense.data.model.PermanentWorker
import com.santhomach.estateexpense.data.model.WeeklySettlement
import com.santhomach.estateexpense.data.model.ExcessBalance
import androidx.room.Room

@Database(
    entities = [
        DailyExpense::class,
        ExpenseType::class,
        IncomeType::class,
        WorkerType::class,
        PermanentWorker::class,
        WeeklySettlement::class,
        ExcessBalance::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dailyExpenseDao(): DailyExpenseDao
    abstract fun expenseTypeDao(): ExpenseTypeDao
    abstract fun incomeTypeDao(): IncomeTypeDao
    abstract fun workerTypeDao(): WorkerTypeDao
    abstract fun permanentWorkerDao(): PermanentWorkerDao
    abstract fun weeklySettlementDao(): WeeklySettlementDao
    abstract fun excessBalanceDao(): ExcessBalanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "estate_expense.db"
                )
                    .fallbackToDestructiveMigration() // WARNING: Only for development
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
