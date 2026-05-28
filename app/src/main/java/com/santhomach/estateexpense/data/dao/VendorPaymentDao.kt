package com.santhomach.estateexpense.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.santhomach.estateexpense.data.model.VendorPayment
import kotlinx.coroutines.flow.Flow

@Dao
interface VendorPaymentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: VendorPayment): Long

    @Delete
    suspend fun delete(payment: VendorPayment)

    @Query("SELECT * FROM vendor_payments ORDER BY date DESC")
    fun getAllFlow(): Flow<List<VendorPayment>>
}
