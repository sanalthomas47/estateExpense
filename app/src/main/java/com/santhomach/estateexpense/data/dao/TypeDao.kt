package com.santhomach.estateexpense.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.santhomach.estateexpense.data.model.ExpenseType
import com.santhomach.estateexpense.data.model.IncomeType
import com.santhomach.estateexpense.data.model.WorkerType
import com.santhomach.estateexpense.data.model.PermanentWorker
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseTypeDao {

    @Insert
    suspend fun insert(expenseType: ExpenseType): Long

    @Update
    suspend fun update(expenseType: ExpenseType)

    @Delete
    suspend fun delete(expenseType: ExpenseType)

    @Query("SELECT * FROM expense_types WHERE id = :id")
    suspend fun getById(id: Int): ExpenseType?

    @Query("SELECT * FROM expense_types WHERE isActive = 1 ORDER BY typeName")
    suspend fun getAllActive(): List<ExpenseType>

    @Query("SELECT * FROM expense_types WHERE isActive = 1 ORDER BY typeName")
    fun getAllActiveFlow(): Flow<List<ExpenseType>>

    @Query("SELECT * FROM expense_types ORDER BY typeName")
    suspend fun getAll(): List<ExpenseType>

    @Query("DELETE FROM expense_types")
    suspend fun deleteAll()
}

@Dao
interface IncomeTypeDao {

    @Insert
    suspend fun insert(incomeType: IncomeType): Long

    @Update
    suspend fun update(incomeType: IncomeType)

    @Delete
    suspend fun delete(incomeType: IncomeType)

    @Query("SELECT * FROM income_types WHERE id = :id")
    suspend fun getById(id: Int): IncomeType?

    @Query("SELECT * FROM income_types WHERE isActive = 1 ORDER BY typeName")
    suspend fun getAllActive(): List<IncomeType>

    @Query("SELECT * FROM income_types WHERE isActive = 1 ORDER BY typeName")
    fun getAllActiveFlow(): Flow<List<IncomeType>>

    @Query("SELECT * FROM income_types ORDER BY typeName")
    suspend fun getAll(): List<IncomeType>

    @Query("DELETE FROM income_types")
    suspend fun deleteAll()
}

@Dao
interface WorkerTypeDao {

    @Insert
    suspend fun insert(workerType: WorkerType): Long

    @Update
    suspend fun update(workerType: WorkerType)

    @Delete
    suspend fun delete(workerType: WorkerType)

    @Query("SELECT * FROM worker_types WHERE id = :id")
    suspend fun getById(id: Int): WorkerType?

    @Query("SELECT * FROM worker_types WHERE isActive = 1 ORDER BY workerTypeName")
    suspend fun getAllActive(): List<WorkerType>

    @Query("SELECT * FROM worker_types WHERE isActive = 1 ORDER BY workerTypeName")
    fun getAllActiveFlow(): Flow<List<WorkerType>>

    @Query("SELECT * FROM worker_types ORDER BY workerTypeName")
    suspend fun getAll(): List<WorkerType>
}

@Dao
interface PermanentWorkerDao {

    @Insert
    suspend fun insert(worker: PermanentWorker): Long

    @Update
    suspend fun update(worker: PermanentWorker)

    @Delete
    suspend fun delete(worker: PermanentWorker)

    @Query("SELECT * FROM permanent_workers WHERE id = :id")
    suspend fun getById(id: Int): PermanentWorker?

    @Query("SELECT * FROM permanent_workers WHERE isActive = 1 ORDER BY name")
    suspend fun getAllActive(): List<PermanentWorker>

    @Query("SELECT * FROM permanent_workers WHERE isActive = 1 ORDER BY name")
    fun getAllActiveFlow(): Flow<List<PermanentWorker>>

    @Query("SELECT * FROM permanent_workers ORDER BY name")
    suspend fun getAll(): List<PermanentWorker>
}
