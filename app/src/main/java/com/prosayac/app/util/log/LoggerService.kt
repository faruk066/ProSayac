package com.prosayac.app.util.log

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

enum class LogTag(val displayName: String) {
    INFO("INFO"),
    WARN("WARN"),
    ERROR("ERROR"),
    HARDWARE("HARDWARE"),
    PARSER("PARSER"),
    SYNC("SYNC")
}

data class LogEntry(
    val timestamp: Long,
    val tag: LogTag,
    val message: String
) {
    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
            return sdf.format(Date(timestamp))
        }

    val formattedLine: String
        get() = "[$formattedTime] [${tag.displayName}] $message"
}

object LoggerService {

    private const val MAX_ENTRIES = 10_000

    private val logBuffer = ArrayDeque<LogEntry>(MAX_ENTRIES)

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    fun log(tag: LogTag, message: String) {
        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            tag = tag,
            message = message
        )

        synchronized(this) {
            logBuffer.addLast(entry)

            while (logBuffer.size > MAX_ENTRIES) {
                logBuffer.removeFirst()
            }

            _logs.value = logBuffer.toList()
        }
    }

    fun clearAll() {
        synchronized(this) {
            logBuffer.clear()
            _logs.value = emptyList()
        }
    }

    fun exportAsText(): String {
        val entries = _logs.value
        if (entries.isEmpty()) return "Sistem Günlükleri - Boş\n"

        val sb = StringBuilder()
        sb.appendLine("╔══════════════════════════════════════════════════════════════╗")
        sb.appendLine("║                   SAYAÇ PRO - SİSTEM GÜNLÜKLERİ               ║")
        sb.appendLine("║                   ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(Date())}                ║")
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
