package com.prosayac.app.presentation.logs

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prosayac.app.util.log.LogEntry
import com.prosayac.app.util.log.LoggerService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class LogsUiState(
    val logs: List<LogEntry> = emptyList(),
    val exportPath: String? = null,
    val showExportDone: Boolean = false
)

@HiltViewModel
class LogsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogsUiState())
    val uiState: StateFlow<LogsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            LoggerService.logs.collect { entries ->
                _uiState.value = _uiState.value.copy(logs = entries)
            }
        }
    }

    fun clearLogs() {
        LoggerService.clearAll()
    }

    fun exportLogs() {
        viewModelScope.launch {
            try {
                val text = LoggerService.exportAsText()

                val exportDir = File(context.cacheDir, "exports")
                exportDir.mkdirs()

                val fileName = "sistem_gunlukleri_${System.currentTimeMillis()}.txt"
                val file = File(exportDir, fileName)
                file.writeText(text, Charsets.UTF_8)

                _uiState.value = _uiState.value.copy(
                    exportPath = file.absolutePath,
                    showExportDone = true
                )
            } catch (e: Exception) {
                // Silent fail - log export is best-effort
            }
        }
    }

    fun dismissExportDone() {
        _uiState.value = _uiState.value.copy(
            showExportDone = false,
            exportPath = null
        )
    }

    fun shareExport() {
        val path = _uiState.value.exportPath ?: return
        try {
            val file = File(path)
            if (!file.exists()) return

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Günlükleri Paylaş")
            // Required when starting activity from non-Activity context (Application)
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            // Best-effort: silently ignore share failures
        }
    }
}
