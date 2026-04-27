package com.santhomach.estateexpense.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.santhomach.estateexpense.data.model.WeeklyFunds
import kotlinx.coroutines.flow.Flow

@Dao
interface WeeklyFundsDao {
    @Insert
    suspend fun insert(funds: WeeklyFunds): Long

    @Update
    suspend fun update(funds: WeeklyFunds)

    @Delete
    suspend fun delete(funds: WeeklyFunds)

    @Query("SELECT * FROM weekly_funds ORDER BY weekStartDate DESC")
    fun getAllFlow(): Flow<List<WeeklyFunds>>

    @Query("SELECT * FROM weekly_funds WHERE weekStartDate = :startDate LIMIT 1")
    suspend fun getByWeek(startDate: String): WeeklyFunds?
}
