package com.prosayac.app.domain.model

data class DailyStats(
    val day: String,
    val count: Int
)

data class MonthlyStats(
    val month: String,
    val count: Int
)

data class TypeStats(
    val reading_type: String,
    val count: Int
)
