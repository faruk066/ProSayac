package com.prosayac.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prosayac.app.data.datastore.AppPreferences
import com.prosayac.app.data.datastore.UserPreferences
import com.prosayac.app.domain.repository.MeterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val meterRepository: MeterRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppPreferences())
    val uiState: StateFlow<AppPreferences> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferences.preferencesFlow.collect { prefs ->
                _uiState.value = prefs
            }
        }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            userPreferences.updateThemeMode(mode)
        }
    }

    fun updateAutoSync(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.updateAutoSync(enabled)
        }
    }

    fun updateOfflineMode(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.updateOfflineMode(enabled)
        }
    }

    fun updateBaudRate(baudRate: Int) {
        viewModelScope.launch {
            val current = _uiState.value
            userPreferences.updateSerialConfig(
                baudRate = baudRate,
                dataBits = current.dataBits,
                stopBits = current.stopBits,
                parity = current.parity
            )
        }
    }

    /**
     * Deletes all meters and readings from the database.
     * Used for "Tüm Verileri Sıfırla" dangerous action.
     */
    fun deleteAllData() {
        viewModelScope.launch {
            try {
                meterRepository.deleteAll()
                meterRepository.deleteAllReadings()
            } catch (e: Exception) {
                // Silent fail - data deletion is best-effort
            }
        }
    }
}