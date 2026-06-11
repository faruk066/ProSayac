package com.prosayac.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.prosayac.app.data.local.Converters
import com.prosayac.app.data.local.dao.MeterDao
import com.prosayac.app.data.local.dao.ReadingDao
import com.prosayac.app.data.local.dao.SiteDao
import com.prosayac.app.data.local.entity.MeterEntity
import com.prosayac.app.data.local.entity.ReadingEntity
import com.prosayac.app.data.local.entity.SiteEntity

@Database(
    entities = [MeterEntity::class, ReadingEntity::class, SiteEntity::class],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun meterDao(): MeterDao
    abstract fun readingDao(): ReadingDao
    abstract fun siteDao(): SiteDao
}
