package com.prosayac.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prosayac.app.data.local.dao.DailyStats
import com.prosayac.app.data.local.dao.MonthlyStats
import com.prosayac.app.data.local.dao.TypeStats
import com.prosayac.app.domain.model.DashboardStats
import com.prosayac.app.domain.repository.MeterRepository
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
    val readingProgress: Float = 0f,
    val syncProgress: Float = 0f,
    val isLoading: Boolean = true,
    val isChartError: Boolean = false,
    val chartErrorMessage: String? = null,

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
    private val meterRepository: MeterRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadAllData()
    }

    fun loadAllData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Launch reactive meter counts collection in its own coroutine
            // (combine collects forever since Room Flows are infinite)
            launch {
                combine(
                    meterRepository.getTotalCount(),
                    meterRepository.getReadCount(),
                    meterRepository.getUnreadCount(),
                    meterRepository.getUnsyncedCount(),
                    meterRepository.getTotalReadingCount()
                ) { total, read, unread, unsynced, readingsCount ->
                    val progress = if (total > 0) read.toFloat() / total else 0f
                    val syncP = if (total > 0) (total - unsynced).toFloat() / total else 0f

                    _uiState.update { currentState ->
                        currentState.copy(
                            totalMeters = total,
                            readMeters = read,
                            unreadMeters = unread,
                            unsyncedMeters = unsynced,
                            totalReadings = readingsCount,
                            readingProgress = progress,
                            syncProgress = syncP
                        )
                    }
                }.collect()
            }

            // Wait briefly for combined counts to emit initial values, then load chart data
            delay(100)
            
            // Load chart data (suspend functions)
            loadChartData()

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun loadChartData() {
        try {
            val calendar = Calendar.getInstance()
            val sevenDaysAgo = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -7)
            }

            // 1. Bar chart: Daily reading stats (last 7 days)
            val dailyStats = meterRepository.getDailyReadingStats(sevenDaysAgo.timeInMillis)
            val barLabels = if (dailyStats.isNotEmpty()) {
                dailyStats.map { it.day.takeLast(5) }
            } else {
                listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
            }
            val barValues = if (dailyStats.isNotEmpty()) {
                dailyStats.map { it.count.toFloat() }
            } else {
                listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f)
            }

            // 2. Donut chart: Reading type distribution (Sync status)
            // Actually show: Senkronize / Bekleyen distribution
            val currentState = _uiState.value
            val total = currentState.totalMeters
            val unsynced = currentState.unsyncedMeters
            val synced = total - unsynced

            _uiState.update {
                it.copy(
                    barChartLabels = barLabels,
                    barChartValues = barValues,
                    donutSegments = listOf(
                        DonutSegmentUi(
                            label = "Senkronize (${synced})",
                            value = synced.toFloat(),
                            color = 0xFF16A34A // Green
                        ),
                        DonutSegmentUi(
                            label = "Bekleyen (${unsynced})",
                            value = (unsynced.toFloat()).coerceAtLeast(0f),
                            color = 0xFFF97316 // Orange
                        )
                    )
                )
            }

            // 3. Line chart: Monthly reading trend
            val monthlyStats = meterRepository.getMonthlyReadingTrend()
            val lineLabels = if (monthlyStats.isNotEmpty()) {
                monthlyStats.map { "${it.month.takeLast(2)}. Ay" }
            } else {
                listOf("Oca", "Şub", "Mar", "Nis", "May", "Haz")
            }
            val lineValues = if (monthlyStats.isNotEmpty()) {
                monthlyStats.map { it.count.toFloat() }
            } else {
                listOf(0f, 0f, 0f, 0f, 0f, 0f)
            }

            _uiState.update {
                it.copy(
                    lineChartLabels = lineLabels,
                    lineChartValues = lineValues
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isChartError = true,
                    chartErrorMessage = e.message ?: "Grafik verileri yüklenemedi"
                )
            }
        }
    }
}