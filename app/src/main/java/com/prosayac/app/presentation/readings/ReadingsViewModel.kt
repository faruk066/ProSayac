package com.prosayac.app.presentation.readings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.prosayac.app.domain.model.ReadingWithMeter
import com.prosayac.app.domain.repository.MeterRepository
import com.prosayac.app.util.excel.ExcelExporter
import com.prosayac.app.worker.UploadSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

data class ReadingsUiState(
    val readings: List<ReadingWithMeter> = emptyList(),
    val isLoading: Boolean = false,
    val isExporting: Boolean = false,
    val exportPath: String? = null,
    val showExportDone: Boolean = false,
    val currentPage: Int = 1,
    val pageSize: Int = 50,
    val error: String? = null
)

@HiltViewModel
class ReadingsViewModel @Inject constructor(
    private val meterRepository: MeterRepository,
    private val excelExporter: ExcelExporter,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReadingsUiState())
    val uiState: StateFlow<ReadingsUiState> = _uiState.asStateFlow()

    init {
        loadReadings()
    }

    fun loadReadings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            meterRepository.getAllReadingsWithMeter()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Okumalar yüklenirken hata oluştu"
                    )
                }
                .collect { readings ->
                    _uiState.value = _uiState.value.copy(
                        readings = readings,
                        isLoading = false
                    )
                }
        }
    }

    fun exportToXlsx() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)

            try {
                val buildingName = _uiState.value.readings.firstOrNull()?.buildingName?.ifBlank { "Bina" } ?: "Bina"

                // Fetch meters AND readings directly from Room, then fill lastReading
                // from the actual reading values so the Excel has real data.
                val meters = withContext(Dispatchers.IO) {
                    val allMeters = meterRepository.getAllMeters().first()
                    val allReadings = meterRepository.getAllReadings().first()
                    val latestByMeterId = allReadings
                        .groupBy { it.meterId }
                        .mapValues { (_, list) -> list.maxByOrNull { it.readingDate }!! }
                    allMeters.map { meter ->
                        val reading = latestByMeterId[meter.id]
                        if (reading != null) {
                            meter.copy(
                                lastReading = reading.readingValue,
                                lastReadingDate = reading.readingDate
                            )
                        } else meter
                    }
                }

                val success = withContext(Dispatchers.IO) {
                    excelExporter.export(meters, buildingName)
                }

                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    showExportDone = success
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    error = "Dışa aktarma sırasında hata oluştu: ${e.message}"
                )
            }
        }
    }

    /**
     * Enqueue [UploadSyncWorker] to upload pending readings to Supabase immediately.
     * Works even if a periodic sync is already scheduled — WorkManager deduplicates
     * by [UploadSyncWorker] class name.
     */
    fun triggerSync() {
        val request = OneTimeWorkRequestBuilder<UploadSyncWorker>()
            .addTag("manual-upload")
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }

    fun dismissExportDone() {
        _uiState.value = _uiState.value.copy(
            showExportDone = false,
            exportPath = null
        )
    }

    fun nextPage() {
        val maxPage = (_uiState.value.readings.size + _uiState.value.pageSize - 1) / _uiState.value.pageSize
        val newPage = (_uiState.value.currentPage + 1).coerceAtMost(maxPage)
        _uiState.value = _uiState.value.copy(currentPage = newPage)
    }

    fun previousPage() {
        val newPage = (_uiState.value.currentPage - 1).coerceAtLeast(1)
        _uiState.value = _uiState.value.copy(currentPage = newPage)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}