package com.prosayac.app.util.excel

import android.content.Context
import android.net.Uri
import com.prosayac.app.domain.model.Meter
import com.prosayac.app.util.log.LoggerService
import com.prosayac.app.util.log.LogTag
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.math.BigDecimal
import java.text.DecimalFormat

// =============================================================================
// DATA CLASSES
// =============================================================================

data class ExcelParseResult(
    val meters: List<Meter>,
    val errors: List<ExcelParseError>,
    val totalRows: Int,
    val successCount: Int,
    val ambiguousFormats: List<ExcelFormat> = emptyList()
)

data class ExcelParseError(
    val row: Int,
    val message: String
)

// =============================================================================
// FORMAT ENUM
// =============================================================================

enum class ExcelFormat {
    TELEGRAM,   // "SAYAÇ NO" + "SAYAÇ TİPİ"
    POLIMETER,  // "id2" + "tip"
    STANDARD    // "ISI SAYACI" + "SICAK SU"
}

// =============================================================================
// CONSTANTS
// =============================================================================

const val METER_TYPE_HEAT = "Isı Sayacı"
const val METER_TYPE_WATER = "Sıcak Su Sayacı"
private const val STATUS_UNREAD = "Unread"
private const val TYPE_CODE_HEAT = 4
private const val TYPE_CODE_WATER = 6

// =============================================================================
// MAIN PARSER
// =============================================================================

class ExcelParser {

