package com.prosayac.app.domain.model

/**
 * Domain models for Dashboard chart data and statistics.
 */
data class DashboardStats(
    val totalMeters: Int = 0,
    val readMeters: Int = 0,
    val unreadMeters: Int = 0,
    val unsyncedMeters: Int = 0,
    val totalReadings: Int = 0,
    val readingProgress: Float = 0f,
    val syncProgress: Float = 0f
)

data class BarChartData(
    val labels: List<String>,
    val values: List<Float>
)

data class DonutChartData(
    val segments: List<DonutSegment>
)

data class DonutSegment(
    val label: String,
    val value: Float,
    val color: Long // ARGB color as Long
)

data class LineChartData(
    val labels: List<String>,
    val values: List<Float>
)