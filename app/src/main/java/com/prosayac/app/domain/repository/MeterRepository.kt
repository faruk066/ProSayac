package com.prosayac.app.domain.repository

import com.prosayac.app.data.local.dao.DailyStats
import com.prosayac.app.data.local.dao.MonthlyStats
import com.prosayac.app.data.local.dao.TypeStats
import com.prosayac.app.domain.model.Meter
import com.prosayac.app.domain.model.Reading
import kotlinx.coroutines.flow.Flow

interface MeterRepository {
    // Meters
    fun getAllMeters(): Flow<List<Meter>>
    fun getMetersByStatus(status: String): Flow<List<Meter>>
    fun getMetersByType(type: String): Flow<List<Meter>>
    fun getMetersByTypeAndStatus(type: String, status: String): Flow<List<Meter>>
    fun getTotalCount(): Flow<Int>
    fun getReadCount(): Flow<Int>
    fun getUnreadCount(): Flow<Int>
    fun getUnsyncedCount(): Flow<Int>
    suspend fun getMeterById(id: Long): Meter?
    suspend fun insertMeter(meter: Meter): Long
    suspend fun insertMeters(meters: List<Meter>): List<Long>
    suspend fun importMetersAtomic(newMeters: List<Meter>)
    suspend fun updateMeter(meter: Meter)
    suspend fun updateMeterReading(id: Long, status: String, reading: String, readingDate: Long)
    suspend fun markAsSynced(ids: List<Long>)
    suspend fun deleteMeter(meter: Meter)
    suspend fun deleteAll()

    // Readings
    fun getAllReadings(): Flow<List<Reading>>
    fun getAllReadingsWithMeter(): Flow<List<com.prosayac.app.data.local.dao.ReadingWithMeter>>
    fun getReadingsByMeterId(meterId: Long): Flow<List<Reading>>
    fun getReadingsByDateRange(startDate: Long, endDate: Long): Flow<List<Reading>>
    fun getTotalReadingCount(): Flow<Int>
    fun getUnsyncedReadingCount(): Flow<Int>
    suspend fun insertReading(reading: Reading): Long
    suspend fun insertReadings(readings: List<Reading>): List<Long>
    suspend fun markReadingsAsSynced(ids: List<Long>)
    suspend fun deleteAllReadings()

    // Chart/Stats queries
    suspend fun getReadingCountSince(since: Long): Int
    suspend fun getDailyReadingStats(since: Long): List<DailyStats>
    suspend fun getReadingTypeDistribution(): List<TypeStats>
    suspend fun getMonthlyReadingTrend(): List<MonthlyStats>
}