package com.prosayac.app.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.prosayac.app.data.local.dao.MeterDao
import com.prosayac.app.data.local.dao.ReadingDao
import com.prosayac.app.data.remote.dto.ReadingUploadDto
import com.prosayac.app.util.log.LogTag
import com.prosayac.app.util.log.LoggerService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest

@HiltWorker
class UploadSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val supabaseClient: SupabaseClient,
    private val readingDao: ReadingDao,
    private val meterDao: MeterDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        LoggerService.log(LogTag.SYNC, "UploadSyncWorker başlatıldı — bekleyen okumalar yükleniyor")

        return try {
            val pendingReadings = readingDao.getPendingReadings()
            Log.d("SYNC_DEBUG", "Bulunan bekleyen okuma sayisi: ${pendingReadings.size}")
            if (pendingReadings.isEmpty()) return Result.success()

            val currentUser = supabaseClient.auth.currentUserOrNull()
            if (currentUser == null) {
                LoggerService.log(LogTag.WARN, "UploadSyncWorker: kullanıcı oturumu yok, yeniden deneniyor")
                return Result.retry()
            }

            val meters = meterDao.getAllMetersOnce().associateBy { it.id }

            val uploadList = pendingReadings.mapNotNull { reading ->
                val meter = meters[reading.meterId]
                if (meter == null) {
                    LoggerService.log(LogTag.WARN, "UploadSyncWorker: sayaç bulunamadı (id=${reading.meterId}), atlanıyor")
                    return@mapNotNull null
                }
                val readingValue = reading.readingValue.toDoubleOrNull()
                if (readingValue == null) {
                    LoggerService.log(LogTag.WARN, "UploadSyncWorker: geçersiz okuma değeri (id=${reading.id}), atlanıyor")
                    return@mapNotNull null
                }
                // Deterministic UUIDv3: same (serial, timestamp) always produces
                // the same valid UUID → idempotent upsert + PostgreSQL type compliance.
                val rawString = "${meter.serialNumber}_${reading.readingDate}"
                val uploadId = java.util.UUID.nameUUIDFromBytes(rawString.toByteArray()).toString()
                ReadingUploadDto(
                    id = uploadId,
                    meter_serial = meter.serialNumber,
                    meter_type = meter.meterType,
                    reading_value = readingValue,
                    unit = if (meter.meterType.contains("Su", ignoreCase = true)) "m³" else "kWh",
                    site_id = meter.siteSupabaseId ?: "",
                    read_by = supabaseClient.auth.currentUserOrNull()?.id,
                    read_at = java.time.Instant.ofEpochMilli(reading.readingDate)
                        .atOffset(java.time.ZoneOffset.UTC)
                        .format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                )
            }

            if (uploadList.isEmpty()) return Result.success()

            LoggerService.log(LogTag.SYNC, "UploadSyncWorker: ${uploadList.size} okuma Supabase'e gönderiliyor")
            supabaseClient.postgrest["readings"].upsert(uploadList) { onConflict = "id" }
            LoggerService.log(LogTag.SYNC, "UploadSyncWorker: Supabase'e kayıt başarılı")

            // Update all pending readings as SYNCED
            pendingReadings.forEach { reading ->
                readingDao.updateSyncStatus(reading.id, "SYNCED")
            }

            LoggerService.log(LogTag.SYNC, "UploadSyncWorker: ${pendingReadings.size} okuma senkronize edildi")
            Result.success()
        } catch (e: Exception) {
            Log.e("SYNC_ERROR", "Upload failed", e)
            Result.retry()
        }
    }
}
