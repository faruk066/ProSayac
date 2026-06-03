package com.prosayac.app.presentation.readings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prosayac.app.data.local.dao.ReadingWithMeter
import com.prosayac.app.domain.repository.MeterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
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

    fun exportToCsv() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)

            try {
                val readings = _uiState.value.readings
                val exportDir = File(context.cacheDir, "exports")
                exportDir.mkdirs()

                val fileName = "okumalar_${System.currentTimeMillis()}.csv"
                val file = File(exportDir, fileName)

                FileOutputStream(file).use { fos ->
                    // BOM for Excel UTF-8 compatibility
                    fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

                    // Header
                    val header = "Seri No,Daire No,Tip,Bina,Okuma Değeri,Tarih,Durum,Senkron\n"
                    fos.write(header.toByteArray(Charsets.UTF_8))

                    // Data rows
                    for (r in readings) {
                        val row = buildString {
                            append("\"${r.serialNumber.ifBlank { "-" }}\",")
                            append("\"${r.flatNumber.ifBlank { "-" }}\",")
                            append("\"${r.meterType}\",")
                            append("\"${r.buildingName.ifBlank { "-" }}\",")
                            append("\"${r.readingValue.ifBlank { "-" }}\",")
                            append("\"${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(r.readingDate))}\",")
                            append("\"${when (r.meterStatus) {
                                "Read" -> "Okundu"
                                "Unread" -> "Okunamadı"
                                "Skipped" -> "Atlandı"
                                else -> r.meterStatus
                            }}\",")
                            append("\"${if (r.isSynced) "Evet" else "Hayır"}\"\n")
                        }
                        fos.write(row.toByteArray(Charsets.UTF_8))
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportPath = file.absolutePath,
                    showExportDone = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    error = "Dışa aktarma sırasında hata oluştu"
                )
            }
        }
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