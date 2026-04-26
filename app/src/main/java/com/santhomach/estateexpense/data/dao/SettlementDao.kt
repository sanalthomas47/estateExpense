package com.santhomach.estateexpense.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.santhomach.estateexpense.data.model.WeeklySettlement
import com.santhomach.estateexpense.data.model.ExcessBalance
import kotlinx.coroutines.flow.Flow

@Dao
interface WeeklySettlementDao {

    @Insert
    suspend fun insert(settlement: WeeklySettlement): Long

    @Update
    suspend fun update(settlement: WeeklySettlement)

    @Delete
    suspend fun delete(settlement: WeeklySettlement)

    @Query("SELECT * FROM weekly_settlements WHERE id = :id")
    suspend fun getById(id: Int): WeeklySettlement?

    @Query("SELECT * FROM weekly_settlements WHERE settlementDate = :date")
    suspend fun getBySettlementDate(date: String): WeeklySettlement?

    @Query("SELECT * FROM weekly_settlements WHERE settlementDate BETWEEN :startDate AND :endDate ORDER BY settlementDate DESC")
    suspend fun getByDateRange(startDate: String, endDate: String): List<WeeklySettlement>

    @Query("SELECT * FROM weekly_settlements ORDER BY settlementDate DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 52): List<WeeklySettlement>

    @Query("SELECT * FROM weekly_settlements ORDER BY settlementDate DESC LIMIT :limit")
    fun getRecentFlow(limit: Int = 52): Flow<List<WeeklySettlement>>

    @Query("SELECT * FROM weekly_settlements ORDER BY settlementDate DESC")
    fun getAllFlow(): Flow<List<WeeklySettlement>>

    @Query("DELETE FROM weekly_settlements WHERE id = :id")
    suspend fun deleteById(id: Int)
}

@Dao
interface ExcessBalanceDao {

    @Insert
    suspend fun insert(balance: ExcessBalance): Long

    @Update
    suspend fun update(balance: ExcessBalance)

    @Delete
    suspend fun delete(balance: ExcessBalance)

    @Query("SELECT * FROM excess_balances WHERE id = :id")
    suspend fun getById(id: Int): ExcessBalance?

    @Query("SELECT * FROM excess_balances WHERE isSettled = 0 ORDER BY date DESC")
    suspend fun getUnsettled(): List<ExcessBalance>

    @Query("SELECT * FROM excess_balances WHERE isSettled = 0 ORDER BY date DESC")
    fun getUnsettledFlow(): Flow<List<ExcessBalance>>

    @Query("SELECT SUM(CASE WHEN direction = 'CREDIT' THEN amount ELSE -amount END) FROM excess_balances WHERE isSettled = 0")
    suspend fun getTotalUnsettledBalance(): String? // Nullable in case no data

    @Query("SELECT * FROM excess_balances WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getByDateRange(startDate: String, endDate: String): List<ExcessBalance>

    @Query("SELECT * FROM excess_balances ORDER BY date DESC")
    fun getAllFlow(): Flow<List<ExcessBalance>>

    @Query("DELETE FROM excess_balances WHERE id = :id")
    suspend fun deleteById(id: Int)
}
