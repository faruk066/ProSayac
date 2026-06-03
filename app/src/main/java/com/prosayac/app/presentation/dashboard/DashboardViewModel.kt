package com.prosayac.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prosayac.app.data.local.dao.DailyStats
import com.prosayac.app.data.local.dao.MonthlyStats
import com.prosayac.app.data.local.dao.TypeStats
import com.prosayac.app.domain.model.DashboardStats
import com.prosayac.app.domain.repository.MeterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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

                    _uiState.value = _uiState.value.copy(
                        totalMeters = total,
                        readMeters = read,
                        unreadMeters = unread,
                        unsyncedMeters = unsynced,
                        totalReadings = readingsCount,
                        readingProgress = progress,
                        syncProgress = syncP
                    )
                }.catch { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }.collect()
            }

            // Load chart data (suspend functions)
            loadChartData()

            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    private suspend fun loadChartData() {
        try {
            val cal = Calendar.getInstance()
            val sevenDaysAgo = cal.apply { add(Calendar.DAY_OF_YEAR, -7) }.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 7) // Reset to today

            // 1. Bar chart: Daily reading stats (last 7 days)
            val dailyStats = meterRepository.getDailyReadingStats(sevenDaysAgo)
            if (dailyStats.isNotEmpty()) {
                val labels = mutableListOf<String>()
                val values = mutableListOf<Float>()
                for (stat in dailyStats) {
                    val shortLabel = stat.day.takeLast(5) // "MM-DD"
                    labels.add(shortLabel)
                    values.add(stat.count.toFloat())
                }
                _uiState.value = _uiState.value.copy(
                    barChartLabels = labels,
                    barChartValues = values
                )
            } else {
                // Mock data for empty state
                _uiState.value = _uiState.value.copy(
                    barChartLabels = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz"),
                    barChartValues = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f)
                )
            }

            // 2. Donut chart: Reading type distribution (Sync status)
            // Actually show: Senkronize / Bekleyen distribution
            val total = _uiState.value.totalMeters
            val unsynced = _uiState.value.unsyncedMeters
            val synced = total - unsynced

            _uiState.value = _uiState.value.copy(
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

            // 3. Line chart: Monthly reading trend
            val monthlyStats = meterRepository.getMonthlyReadingTrend()
            if (monthlyStats.isNotEmpty()) {
                val labels = mutableListOf<String>()
                val values = mutableListOf<Float>()
                for (stat in monthlyStats) {
                    val shortLabel = stat.month.takeLast(2) + ". Ay" // "01. Ay"
                    labels.add(shortLabel)
                    values.add(stat.count.toFloat())
                }
                _uiState.value = _uiState.value.copy(
                    lineChartLabels = labels,
                    lineChartValues = values
                )
            } else {
                val cal2 = Calendar.getInstance()
                val months = listOf("Oca", "Şub", "Mar", "Nis", "May", "Haz")
                _uiState.value = _uiState.value.copy(
                    lineChartLabels = months,
                    lineChartValues = listOf(0f, 0f, 0f, 0f, 0f, 0f)
                )
            }
        } catch (e: Exception) {
            // Silently fall back to empty charts
            _uiState.value = _uiState.value.copy(
                barChartLabels = listOf("Veri Yok"),
                barChartValues = listOf(0f),
                lineChartLabels = listOf("Veri Yok"),
                lineChartValues = listOf(0f)
            )
        }
    }
}