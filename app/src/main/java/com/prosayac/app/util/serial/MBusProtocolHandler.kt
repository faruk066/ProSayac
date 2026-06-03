package com.prosayac.app.util.serial

import com.prosayac.app.util.log.LoggerService
import com.prosayac.app.util.log.LogTag
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.ByteArrayOutputStream

/**
 * M-Bus Protocol Handler (EN 13757-2/3)
 *
 * Implements real M-Bus frame construction and parsing:
 *  - SND_UD: Request frame sent to meter
 *  - Rsp_UD: Response frame received from meter
 *  - Frame validation via checksum
 *  - Endeks (reading) extraction from variable data response
 *  - 5-second hardware timeout per meter
 */
object MBusProtocolHandler {

    /** SND_UD C-Field (Request to meter with secondary addressing) */
    private const val SND_UD = 0x5B.toByte()

    /** Secondary addressing prefix */
    private const val SECONDARY_ADDR = 0xFD.toByte()

    /** CI field: Application Reset / Data Send */
    private const val CI_DATA_SEND = 0x51.toByte()

    /** Rsp_UD start delimiter */
    private const val RSP_START = 0x68.toByte()

    /** CI field: Variable Data Respond */
    private const val CI_VAR_RESPOND = 0x72.toByte()

    /** Timeout for meter response in milliseconds */
    const val RESPONSE_TIMEOUT_MS = 5000L

    /** Inter-frame delay between successive meter polls (ms) */
    const val INTER_FRAME_DELAY_MS = 200L

    // =============================================================================
    // SND_UD FRAME CONSTRUCTION
    // =============================================================================

    /**
     * Builds a SND_UD frame with secondary addressing using the meter serial number.
     * Frame: 10 | C | A | CI | secondary_addr... | CS | 16
     *
     * @param serialNumber The meter's serial number, used as secondary address
     * @return Complete SND_UD frame as hex string and raw bytes
     */
    fun buildSND_UD(serialNumber: String): Pair<String, ByteArray> {
        val out = ByteArrayOutputStream()

        // Start byte
        out.write(0x10)

        // C-Field: SND_UD
        out.write(SND_UD.toInt())

        // A-Field: secondary addressing
        out.write(SECONDARY_ADDR.toInt())

        // CI-Field: data send
        out.write(CI_DATA_SEND.toInt())

        // Secondary address: BCD-encoded serial number padded to 8 bytes
        val serialBytes = serialNumberToBcdBytes(serialNumber)
        for (b in serialBytes) {
            out.write(b.toInt())
        }

        // Checksum: sum of C + A + CI + secondary_addr bytes, mod 256
        var checksum = SND_UD.toInt() and 0xFF
        checksum = (checksum + (SECONDARY_ADDR.toInt() and 0xFF))
        checksum = (checksum + (CI_DATA_SEND.toInt() and 0xFF))
        for (b in serialBytes) {
            checksum = (checksum + b) and 0xFF
        }
        out.write(checksum)

        // Stop byte
        out.write(0x16)

        val frame = out.toByteArray()
        val hex = frame.joinToString(" ") { "%02X".format(it) }

        LoggerService.log(LogTag.HARDWARE, "SND_UD oluşturuldu [$serialNumber]: $hex")

        return Pair(hex, frame)
    }

    /**
     * Converts a serial number string to BCD-encoded bytes, padded to 8 bytes.
     * Example: "12345678" → [0x12, 0x34, 0x56, 0x78] (4 bytes) → padded to 8 bytes
     */
    private fun serialNumberToBcdBytes(serial: String): ByteArray {
        val digitsOnly = serial.filter { it.isDigit() }
        val padded = if (digitsOnly.length % 2 != 0) "0$digitsOnly" else digitsOnly
        val result = mutableListOf<Int>()

        var i = 0
        while (i < padded.length) {
            val high = (padded[i] - '0') shl 4
            val low = if (i + 1 < padded.length) (padded[i + 1] - '0') else 0
            result.add(high or low)
            i += 2
        }

        // Pad to exactly 8 bytes with 0x00 on the left
        val bytes = IntArray(8) { 0 }
        val srcStart = 8 - result.size
        for (j in result.indices) {
            if (srcStart + j >= 0 && srcStart + j < 8) {
                bytes[srcStart + j] = result[j]
            }
        }

        return bytes.map { it.toByte() }.toByteArray()
    }

    // =============================================================================
    // Rsp_UD FRAME PARSING
    // =============================================================================

