package com.prosayac.app.util.log

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

/**
 * Log categories for color-coded terminal display.
 */
enum class LogTag(val displayName: String) {
    INFO("INFO"),
    WARN("WARN"),
    ERROR("ERROR"),
    HARDWARE("HARDWARE"),
    PARSER("PARSER")
}

/**
 * Individual log entry stored in the ring buffer.
 */
data class LogEntry(
    val timestamp: Long,
    val tag: LogTag,
    val message: String
) {
    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }

    val formattedLine: String
        get() = "[$formattedTime] [${tag.displayName}] $message"
}

/**
 * Singleton LoggerService - Black Box (Kara Kutu) for ProSayac.
 *
 * Intercepts:
 *  - Hardware HEX data (incoming/outgoing)
 *  - Parser events (Template detected, Row processing status)
 *  - Exceptions (try-catch logs from all layers)
 *  - General info/warn messages
 *
 * Thread-safe, observable via StateFlow.
 * MAX_ENTRIES limit prevents unbounded memory growth.
 */
object LoggerService {

    /** Maximum log entries in the ring buffer */
    private const val MAX_ENTRIES = 10_000

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    /**
     * Records a log entry at the given tag level.
     *
     * @param tag The log category (INFO, WARN, ERROR, HARDWARE, PARSER)
     * @param message The log message
     */
    fun log(tag: LogTag, message: String) {
        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            tag = tag,
            message = message
        )

        synchronized(this) {
            val current = _logs.value.toMutableList()
            current.add(entry)

            // Trim to MAX_ENTRIES (ring buffer behavior)
            if (current.size > MAX_ENTRIES) {
                val overflow = current.size - MAX_ENTRIES
                for (i in 0 until overflow) {
                    current.removeAt(0)
                }
            }

            _logs.value = current
        }
    }

    /**
     * Clears all log entries from the buffer.
     */
    fun clearAll() {
        _logs.value = emptyList()
    }

    /**
     * Exports all current log entries as a formatted text string.
     *
     * @return Complete log text with header, suitable for file export
     */
    fun exportAsText(): String {
        val entries = _logs.value
        if (entries.isEmpty()) return "Sistem Günlükleri - Boş\n"

        val sb = StringBuilder()
        sb.appendLine("╔══════════════════════════════════════════════════════════════╗")
        sb.appendLine("║                   SAYAÇ PRO - SİSTEM GÜNLÜKLERİ               ║")
        sb.appendLine("║                   ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}                ║")
        sb.appendLine("╚══════════════════════════════════════════════════════════════╝")
        sb.appendLine()
        sb.appendLine("Toplam kayıt: ${entries.size}")
        sb.appendLine("─".repeat(64))

        entries.forEach { entry ->
            sb.appendLine(entry.formattedLine)
        }

        sb.appendLine()
        sb.appendLine("─".repeat(64))
        sb.appendLine("── SAYAÇ PRO v1.0.0 · Pro Max ──")

        return sb.toString()
    }
}