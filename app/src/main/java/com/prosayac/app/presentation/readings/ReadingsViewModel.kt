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
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
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
                val readings = _uiState.value.readings
                val exportDir = File(context.cacheDir, "exports")
                exportDir.mkdirs()

                val fileName = "SayacPro_Okumalar.xlsx"
                val file = File(exportDir, fileName)

                val workbook = XSSFWorkbook()
                val sheet = workbook.createSheet("Okumalar")

                // ── Styles ──────────────────────────────────────────────
                // Header style: light blue background, bold, centered, thin borders
                val headerStyle = workbook.createCellStyle().apply {
                    fillForegroundColor = IndexedColors.LIGHT_CORNFLOWER_BLUE.index
                    fillPattern = FillPatternType.SOLID_FOREGROUND
                    val font = workbook.createFont().apply {
                        bold = true
                        fontHeightInPoints = 11
                    }
                    setFont(font)
                    alignment = HorizontalAlignment.CENTER
                    setBorderTop(BorderStyle.THIN)
                    setBorderBottom(BorderStyle.THIN)
                    setBorderLeft(BorderStyle.THIN)
                    setBorderRight(BorderStyle.THIN)
                }

                // Data cell style: thin borders, center alignment for date columns
                val dataStyle = workbook.createCellStyle().apply {
                    setBorderTop(BorderStyle.THIN)
                    setBorderBottom(BorderStyle.THIN)
                    setBorderLeft(BorderStyle.THIN)
                    setBorderRight(BorderStyle.THIN)
                }

                // Number cell style: thin borders + right-aligned numeric
                val numberStyle = workbook.createCellStyle().apply {
                    setBorderTop(BorderStyle.THIN)
                    setBorderBottom(BorderStyle.THIN)
                    setBorderLeft(BorderStyle.THIN)
                    setBorderRight(BorderStyle.THIN)
                    alignment = HorizontalAlignment.RIGHT
                }

                // Date cell style: thin borders + center
                val dateStyle = workbook.createCellStyle().apply {
                    setBorderTop(BorderStyle.THIN)
                    setBorderBottom(BorderStyle.THIN)
                    setBorderLeft(BorderStyle.THIN)
                    setBorderRight(BorderStyle.THIN)
                    alignment = HorizontalAlignment.CENTER
                }

                // ── Header Row ──────────────────────────────────────────
                val headerRow = sheet.createRow(0)
                val headers = arrayOf(
                    "Tarih / Saat",
                    "Bina / Blok",
                    "Sayaç No",
                    "Sayaç Tipi",
                    "Okunan Endeks",
                    "Birim"
                )
                for ((i, h) in headers.withIndex()) {
                    val cell = headerRow.createCell(i)
                    cell.setCellValue(h)
                    cell.cellStyle = headerStyle
                }

                // ── Data Rows ───────────────────────────────────────────
                val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                for ((rowIdx, r) in readings.withIndex()) {
                    val row = sheet.createRow(rowIdx + 1)

                    // 1. Tarih / Saat
                    val dateCell = row.createCell(0)
                    dateCell.setCellValue(dateFormat.format(Date(r.readingDate)))
                    dateCell.cellStyle = dateStyle

                    // 2. Bina / Blok (buildingName + flatNumber combined)
                    val buildingCell = row.createCell(1)
                    val buildingText = buildString {
                        append(r.buildingName.ifBlank { "" })
                        if (r.flatNumber.isNotBlank()) {
                            if (isNotEmpty()) append(" / ")
                            append(r.flatNumber)
                        }
                    }.ifBlank { "-" }
                    buildingCell.setCellValue(buildingText)
                    buildingCell.cellStyle = dataStyle

                    // 3. Sayaç No
                    val serialCell = row.createCell(2)
                    serialCell.setCellValue(r.serialNumber.ifBlank { "-" })
                    serialCell.cellStyle = dataStyle

                    // 4. Sayaç Tipi
                    val typeCell = row.createCell(3)
                    typeCell.setCellValue(r.meterType)
                    typeCell.cellStyle = dataStyle

                    // 5. Okunan Endeks (NUMERIC - critical for Excel formulas)
                    val valueCell = row.createCell(4)
                    val numericValue = r.readingValue
                        .replace(",", ".")
                        .toDoubleOrNull()
                    if (numericValue != null) {
                        valueCell.setCellValue(numericValue)
                    } else {
                        valueCell.setCellValue(r.readingValue.ifBlank { "-" })
                    }
                    valueCell.cellStyle = numberStyle

                    // 6. Birim (m³ or kWh based on meter type)
                    val unitCell = row.createCell(5)
                    unitCell.setCellValue(
                        when {
                            r.meterType.contains("Su", ignoreCase = true) -> "m³"
                            r.meterType.contains("Isı", ignoreCase = true) -> "kWh"
                            else -> ""
                        }
                    )
                    unitCell.cellStyle = dataStyle
                }

                // ── Auto-size columns ───────────────────────────────────
                for (i in 0 until 6) {
                    sheet.autoSizeColumn(i)
                    // Add a little extra padding so columns aren't too tight
                    val currentWidth = sheet.getColumnWidth(i)
                    sheet.setColumnWidth(i, (currentWidth + 1024).coerceAtMost(65280))
                }

                // ── Freeze header row ───────────────────────────────────
                sheet.createFreezePane(0, 1)

                // ── Write to file ───────────────────────────────────────
                FileOutputStream(file).use { fos ->
                    workbook.write(fos)
                }
                workbook.close()

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