package com.santhomach.estateexpense.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.santhomach.estateexpense.data.model.WorkerPayment
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkerPaymentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: WorkerPayment): Long

    @Update
    suspend fun update(payment: WorkerPayment)

    @Delete
    suspend fun delete(payment: WorkerPayment)

    @Query("SELECT * FROM worker_payments WHERE workerId = :workerId ORDER BY paymentDate DESC")
    fun getPaymentsByWorkerFlow(workerId: Int): Flow<List<WorkerPayment>>

    @Query("SELECT * FROM worker_payments ORDER BY paymentDate DESC")
    fun getAllPaymentsFlow(): Flow<List<WorkerPayment>>

    @Query("SELECT * FROM worker_payments WHERE paymentDate BETWEEN :startDate AND :endDate ORDER BY paymentDate DESC")
    fun getPaymentsByDateRangeFlow(startDate: String, endDate: String): Flow<List<WorkerPayment>>

    @Query("SELECT * FROM worker_payments WHERE workerId = :workerId AND paymentDate = :date LIMIT 1")
    suspend fun getByWorkerAndDate(workerId: Int, date: String): WorkerPayment?

    @Query("SELECT * FROM worker_payments")
    suspend fun getAll(): List<WorkerPayment>
}
