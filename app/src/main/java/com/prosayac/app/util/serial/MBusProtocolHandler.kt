package com.prosayac.app.util.serial

import com.prosayac.app.util.log.LoggerService
import com.prosayac.app.util.log.LogTag

/**
 * MBusParser — 1:1 port of mbus_parser.dart.
 *
 * Parses raw M-Bus Rsp_UD frames and extracts meter ID, energy (Wh), and volume (m³).
 * Handles water meter vs energy meter distinction via the isWaterMeter flag.
 *
 * CRITICAL: Kotlin Byte is signed (-128..127). All bitwise operations MUST use
 * (byte.toInt() and 0xFF) to match Dart's unsigned byte behavior (0..255).
 */
object MBusProtocolHandler {

    /** Result of parsing an M-Bus response frame. */
    data class ParseResult(
        val meterId: String?,
        val energy: Double,
        val volume: Double,
        val rawHex: String,
        val isValid: Boolean,
        val errorMessage: String? = null
    )

    /** Legacy result type for backward compatibility with pollMeter callers. */
    data class ResponseResult(
        val isValid: Boolean,
        val readingValue: String?,
        val rawHex: String,
        val errorMessage: String? = null,
        val meterId: String? = null
    )

    /** Inter-frame delay between successive meter polls (ms) */
    const val INTER_FRAME_DELAY_MS = 200L

    // ── Medium byte constants (CI field / Measurement Medium) ──
    private const val MEDIUM_HEAT = 0x04       // Heat / Energy meter
    private const val MEDIUM_WARM_WATER = 0x06 // Warm water
    private const val MEDIUM_COLD_WATER = 0x07 // Cold water

