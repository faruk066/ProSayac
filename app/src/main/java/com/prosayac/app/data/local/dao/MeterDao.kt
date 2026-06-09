package com.prosayac.app.data.local.dao

import androidx.room.*
import com.prosayac.app.data.local.entity.MeterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeterDao {

    @Query("SELECT * FROM meters ORDER BY created_at DESC")
    fun getAllMeters(): Flow<List<MeterEntity>>

    @Query("SELECT * FROM meters")
    suspend fun getAllMetersOnce(): List<MeterEntity>

    @Query("SELECT * FROM meters WHERE id = :id")
    suspend fun getMeterById(id: Long): MeterEntity?

    @Query("SELECT * FROM meters WHERE status = :status ORDER BY created_at DESC")
    fun getMetersByStatus(status: String): Flow<List<MeterEntity>>

    @Query("SELECT * FROM meters WHERE meter_type = :type ORDER BY created_at DESC")
    fun getMetersByType(type: String): Flow<List<MeterEntity>>

    @Query("SELECT * FROM meters WHERE meter_type = :type AND status = :status ORDER BY created_at DESC")
    fun getMetersByTypeAndStatus(type: String, status: String): Flow<List<MeterEntity>>

    @Query("SELECT COUNT(*) FROM meters")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM meters WHERE status = 'Read'")
    fun getReadCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM meters WHERE status = 'Unread'")
    fun getUnreadCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM meters WHERE is_synced = 0")
    fun getUnsyncedCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeter(meter: MeterEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeters(meters: List<MeterEntity>): List<Long>

    /**
     * Atomic import with upsert strategy: preserves existing meter IDs to prevent
     * CASCADE deletion of historical readings. Matches meters by serial_number.
     * - If meter exists: updates metadata fields (flat, type, owner, address, building)
     *   while preserving ID, status, reading data, sync state, and creation timestamp.
     * - If meter is new: inserts it.
     * 
     * This prevents the data loss bug where deleteAll() + CASCADE would wipe all readings.
     */
    @Transaction
    suspend fun importAllAtomic(newMeters: List<MeterEntity>) {
        val existingMeters = getAllMetersOnce()
        val existingBySerial = existingMeters.associateBy { it.serialNumber }
        
        for (meter in newMeters) {
            val existing = existingBySerial[meter.serialNumber]
            if (existing != null) {
                // Update preserving the ID and reading-related fields to avoid CASCADE delete
                val updated = meter.copy(
                    id = existing.id,
                    status = existing.status,
                    lastReading = existing.lastReading,
                    lastReadingDate = existing.lastReadingDate,
                    isSynced = existing.isSynced,
                    createdAt = existing.createdAt
                )
                updateMeter(updated)
            } else {
                insertMeter(meter)
            }
        }
    }

    @Update
    suspend fun updateMeter(meter: MeterEntity)

    @Query("UPDATE meters SET status = :status, last_reading = :reading, last_reading_date = :readingDate WHERE id = :id")
    suspend fun updateMeterReading(id: Long, status: String, reading: String, readingDate: Long)

    @Query("UPDATE meters SET is_synced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<Long>)

    @Delete
    suspend fun deleteMeter(meter: MeterEntity)

    @Query("DELETE FROM meters")
    suspend fun deleteAll()
}