package com.prosayac.app.domain.model

data class ReadingWithMeter(
    val id: Long,
    val meterId: Long,
    val serialNumber: String,
    val flatNumber: String,
    val meterType: String,
    val buildingName: String,
    val readingValue: String,
    val readingDate: Long,
    val isSynced: Boolean,
    val syncStatus: String = "PENDING",
    val readingType: String,
    val notes: String?,
    val meterStatus: String
)
