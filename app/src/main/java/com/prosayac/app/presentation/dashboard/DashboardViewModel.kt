package com.prosayac.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prosayac.app.domain.model.DailyStats
import com.prosayac.app.domain.model.MonthlyStats
import com.prosayac.app.domain.model.TypeStats
import com.prosayac.app.domain.repository.AuthRepository
import com.prosayac.app.domain.repository.MeterRepository
import com.prosayac.app.domain.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.Calendar

data class DashboardUiState(
    val totalMeters: Int = 0,
    val readMeters: Int = 0,
    val unreadMeters: Int = 0,
    val unsyncedMeters: Int = 0,
    val totalReadings: Int = 0,
    val pendingReadings: Int = 0,
    val syncedReadings: Int = 0,
    val readingProgress: Float = 0f,
    val syncProgress: Float = 0f,
    val isLoading: Boolean = true,
    val isChartError: Boolean = false,
    val chartErrorMessage: String? = null,
    val userEmail: String? = null,

    // Chart data
    val barChartLabels: List<String> = emptyList(),
    val barChartValues: List<Float> = emptyList(),
    val donutSegments: List<DonutSegmentUi> = emptyList(),
    val lineChartLabels: List<String> = emptyList(),
    val lineChartValues: List<Float> = emptyList()
)

data class DonutSegmentUi(
    val label: String,
    val value: Float,
    val color: Long
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val meterRepository: MeterRepository,
    private val syncRepository: SyncRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    // Reactive reading sync counts — Room Flow → StateFlow via stateIn.
    // The UI recomposes instantly when sync_status changes, no polling needed.
    private val pendingReadingCount: StateFlow<Int> = meterRepository.getPendingReadingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val syncedReadingCount: StateFlow<Int> = meterRepository.getSyncedReadingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        // Populate active user email (non-suspend — reads session directly)
        _uiState.value = _uiState.value.copy(userEmail = authRepository.getCurrentUserEmail())

        // Launch sync and chart loading sequentially: chart data waits for sync to finish
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Step 1: Fetch fresh data from Supabase
            syncRepository.fetchAndSaveAssignments()

            // Step 2: Now that Room DB is populated, load chart data
            loadChartData()

            _uiState.update { it.copy(isLoading = false) }
        }

        // Permanent collector for reactive meter counts — updates instantly when Room DB changes
        viewModelScope.launch {
            combine(
                meterRepository.getTotalCount(),
                meterRepository.getReadCount(),
                meterRepository.getUnreadCount(),
                meterRepository.getUnsyncedCount(),
                meterRepository.getTotalReadingCount(),
                pendingReadingCount,
                syncedReadingCount
            ) { args ->
                val total        = args[0] as Int
                val read         = args[1] as Int
                val unread       = args[2] as Int
                val unsynced     = args[3] as Int
                val readingsCount = args[4] as Int
                val pending      = args[5] as Int
                val synced       = args[6] as Int

                val progress = if (total > 0) read.toFloat() / total else 0f
                val syncP = if (total > 0) (total - unsynced).toFloat() / total else 0f

                _uiState.update { currentState ->
                    currentState.copy(
                        totalMeters = total,
                        readMeters = read,
                        unreadMeters = unread,
                        unsyncedMeters = unsynced,
                        totalReadings = readingsCount,
                        pendingReadings = pending,
                        syncedReadings = synced,
                        readingProgress = progress,
                        syncProgress = syncP
                    )
                }
            }.collect()
        }
    }

    /**
     * Called when the screen resumes — re-fetches the latest Meter count once
     * on the UI thread (the reactive Flow handles subsequent updates).
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            loadChartData()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /**
     * Manual sync button: fetch fresh data from Supabase and force UI update.
     */
    fun syncNow() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            syncRepository.fetchAndSaveAssignments()
            loadChartData()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun loadChartData() {
        // Default fallbacks — always set these so UI never receives empty lists
        val fallbackBarLabels = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
        val fallbackBarValues = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f)
        val fallbackLineLabels = listOf("Oca", "Şub", "Mar", "Nis", "May", "Haz")
        val fallbackLineValues = listOf(0f, 0f, 0f, 0f, 0f, 0f)

        try {
            val calendar = Calendar.getInstance()
            val sevenDaysAgo = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -7)
            }

            // 1. Bar chart: Daily reading stats (last 7 days)
            val dailyStats = meterRepository.getDailyReadingStats(sevenDaysAgo.timeInMillis)
            val barLabels = dailyStats.map { it.day.takeLast(5) }.ifEmpty { fallbackBarLabels }
            val barValues = dailyStats.map { it.count.toFloat() }.ifEmpty { fallbackBarValues }

            // 2. Donut chart: Sync status distribution
            // Read state snapshot safely — counts may be 0 on first emission
            val currentState = _uiState.value
            val total = currentState.totalMeters.coerceAtLeast(0)
            val unsynced = currentState.unsyncedMeters.coerceAtLeast(0)
            val synced = (total - unsynced).coerceAtLeast(0)

            val donutSegments = listOf(
                DonutSegmentUi(
                    label = "Senkronize (${synced})",
                    value = synced.toFloat().coerceAtLeast(0f),
                    color = 0xFF16A34A
                ),
                DonutSegmentUi(
                    label = "Bekleyen (${unsynced})",
                    value = unsynced.toFloat().coerceAtLeast(0f),
                    color = 0xFFF97316
                )
            )

            // 3. Line chart: Monthly reading trend
            val monthlyStats = meterRepository.getMonthlyReadingTrend()
            val lineLabels = monthlyStats.map { "${it.month.takeLast(2)}. Ay" }.ifEmpty { fallbackLineLabels }
            val lineValues = monthlyStats.map { it.count.toFloat() }.ifEmpty { fallbackLineValues }

            _uiState.update {
                it.copy(
                    isChartError = false,
                    chartErrorMessage = null,
                    barChartLabels = barLabels,
                    barChartValues = barValues,
                    donutSegments = donutSegments,
                    lineChartLabels = lineLabels,
                    lineChartValues = lineValues
                )
            }
        } catch (e: Exception) {
            // On error, set safe fallback values instead of leaving arrays empty
            val currentState = _uiState.value
            val total = currentState.totalMeters.coerceAtLeast(0)
            val unsynced = currentState.unsyncedMeters.coerceAtLeast(0)
            _uiState.update {
                it.copy(
                    isChartError = true,
                    chartErrorMessage = e.message ?: "Grafik verileri yüklenemedi",
                    barChartLabels = fallbackBarLabels,
                    barChartValues = fallbackBarValues,
                    donutSegments = listOf(
                        DonutSegmentUi("Senkronize (${total - unsynced})", (total - unsynced).toFloat().coerceAtLeast(0f), 0xFF16A34A),
                        DonutSegmentUi("Bekleyen (${unsynced})", unsynced.toFloat().coerceAtLeast(0f), 0xFFF97316)
                    ),
                    lineChartLabels = fallbackLineLabels,
                    lineChartValues = fallbackLineValues
                )
            }
        }
    }
}