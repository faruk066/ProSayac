package com.prosayac.app.util.excel

import android.content.Context
import android.net.Uri
import android.util.Xml
import com.prosayac.app.domain.model.Meter
import com.prosayac.app.util.log.LoggerService
import com.prosayac.app.util.log.LogTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.util.zip.ZipInputStream

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
// NATIVE KOTLIN XLSX PARSER (NO APACHE POI)
// =============================================================================

class ExcelParser {

    suspend fun parse(context: Context, uri: Uri, buildingName: String, forceFormat: ExcelFormat? = null): ExcelParseResult = withContext(Dispatchers.IO) {
        val errors = mutableListOf<ExcelParseError>()
        LoggerService.log(LogTag.PARSER, "Native XLSX parse başlatıldı: bina=\"$buildingName\"")

        try {
            val zipBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: run {
                    LoggerService.log(LogTag.ERROR, "Excel dosyası açılamadı (inputStream null)")
                    errors.add(ExcelParseError(0, "Dosya açılamadı"))
                    return@withContext ExcelParseResult(emptyList(), errors, 0, 0)
                }

            // Pass 1: Extract shared strings
            val sharedStrings = extractSharedStrings(zipBytes)
            LoggerService.log(LogTag.PARSER, "Shared strings extracted: ${sharedStrings.size}")

            // Pass 2: Parse sheet1.xml
            val sheetData = parseSheetData(zipBytes, sharedStrings)

            if (sheetData.isEmpty()) {
                LoggerService.log(LogTag.ERROR, "Excel parse edilemedi: sayfa boş veya bulunamadı")
                errors.add(ExcelParseError(0, "Excel dosyasında veri bulunamadı"))
                return@withContext ExcelParseResult(emptyList(), errors, 0, 0)
            }

            val headerRow = sheetData.firstOrNull()
            if (headerRow == null || headerRow.isEmpty()) {
                LoggerService.log(LogTag.ERROR, "Excel parse edilemedi: başlık satırı yok")
                errors.add(ExcelParseError(0, "Başlık satırı bulunamadı. Lütfen başlık satırı içeren bir Excel dosyası yükleyin."))
                return@withContext ExcelParseResult(emptyList(), errors, 0, 0)
            }

            val matchingFormats = detectFormats(headerRow)

            if (matchingFormats.isEmpty()) {
                val foundHeaders = headerRow.filter { it.isNotBlank() }.joinToString(", ")
                LoggerService.log(LogTag.WARN, "Excel format algılanamadı. Başlıklar: [$foundHeaders]")
                errors.add(
                    ExcelParseError(
                        0,
                        "Bilinmeyen Excel formatı. " +
                            "Tespit edilen başlıklar: [$foundHeaders]. " +
                            "Desteklenen formatlar: Telegram, Polimeter, Standart."
                    )
                )
                return@withContext ExcelParseResult(emptyList(), errors, 0, 0)
            }

            if (matchingFormats.size > 1) {
                LoggerService.log(LogTag.WARN, "Birden fazla format uyustu: ${matchingFormats.joinToString { it.name }}")
                return@withContext ExcelParseResult(
                    emptyList(),
                    errors,
                    0,
                    0,
                    ambiguousFormats = matchingFormats
                )
            }

            val format = forceFormat ?: matchingFormats.first()
            LoggerService.log(LogTag.PARSER, "Format algılandı: $format, parse başlıyor...")

            val dataRows = sheetData.drop(1)
            val result = when (format) {
                ExcelFormat.TELEGRAM -> parseTelegram(dataRows, headerRow, errors, buildingName)
                ExcelFormat.POLIMETER -> parsePolimeter(dataRows, headerRow, errors, buildingName)
                ExcelFormat.STANDARD -> parseStandard(dataRows, headerRow, errors, buildingName)
            }

            LoggerService.log(
                LogTag.INFO,
                "Excel parse tamamlandı: ${result.successCount} başarılı, " +
                    "${result.errors.size} hatalı, ${result.totalRows} satır işlendi"
            )
            return@withContext result

        } catch (e: Exception) {
            LoggerService.log(LogTag.ERROR, "Excel parse CRASH: ${e.stackTraceToString()}")
            errors.add(
                ExcelParseError(
                    0,
                    "Excel dosyası işlenirken hata: ${e.message ?: "bilinmeyen hata"}"
                )
            )
            return@withContext ExcelParseResult(emptyList(), errors, 0, 0)
        }
    }

