package com.prosayac.app.presentation.readings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prosayac.app.data.local.dao.ReadingWithMeter
import com.prosayac.app.domain.repository.MeterRepository
import com.prosayac.app.util.excel.ExcelExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
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
    private val excelExporter: ExcelExporter
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
                val readings = _uiState.value.readings
                val buildingName = readings.firstOrNull()?.buildingName?.ifBlank { "Bina" } ?: "Bina"

                val success = withContext(Dispatchers.IO) {
                    excelExporter.export(readings, buildingName)
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