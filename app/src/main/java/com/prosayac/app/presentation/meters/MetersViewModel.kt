package com.prosayac.app.presentation.meters

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prosayac.app.domain.model.Meter
import com.prosayac.app.domain.repository.MeterRepository
import com.prosayac.app.util.excel.ExcelParser
import com.prosayac.app.util.excel.ExcelParseError
import com.prosayac.app.util.log.LoggerService
import com.prosayac.app.util.log.LogTag
import com.prosayac.app.util.serial.ConnectionState
import com.prosayac.app.util.serial.MBusProtocolHandler
import com.prosayac.app.util.serial.MBusSerialManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MetersUiState(
    val meters: List<Meter> = emptyList(),
    val filteredMeters: List<Meter> = emptyList(),
    val isLoading: Boolean = false,
    val isImporting: Boolean = false,
    val importProgress: String = "",
    val selectedTypeFilter: String = "All",
    val selectedStatusFilter: String = "All",
    val showImportResult: Boolean = false,
    val importResult: ImportResultState? = null,
    val showDropzone: Boolean = false,
    val showBuildingNameDialog: Boolean = false,
    val pendingImportUri: Uri? = null,
    val error: String? = null,
    val isReadingInProgress: Boolean = false,
    val readingProgressMessage: String = "",
    /** Per-meter reading status: meterId -> status ("polling", "success", "timeout", "error") */
    val meterReadStatuses: Map<Long, String> = emptyMap(),
    /** Current reading value being displayed per meter while polling */
    val meterReadingValues: Map<Long, String> = emptyMap()
)

data class ImportResultState(
    val successCount: Int,
    val errorCount: Int,
    val errors: List<ExcelParseError>
)

