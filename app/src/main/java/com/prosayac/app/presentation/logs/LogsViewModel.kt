package com.prosayac.app.presentation.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prosayac.app.util.log.LogEntry
import com.prosayac.app.util.log.LogExportService
import com.prosayac.app.util.log.LoggerService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LogsUiState(
    val logs: List<LogEntry> = emptyList(),
    val exportPath: String? = null,
    val showExportDone: Boolean = false
)

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val logExportService: LogExportService
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
            val text = LoggerService.exportAsText()
            val path = logExportService.exportToFile(text)
            if (path != null) {
                _uiState.value = _uiState.value.copy(
                    exportPath = path,
                    showExportDone = true
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

    fun shareExport() {
        val path = _uiState.value.exportPath ?: return
        logExportService.shareFile(path)
    }
}
