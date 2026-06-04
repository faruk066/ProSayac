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
    PARSER("PARSER")
}

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

object LoggerService {

    private const val MAX_ENTRIES = 10_000

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    fun log(tag: LogTag, message: String) {
        val entry = LogEntry(
            timestamp = System.currentTimeMillis(),
            tag = tag,
            message = message
        )

        synchronized(this) {
            val current = _logs.value.toMutableList()
            current.add(entry)

            if (current.size > MAX_ENTRIES) {
                val overflow = current.size - MAX_ENTRIES
                repeat(overflow) { current.removeAt(0) }
            }

            _logs.value = current
        }
    }

    fun clearAll() {
        synchronized(this) {
            _logs.value = emptyList()
        }
    }

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
