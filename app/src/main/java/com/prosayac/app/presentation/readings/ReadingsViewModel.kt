package com.prosayac.app.presentation.readings

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prosayac.app.data.local.dao.ReadingWithMeter
import com.prosayac.app.domain.repository.MeterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jxl.Workbook
import jxl.write.Label
import jxl.write.Number
import jxl.write.WritableCellFormat
import jxl.write.WritableFont
import jxl.format.Border
import jxl.format.BorderLineStyle
import jxl.format.Colour
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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

    fun exportToXlsx() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)

            try {
                val exportedFilePath = withContext(Dispatchers.IO) {
                    val readings = _uiState.value.readings
                    val exportDir = File(context.cacheDir, "exports")
                    exportDir.mkdirs()

                    val fileName = "SayacPro_Okumalar.xls"
                    val file = File(exportDir, fileName)
                    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

                    // ── Create workbook and sheet ──────────────────────────────
                    val workbook = Workbook.createWorkbook(file)
                    val sheet = workbook.createSheet("Okumalar", 0)

                    // ── Styles ─────────────────────────────────────────────────
                    // Header style: bold, grey background, thin borders
                    val headerFont = WritableFont(WritableFont.ARIAL, 11, WritableFont.BOLD)
                    val headerFormat = WritableCellFormat(headerFont).apply {
                        setBackground(Colour.GRAY_25)
                        setBorder(Border.ALL, BorderLineStyle.THIN)
                    }

                    // Data cell style: thin borders
                    val dataFormat = WritableCellFormat().apply {
                        setBorder(Border.ALL, BorderLineStyle.THIN)
                    }

                    // Number cell style: thin borders
                    val numberFormat = WritableCellFormat().apply {
                        setBorder(Border.ALL, BorderLineStyle.THIN)
                    }

                    // Date cell style: thin borders
                    val dateFormatStyle = WritableCellFormat().apply {
                        setBorder(Border.ALL, BorderLineStyle.THIN)
                    }

                    // ── Header Row ─────────────────────────────────────────────
                    val headers = arrayOf(
                        "Tarih / Saat",
                        "Bina / Blok",
                        "Sayaç No",
                        "Sayaç Tipi",
                        "Okunan Endeks",
                        "Birim"
                    )
                    for ((i, h) in headers.withIndex()) {
                        sheet.addCell(Label(i, 0, h, headerFormat))
                    }

                    // ── Data Rows ──────────────────────────────────────────────
                    for ((rowIdx, r) in readings.withIndex()) {
                        val row = rowIdx + 1

                        // 1. Tarih / Saat
                        sheet.addCell(Label(0, row, dateFormat.format(Date(r.readingDate)), dateFormatStyle))

                        // 2. Bina / Blok (buildingName + flatNumber combined)
                        val buildingText = buildString {
                            append(r.buildingName.ifBlank { "" })
                            if (r.flatNumber.isNotBlank()) {
                                if (isNotEmpty()) append(" / ")
                                append(r.flatNumber)
                            }
                        }.ifBlank { "-" }
                        sheet.addCell(Label(1, row, buildingText, dataFormat))

                        // 3. Sayaç No
                        sheet.addCell(Label(2, row, r.serialNumber.ifBlank { "-" }, dataFormat))

                        // 4. Sayaç Tipi
                        sheet.addCell(Label(3, row, r.meterType, dataFormat))

                        // 5. Okunan Endeks (NUMERIC)
                        val numericValue = r.readingValue
                            .replace(",", ".")
                            .toDoubleOrNull()
                        if (numericValue != null) {
                            sheet.addCell(Number(4, row, numericValue, numberFormat))
                        } else {
                            sheet.addCell(Label(4, row, r.readingValue.ifBlank { "-" }, dataFormat))
                        }

                        // 6. Birim (m³ or kWh based on meter type)
                        val unit = when {
                            r.meterType.contains("Su", ignoreCase = true) -> "m³"
                            r.meterType.contains("Isı", ignoreCase = true) -> "kWh"
                            else -> ""
                        }
                        sheet.addCell(Label(5, row, unit, dataFormat))
                    }

                    // ── Set column widths ──────────────────────────────────────
                    val columnWidths = intArrayOf(20, 25, 18, 18, 18, 10)
                    for (i in 0 until 6) {
                        sheet.setColumnView(i, columnWidths[i])
                    }

                    // ── Write and close ────────────────────────────────────────
                    workbook.write()
                    workbook.close()

                    // Return the file path from IO block
                    file.absolutePath
                }

                // ── Update UI on Main thread ───────────────────────────────────
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportPath = exportedFilePath,
                    showExportDone = true
                )
            } catch (e: Exception) {
                android.util.Log.e("ReadingsViewModel", "Export error", e)
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    error = "Dışa aktarma sırasında hata oluştu: ${e.message}"
                )
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Dışa aktarma hatası: ${e.message}", Toast.LENGTH_LONG).show()
                }
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