    fun parse(context: Context, uri: Uri, buildingName: String, forceFormat: ExcelFormat? = null): ExcelParseResult {
        val errors = mutableListOf<ExcelParseError>()
        LoggerService.log(LogTag.PARSER, "Excel parse başlatıldı: bina=\"$buildingName\"")

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val workbook = WorkbookFactory.create(inputStream)
                val sheet = workbook.getSheetAt(0)

                if (sheet == null) {
                    LoggerService.log(LogTag.ERROR, "Excel parse edilemedi: sayfa bulunamadı")
                    errors.add(ExcelParseError(0, "Excel dosyasında sayfa bulunamadı"))
                    return ExcelParseResult(emptyList(), errors, 0, 0)
                }

                val headerRow = sheet.getRow(0)
                if (headerRow == null) {
                    LoggerService.log(LogTag.ERROR, "Excel parse edilemedi: başlık satırı yok")
                    errors.add(ExcelParseError(0, "Başlık satırı bulunamadı. Lütfen başlık satırı içeren bir Excel dosyası yükleyin."))
                    return ExcelParseResult(emptyList(), errors, 0, 0)
                }

                val matchingFormats = detectFormats(headerRow)

                if (matchingFormats.isEmpty()) {
                    val foundHeaders = (0 until headerRow.lastCellNum)
                        .mapNotNull { i ->
                            val c = headerRow.getCell(i)
                            if (c != null) safeGetCellRaw(c)?.trim() else null
                        }
                        .filter { it.isNotBlank() }
                        .joinToString(", ")
                    LoggerService.log(LogTag.WARN, "Excel format algılanamadı. Başlıklar: [$foundHeaders]")
                    errors.add(
                        ExcelParseError(
                            0,
                            "Bilinmeyen Excel formatı. " +
                                "Tespit edilen başlıklar: [$foundHeaders]. " +
                                "Desteklenen formatlar: Telegram, Polimeter, Standart."
                        )
                    )
                    return ExcelParseResult(emptyList(), errors, 0, 0)
                }

                if (matchingFormats.size > 1) {
                    LoggerService.log(LogTag.WARN, "Birden fazla format uyustu: ${matchingFormats.joinToString { it.name }}")
                    return ExcelParseResult(
                        emptyList(),
                        errors,
                        0,
                        0,
                        ambiguousFormats = matchingFormats
                    )
                }

                val format = matchingFormats.first()
                LoggerService.log(LogTag.PARSER, "Format algılandı: $format, parse başlıyor...")

                val result = when (format) {
                    ExcelFormat.TELEGRAM -> parseTelegram(sheet, headerRow, errors, buildingName)
                    ExcelFormat.POLIMETER -> parsePolimeter(sheet, headerRow, errors, buildingName)
                    ExcelFormat.STANDARD -> parseStandard(sheet, headerRow, errors, buildingName)
                }

                LoggerService.log(
                    LogTag.INFO,
                    "Excel parse tamamlandı: ${result.successCount} başarılı, " +
                        "${result.errors.size} hatalı, ${result.totalRows} satır işlendi"
                )
                return result
            } ?: run {
                LoggerService.log(LogTag.ERROR, "Excel dosyası açılamadı (inputStream null)")
                errors.add(ExcelParseError(0, "Dosya açılamadı"))
                return ExcelParseResult(emptyList(), errors, 0, 0)
            }
        } catch (e: Exception) {
            LoggerService.log(LogTag.ERROR, "Excel parse istisna: ${e.message}")
            errors.add(
                ExcelParseError(
                    0,
                    "Excel dosyası işlenirken hata: ${e.message ?: "bilinmeyen hata"}"
                )
            )
            return ExcelParseResult(emptyList(), errors, 0, 0)
        }
    }

    // =========================================================================
    // FORMAT DETECTION
    // =========================================================================

    /**
     * Normalizes Turkish characters for robust header matching.
     * "SAYAÇ" and "SAYAC" both normalize to "SAYAC".
     */
    private fun normalizeHeader(s: String): String {
        return s.uppercase()
            .replace("Ç", "C").replace("Ğ", "G").replace("İ", "I").replace("Ö", "O")
            .replace("Ş", "S").replace("Ü", "U")
            .replace("ç", "C").replace("ğ", "G").replace("ı", "I").replace("ö", "O")
            .replace("ş", "S").replace("ü", "U")
    }

    /**
     * Scans the header row (case‑insensitive, whitespace‑trimmed) to identify all
     * supported templates that match. Returns an empty list only when none match.
     */
    internal fun detectFormats(headerRow: Row): List<ExcelFormat> {
        val headers = (0 until headerRow.lastCellNum)
            .mapNotNull { i ->
                val c = headerRow.getCell(i) ?: return@mapNotNull null
                normalizeHeader(safeGetCellRaw(c)?.trim() ?: return@mapNotNull null)
                    .takeIf { it.isNotBlank() }
            }
            .toSet()

        val matches = mutableListOf<ExcelFormat>()

        val hasSayacNo = headers.any { 
            it == "SAYAÇ NO" || it == "SAYAC NO" || it.contains("SAYAC") && it.contains("NO") 
        }
        val hasSayacTipi = headers.any { 
            it == "SAYAÇ TİPİ" || it == "SAYAC TIPI" || it == "SAYAC TİPİ" || it.contains("SAYAC") && it.contains("TIP") 
        }
        if (hasSayacNo && hasSayacTipi) {
            matches.add(ExcelFormat.TELEGRAM)
        }

        val hasId2 = headers.any { it == "ID2" }
        val hasTip = headers.any { it == "TIP" || it == "TİP" }
        if (hasId2 && hasTip) {
            matches.add(ExcelFormat.POLIMETER)
        }

        val hasIsiSayaci = headers.any {
            it == "ISI SAYACI" ||
                    it == "ISI SAYAC" ||
                    it == "ISI_S" ||
                    it.contains("ISI") && it.contains("SAYACI")
        }
        val hasSicakSu = headers.any {
            it == "SICAK SU" ||
                    it == "SICAKSU" ||
                    it.contains("SICAK") && it.contains("SU")
        }
        if (hasIsiSayaci && hasSicakSu) {
            matches.add(ExcelFormat.STANDARD)
        }

        return matches
    }

    // =========================================================================
    // TELEGRAM FORMAT PARSER
    // =========================================================================
    // TELEGRAM FORMAT PARSER
    // =========================================================================

    /**
     * Telegram template:
     *   Flat  = "DAİRE NO"
     *   Serial = "SAYAÇ NO"
     *   Type   = "SAYAÇ TİPİ"   (4 = Isı Sayacı, 6 = Sıcak Su Sayacı)
     */
    internal fun parseTelegram(
        sheet: org.apache.poi.ss.usermodel.Sheet,
        headerRow: Row,
        errors: MutableList<ExcelParseError>,
        buildingName: String
    ): ExcelParseResult {
        val meters = mutableListOf<Meter>()
        var totalRows = 0

        // Map columns
        var flatCol = -1
        var serialCol = -1
        var typeCol = -1

        for (i in 0 until headerRow.lastCellNum) {
            val c = headerRow.getCell(i) ?: continue
            val h = safeGetCellRaw(c)?.trim()?.uppercase() ?: continue
            when {
                h == "DAİRE NO" || h == "DAIRE NO" -> flatCol = i
                h == "SAYAÇ NO" || h == "SAYAC NO" -> serialCol = i
                h == "SAYAÇ TİPİ" || h == "SAYAC TIPI" || h == "SAYAÇ TIPI" || h == "SAYAC TİPİ" -> typeCol = i
            }
        }

        if (serialCol < 0 || typeCol < 0) {
            errors.add(ExcelParseError(0, "Telegram formatında SAYAÇ NO veya SAYAÇ TİPİ sütunu eksik"))
            return ExcelParseResult(emptyList(), errors, 0, 0)
        }

        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            if (isRowCompletelyEmpty(row)) continue

            totalRows++

            try {
                val serial = safeGetCellAsText(row, serialCol)
                if (serial.isBlank()) continue  // silently skip

                val typeCode = safeGetCellAsInt(row, typeCol)
                val meterType = when (typeCode) {
                    TYPE_CODE_HEAT -> METER_TYPE_HEAT
                    TYPE_CODE_WATER -> METER_TYPE_WATER
                    else -> {
                        errors.add(
                            ExcelParseError(
                                rowIndex + 1,
                                "Bilinmeyen sayaç tip kodu: $typeCode (beklenen: 4 veya 6)"
                            )
                        )
                        continue
                    }
                }

                val flatNumber = safeGetCell(row, flatCol)
                meters.add(
                    Meter(
                        serialNumber = serial,
                        flatNumber = flatNumber,
                        meterType = meterType,
                        buildingName = buildingName,
                        status = STATUS_UNREAD
                    )
                )
            } catch (e: Exception) {
                errors.add(
                    ExcelParseError(
                        rowIndex + 1,
                        "Satır işlenirken hata: ${e.message ?: "bilinmeyen"}"
                    )
                )
            }
        }

        return ExcelParseResult(meters, errors, totalRows, meters.size)
    }

    // =========================================================================
    // POLIMETER FORMAT PARSER
    // =========================================================================

    /**
     * Polimeter template:
     *   Flat   = "daire" or "DAİRE"
     *   Serial = "id2"
     *   Type   = "tip" or "TİP"   (4 = Isı Sayacı, 6 = Sıcak Su Sayacı)
     */
    internal fun parsePolimeter(
        sheet: org.apache.poi.ss.usermodel.Sheet,
        headerRow: Row,
        errors: MutableList<ExcelParseError>,
        buildingName: String
    ): ExcelParseResult {
        val meters = mutableListOf<Meter>()
        var totalRows = 0

        var flatCol = -1
        var serialCol = -1
        var typeCol = -1

        for (i in 0 until headerRow.lastCellNum) {
            val c = headerRow.getCell(i) ?: continue
            val h = safeGetCellRaw(c)?.trim()?.uppercase() ?: continue
            when {
                h == "DAIRE" || h == "DAİRE" || h == "DAIRE NO" || h == "DAİRE NO" -> flatCol = i
                h == "ID2" -> serialCol = i
                h == "TIP" || h == "TİP" -> typeCol = i
            }
        }

        if (serialCol < 0 || typeCol < 0) {
            errors.add(ExcelParseError(0, "Polimeter formatında id2 veya tip sütunu eksik"))
            return ExcelParseResult(emptyList(), errors, 0, 0)
        }

        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            if (isRowCompletelyEmpty(row)) continue

            totalRows++

            try {
                val serial = safeGetCellAsText(row, serialCol)
                if (serial.isBlank()) continue  // silently skip

                val typeCode = safeGetCellAsInt(row, typeCol)
                val meterType = when (typeCode) {
                    TYPE_CODE_HEAT -> METER_TYPE_HEAT
                    TYPE_CODE_WATER -> METER_TYPE_WATER
                    else -> {
                        errors.add(
                            ExcelParseError(
                                rowIndex + 1,
                                "Bilinmeyen sayaç tip kodu: $typeCode (beklenen: 4 veya 6)"
                            )
                        )
                        continue
                    }
                }

                val flatNumber = safeGetCell(row, flatCol)
                meters.add(
                    Meter(
                        serialNumber = serial,
                        flatNumber = flatNumber,
                        meterType = meterType,
                        buildingName = buildingName,
                        status = STATUS_UNREAD
                    )
                )
            } catch (e: Exception) {
                errors.add(
                    ExcelParseError(
                        rowIndex + 1,
                        "Satır işlenirken hata: ${e.message ?: "bilinmeyen"}"
                    )
                )
            }
        }

        return ExcelParseResult(meters, errors, totalRows, meters.size)
    }

    // =========================================================================
    // STANDARD FORMAT PARSER (DUAL METER ROW)
    // =========================================================================

    /**
     * Standard template (dual meter per row):
     *   Flat         = "DAİRE NO" or "daire"
     *   Heat Serial  = "ISI SAYACI"
     *   Water Serial = "SICAK SU"
     *
     * No type codes. One row can yield TWO Meter objects.
     */
    internal fun parseStandard(
        sheet: org.apache.poi.ss.usermodel.Sheet,
        headerRow: Row,
        errors: MutableList<ExcelParseError>,
        buildingName: String
    ): ExcelParseResult {
        val meters = mutableListOf<Meter>()
        var totalRows = 0

        var flatCol = -1
        var heatSerialCol = -1
        var waterSerialCol = -1

        for (i in 0 until headerRow.lastCellNum) {
            val c = headerRow.getCell(i) ?: continue
            val h = safeGetCellRaw(c)?.trim()?.uppercase() ?: continue
            when {
                h == "DAIRE" || h == "DAİRE" ||
                h == "DAIRE NO" || h == "DAİRE NO" -> flatCol = i

                h == "ISI SAYACI" || h == "ISI SAYAC" ||
                (h.contains("ISI") && h.contains("SAYACI")) -> heatSerialCol = i

                h == "SICAK SU" || h == "SICAKSU" ||
                (h.contains("SICAK") && h.contains("SU")) -> waterSerialCol = i
            }
        }

        if (heatSerialCol < 0 && waterSerialCol < 0) {
            errors.add(ExcelParseError(0, "Standart formatta ISI SAYACI veya SICAK SU sütunu bulunamadı"))
            return ExcelParseResult(emptyList(), errors, 0, 0)
        }

        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            if (isRowCompletelyEmpty(row)) continue

            totalRows++

            try {
                val flatNumber = safeGetCell(row, flatCol)
                var rowHasAnyMeter = false

                // Heat meter (Isı Sayacı)
                if (heatSerialCol >= 0) {
                    val heatSerial = safeGetCellAsText(row, heatSerialCol)
                    if (heatSerial.isNotBlank()) {
                        meters.add(
                            Meter(
                                serialNumber = heatSerial,
                                flatNumber = flatNumber,
                                meterType = METER_TYPE_HEAT,
                                buildingName = buildingName,
                                status = STATUS_UNREAD
                            )
                        )
                        rowHasAnyMeter = true
                    }
                }

                // Hot water meter (Sıcak Su Sayacı)
                if (waterSerialCol >= 0) {
                    val waterSerial = safeGetCellAsText(row, waterSerialCol)
                    if (waterSerial.isNotBlank()) {
                        meters.add(
                            Meter(
                                serialNumber = waterSerial,
                                flatNumber = flatNumber,
                                meterType = METER_TYPE_WATER,
                                buildingName = buildingName,
                                status = STATUS_UNREAD
                            )
                        )
                        rowHasAnyMeter = true
                    }
                }

                if (!rowHasAnyMeter) {
                    errors.add(
                        ExcelParseError(
                            rowIndex + 1,
                            "Bu satırda geçerli bir sayaç seri numarası bulunamadı."
                        )
                    )
                }
            } catch (e: Exception) {
                errors.add(
                    ExcelParseError(
                        rowIndex + 1,
                        "Satır işlenirken hata: ${e.message ?: "bilinmeyen"}"
                    )
                )
            }
        }

        return ExcelParseResult(meters, errors, totalRows, meters.size)
    }

    // =========================================================================
    // CELL VALUE EXTRACTION
    // =========================================================================

    /** Returns the string value of a cell, or "" if missing/blank. */
    private fun safeGetCell(row: Row, cellIndex: Int): String {
        if (cellIndex < 0) return ""
        val cell = row.getCell(cellIndex) ?: return ""
        return safeGetCellRaw(cell)?.trim() ?: ""
    }

    /** Returns the integer value of a cell, or 0 if unreadable. */
    private fun safeGetCellAsInt(row: Row, cellIndex: Int): Int {
        if (cellIndex < 0) return 0
        val cell = row.getCell(cellIndex) ?: return 0
        return try {
            when (cell.cellType) {
                CellType.NUMERIC -> cell.numericCellValue.toInt()
                CellType.STRING -> cell.stringCellValue.trim().toDoubleOrNull()?.toInt() ?: 0
                CellType.FORMULA -> {
                    try {
                        val eval = cell.sheet.workbook.creationHelper.createFormulaEvaluator()
                        val result = eval.evaluate(cell)
                        when (result.cellType) {
                            CellType.NUMERIC -> result.numberValue.toInt()
                            CellType.STRING -> result.stringValue?.toDoubleOrNull()?.toInt() ?: 0
                            else -> 0
                        }
                    } catch (_: Exception) {
                        0
                    }
                }
                else -> 0
            }
        } catch (_: Exception) {
            0
        }
    }

    /**
     * Reads the raw string representation of ANY cell type.
     * Returns null ONLY if the cell is genuinely blank.
     *
     * CRITICAL: Numeric cells in scientific notation (e.g. 9E+06)
     * are converted to plain‑digit strings via BigDecimal.
     */
    private fun safeGetCellRaw(cell: Cell): String? {
        return try {
            when (cell.cellType) {
                CellType.STRING -> {
                    val v = cell.stringCellValue
                    if (v.isNullOrBlank()) null else v
                }

                CellType.NUMERIC -> {
                    // Dates stored as numbers → return null
                    if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                        return null
                    }
                    formatNumericValue(cell.numericCellValue)
                }

                CellType.BOOLEAN -> cell.booleanCellValue.toString()
                CellType.BLANK -> null

                CellType.FORMULA -> {
                    try {
                        val eval = cell.sheet.workbook.creationHelper.createFormulaEvaluator()
                        val result = eval.evaluate(cell)
                        when (result.cellType) {
                            CellType.STRING -> result.stringValue?.takeIf { it.isNotBlank() }
                            CellType.NUMERIC -> formatNumericValue(result.numberValue)
                            CellType.BOOLEAN -> result.booleanValue.toString()
                            CellType.BLANK -> null
                            else -> null
                        }
                    } catch (_: Exception) {
                        cell.stringCellValue?.trim()?.takeIf { it.isNotBlank() }
                    }
                }

                else -> {
                    try {
                        cell.stringCellValue?.trim()?.takeIf { it.isNotBlank() }
                    } catch (_: Exception) {
                        null
                    }
                }
            }
        } catch (_: Exception) {
            try {
                cell.stringCellValue?.trim()?.takeIf { it.isNotBlank() }
            } catch (_: Exception) {
                null
            }
        }
    }

    // =========================================================================
    // NUMERIC → STRING (NO SCIENTIFIC NOTATION, NO TRAILING .0)
    // =========================================================================

    /**
     * Converts a Double to a plain string:
     *   9.0E6       → "9000000"
     *   123456789.0 → "123456789"
     *   42.5        → "42.5"
     *   NaN/Inf     → null
     */
    private fun formatNumericValue(value: Double): String? {
        if (value.isNaN() || value.isInfinite()) return null

        // Fast path: exact whole number within Long range
        if (value == value.toLong().toDouble() && value.isFinite()) {
            val df = DecimalFormat("#")
            df.maximumFractionDigits = 0
            df.isGroupingUsed = false
            return df.format(value)
        }

        // BigDecimal path: guarantees no scientific notation
        try {
            val plain = value.toBigDecimal().toPlainString()
            if (plain.contains('.')) {
                val stripped = plain.trimEnd('0').trimEnd('.')
                return stripped.ifEmpty { plain }
            }
            return plain
        } catch (_: Exception) {
            val df = DecimalFormat("#.##############################")
            df.maximumFractionDigits = 30
            df.isGroupingUsed = false
            return df.format(value)
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private fun isRowCompletelyEmpty(row: Row): Boolean {
        for (i in 0 until row.lastCellNum) {
            val cell = row.getCell(i)
            if (cell != null && cell.cellType != CellType.BLANK) {
                val v = safeGetCellRaw(cell)
                if (!v.isNullOrBlank()) return false
            }
        }
        return true
    }

    /** Returns the text value of a cell, preserving leading zeros for serial numbers. */
    private fun safeGetCellAsText(row: Row, cellIndex: Int): String {
        if (cellIndex < 0) return ""
        val cell = row.getCell(cellIndex) ?: return ""
        return try {
            when (cell.cellType) {
                CellType.STRING -> cell.stringCellValue.trim()
                CellType.NUMERIC -> {
                    // Dates stored as numbers → return empty
                    if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                        return ""
                    }
                    // Force text formatting to preserve leading zeros
                    val numVal = cell.numericCellValue
                    if (numVal == numVal.toLong().toDouble()) {
                        numVal.toLong().toString()
                    } else {
                        numVal.toString()
                    }
                }
                CellType.FORMULA -> {
                    try {
                        val eval = cell.sheet.workbook.creationHelper.createFormulaEvaluator()
                        val result = eval.evaluate(cell)
                        when (result.cellType) {
                            CellType.STRING -> result.stringValue?.trim() ?: ""
                            CellType.NUMERIC -> {
                                val numVal = result.numberValue
                                if (numVal == numVal.toLong().toDouble()) {
                                    numVal.toLong().toString()
                                } else {
                                    numVal.toString()
                                }
                            }
                            else -> ""
                        }
                    } catch (_: Exception) {
                        cell.stringCellValue?.trim() ?: ""
                    }
                }
                else -> cell.toString().trim()
            }
        } catch (_: Exception) {
            ""
        }
    }
}