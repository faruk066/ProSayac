package com.prosayac.app.data.repository

import com.prosayac.app.data.local.dao.MeterDao
import com.prosayac.app.data.local.dao.SiteDao
import com.prosayac.app.data.local.entity.MeterEntity
import com.prosayac.app.data.local.entity.SiteEntity
import com.prosayac.app.data.remote.dto.MeterDto
import com.prosayac.app.data.remote.dto.SiteDto
import com.prosayac.app.domain.model.MeterStatus
import com.prosayac.app.domain.repository.SyncRepository
import com.prosayac.app.util.log.LogTag
import com.prosayac.app.util.log.LoggerService
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val siteDao: SiteDao,
    private val meterDao: MeterDao
) : SyncRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun fetchAndSaveAssignments(): Result<Unit> {
        return try {
            LoggerService.log(LogTag.SYNC, "Supabase sync başlatıldı — siteler ve sayaçlar indiriliyor")

            // Fetch sites from Supabase (filtered by RLS for the authenticated user)
            val siteDtos: List<SiteDto> = supabaseClient.postgrest["sites"]
                .select()
                .decodeList()

            LoggerService.log(LogTag.SYNC, "${siteDtos.size} site indirildi")

            // Fetch meters from Supabase (filtered by RLS)
            val meterDtos: List<MeterDto> = supabaseClient.postgrest["meters"]
                .select()
                .decodeList()

            LoggerService.log(LogTag.SYNC, "${meterDtos.size} sayaç indirildi")

            // Map DTOs to Room entities
            val siteEntities = siteDtos.map { dto ->
                SiteEntity(
                    id = dto.id,
                    name = dto.name,
                    address = dto.address
                )
            }

            // Build site name lookup: site_id → site name (fixes UUID bug)
            val siteNameMap = siteEntities.associate { it.id to it.name }

            val meterEntities = meterDtos.map { dto ->
                MeterEntity(
                    serialNumber = dto.meter_serial,
                    flatNumber = dto.apartment_number ?: "",
                    meterType = dto.meter_type ?: "Sıcak Su Sayacı",
                    buildingName = siteNameMap[dto.site_id] ?: dto.site_id,
                    siteSupabaseId = dto.site_id,
                    status = MeterStatus.UNREAD
                )
            }

            // ID-preserving upsert: NEVER delete local meters/readings.
            //
            // Sites: inserted with OnConflictStrategy.REPLACE (PK = Supabase UUID, no CASCADE risk).
            // Meters: matched by serial_number via importAllAtomic — existing meters get their
            // metadata fields updated while preserving local ID, status, last_reading, and
            // most importantly, all linked readings (avoiding CASCADE data loss).
            //
            // Meters absent from the server response are NEVER deleted — they may have
            // pending (sync_status = PENDING) readings that haven't reached the cloud yet.
            siteDao.insertSites(siteEntities)
            meterDao.importAllAtomic(meterEntities)

            LoggerService.log(LogTag.SYNC, "Senkronizasyon tamamlandı: ${siteEntities.size} site, ${meterEntities.size} sayaç (mevcut okumalar korundu)")
            Result.success(Unit)
        } catch (e: Exception) {
            LoggerService.log(LogTag.ERROR, "Supabase senkronizasyon hatası: ${e.message}")
            Result.failure(e)
        }
    }
}
