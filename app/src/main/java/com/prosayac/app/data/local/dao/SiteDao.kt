package com.prosayac.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.prosayac.app.data.local.entity.MeterEntity
import com.prosayac.app.data.local.entity.SiteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SiteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSites(sites: List<SiteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeters(meters: List<MeterEntity>)

    @Query("SELECT * FROM sites")
    fun getAssignedSites(): Flow<List<SiteEntity>>

    @Query("SELECT * FROM sites WHERE id = :id")
    suspend fun getSiteById(id: String): SiteEntity?

    @Query("DELETE FROM sites")
    suspend fun deleteAllSites()

    @Query("DELETE FROM meters")
    suspend fun deleteAllMeters()

    /**
     * Atomically replace all synced data: deletes old sites+meters then inserts new ones.
     * Wrapped in [Transaction] so a crash mid-way does not leave the local DB empty.
     */
    @Transaction
    suspend fun replaceAllData(siteEntities: List<SiteEntity>, meterEntities: List<MeterEntity>) {
        deleteAllSites()
        deleteAllMeters()
        insertSites(siteEntities)
        insertMeters(meterEntities)
    }
}
