package com.prosayac.app.data.repository

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
    private val siteDao: SiteDao
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

            val meterEntities = meterDtos.map { dto ->
                MeterEntity(
                    serialNumber = dto.meter_serial,
                    meterType = dto.meter_type ?: "Sıcak Su Sayacı",
                    buildingName = dto.site_id,
                    status = MeterStatus.UNREAD
                )
            }

            // Replace old data with new
            siteDao.deleteAllSites()
            siteDao.insertSites(siteEntities)
            siteDao.insertMeters(meterEntities)

            LoggerService.log(LogTag.SYNC, "Senkronizasyon tamamlandı: ${siteEntities.size} site, ${meterEntities.size} sayaç")
            Result.success(Unit)
        } catch (e: Exception) {
            LoggerService.log(LogTag.ERROR, "Supabase senkronizasyon hatası: ${e.message}")
            Result.failure(e)
        }
    }
}