    /**
     * Result of parsing an M-Bus response frame.
     */
    data class ResponseResult(
        val isValid: Boolean,
        val readingValue: String?,
        val rawHex: String,
        val errorMessage: String? = null
    )

    /**
     * Parses a raw byte stream for an Rsp_UD frame and extracts the meter reading.
     *
     * Rsp_UD structure:
     *   68 | L | L | 68 | C | A... | CI | Data... | CS | 16
     *
     * @param bytes Raw bytes received from the serial port
     * @return Parsed response with extracted reading value or error info
     */
    fun parseRspUD(bytes: ByteArray): ResponseResult {
        val hex = bytes.joinToString(" ") { "%02X".format(it) }
        LoggerService.log(LogTag.HARDWARE, "Rsp_UD ham veri (${bytes.size} byte): $hex")

        if (bytes.size < 9) {
            LoggerService.log(LogTag.ERROR, "Rsp_UD geçersiz: çok kısa (${bytes.size} byte)")
            return ResponseResult(
                isValid = false,
                readingValue = null,
                rawHex = hex,
                errorMessage = "Yanıt çerçevesi çok kısa (${bytes.size} byte)"
            )
        }

        try {
            // Locate start delimiter(s)
            var idx = 0
            // Skip leading non-frame bytes
            while (idx < bytes.size - 1) {
                if (bytes[idx] == RSP_START) break
                idx++
            }

            if (idx >= bytes.size - 4) {
                return ResponseResult(
                    isValid = false,
                    readingValue = null,
                    rawHex = hex,
                    errorMessage = "Başlangıç ayracı (68h) bulunamadı"
                )
            }

            val startIdx = idx
            val lField = bytes[startIdx + 1].toInt() and 0xFF
            val lField2 = bytes[startIdx + 2].toInt() and 0xFF

            // L-field repeated for robustness
            if (lField != lField2) {
                LoggerService.log(LogTag.WARN, "Rsp_UD L-alanı uyuşmazlığı: $lField vs $lField2")
            }

            val start2 = bytes[startIdx + 3]
            if (start2 != RSP_START) {
                return ResponseResult(
                    isValid = false,
                    readingValue = null,
                    rawHex = hex,
                    errorMessage = "İkinci başlangıç ayracı (68h) hatalı: ${"%02X".format(start2)}"
                )
            }

            // Calculate frame end
            val frameEnd = startIdx + 4 + lField + 1 // +1 for CS, +1 for stop
            val dataStart = startIdx + 5 // After 68 L L 68 C A...

            if (frameEnd >= bytes.size) {
                return ResponseResult(
                    isValid = false,
                    readingValue = null,
                    rawHex = hex,
                    errorMessage = "Çerçeve eksik: ${bytes.size}/${frameEnd + 1} byte"
                )
            }

            val cField = bytes[startIdx + 4]
            LoggerService.log(LogTag.HARDWARE, "Rsp_UD C-alanı: ${"%02X".format(cField)}")

            // Find CI field (after variable-length A-field)
            // A-field can be 1 byte (primary) or multiple bytes (secondary addressing)
            var ciIdx = dataStart
            // In real M-Bus, we'd parse the A-field properly. For now find CI.
            // CI is typically at a known offset after address bytes for secondary addressing.
            // For secondary addressing (when A-field starts with FD), A is 1+8 bytes.
            // We look for CI value that makes sense (72h for variable data respond)
            val ciValue = findCIByte(bytes, dataStart)
            if (ciValue < 0) {
                return ResponseResult(
                    isValid = false,
                    readingValue = null,
                    rawHex = hex,
                    errorMessage = "CI alanı bulunamadı"
                )
            }

            ciIdx = ciValue // actual index of CI byte
            val dataStartIdx = ciIdx + 1

            // Extract data portion
            val dataSize = (startIdx + 4 + lField) - dataStartIdx
            if (dataSize <= 0) {
                return ResponseResult(
                    isValid = false,
                    readingValue = null,
                    rawHex = hex,
                    errorMessage = "Veri kısmı boş"
                )
            }

            val dataBytes = bytes.copyOfRange(dataStartIdx, dataStartIdx + dataSize)

            // Verify checksum
            var calculatedCs = cField.toInt() and 0xFF
            for (i in startIdx + 5 until startIdx + 4 + lField) {
                calculatedCs = (calculatedCs + (bytes[i].toInt() and 0xFF)) and 0xFF
            }
            val receivedCs = bytes[startIdx + 4 + lField].toInt() and 0xFF

            val checksumOk = calculatedCs == receivedCs
            LoggerService.log(
                LogTag.HARDWARE,
                "Rsp_UD checksum: hesaplanan=${"%02X".format(calculatedCs)} " +
                    "gelen=${"%02X".format(receivedCs)} ${if (checksumOk) "DOĞRU" else "HATALI"}"
            )

            if (!checksumOk) {
                LoggerService.log(LogTag.WARN, "Rsp_UD checksum hatası - veri yine de işlenecek")
            }

            // Extract reading value from data blocks
            val reading = extractReadingFromData(dataBytes)
            LoggerService.log(
                LogTag.INFO,
                "Rsp_UD parse edildi: endeks=$reading, checksum=${if (checksumOk) "OK" else "FAIL"}"
            )

            return ResponseResult(
                isValid = checksumOk,
                readingValue = reading,
                rawHex = hex,
                errorMessage = if (checksumOk) null else "Checksum hatası"
            )
        } catch (e: Exception) {
            LoggerService.log(LogTag.ERROR, "Rsp_UD parse hatası: ${e.message}")
            return ResponseResult(
                isValid = false,
                readingValue = null,
                rawHex = hex,
                errorMessage = "Parse hatası: ${e.message}"
            )
        }
    }

