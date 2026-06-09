package com.prosayac.app.data.local

import androidx.room.TypeConverter
import com.prosayac.app.domain.model.MeterStatus

class Converters {
    @TypeConverter
    fun fromMeterStatus(status: MeterStatus): String = status.dbValue

    @TypeConverter
    fun toMeterStatus(value: String): MeterStatus = MeterStatus.fromString(value)
}
