package com.santhomach.estateexpense.data

import androidx.room.TypeConverter
import java.math.BigDecimal

class RoomConverters {
    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? {
        return value?.toString()
    }

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? {
        return value?.let {
            try { BigDecimal(it) } catch (e: NumberFormatException) { BigDecimal.ZERO }
        }
    }
}