    /**
     * Searches for the CI byte in the A-field region.
     * For secondary addressing, returns the index after the 8-byte address.
     */
    private fun findCIByte(bytes: ByteArray, dataStart: Int): Int {
        // Try the CI as byte right after 1-byte primary address
        var idx = dataStart + 1
        if (idx < bytes.size && bytes[idx] == CI_VAR_RESPOND) {
            return idx
        }

        // Try secondary addressing: skip A-field prefix (FD) + 8 bytes secondary address
        idx = dataStart + 1 + 8 // FD + 8 byte secondary address
        if (idx < bytes.size) {
            return idx
        }

        // Fallback: scan for CI byte
        for (i in dataStart until minOf(bytes.size, dataStart + 12)) {
            if (bytes[i].toInt() and 0xFF in listOf(0x70, 0x71, 0x72, 0x73, 0x76, 0x78)) {
                return i
            }
        }

        // Last resort: assume 1-byte address, CI right after
        return dataStart + 1
    }

    // =============================================================================
    // DATA EXTRACTION (Endeks / Reading Value)
    // =============================================================================

    /**
     * Extracts the reading value (Endeks) from M-Bus variable data response blocks.
     *
     * M-Bus data blocks encode values with DIF (Data Information Field) and
     * VIF (Value Information Field) followed by the actual data bytes.
     *
     * This implementation searches for the first numeric DIF/VIF combination
     * and extracts the associated integer value.
     *
     * DIF format (1 byte):
     *   bit 7-6: data type (0=no data, 1=8bit, 2=16bit, 3=24bit, 4=32bit, 5=32bit/N, 6=48bit, 7=64bit)
     *   bit 5: storage number
     *   bit 4-0: tariff (for storage)
     *
     * VIF format (1 byte):
     *   bit 7: VIF extension
     *   bit 6-0: value information
     *
     * Common energy reading VIF: 0x00-0x07 (Energy in Wh, ×10^(VIF-3))
     * Common volume reading VIF: 0x10-0x17 (Volume in m³, ×10^(VIF-5))
     */
    private fun extractReadingFromData(data: ByteArray): String? {
        if (data.isEmpty()) return null

        var i = 0
        while (i < data.size - 2) {
            val dif = data[i].toInt() and 0xFF
            i++

            if (dif == 0x0F || dif == 0x1F || dif == 0x2F) {
                // DIFE byte, skip and continue
                val difType = (dif shr 6) and 0x03
                if (difType > 0) {
                    // Has data after DIFE
                    continue
                }
                continue
            }

            if (dif == 0x2F || dif == 0x3F || dif == 0x4F || dif == 0x5F || dif == 0x6F || dif == 0x7F) {
                continue // DIFE only, no data
            }

            if (i >= data.size) break

            val vif = data[i].toInt() and 0xFF
            i++

            // VIFE handling
            if (vif == 0xFD || vif == 0xFB) {
                if (i < data.size) {
                    val vife = data[i].toInt() and 0xFF
                    i++
                    LoggerService.log(LogTag.HARDWARE, "VIFE tespit edildi: ${"%02X".format(vife)}")
                }
                continue // Skip VIFE for now, try next block
            }

            val dataBytes: Int = when ((dif shr 6) and 0x03) {
                0 -> 0    // no data
                1 -> 1    // 8 bit integer
                2 -> 2    // 16 bit integer
                3 -> 3    // 24 bit integer
                4 -> 4    // 32 bit integer
                5 -> { // 32 bit N (sometimes 4 bytes + 1)
                    i++
                    4
                }
                6 -> 8    // 48 bit integer (6 bytes, padded to 8)
                7 -> 8    // 64 bit integer
                else -> 0
            }

            if (dataBytes == 0 || i + dataBytes > data.size) continue

            // Read multi-byte value (LSB first)
            var value: Long = 0
            for (b in 0 until dataBytes) {
                value = value or ((data[i + b].toLong() and 0xFF) shl (b * 8))
            }
            i += dataBytes

            // VIF decoding for multiplier
            val vifType = vif and 0x7F
            val multiplier = when {
                vifType in 0x00..0x07 -> Math.pow(10.0, (vifType - 3).toDouble()).toLong() // Energy Wh -> kWh
                vifType in 0x08..0x0F -> Math.pow(10.0, (vifType - 3).toDouble()).toLong() // Energy J -> kJ
                vifType in 0x10..0x17 -> Math.pow(10.0, (vifType - 6).toDouble()).toLong() // Volume m3
                vifType in 0x18..0x1F -> Math.pow(10.0, (vifType - 6).toDouble()).toLong() // Volume m3
                vifType in 0x20..0x27 -> Math.pow(10.0, (vifType - 6).toDouble()).toLong() // Volume m3/min
                vifType in 0x28..0x2F -> Math.pow(10.0, (vifType - 6).toDouble()).toLong() // Volume m3/h
                vifType in 0x30..0x37 -> Math.pow(10.0, (vifType - 3).toDouble()).toLong() // Power W
                vifType in 0x38..0x3F -> Math.pow(10.0, (vifType - 3).toDouble()).toLong() // Power J/h
                else -> 1
            }

            if (value > 0) {
                val finalValue = value * multiplier
                val displayValue = if (multiplier == 1L) {
                    value.toString()
                } else {
                    String.format("%.0f", value.toDouble() * multiplier)
                }

                LoggerService.log(
                    LogTag.INFO,
                    "Endeks çıkarıldı: ham=$value, çarpan=$multiplier, sonuç=$displayValue"
                )
                return displayValue
            }
        }

        LoggerService.log(LogTag.WARN, "Veri bloklarından endeks çıkarılamadı")
        return null
    }

