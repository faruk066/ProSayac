package com.prosayac.app.data.repository

import com.prosayac.app.data.local.dao.DailyStats
import com.prosayac.app.data.local.dao.MeterDao
import com.prosayac.app.data.local.dao.MonthlyStats
import com.prosayac.app.data.local.dao.ReadingDao
import com.prosayac.app.data.local.dao.TypeStats
import com.prosayac.app.data.local.entity.MeterEntity
import com.prosayac.app.data.local.entity.ReadingEntity
import com.prosayac.app.domain.model.Meter
import com.prosayac.app.domain.model.Reading
import com.prosayac.app.domain.repository.MeterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeterRepositoryImpl @Inject constructor(
    private val meterDao: MeterDao,
    private val readingDao: ReadingDao
) : MeterRepository {

    // ========== METERS ==========

    override fun getAllMeters(): Flow<List<Meter>> {
        return meterDao.getAllMeters().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getMetersByStatus(status: String): Flow<List<Meter>> {
        return meterDao.getMetersByStatus(status).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getMetersByType(type: String): Flow<List<Meter>> {
        return meterDao.getMetersByType(type).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getMetersByTypeAndStatus(type: String, status: String): Flow<List<Meter>> {
        return meterDao.getMetersByTypeAndStatus(type, status).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTotalCount(): Flow<Int> = meterDao.getTotalCount()
    override fun getReadCount(): Flow<Int> = meterDao.getReadCount()
    override fun getUnreadCount(): Flow<Int> = meterDao.getUnreadCount()
    override fun getUnsyncedCount(): Flow<Int> = meterDao.getUnsyncedCount()

    override suspend fun getMeterById(id: Long): Meter? {
        return meterDao.getMeterById(id)?.toDomain()
    }

    override suspend fun insertMeter(meter: Meter): Long {
        return meterDao.insertMeter(meter.toEntity())
    }

    override suspend fun insertMeters(meters: List<Meter>): List<Long> {
        return meterDao.insertMeters(meters.map { it.toEntity() })
    }

    override suspend fun importMetersAtomic(newMeters: List<Meter>) {
        meterDao.importAllAtomic(newMeters.map { it.toEntity() })
    }

    override suspend fun updateMeter(meter: Meter) {
        meterDao.updateMeter(meter.toEntity())
    }

    override suspend fun updateMeterReading(id: Long, status: String, reading: String, readingDate: Long) {
        require(status.isNotBlank()) { "Meter status boş olamaz" }
        meterDao.updateMeterReading(id, status, reading, readingDate)
    }

    override suspend fun markAsSynced(ids: List<Long>) {
        meterDao.markAsSynced(ids)
    }

    override suspend fun deleteMeter(meter: Meter) {
        meterDao.deleteMeter(meter.toEntity())
    }

    override suspend fun deleteAll() {
        meterDao.deleteAll()
    }

    // ========== READINGS ==========

    override fun getAllReadings(): Flow<List<Reading>> {
        return readingDao.getAllReadings().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllReadingsWithMeter(): Flow<List<com.prosayac.app.data.local.dao.ReadingWithMeter>> {
        return readingDao.getAllReadingsWithMeter()
    }

    override fun getReadingsByMeterId(meterId: Long): Flow<List<Reading>> {
        return readingDao.getReadingsByMeterId(meterId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getReadingsByDateRange(startDate: Long, endDate: Long): Flow<List<Reading>> {
        return readingDao.getReadingsByDateRange(startDate, endDate).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTotalReadingCount(): Flow<Int> = readingDao.getTotalReadingCount()
    override fun getUnsyncedReadingCount(): Flow<Int> = readingDao.getUnsyncedReadingCount()

    override suspend fun insertReading(reading: Reading): Long {
        return readingDao.insertReading(reading.toEntity())
    }

    override suspend fun insertReadings(readings: List<Reading>): List<Long> {
        return readingDao.insertReadings(readings.map { it.toEntity() })
    }

    override suspend fun markReadingsAsSynced(ids: List<Long>) {
        readingDao.markAsSynced(ids)
    }

    override suspend fun deleteAllReadings() {
        readingDao.deleteAll()
    }

    // ========== CHART STATS ==========

    override suspend fun getReadingCountSince(since: Long): Int {
        return readingDao.getReadingCountSince(since)
    }

    override suspend fun getDailyReadingStats(since: Long): List<DailyStats> {
        return readingDao.getDailyReadingStats(since)
    }

    override suspend fun getReadingTypeDistribution(): List<TypeStats> {
        return readingDao.getReadingTypeDistribution()
    }

    override suspend fun getMonthlyReadingTrend(): List<MonthlyStats> {
        return readingDao.getMonthlyReadingTrend()
    }
}

// ========== MAPPING EXTENSIONS ==========

fun MeterEntity.toDomain(): Meter {
    return Meter(
        id = id,
        serialNumber = serialNumber,
        flatNumber = flatNumber,
        meterType = meterType,
        ownerName = ownerName,
        address = address,
        buildingName = buildingName,
        status = status,
        lastReading = lastReading,
        lastReadingDate = lastReadingDate,
        isSynced = isSynced,
        createdAt = createdAt
    )
}

fun Meter.toEntity(): MeterEntity {
    return MeterEntity(
        id = id,
        serialNumber = serialNumber,
        flatNumber = flatNumber,
        meterType = meterType,
        ownerName = ownerName,
        address = address,
        buildingName = buildingName,
        status = status,
        lastReading = lastReading,
        lastReadingDate = lastReadingDate,
        isSynced = isSynced,
        createdAt = createdAt
    )
}

fun ReadingEntity.toDomain(): Reading {
    return Reading(
        id = id,
        meterId = meterId,
        readingValue = readingValue,
        readingDate = readingDate,
        isSynced = isSynced,
        readingType = readingType,
        notes = notes
    )
}

fun Reading.toEntity(): ReadingEntity {
    return ReadingEntity(
        id = id,
        meterId = meterId,
        readingValue = readingValue,
        readingDate = readingDate,
        isSynced = isSynced,
        readingType = readingType,
        notes = notes
    )
}