    // ─────────────────────────────────────────────────────────────────────────
    // PARSE DATA (1:1 port of MBusParser.parseData)
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Parses the raw byte array from an M-Bus Rsp_UD frame.
     *
     * Auto-detects water vs heat meter from the medium byte at bytes[6].
     *
     * @param bytes Raw response bytes (should be echo-filtered already)
     * @return ParseResult with extracted meter ID, energy, and volume values
     */
    fun parseData(bytes: ByteArray): ParseResult {
        val isWaterMeter = if (bytes.size > 6) {
            val medium = bytes[6].toInt() and 0xFF
            medium == MEDIUM_WARM_WATER || medium == MEDIUM_COLD_WATER
        } else {
            false
        }
        val rawHex = bytes.joinToString(" ") { "%02X".format(it) }

        if (bytes.isEmpty()) {
            return ParseResult(null, 0.0, 0.0, rawHex, false, "Boş veri")
        }

        // Check start delimiter 0x68
        if ((bytes[0].toInt() and 0xFF) != 0x68) {
            return ParseResult(null, 0.0, 0.0, rawHex, false, "Başlangıç ayracı (68h) bulunamadı")
        }

        // Need at least 19 bytes for header + CI
        if (bytes.size < 19) {
            return ParseResult(null, 0.0, 0.0, rawHex, false, "Veri çok kısa (min 19 byte gerekli)")
        }

        // Extract meter ID: bytes[7..10] (4 bytes), decoded as BCD
        val idBytes = bytes.copyOfRange(7, 11).map { it.toInt() and 0xFF }
        val meterId = decodeBcd(idBytes.toList())

        var energy = 0.0
        var volume = 0.0

        var energyFound = false
        var volumeFound = false

        var i = 19
        while (i < bytes.size) {
            try {
                val dif = bytes[i].toInt() and 0xFF

                // Break on fill bytes (0x0F) or 0x1F
                if (dif == 0x0F || dif == 0x1F) break

                val dataType = dif and 0x0F
                i++

                // Skip DIF extension bytes (DIF has bit 7 set)
                while (i < bytes.size && ((bytes[i - 1].toInt() and 0xFF) and 0x80) != 0) {
                    if (((bytes[i].toInt() and 0xFF) and 0x80) == 0) {
                        i++
                        break
                    }
                    i++
                }
                if (i >= bytes.size) break

                // Read VIF
                val vif = bytes[i].toInt() and 0xFF
                i++

                // Skip VIF extension bytes (VIF has bit 7 set)
                while (i < bytes.size && ((bytes[i - 1].toInt() and 0xFF) and 0x80) != 0) {
                    if (((bytes[i].toInt() and 0xFF) and 0x80) == 0) {
                        i++
                        break
                    }
                    i++
                }
                if (i >= bytes.size) break

                // Determine data length from DIF data type
                val length = dataLength(dataType)
                // Unknown DIF type: skip this block cleanly — do NOT abort the entire parse.
                // Manufacturer-specific blocks (e.g. 42 6C) must not invalidate already-parsed values.
                if (length < 0) continue
                if (i + length > bytes.size) break

                val valueBytes = bytes.copyOfRange(i, i + length)
                i += length

                // Decode value based on data type
                var rawVal = 0.0
                when (dataType) {
                    0x04 -> rawVal = decodeInt32(valueBytes)        // 4-byte signed integer
                    0x0C -> rawVal = decodeBcdIntForParse(valueBytes) // BCD encoded
                    else -> continue
                }

                val vifCode = vif and 0x7F

                // Volume VIF: 0x10..0x17 (Volume in m³, ×10^(VIF-6))
                if (vifCode in 0x10..0x17) {
                    val exponent = vifCode - 0x16
                    volume += rawVal * pow10(exponent)
                    volumeFound = true
                    if (isWaterMeter) break
                }
                // Energy VIF: 0x00..0x07 (Energy in Wh, ×10^(VIF-3))
                else if (!isWaterMeter && vifCode in 0x00..0x07) {
                    val exponent = vifCode - 0x03
                    energy += rawVal * pow10(exponent)
                    energyFound = true
                    break
                }
            } catch (e: Exception) {
                LoggerService.log(LogTag.WARN, "Error parsing M-Bus data block at index $i. Stopping parse but preserving found values. Error: ${e.message}")
                break
            }
        }

        // Convert energy from Wh to kWh
        val energyInKwh = energy / 1000.0

        val isValid = meterId.isNotEmpty() && (energyFound || volumeFound)
        val errorMsg = when {
            meterId.isEmpty() -> "Sayaç ID çözümlenemedi"
            !isValid -> "Enerji/Volume değeri bulunamadı"
            else -> null
        }

        LoggerService.log(
            LogTag.INFO,
            "Parse: ID=$meterId, Energy=${energyInKwh} kWh, Volume=$volume m³, Valid=$isValid"
        )

        return ParseResult(
            meterId = meterId.ifEmpty { null },
            energy = energyInKwh,
            volume = volume,
            rawHex = rawHex,
            isValid = isValid,
            errorMessage = errorMsg
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DECODE BCD (1:1 port of MBusParser._decodeBcd)
    // Reads bytes in reverse, outputs hex string.
    // ─────────────────────────────────────────────────────────────────────────
    private fun decodeBcd(bytes: List<Int>): String {
        val sb = StringBuilder()
        for (i in bytes.indices.reversed()) {
            val b = bytes[i]
            sb.append(((b shr 4) and 0x0F).toString(16))
            sb.append((b and 0x0F).toString(16))
        }
        return sb.toString()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DECODE BCD INT (1:1 port of MBusParser._decodeBcdInt)
    // Reads bytes forward, low nibble first.
    // ─────────────────────────────────────────────────────────────────────────
    private fun decodeBcdIntForParse(bytes: ByteArray): Double {
        var res = 0.0
        var multiplier = 1.0
        for (i in bytes.indices) {
            val b = bytes[i].toInt() and 0xFF
            val low = b and 0x0F
            val high = (b shr 4) and 0x0F
            res += low * multiplier
            multiplier *= 10
            res += high * multiplier
            multiplier *= 10
        }
        return res
    }

    /** Visible for testing. */
    fun decodeBcdInt(bytes: List<Int>): Double {
        var res = 0.0
        var multiplier = 1.0
        for (i in bytes.indices) {
            val b = bytes[i]
            val low = b and 0x0F
            val high = (b shr 4) and 0x0F
            res += low * multiplier
            multiplier *= 10
            res += high * multiplier
            multiplier *= 10
        }
        return res
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DATA LENGTH (1:1 port of MBusParser._dataLength)
    // ─────────────────────────────────────────────────────────────────────────
    private fun dataLength(dataType: Int): Int {
        return when (dataType) {
            0x00 -> 0
            0x01 -> 1
            0x02 -> 2
            0x03 -> 3
            0x04 -> 4
            0x05 -> 4
            0x06 -> 6
            0x07 -> 8
            0x0C -> 4
            0x0D -> -1
            0x0E -> 6
            else -> -1
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DECODE INT32 (1:1 port of MBusParser._decodeInt32)
    // Little-endian 4-byte signed integer.
    // ─────────────────────────────────────────────────────────────────────────
    private fun decodeInt32(bytes: ByteArray): Double {
        if (bytes.size < 4) return 0.0

        // Build as unsigned int first (all bytes treated as 0..255)
        var v = (bytes[0].toInt() and 0xFF) or
                ((bytes[1].toInt() and 0xFF) shl 8) or
                ((bytes[2].toInt() and 0xFF) shl 16) or
                ((bytes[3].toInt() and 0xFF) shl 24)

        // Sign-extend if bit 31 is set
        if ((v and 0x80000000.toInt()) != 0) {
            v = v - 0x100000000.toInt()  // convert to negative
        }
        return v.toDouble()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POW10 LOOKUP (1:1 port of MBusParser._pow10Lookup and _pow10)
    // ─────────────────────────────────────────────────────────────────────────
    private val pow10Lookup = doubleArrayOf(
        1.0, 10.0, 100.0, 1000.0, 10000.0,
        100000.0, 1000000.0, 10000000.0, 100000000.0, 1000000000.0
    )

    private fun pow10(exponent: Int): Double {
        return if (exponent in pow10Lookup.indices) {
            pow10Lookup[exponent]
        } else {
            Math.pow(10.0, exponent.toDouble())
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LEGACY: parseRspUD (backward compatibility adapter)
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Parses a raw byte stream for an Rsp_UD frame and extracts the meter reading.
     * Kept for backward compatibility — delegates to parseData.
     */
    fun parseRspUD(bytes: ByteArray): ResponseResult {
        val hex = bytes.joinToString(" ") { "%02X".format(it) }
        LoggerService.log(LogTag.HARDWARE, "Rsp_UD ham veri (${bytes.size} byte): $hex")

        val result = parseData(bytes)

        val isWaterMeter = if (bytes.size > 6) {
            val medium = bytes[6].toInt() and 0xFF
            medium == MEDIUM_WARM_WATER || medium == MEDIUM_COLD_WATER
        } else {
            false
        }

        // Determine the primary reading value from the parser's auto-detection
        val readingValue = if (result.isValid) {
            if (isWaterMeter) {
                String.format("%.3f", result.volume)
            } else { // Heat meter, energy is in kWh
                String.format("%.3f", result.energy)
            }
        } else {
            null
        }

        return ResponseResult(
            isValid = result.isValid,
            readingValue = readingValue,
            rawHex = result.rawHex,
            errorMessage = result.errorMessage,
            meterId = result.meterId
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LEGACY: pollMeter (backward compatibility, uses sendReadRequest pattern)
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Polls a single meter via the serial manager.
     * Now delegates to sendReadRequest for proper Calmet wake-up sequence.
     * Waits for response with a timeout.
     */
    suspend fun pollMeter(
        serialNumber: String,
        serialManager: MBusSerialManager
    ): ResponseResult {
        LoggerService.log(LogTag.HARDWARE, "Sayaç sorgulama başlatıldı: $serialNumber")

        try {
            if (serialManager.connectionState.value != ConnectionState.CONNECTED) {
                return ResponseResult(
                    isValid = false,
                    readingValue = null,
                    rawHex = "",
                    errorMessage = "M-Bus bağlı değil"
                )
            }

            // ── AGGRESSIVE BUFFER PURGE before starting ──
            // Ensures no stale bytes from previous cycles are present.
            serialManager.purgeAllBuffers()

            // Use the full Calmet sequence from sendReadRequest
            // (which also calls purgeAllBuffers internally before the REQ_UD2)
            serialManager.sendReadRequest(serialNumber)

            // ── SMART FRAME ACCUMULATOR ──
            // Hunts for 0x68, discards garbage (F9/E5/00), waits for
            // L+6 bytes. Default 1500ms timeout.
            val response = serialManager.waitForRspUdFrame(timeoutMs = 1500L)

            if (response == null) {
                LoggerService.log(LogTag.WARN, "Sayaç [$serialNumber] 1500ms içinde Rsp_UD çerçevesi alınamadı")
                return ResponseResult(
                    isValid = false,
                    readingValue = null,
                    rawHex = "",
                    errorMessage = "Cihaz Yanıt Vermedi (1500ms timeout)"
                )
            }

            // Parse the response
            return parseRspUD(response)
        } catch (e: Exception) {
            LoggerService.log(LogTag.ERROR, "Sayaç [$serialNumber] sorgulama hatası: ${e.message}")
            return ResponseResult(
                isValid = false,
                readingValue = null,
                rawHex = "",
                errorMessage = "Sorgulama hatası: ${e.message}"
            )
        }
    }
}