    // =============================================================================
    // METER POLLING PIPELINE
    // =============================================================================

    /**
     * Polls a single meter via the serial manager.
     *
     * Sends SND_UD, waits for Rsp_UD with a 5-second timeout.
     *
     * @param serialNumber The meter's serial number for addressing
     * @param serialManager The active serial manager to use for communication
     * @return The parsed reading value, or null if timeout/error
     */
    suspend fun pollMeter(
        serialNumber: String,
        serialManager: MBusSerialManager
    ): ResponseResult = withContext(Dispatchers.IO) {
        LoggerService.log(LogTag.HARDWARE, "Sayaç sorgulama başlatıldı: $serialNumber")

        // Build and send SND_UD
        val (hex, frame) = buildSND_UD(serialNumber)

        try {
            if (serialManager.connectionState.value != ConnectionState.CONNECTED) {
                return@withContext ResponseResult(
                    isValid = false,
                    readingValue = null,
                    rawHex = "",
                    errorMessage = "M-Bus bağlı değil"
                )
            }

            serialManager.send(frame)
            LoggerService.log(LogTag.HARDWARE, "SND_UD gönderildi: $hex")

            // Wait for response with timeout
            val response = withTimeoutOrNull(RESPONSE_TIMEOUT_MS) {
                serialManager.waitForRawResponse()
            }

            if (response == null) {
                LoggerService.log(LogTag.WARN, "Sayaç [$serialNumber] 5 saniye içinde yanıt vermedi")
                return@withContext ResponseResult(
                    isValid = false,
                    readingValue = null,
                    rawHex = "",
                    errorMessage = "Cihaz Yanıt Vermedi (5s timeout)"
                )
            }

            // Parse the response
            parseRspUD(response)
        } catch (e: Exception) {
            LoggerService.log(LogTag.ERROR, "Sayaç [$serialNumber] sorgulama hatası: ${e.message}")
            ResponseResult(
                isValid = false,
                readingValue = null,
                rawHex = hex,
                errorMessage = "Sorgulama hatası: ${e.message}"
            )
        }
    }
}