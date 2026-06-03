package com.prosayac.app.presentation.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prosayac.app.util.serial.ConnectionState
import com.prosayac.app.util.serial.MBusSerialManager
import com.prosayac.app.util.serial.SerialConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConnectionUiState(
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val baudRate: Int = 9600,
    val isConnecting: Boolean = false
)

@HiltViewModel
class ConnectionViewModel @Inject constructor(
    private val serialManager: MBusSerialManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectionUiState())
    val uiState: StateFlow<ConnectionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            serialManager.connectionState.collect { state ->
                _uiState.value = _uiState.value.copy(
                    connectionState = state,
                    isConnecting = state == ConnectionState.CONNECTING
                )
            }
        }
    }

    fun connect(baudRate: Int = 9600) {
        _uiState.value = _uiState.value.copy(baudRate = baudRate)
        viewModelScope.launch {
            serialManager.connect(
                SerialConfig(baudRate = baudRate)
            )
        }
    }

    fun disconnect() {
        serialManager.disconnect()
    }

    fun refreshState() {
        serialManager.refreshConnectionState()
    }

    fun updateBaudRate(baudRate: Int) {
        _uiState.value = _uiState.value.copy(baudRate = baudRate)
    }
}