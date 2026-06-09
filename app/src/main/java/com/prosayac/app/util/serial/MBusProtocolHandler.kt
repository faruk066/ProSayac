package com.prosayac.app.util.serial

import com.prosayac.app.domain.model.PollOutcome
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
        val meterId: String? = null,
        val energy: Double = 0.0,
        val volume: Double = 0.0
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
        val isWaterMeter = if (bytes.size > 14) {
            val medium = bytes[14].toInt() and 0xFF  // Medium is at byte 14, NOT byte 6 (which is CI)
            medium == MEDIUM_WARM_WATER || medium == MEDIUM_COLD_WATER
        } else {
            false
        }
        val rawHex = bytes.joinToString(" ") { "%02X".format(it) }

        // ── M-BUS FRAME VALIDATION ──
        // Validate checksum and stop byte before parsing
        if (bytes.size >= 9) {
            val lastByte = bytes[bytes.size - 1].toInt() and 0xFF
            if (lastByte != 0x16) {
                LoggerService.log(LogTag.WARN, "M-Bus çerçeve geçersiz: stop byte (0x16) bulunamadı, bulunan: %02X".format(lastByte))
                return ParseResult(null, 0.0, 0.0, rawHex, false, "Geçersiz çerçeve (stop byte yok)")
            }

            // Checksum validation: sum bytes between the two 0x68 markers
            if (bytes[0].toInt() and 0xFF == 0x68 && bytes.size >= 9) {
                val lengthByte = bytes[1].toInt() and 0xFF
                val expectedSize = lengthByte + 6

                if (bytes.size >= expectedSize) {
                    var checksumCalc = 0
                    // Sum from C field (index 4) to last data byte (before checksum byte)
                    for (i in 4 until (expectedSize - 2)) {
                        checksumCalc = (checksumCalc + (bytes[i].toInt() and 0xFF)) and 0xFF
                    }

                    val checksumReceived = bytes[expectedSize - 2].toInt() and 0xFF
                    if (checksumCalc != checksumReceived) {
                        LoggerService.log(LogTag.WARN,
                            "M-Bus checksum uyuşmazlığı: hesaplanan=%02X, alınan=%02X, ham=%s".format(
                                checksumCalc, checksumReceived, rawHex))
                        return ParseResult(null, 0.0, 0.0, rawHex, false, "Checksum hatası")
                    }
                }
            }
        }

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
                    0x01 -> rawVal = decodeInt8(valueBytes)         // 1-byte integer
                    0x02 -> rawVal = decodeInt16(valueBytes)        // 2-byte integer
                    0x03 -> rawVal = decodeInt24(valueBytes)        // 3-byte integer
                    0x04 -> rawVal = decodeInt32(valueBytes)        // 4-byte signed integer
                    0x06 -> rawVal = decodeInt48(valueBytes)        // 6-byte integer
                    0x07 -> rawVal = decodeInt64(valueBytes)        // 8-byte integer
                    0x09 -> rawVal = decodeBcd2(valueBytes)         // 2-digit BCD (1 byte)
                    0x0A -> rawVal = decodeBcd4(valueBytes)         // 4-digit BCD (2 bytes)
                    0x0B -> rawVal = decodeBcd6(valueBytes)         // 6-digit BCD (3 bytes)
                    0x0C -> rawVal = decodeBcdIntForParse(valueBytes) // BCD encoded (existing)
                    0x0D -> {
                        LoggerService.log(LogTag.WARN, "DIF 0x0D (variable length) atlandı - index $i")
                        continue  // Skip variable length gracefully
                    }
                    else -> {
                        LoggerService.log(LogTag.WARN, "Bilinmeyen DIF tipi: %02X - index $i atlandı".format(dataType))
                        continue
                    }
                }

                val vifCode = vif and 0x7F

                // Volume VIF: 0x10..0x17 (Volume in m³, ×10^(VIF-6))
                if (vifCode in 0x10..0x17) {
                    val exponent = vifCode - 0x16
                    volume += rawVal * pow10(exponent)
                    volumeFound = true
                }
                // Energy VIF: 0x00..0x07 (Energy in Wh, ×10^(VIF-3))
                else if (!isWaterMeter && vifCode in 0x00..0x07) {
                    val exponent = vifCode - 0x03
                    energy += rawVal * pow10(exponent)
                    energyFound = true
                    break
                }
                // Fallback: log unhandled VIF codes for field debugging
                else {
                    LoggerService.log(LogTag.WARN, "Unhandled VIF code: 0x${vifCode.toString(16).uppercase().padStart(2, '0')} (raw value: $rawVal)")
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
    // Validates nibbles: valid BCD is 0-9, logs warning and uses uppercase hex for invalid values.
    // ─────────────────────────────────────────────────────────────────────────
    private fun decodeBcd(bytes: List<Int>): String {
        val sb = StringBuilder()
        for (i in bytes.indices.reversed()) {
            val b = bytes[i]
            val highNibble = (b shr 4) and 0x0F
            val lowNibble = b and 0x0F
            
            // Validate BCD: nibbles should be 0-9
            if (highNibble > 9 || lowNibble > 9) {
                LoggerService.log(LogTag.WARN, "Invalid BCD nibble detected: byte=$b (0x${b.toString(16).uppercase().padStart(2, '0')}), using uppercase hex fallback")
            }
            
            sb.append(highNibble.toString(16).uppercase())
            sb.append(lowNibble.toString(16).uppercase())
        }
        return sb.toString()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DECODE BCD INT (1:1 port of MBusParser._decodeBcdInt)
    // Reads bytes forward, low nibble first.
    // Validates nibbles: valid BCD is 0-9, logs warning for invalid values.
    // ─────────────────────────────────────────────────────────────────────────
    private fun decodeBcdIntForParse(bytes: ByteArray): Double {
        var res = 0.0
        var multiplier = 1.0
        for (i in bytes.indices) {
            val b = bytes[i].toInt() and 0xFF
            val low = b and 0x0F
            val high = (b shr 4) and 0x0F
            
            // Validate BCD: nibbles should be 0-9
            if (low > 9 || high > 9) {
                LoggerService.log(LogTag.WARN, "Invalid BCD nibble in decodeBcdIntForParse: byte=$b (0x${b.toString(16).uppercase().padStart(2, '0')})")
            }
            
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
            
            // Validate BCD: nibbles should be 0-9
            if (low > 9 || high > 9) {
                LoggerService.log(LogTag.WARN, "Invalid BCD nibble in decodeBcdInt: byte=$b (0x${b.toString(16).uppercase().padStart(2, '0')})")
            }
            
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
            return (v.toLong() - 0x100000000L).toDouble()  // convert to negative via Long
        }
        return v.toDouble()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADDITIONAL INT DECODERS (1, 2, 3, 6, 8 byte integers)
    // ─────────────────────────────────────────────────────────────────────────
    private fun decodeInt8(bytes: ByteArray): Double {
        if (bytes.isEmpty()) return 0.0
        var v = bytes[0].toInt()
        // Sign extend if bit 7 is set
        if ((v and 0x80) != 0) {
            v = v or 0xFFFFFF00.toInt()
        }
        return v.toDouble()
    }

    private fun decodeInt16(bytes: ByteArray): Double {
        if (bytes.size < 2) return 0.0
        var v = (bytes[0].toInt() and 0xFF) or ((bytes[1].toInt() and 0xFF) shl 8)
        // Sign extend if bit 15 is set
        if ((v and 0x8000) != 0) {
            v = v or 0xFFFF0000.toInt()
        }
        return v.toDouble()
    }

    private fun decodeInt24(bytes: ByteArray): Double {
        if (bytes.size < 3) return 0.0
        var v = (bytes[0].toInt() and 0xFF) or
                ((bytes[1].toInt() and 0xFF) shl 8) or
                ((bytes[2].toInt() and 0xFF) shl 16)
        // Sign extend if bit 23 is set
        if ((v and 0x800000) != 0) {
            v = v or 0xFF000000.toInt()
        }
        return v.toDouble()
    }

    private fun decodeInt48(bytes: ByteArray): Double {
        if (bytes.size < 6) return 0.0
        var v = (bytes[0].toInt() and 0xFF).toLong() or
                ((bytes[1].toInt() and 0xFF).toLong() shl 8) or
                ((bytes[2].toInt() and 0xFF).toLong() shl 16) or
                ((bytes[3].toInt() and 0xFF).toLong() shl 24) or
                ((bytes[4].toInt() and 0xFF).toLong() shl 32) or
                ((bytes[5].toInt() and 0xFF).toLong() shl 40)
        // Sign extend if bit 47 is set
        if ((v and 0x800000000000L) != 0L) {
            v = v or -0x1000000000000L  // 0xFFFF000000000000 as negative
        }
        return v.toDouble()
    }

    private fun decodeInt64(bytes: ByteArray): Double {
        if (bytes.size < 8) return 0.0
        val v = (bytes[0].toInt() and 0xFF).toLong() or
                ((bytes[1].toInt() and 0xFF).toLong() shl 8) or
                ((bytes[2].toInt() and 0xFF).toLong() shl 16) or
                ((bytes[3].toInt() and 0xFF).toLong() shl 24) or
                ((bytes[4].toInt() and 0xFF).toLong() shl 32) or
                ((bytes[5].toInt() and 0xFF).toLong() shl 40) or
                ((bytes[6].toInt() and 0xFF).toLong() shl 48) or
                ((bytes[7].toInt() and 0xFF).toLong() shl 56)
        return v.toDouble()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADDITIONAL BCD DECODERS (2, 4, 6 digit BCD)
    // ─────────────────────────────────────────────────────────────────────────
    private fun decodeBcd2(bytes: ByteArray): Double {
        if (bytes.isEmpty()) return 0.0
        val b = bytes[0].toInt() and 0xFF
        val low = b and 0x0F
        val high = (b shr 4) and 0x0F
        
        // Validate BCD: nibbles should be 0-9
        if (low > 9 || high > 9) {
            LoggerService.log(LogTag.WARN, "Invalid BCD nibble in decodeBcd2: byte=$b (0x${b.toString(16).uppercase().padStart(2, '0')})")
        }
        
        return (high * 10 + low).toDouble()
    }

    private fun decodeBcd4(bytes: ByteArray): Double {
        if (bytes.size < 2) return 0.0
        var res = 0.0
        var multiplier = 1.0
        for (i in 0 until 2) {
            val b = bytes[i].toInt() and 0xFF
            val low = b and 0x0F
            val high = (b shr 4) and 0x0F
            
            // Validate BCD: nibbles should be 0-9
            if (low > 9 || high > 9) {
                LoggerService.log(LogTag.WARN, "Invalid BCD nibble in decodeBcd4: byte=$b (0x${b.toString(16).uppercase().padStart(2, '0')})")
            }
            
            res += low * multiplier
            multiplier *= 10
            res += high * multiplier
            multiplier *= 10
        }
        return res
    }

    private fun decodeBcd6(bytes: ByteArray): Double {
        if (bytes.size < 3) return 0.0
        var res = 0.0
        var multiplier = 1.0
        for (i in 0 until 3) {
            val b = bytes[i].toInt() and 0xFF
            val low = b and 0x0F
            val high = (b shr 4) and 0x0F
            
            // Validate BCD: nibbles should be 0-9
            if (low > 9 || high > 9) {
                LoggerService.log(LogTag.WARN, "Invalid BCD nibble in decodeBcd6: byte=$b (0x${b.toString(16).uppercase().padStart(2, '0')})")
            }
            
            res += low * multiplier
            multiplier *= 10
            res += high * multiplier
            multiplier *= 10
        }
        return res
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

        val isWaterMeter = if (bytes.size > 14) {
            val medium = bytes[14].toInt() and 0xFF
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
            meterId = result.meterId,
            energy = result.energy,
            volume = result.volume
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DIRECT POLL (blind 0x7B — no Calmet wake-up)
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Polls a single meter using ONLY the 0x7B REQ_UD2 command, skipping the
     * entire Calmet wake-up sequence (ping, NKE reset, E5 selection).
     *
     * This is used as a fallback when [pollMeter] fails, because some older
     * or battery-save meters (e.g. 280-series) ignore selection frames but
     * respond to a direct 0x7B request.
     */
    suspend fun pollMeterDirect(
        serialNumber: String,
        serialManager: MBusSerialManager
    ): PollOutcome {
        LoggerService.log(LogTag.HARDWARE, "KÖR OKUMA başlatıldı (Calmet atlanıyor): $serialNumber")

        try {
            if (serialManager.connectionState.value != ConnectionState.CONNECTED) {
                return PollOutcome.DeviceNotFound(null)
            }

            serialManager.sendDirectReadRequest(serialNumber)

            val startTime = System.currentTimeMillis()
            val response = serialManager.waitForRspUdFrame(timeoutMs = 2000L)
            val elapsed = System.currentTimeMillis() - startTime

            if (response == null) {
                LoggerService.log(
                    LogTag.WARN,
                    "KÖR OKUMA: $serialNumber yanıt vermedi (${elapsed}ms)"
                )
                return PollOutcome.Timeout(serialNumber, elapsed)
            }

            val result = parseRspUD(response)

            if (!result.isValid || result.readingValue == null) {
                return PollOutcome.ProtocolError(result.meterId, result.errorMessage ?: "Parse hatası")
            }

            return PollOutcome.Success(
                meterId = result.meterId ?: serialNumber,
                value = result.volume,
                unit = "m³",
                energy = result.energy,
                volume = result.volume
            )

        } catch (e: Exception) {
            LoggerService.log(LogTag.ERROR, "KÖR OKUMA hatası [$serialNumber]: ${e.message}")
            return PollOutcome.ProtocolError(null, "Kör okuma hatası: ${e.message}")
        }
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
    ): PollOutcome {
        LoggerService.log(LogTag.HARDWARE, "Sayaç sorgulama başlatıldı: $serialNumber")

        try {
            if (serialManager.connectionState.value != ConnectionState.CONNECTED) {
                return PollOutcome.DeviceNotFound(null)
            }

            // ── AGGRESSIVE BUFFER PURGE before starting ──
            serialManager.purgeAllBuffers()

            // Use the full Calmet sequence from sendReadRequest
            try {
                serialManager.sendReadRequest(serialNumber)
            } catch (e: IllegalArgumentException) {
                return PollOutcome.InvalidSerial(serialNumber, e.message ?: "Geçersiz format")
            }

            // ── SMART FRAME ACCUMULATOR ──
            val startTime = System.currentTimeMillis()
            val response = serialManager.waitForRspUdFrame(timeoutMs = 1500L)
            val elapsed = System.currentTimeMillis() - startTime

            if (response == null) {
                LoggerService.log(LogTag.WARN, "Sayaç [$serialNumber] 1500ms içinde Rsp_UD çerçevesi alınamadı")
                return PollOutcome.Timeout(serialNumber, elapsed)
            }

            // Parse the response
            val result = parseRspUD(response)

            if (!result.isValid || result.readingValue == null) {
                return PollOutcome.ProtocolError(result.meterId, result.errorMessage ?: "Parse hatası")
            }

            // Return both energy and volume; the caller (MetersViewModel) will select
            // the appropriate value based on the database meter type
            return PollOutcome.Success(
                meterId = result.meterId ?: serialNumber,
                value = result.volume,
                unit = "m³",
                energy = result.energy,
                volume = result.volume
            )

        } catch (e: Exception) {
            LoggerService.log(LogTag.ERROR, "Sayaç [$serialNumber] sorgulama hatası: ${e.message}")
            return PollOutcome.ProtocolError(null, "Sorgulama hatası: ${e.message}")
        }
    }
}
