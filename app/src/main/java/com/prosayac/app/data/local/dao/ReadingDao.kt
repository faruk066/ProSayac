package com.prosayac.app.data.local.dao

import androidx.room.*
import com.prosayac.app.data.local.entity.ReadingEntity
import com.prosayac.app.domain.model.DailyStats
import com.prosayac.app.domain.model.MonthlyStats
import com.prosayac.app.domain.model.ReadingWithMeter
import com.prosayac.app.domain.model.TypeStats
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingDao {

    @Query("SELECT * FROM readings ORDER BY reading_date DESC")
    fun getAllReadings(): Flow<List<ReadingEntity>>

    @Query("SELECT * FROM readings WHERE meter_id = :meterId ORDER BY reading_date DESC")
    fun getReadingsByMeterId(meterId: Long): Flow<List<ReadingEntity>>

    @Query("SELECT * FROM readings WHERE reading_date BETWEEN :startDate AND :endDate ORDER BY reading_date DESC")
    fun getReadingsByDateRange(startDate: Long, endDate: Long): Flow<List<ReadingEntity>>

    @Query("""
        SELECT r.id, r.meter_id AS meterId, m.serial_number AS serialNumber,
               m.flat_number AS flatNumber, m.meter_type AS meterType,
               m.building_name AS buildingName,
               r.reading_value AS readingValue, r.reading_date AS readingDate,
               r.is_synced AS isSynced, r.reading_type AS readingType,
               r.notes, m.status AS meterStatus
        FROM readings r
        INNER JOIN meters m ON r.meter_id = m.id
        ORDER BY r.reading_date DESC
    """)
    fun getAllReadingsWithMeter(): Flow<List<ReadingWithMeter>>

    @Query("SELECT COUNT(*) FROM readings")
    fun getTotalReadingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM readings WHERE is_synced = 0")
    fun getUnsyncedReadingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM readings WHERE reading_date >= :since")
    suspend fun getReadingCountSince(since: Long): Int

    @Query("""
        SELECT strftime('%Y-%m-%d', reading_date / 1000, 'unixepoch') as day, 
               COUNT(*) as count 
        FROM readings 
        WHERE reading_date >= :since 
        GROUP BY day 
        ORDER BY day ASC
    """)
    suspend fun getDailyReadingStats(since: Long): List<DailyStats>

    @Query("""
        SELECT reading_type, COUNT(*) as count 
        FROM readings 
        GROUP BY reading_type
    """)
    suspend fun getReadingTypeDistribution(): List<TypeStats>

    @Query("""
        SELECT strftime('%Y-%m', reading_date / 1000, 'unixepoch') as month, 
               COUNT(*) as count 
        FROM readings 
        GROUP BY month 
        ORDER BY month ASC 
        LIMIT 12
    """)
    suspend fun getMonthlyReadingTrend(): List<MonthlyStats>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: ReadingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadings(readings: List<ReadingEntity>): List<Long>

    @Update
    suspend fun updateReading(reading: ReadingEntity)

    @Query("UPDATE readings SET is_synced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)

    @Delete
    suspend fun deleteReading(reading: ReadingEntity)

    @Query("DELETE FROM readings")
    suspend fun deleteAll()
}

// DailyStats, MonthlyStats, TypeStats, ReadingWithMeter moved to domain.model
// to maintain clean architecture (domain layer must not depend on data layer)