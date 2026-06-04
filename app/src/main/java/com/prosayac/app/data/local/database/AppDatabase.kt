package com.prosayac.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.prosayac.app.data.local.dao.MeterDao
import com.prosayac.app.data.local.dao.ReadingDao
import com.prosayac.app.data.local.entity.MeterEntity
import com.prosayac.app.data.local.entity.ReadingEntity

@Database(
    entities = [MeterEntity::class, ReadingEntity::class],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun meterDao(): MeterDao
    abstract fun readingDao(): ReadingDao
}