@HiltViewModel
class MetersViewModel @Inject constructor(
    private val meterRepository: MeterRepository,
    private val serialManager: MBusSerialManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(MetersUiState())
    val uiState: StateFlow<MetersUiState> = _uiState.asStateFlow()

    private val excelParser = ExcelParser()

    init {
        loadMeters()
    }

    fun loadMeters() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            meterRepository.getAllMeters()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Sayaçlar yüklenirken hata oluştu"
                    )
                }
                .collect { meters ->
                    val sortedMeters = meters.sortedWith(naturalDaireComparator)
                    _uiState.value = _uiState.value.copy(
                        meters = sortedMeters,
                        isLoading = false
                    )
                    applyFilters()
                }
        }
    }

    fun toggleDropzone() {
        _uiState.value = _uiState.value.copy(showDropzone = !_uiState.value.showDropzone)
    }

    /**
     * Step 1: User selects Excel file. We store the URI and show the Building Name dialog.
     */
    fun onFileSelected(uri: Uri) {
        _uiState.value = _uiState.value.copy(
            pendingImportUri = uri,
            showBuildingNameDialog = true,
            showDropzone = false
        )
    }

    /**
     * Step 2: User confirms the building name. We proceed with the import.
     */
    fun confirmBuildingName(buildingName: String) {
        val uri = _uiState.value.pendingImportUri ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                showBuildingNameDialog = false,
                isImporting = true,
                importProgress = "Excel dosyası işleniyor..."
            )

            try {
                val result = excelParser.parse(context, uri, buildingName)
                _uiState.value = _uiState.value.copy(importProgress = "Veriler kaydediliyor...")

                // Clear existing meters before import - a new Excel is a fresh start
                meterRepository.deleteAll()

                if (result.meters.isNotEmpty()) {
                    meterRepository.insertMeters(result.meters)
                }

                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    importProgress = "",
                    showImportResult = true,
                    pendingImportUri = null,
                    importResult = ImportResultState(
                        successCount = result.successCount,
                        errorCount = result.errors.size,
                        errors = result.errors
                    )
                )
                loadMeters()
            } catch (e: Exception) {
                LoggerService.log(LogTag.ERROR, "İçe aktarma hatası: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    importProgress = "",
                    pendingImportUri = null,
                    error = "İçe aktarma sırasında hata: ${e.message}"
                )
            }
        }
    }

    fun dismissBuildingNameDialog() {
        _uiState.value = _uiState.value.copy(
            showBuildingNameDialog = false,
            pendingImportUri = null
        )
    }

    fun dismissImportResult() {
        _uiState.value = _uiState.value.copy(showImportResult = false, importResult = null)
    }

    // =============================================================================
    // HARDWARE READING PIPELINE (REAL M-Bus POLLING)
    // =============================================================================

    /**
     * "Okumaya Başla" - Start hardware polling for all "Unread" meters.
     *
     * For each meter with status "Unread":
     *  1. Build and send SND_UD via MBusProtocolHandler
     *  2. Wait for Rsp_UD response (5 second timeout)
     *  3. Parse response and extract Endeks (reading value)
     *  4. Update meter status in DB and UI
     *  5. If timeout, mark as "Cihaz Yanıt Vermedi"
     */
    fun startReading() {
        viewModelScope.launch {
            LoggerService.log(LogTag.INFO, "======= HARDWARE OKUMA BAŞLATILDI =======")

            // Check connection state
            if (serialManager.connectionState.value != ConnectionState.CONNECTED) {
                LoggerService.log(LogTag.WARN, "M-Bus bağlı değil - okuma başlatılamadı")
                _uiState.value = _uiState.value.copy(
                    error = "M-Bus bağlantısı kurulu değil. Lütfen önce Bağlantı ekranından bağlanın.",
                    isReadingInProgress = false,
                    readingProgressMessage = ""
                )
                return@launch
            }

            val unreadMeters = _uiState.value.meters.filter { it.status == "Unread" }
            if (unreadMeters.isEmpty()) {
                LoggerService.log(LogTag.INFO, "Okunmamış sayaç yok - okuma atlandı")
                _uiState.value = _uiState.value.copy(
                    readingProgressMessage = "Tüm sayaçlar zaten okunmuş durumda",
                    isReadingInProgress = false
                )
                return@launch
            }

            LoggerService.log(LogTag.INFO, "Toplam ${unreadMeters.size} okunmamış sayaç bulundu")

            _uiState.value = _uiState.value.copy(
                isReadingInProgress = true,
                readingProgressMessage = "0 / ${unreadMeters.size} okundu",
                meterReadStatuses = emptyMap(),
                meterReadingValues = emptyMap()
            )

            var readCount = 0
            var timeoutCount = 0
            var errorCount = 0

            for ((index, meter) in unreadMeters.withIndex()) {
                if (!isActive) {
                    LoggerService.log(LogTag.WARN, "Okuma iptal edildi (scope inactive)")
                    break
                }

                // Update per-meter status to "polling"
                updateMeterStatus(meter.id, "polling")

                val progressMsg = "$index / ${unreadMeters.size} okundu"
                _uiState.value = _uiState.value.copy(
                    readingProgressMessage = progressMsg
                )

                LoggerService.log(
                    LogTag.HARDWARE,
                    "Sayaç [$index/${unreadMeters.size}] sorgulanıyor: ${meter.serialNumber}"
                )

                // Poll the meter via M-Bus protocol
                val result = MBusProtocolHandler.pollMeter(
                    serialNumber = meter.serialNumber,
                    serialManager = serialManager
                )

                when {
                    result.readingValue != null -> {
                        // SUCCESS: Real reading received
                        LoggerService.log(
                            LogTag.INFO,
                            "OKUNDU: ${meter.serialNumber} = ${result.readingValue}"
                        )
                        onMeterReadingReceived(meter.id, result.readingValue)
                        updateMeterStatus(meter.id, "success")
                        updateMeterReadingValue(meter.id, result.readingValue)
                        readCount++
                    }
                    result.errorMessage != null && result.errorMessage.contains("Cihaz Yanıt Vermedi", ignoreCase = true) -> {
                        // TIMEOUT
                        LoggerService.log(
                            LogTag.WARN,
                            "ZAMAN AŞIMI: ${meter.serialNumber} - 5 saniyede yanıt gelmedi"
                        )
                        updateMeterStatus(meter.id, "timeout")
                        timeoutCount++
                    }
                    else -> {
                        // OTHER ERROR
                        LoggerService.log(
                            LogTag.ERROR,
                            "HATA: ${meter.serialNumber} - ${result.errorMessage ?: "bilinmeyen"}"
                        )
                        updateMeterStatus(meter.id, "error")
                        errorCount++
                    }
                }

                // Inter-frame delay between successive meter polls
                if (index < unreadMeters.size - 1) {
                    delay(MBusProtocolHandler.INTER_FRAME_DELAY_MS)
                }
            }

            LoggerService.log(
                LogTag.INFO,
                "======= HARDWARE OKUMA TAMAMLANDI =======" +
                    " Okunan: $readCount, Zaman Aşımı: $timeoutCount, Hata: $errorCount"
            )

            _uiState.value = _uiState.value.copy(
                isReadingInProgress = false,
                readingProgressMessage = "Tamamlandı: $readCount okundu, $timeoutCount cevap vermedi"
            )
        }
    }

    /**
     * Cancels an ongoing reading session.
     */
    fun cancelReading() {
        LoggerService.log(LogTag.WARN, "Okuma kullanıcı tarafından iptal edildi")
        _uiState.value = _uiState.value.copy(
            isReadingInProgress = false,
            readingProgressMessage = "Okuma iptal edildi",
            meterReadStatuses = emptyMap(),
            meterReadingValues = emptyMap()
        )
    }

    /**
     * Called when real M-Bus reading data is received from the hardware gateway.
     * Updates the meter status to "Read" with the actual value.
     */
    private suspend fun onMeterReadingReceived(meterId: Long, readingValue: String) {
        try {
            meterRepository.updateMeterReading(
                id = meterId,
                status = "Read",
                reading = readingValue,
                readingDate = System.currentTimeMillis()
            )

            // Also insert a Reading record
            meterRepository.insertReading(
                com.prosayac.app.domain.model.Reading(
                    meterId = meterId,
                    readingValue = readingValue,
                    readingDate = System.currentTimeMillis(),
                    isSynced = false,
                    readingType = "m-bus"
                )
            )
        } catch (e: Exception) {
            LoggerService.log(LogTag.ERROR, "Okuma kaydedilirken hata (id=$meterId): ${e.message}")
            _uiState.value = _uiState.value.copy(
                error = "Okuma kaydedilirken hata oluştu"
            )
        }
    }

    /**
     * Update per-meter polling status in UI.
     */
    private fun updateMeterStatus(meterId: Long, status: String) {
        val current = _uiState.value.meterReadStatuses.toMutableMap()
        current[meterId] = status
        _uiState.value = _uiState.value.copy(meterReadStatuses = current)
    }

    /**
     * Update per-meter reading value displayed during polling.
     */
    private fun updateMeterReadingValue(meterId: Long, value: String) {
        val current = _uiState.value.meterReadingValues.toMutableMap()
        current[meterId] = value
        _uiState.value = _uiState.value.copy(meterReadingValues = current)
    }

    /**
     * Called when all meters have been read (or the reading session ends).
     */
    fun finishReading() {
        _uiState.value = _uiState.value.copy(
            isReadingInProgress = false,
            readingProgressMessage = "",
            meterReadStatuses = emptyMap(),
            meterReadingValues = emptyMap()
        )
    }

    fun setTypeFilter(type: String) {
        _uiState.value = _uiState.value.copy(selectedTypeFilter = type)
        applyFilters()
    }

    fun setStatusFilter(status: String) {
        _uiState.value = _uiState.value.copy(selectedStatusFilter = status)
        applyFilters()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private val naturalDaireComparator = Comparator<Meter> { a, b ->
        val flatA = a.flatNumber
        val flatB = b.flatNumber
        if (flatA.isBlank() && flatB.isBlank()) 0
        else if (flatA.isBlank()) 1
        else if (flatB.isBlank()) -1
        else naturalOrderCompare(flatA, flatB)
    }

    private fun naturalOrderCompare(a: String, b: String): Int {
        var i = 0
        var j = 0
        while (i < a.length && j < b.length) {
            val charA = a[i]
            val charB = b[j]
            if (charA.isDigit() && charB.isDigit()) {
                var numA = 0L
                while (i < a.length && a[i].isDigit()) {
                    numA = numA * 10 + (a[i] - '0')
                    i++
                }
                var numB = 0L
                while (j < b.length && b[j].isDigit()) {
                    numB = numB * 10 + (b[j] - '0')
                    j++
                }
                if (numA != numB) return numA.compareTo(numB)
            } else {
                if (charA != charB) return charA.compareTo(charB)
                i++
                j++
            }
        }
        return a.length.compareTo(b.length)
    }

    private fun applyFilters() {
        val currentState = _uiState.value
        val allMeters = currentState.meters
        val filtered = allMeters.filter { meter ->
            val typeMatch = currentState.selectedTypeFilter == "All" || meter.meterType == currentState.selectedTypeFilter
            val statusMatch = currentState.selectedStatusFilter == "All" || meter.status == currentState.selectedStatusFilter
            typeMatch && statusMatch
        }
        _uiState.value = currentState.copy(filteredMeters = filtered)
    }
}