    // =========================================================================
    // ZIP & XML EXTRACTION
    // =========================================================================

    private fun extractSharedStrings(zipBytes: ByteArray): List<String> {
        val sharedStrings = ArrayList<String>()

        ZipInputStream(zipBytes.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name.equals("xl/sharedStrings.xml", ignoreCase = true)) {
                    val parser = Xml.newPullParser()
                    parser.setInput(zis, "UTF-8")
                    var eventType = parser.eventType
                    var currentText = StringBuilder()
                    var inT = false

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        when (eventType) {
                            XmlPullParser.START_TAG -> {
                                if (parser.name == "t") {
                                    inT = true
                                    currentText.clear()
                                }
                            }
                            XmlPullParser.TEXT -> {
                                if (inT) {
                                    currentText.append(parser.text)
                                }
                            }
                            XmlPullParser.END_TAG -> {
                                if (parser.name == "t") {
                                    inT = false
                                    sharedStrings.add(currentText.toString())
                                }
                            }
                        }
                        eventType = parser.next()
                    }
                    break
                }
                entry = zis.nextEntry
            }
        }

        return sharedStrings
    }

    private fun parseSheetData(zipBytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()

        ZipInputStream(zipBytes.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name.equals("xl/worksheets/sheet1.xml", ignoreCase = true) ||
                    entry.name.equals("xl/worksheets/sheet.xml", ignoreCase = true)
                ) {
                    val parser = Xml.newPullParser()
                    parser.setInput(zis, "UTF-8")
                    var eventType = parser.eventType

                    var currentRow = mutableListOf<String>()
                    var currentCellValue = StringBuilder()
                    var isSharedString = false
                    var inV = false
                    var columnIndex = 0
                    var maxColIndex = -1

                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        when (eventType) {
                            XmlPullParser.START_TAG -> {
                                when (parser.name) {
                                    "row" -> {
                                        currentRow = mutableListOf()
                                        columnIndex = 0
                                        maxColIndex = -1
                                    }
                                    "c" -> {
                                        isSharedString = parser.getAttributeValue(null, "t") == "s"
                                        val ref = parser.getAttributeValue(null, "r")
                                        if (ref != null) {
                                            val colRef = ref.filter { it.isLetter() }
                                            columnIndex = columnRefToIndex(colRef)
                                        }
                                    }
                                    "v" -> {
                                        inV = true
                                        currentCellValue.clear()
                                    }
                                }
                            }
                            XmlPullParser.TEXT -> {
                                if (inV) {
                                    currentCellValue.append(parser.text)
                                }
                            }
                            XmlPullParser.END_TAG -> {
                                when (parser.name) {
                                    "v" -> {
                                        inV = false
                                        val text = currentCellValue.toString()
                                        val value = if (isSharedString) {
                                            val idx = text.toIntOrNull() ?: 0
                                            sharedStrings.getOrNull(idx) ?: text
                                        } else {
                                            text
                                        }
                                        while (currentRow.size <= columnIndex) {
                                            currentRow.add("")
                                        }
                                        currentRow[columnIndex] = value
                                        if (columnIndex > maxColIndex) maxColIndex = columnIndex
                                    }
                                    "row" -> {
                                        if (maxColIndex >= 0) {
                                            rows.add(currentRow.take(maxColIndex + 1))
                                        } else if (currentRow.isNotEmpty()) {
                                            rows.add(currentRow)
                                        }
                                    }
                                }
                            }
                        }
                        eventType = parser.next()
                    }
                    break
                }
                entry = zis.nextEntry
            }
        }

        return rows
    }

    private fun columnRefToIndex(colRef: String): Int {
        var result = 0
        for (c in colRef.uppercase()) {
            result = result * 26 + (c - 'A' + 1)
        }
        return result - 1
    }

    // =========================================================================
    // FORMAT DETECTION
    // =========================================================================

    private fun normalizeHeader(s: String): String {
        return s.uppercase()
            .replace("Ç", "C").replace("Ğ", "G").replace("İ", "I").replace("Ö", "O")
            .replace("Ş", "S").replace("Ü", "U")
            .replace("ç", "C").replace("ğ", "G").replace("ı", "I").replace("ö", "O")
            .replace("ş", "S").replace("ü", "U")
    }

    internal fun detectFormats(headerRow: List<String>): List<ExcelFormat> {
        val headers = headerRow
            .mapNotNull { h ->
                normalizeHeader(h.trim()).takeIf { it.isNotBlank() }
            }
            .toSet()

        val matches = mutableListOf<ExcelFormat>()

        val hasSayacNo = headers.any {
            it == "SAYAÇ NO" || it == "SAYAC NO" || (it.contains("SAYAC") && it.contains("NO"))
        }
        val hasSayacTipi = headers.any {
            it == "SAYAÇ TİPİ" || it == "SAYAC TIPI" || it == "SAYAC TİPİ" || (it.contains("SAYAC") && it.contains("TIP"))
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
            it == "ISI SAYACI" || it == "ISI SAYAC" || (it.contains("ISI") && it.contains("SAYACI"))
        }
        val hasSicakSu = headers.any {
            it == "SICAK SU" || it == "SICAKSU" || (it.contains("SICAK") && it.contains("SU"))
        }
        if (hasIsiSayaci && hasSicakSu) {
            matches.add(ExcelFormat.STANDARD)
        }

        return matches
    }

    // =========================================================================
    // TELEGRAM FORMAT PARSER
    // =========================================================================

    internal fun parseTelegram(
        dataRows: List<List<String>>,
        headerRow: List<String>,
        errors: MutableList<ExcelParseError>,
        buildingName: String
    ): ExcelParseResult {
        val meters = mutableListOf<Meter>()
        var totalRows = 0

        var flatCol = -1
        var serialCol = -1
        var typeCol = -1

        for (i in headerRow.indices) {
            val h = headerRow.getOrNull(i)?.trim()?.uppercase() ?: continue
            when {
                h == "DAİRE NO" || h == "DAIRE NO" -> flatCol = i
                h == "SAYAÇ NO" || h == "SAYAC NO" -> serialCol = i
                h == "SAYAÇ TİPİ" || h == "SAYAC TIPI" || h == "SAYAC TİPİ" -> typeCol = i
            }
        }

        if (serialCol < 0 || typeCol < 0) {
            errors.add(ExcelParseError(0, "Telegram formatında SAYAÇ NO veya SAYAÇ TİPİ sütunu eksik"))
            return ExcelParseResult(emptyList(), errors, 0, 0)
        }

        for (rowIndex in dataRows.indices) {
            val row = dataRows[rowIndex]
            if (isRowCompletelyEmpty(row)) continue

            totalRows++

            try {
                val serial = safeGetCellAsText(row, serialCol)
                if (serial.isBlank()) continue

                val typeCode = safeGetCellAsInt(row, typeCol)
                val meterType = when (typeCode) {
                    TYPE_CODE_HEAT -> METER_TYPE_HEAT
                    TYPE_CODE_WATER -> METER_TYPE_WATER
                    else -> {
                        errors.add(
                            ExcelParseError(
                                rowIndex + 2,
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
                        rowIndex + 2,
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

    internal fun parsePolimeter(
        dataRows: List<List<String>>,
        headerRow: List<String>,
        errors: MutableList<ExcelParseError>,
        buildingName: String
    ): ExcelParseResult {
        val meters = mutableListOf<Meter>()
        var totalRows = 0

        var flatCol = -1
        var serialCol = -1
        var typeCol = -1

        for (i in headerRow.indices) {
            val h = headerRow.getOrNull(i)?.trim()?.uppercase() ?: continue
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

        for (rowIndex in dataRows.indices) {
            val row = dataRows[rowIndex]
            if (isRowCompletelyEmpty(row)) continue

            totalRows++

            try {
                val serial = safeGetCellAsText(row, serialCol)
                if (serial.isBlank()) continue

                val typeCode = safeGetCellAsInt(row, typeCol)
                val meterType = when (typeCode) {
                    TYPE_CODE_HEAT -> METER_TYPE_HEAT
                    TYPE_CODE_WATER -> METER_TYPE_WATER
                    else -> {
                        errors.add(
                            ExcelParseError(
                                rowIndex + 2,
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
                        rowIndex + 2,
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

    internal fun parseStandard(
        dataRows: List<List<String>>,
        headerRow: List<String>,
        errors: MutableList<ExcelParseError>,
        buildingName: String
    ): ExcelParseResult {
        val meters = mutableListOf<Meter>()
        var totalRows = 0

        var flatCol = -1
        var heatSerialCol = -1
        var waterSerialCol = -1

        for (i in headerRow.indices) {
            val h = headerRow.getOrNull(i)?.trim()?.uppercase() ?: continue
            when {
                h == "DAIRE" || h == "DAİRE" || h == "DAIRE NO" || h == "DAİRE NO" -> flatCol = i
                h == "ISI SAYACI" || h == "ISI SAYAC" || (h.contains("ISI") && h.contains("SAYACI")) -> heatSerialCol = i
                h == "SICAK SU" || h == "SICAKSU" || (h.contains("SICAK") && h.contains("SU")) -> waterSerialCol = i
            }
        }

        if (heatSerialCol < 0 && waterSerialCol < 0) {
            errors.add(ExcelParseError(0, "Standart formatta ISI SAYACI veya SICAK SU sütunu bulunamadı"))
            return ExcelParseResult(emptyList(), errors, 0, 0)
        }

        for (rowIndex in dataRows.indices) {
            val row = dataRows[rowIndex]
            if (isRowCompletelyEmpty(row)) continue

            totalRows++

            try {
                val flatNumber = safeGetCell(row, flatCol)
                var rowHasAnyMeter = false

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
                            rowIndex + 2,
                            "Bu satırda geçerli bir sayaç seri numarası bulunamadı."
                        )
                    )
                }
            } catch (e: Exception) {
                errors.add(
                    ExcelParseError(
                        rowIndex + 2,
                        "Satır işlenirken hata: ${e.message ?: "bilinmeyen"}"
                    )
                )
            }
        }

        return ExcelParseResult(meters, errors, totalRows, meters.size)
    }

    // =========================================================================
    // CELL VALUE EXTRACTION (for List<String> rows)
    // =========================================================================

    private fun safeGetCell(row: List<String>, cellIndex: Int): String {
        if (cellIndex < 0 || cellIndex >= row.size) return ""
        return row[cellIndex].trim()
    }

    private fun safeGetCellAsInt(row: List<String>, cellIndex: Int): Int {
        if (cellIndex < 0 || cellIndex >= row.size) return 0
        val value = row[cellIndex].trim()
        return value.toDoubleOrNull()?.toInt() ?: value.toIntOrNull() ?: 0
    }

    private fun safeGetCellAsText(row: List<String>, cellIndex: Int): String {
        if (cellIndex < 0 || cellIndex >= row.size) return ""
        val raw = row[cellIndex].trim()
        val num = raw.toDoubleOrNull()
        return if (num != null && num == num.toLong().toDouble()) {
            num.toLong().toString()
        } else {
            raw
        }
    }

    private fun isRowCompletelyEmpty(row: List<String>): Boolean {
        return row.all { it.isBlank() }
    }
}