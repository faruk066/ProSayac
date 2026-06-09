package com.prosayac.app.presentation.meters

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prosayac.app.domain.model.Meter
import com.prosayac.app.domain.model.MeterStatus
import com.prosayac.app.domain.model.PollOutcome
import com.prosayac.app.domain.model.Reading
import com.prosayac.app.domain.repository.MeterRepository
import com.prosayac.app.util.excel.ExcelFormat
import com.prosayac.app.util.excel.ExcelParseError
import com.prosayac.app.util.excel.ExcelParser
import com.prosayac.app.util.log.LoggerService
import com.prosayac.app.util.log.LogTag
import com.prosayac.app.util.excel.METER_TYPE_HEAT
import com.prosayac.app.util.excel.METER_TYPE_WATER
import com.prosayac.app.util.serial.ConnectionState
import com.prosayac.app.util.serial.MBusProtocolHandler
import com.prosayac.app.util.serial.MBusSerialManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.currentCoroutineContext
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
    val meterReadStatuses: Map<Long, String> = emptyMap(),
    val meterReadingValues: Map<Long, String> = emptyMap(),
    val pendingFormatChoiceUri: Uri? = null,
    val pendingFormatOptions: List<ExcelFormat> = emptyList()
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
    private val excelParser: ExcelParser
) : ViewModel() {

    private val _uiState = MutableStateFlow(MetersUiState())
    val uiState: StateFlow<MetersUiState> = _uiState.asStateFlow()

    private var readingJob: Job? = null
    private var metersJob: Job? = null

    init {
        loadMeters()
    }

    fun loadMeters() {
        metersJob?.cancel()
        metersJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            meterRepository.getAllMeters()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Sayaçlar yüklenirken hata oluştu"
                    )
                }
                .collect { meters ->
                    val sortedMeters = meters.sortedWith(naturalComparator)
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

    fun onFileSelected(uri: Uri) {
        _uiState.value = _uiState.value.copy(
            pendingImportUri = uri,
            showBuildingNameDialog = true,
            showDropzone = false
        )
    }

    fun confirmBuildingName(buildingName: String) {
        val uri = _uiState.value.pendingImportUri ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                showBuildingNameDialog = false,
                isImporting = true,
                importProgress = "Excel dosyası işleniyor..."
            )

            try {
                val result = withContext(Dispatchers.IO) {
                    excelParser.parse(uri, buildingName)
                }

                if (result.ambiguousFormats.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        pendingFormatChoiceUri = uri,
                        pendingFormatOptions = result.ambiguousFormats,
                        importProgress = ""
                    )
                    return@launch
                }

                _uiState.value = _uiState.value.copy(importProgress = "Veriler kaydediliyor...")

                LoggerService.log(LogTag.INFO, "Atomik içe aktarma başlatılıyor (${result.meters.size} sayaç)")
                withContext(Dispatchers.IO) {
                    meterRepository.importMetersAtomic(result.meters)
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
                LoggerService.log(LogTag.ERROR, "İçe aktarma CRASH: ${e.stackTraceToString()}")
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    importProgress = "",
                    pendingImportUri = null,
                    error = "İçe aktarma sırasında hata: ${e.message ?: "bilinmeyen hata"}"
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

    fun onFormatPicked(format: ExcelFormat) {
        val uri = _uiState.value.pendingFormatChoiceUri ?: return
        _uiState.value = _uiState.value.copy(
            pendingFormatChoiceUri = null,
            pendingFormatOptions = emptyList()
        )
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                showBuildingNameDialog = true,
                pendingImportUri = uri,
                showDropzone = false
            )
        }
    }

    fun dismissFormatChoice() {
        _uiState.value = _uiState.value.copy(
            pendingFormatChoiceUri = null,
            pendingFormatOptions = emptyList()
        )
    }

    val connectionState: StateFlow<ConnectionState> = serialManager.connectionState

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    fun connectToMBus() {
        LoggerService.log(LogTag.HARDWARE, "M-Bus bağlantısı Meters ekranından başlatıldı")
        serialManager.connect()
    }

    fun disconnectFromMBus() {
        LoggerService.log(LogTag.HARDWARE, "M-Bus bağlantısı Meters ekranından kesiliyor")
        serialManager.disconnect()
    }

    fun togglePause() {
        _isPaused.value = !_isPaused.value
        val msg = if (_isPaused.value) "duraklatıldı" else "devam ediyor"
        LoggerService.log(LogTag.INFO, "Okuma $msg")
    }

    fun startReading() {
        readingJob?.cancel()
        readingJob = viewModelScope.launch {
            LoggerService.log(LogTag.INFO, "======= HARDWARE OKUMA BAŞLATILDI =======")

            if (serialManager.connectionState.value != ConnectionState.CONNECTED) {
                LoggerService.log(LogTag.WARN, "M-Bus bağlı değil - okuma başlatılamadı")
                _uiState.value = _uiState.value.copy(
                    error = "M-Bus bağlantısı kurulu değil. Lütfen önce Bağlantı ekranından bağlanın.",
                    isReadingInProgress = false,
                    readingProgressMessage = ""
                )
                return@launch
            }

            val unreadMeters = _uiState.value.meters.filter { it.status == MeterStatus.UNREAD }
            if (unreadMeters.isEmpty()) {
                LoggerService.log(LogTag.INFO, "Okunmamış sayaç yok - okuma atlandı")
                _uiState.value = _uiState.value.copy(
                    readingProgressMessage = "Tüm sayaçlar zaten okunmuş durumda",
                    isReadingInProgress = false
                )
                return@launch
            }

            runReadingLoop(unreadMeters, "Okunmamış sayaç yok, okuma atlandı")
        }
    }

    /**
     * Poll a single meter by serial number. Shows result on the card immediately.
     * Only available when no bulk reading is in progress.
     */
    fun pollSingleMeter(meter: Meter) {
        if (_uiState.value.isReadingInProgress) {
            LoggerService.log(LogTag.WARN, "Okuma devam ederken tekli sorgu başlatılamaz")
            return
        }
        if (serialManager.connectionState.value != ConnectionState.CONNECTED) {
            _uiState.value = _uiState.value.copy(
                error = "M-Bus bağlantısı kurulu değil."
            )
            return
        }
        viewModelScope.launch {
            LoggerService.log(LogTag.INFO, "======= TEKLİ OKUMA ======= ${meter.serialNumber}")
            pollSingleMeterInternal(meter, 1, 1)
            LoggerService.log(LogTag.INFO, "======= TEKLİ OKUMA TAMAM ======= ${meter.serialNumber}")
        }
    }

    /**
     * Re-poll all meters with UNREAD status. Shows the same progress UX as
     * startReading() but pre-filtered to only unread/failed meters.
     */
    fun pollFailedMeters() {
        val failed = _uiState.value.meters.filter { it.status == MeterStatus.UNREAD }
        if (failed.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                error = "Hatalı veya okunmamış sayaç bulunamadı"
            )
            return
        }
        readingJob?.cancel()
        readingJob = viewModelScope.launch {
            if (serialManager.connectionState.value != ConnectionState.CONNECTED) {
                _uiState.value = _uiState.value.copy(
                    error = "M-Bus bağlantısı kurulu değil. Lütfen önce bağlanın."
                )
                return@launch
            }
            LoggerService.log(LogTag.INFO, "======= YENİDEN OKUMA BAŞLATILDI (${failed.size} sayaç) =======")
            runReadingLoop(failed, "Yeniden okunacak sayaç yok")
        }
    }

    /**
     * Core reading loop shared by startReading() and pollFailedMeters().
     * Iterates [metersToList], checks pause/cancel, calls [pollSingleMeterInternal]
     * for each, and reports completion.
     */
    private suspend fun runReadingLoop(metersToList: List<Meter>, emptyMsg: String) {
        if (metersToList.isEmpty()) {
            LoggerService.log(LogTag.INFO, emptyMsg)
            _uiState.value = _uiState.value.copy(
                readingProgressMessage = emptyMsg,
                isReadingInProgress = false
            )
            return
        }

        LoggerService.log(LogTag.INFO, "Toplam ${metersToList.size} sayaç okunacak")

        _uiState.value = _uiState.value.copy(
            isReadingInProgress = true,
            readingProgressMessage = "0 / ${metersToList.size} okundu",
            meterReadStatuses = emptyMap(),
            meterReadingValues = emptyMap()
        )

        var readCount = 0
        var timeoutCount = 0
        var errorCount = 0

        for ((index, meter) in metersToList.withIndex()) {
            if (!currentCoroutineContext().isActive) {
                LoggerService.log(LogTag.WARN, "Okuma iptal edildi (scope inactive)")
                break
            }

            // Pause check: spin while paused, resume seamlessly
            while (_isPaused.value) {
                delay(500)
                if (!currentCoroutineContext().isActive) break
            }
            if (!currentCoroutineContext().isActive) break

            when (pollSingleMeterInternal(meter, index, metersToList.size)) {
                "success" -> readCount++
                "timeout" -> timeoutCount++
                else -> errorCount++
            }

            if (index < metersToList.size - 1) {
                delay(MBusProtocolHandler.INTER_FRAME_DELAY_MS)
            }
        }

        _isPaused.value = false

        LoggerService.log(
            LogTag.INFO,
            "======= OKUMA TAMAMLANDI =======" +
                " Okunan: $readCount, Zaman Aşımı: $timeoutCount, Hata: $errorCount"
        )

        _uiState.value = _uiState.value.copy(
            isReadingInProgress = false,
            readingProgressMessage = "Tamamlandı: $readCount okundu, $timeoutCount cevap vermedi"
        )
    }

    /**
     * Poll a single meter, update UI state with live status on the card.
     * Shared by the bulk loop, pollSingleMeter(), and pollFailedMeters().
     * Returns "success", "timeout", or "error".
     */
    private suspend fun pollSingleMeterInternal(meter: Meter, index: Int, total: Int): String {
        updateMeterStatus(meter.id, "polling")

        val progressMsg = "$index / $total okundu"
        _uiState.value = _uiState.value.copy(readingProgressMessage = progressMsg)

        LoggerService.log(
            LogTag.HARDWARE,
            "Sayaç [$index/$total] sorgulanıyor: ${meter.serialNumber}"
        )

        val outcome = MBusProtocolHandler.pollMeter(
            serialNumber = meter.serialNumber,
            serialManager = serialManager
        )

        return when (outcome) {
            is PollOutcome.Success -> {
                val matchedMeter = _uiState.value.meters.firstOrNull { it.serialNumber == outcome.meterId }
                val targetMeterId = matchedMeter?.id ?: meter.id
                val displaySerial = matchedMeter?.serialNumber ?: meter.serialNumber

                val meterForType = matchedMeter ?: meter
                val selectedValue = if (meterForType.meterType.contains("Su")) {
                    String.format("%.3f", outcome.volume)
                } else {
                    String.format("%.3f", outcome.energy)
                }
                val selectedUnit = if (meterForType.meterType.contains("Su")) "m³" else "kWh"

                LoggerService.log(
                    LogTag.INFO,
                    "OKUNDU: $displaySerial = $selectedValue $selectedUnit (type=${meterForType.meterType})"
                )
                onMeterReadingReceived(targetMeterId, selectedValue)
                updateMeterStatus(targetMeterId, "success")
                updateMeterReadingValue(targetMeterId, selectedValue)
                "success"
            }
            is PollOutcome.Timeout -> {
                LoggerService.log(
                    LogTag.WARN,
                    "ZAMAN AŞIMI: ${outcome.meterId ?: meter.serialNumber} - ${outcome.durationMs}ms içinde yanıt gelmedi"
                )
                updateMeterStatus(meter.id, "timeout")
                "timeout"
            }
            is PollOutcome.ProtocolError -> {
                LoggerService.log(
                    LogTag.ERROR,
                    "PROTOKOL HATASI: ${outcome.meterId ?: meter.serialNumber} - ${outcome.message}"
                )
                updateMeterStatus(meter.id, "error")
                "error"
            }
            is PollOutcome.InvalidSerial -> {
                LoggerService.log(
                    LogTag.ERROR,
                    "GEÇERSİZ SERİ NO: ${outcome.serial} - ${outcome.reason}"
                )
                updateMeterStatus(meter.id, "error")
                "error"
            }
            is PollOutcome.DeviceNotFound -> {
                LoggerService.log(
                    LogTag.ERROR,
                    "CİHAZ BULUNAMADI: ${meter.serialNumber}"
                )
                updateMeterStatus(meter.id, "error")
                "error"
            }
        }
    }

    fun cancelReading() {
        LoggerService.log(LogTag.WARN, "Okuma kullanıcı tarafından iptal edildi")
        _isPaused.value = false
        readingJob?.cancel()
        readingJob = null
        _uiState.value = _uiState.value.copy(
            isReadingInProgress = false,
            readingProgressMessage = "İptal Edildi",
            meterReadStatuses = emptyMap(),
            meterReadingValues = emptyMap()
        )
    }

    private suspend fun onMeterReadingReceived(meterId: Long, readingValue: String) {
        try {
            meterRepository.updateMeterReading(
                id = meterId,
                status = MeterStatus.READ.dbValue,
                reading = readingValue,
                readingDate = System.currentTimeMillis()
            )

            meterRepository.insertReading(
                Reading(
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

    private fun updateMeterStatus(meterId: Long, status: String) {
        val current = _uiState.value.meterReadStatuses.toMutableMap()
        current[meterId] = status
        _uiState.value = _uiState.value.copy(meterReadStatuses = current)
    }

    private fun updateMeterReadingValue(meterId: Long, value: String) {
        val current = _uiState.value.meterReadingValues.toMutableMap()
        current[meterId] = value
        _uiState.value = _uiState.value.copy(meterReadingValues = current)
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

    private val naturalComparator = Comparator<Meter> { a, b ->
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
            val statusMatch = currentState.selectedStatusFilter == "All" || meter.status.dbValue == currentState.selectedStatusFilter
            typeMatch && statusMatch
        }
        _uiState.value = currentState.copy(filteredMeters = filtered)
    }
}