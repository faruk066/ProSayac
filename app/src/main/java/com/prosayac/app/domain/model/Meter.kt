package com.prosayac.app.domain.model

data class Meter(
    val id: Long = 0,
    val serialNumber: String,
    val flatNumber: String = "",
    val meterType: String = "Sıcak Su Sayacı",
    val ownerName: String = "",
    val address: String = "",
    val buildingName: String = "",
    val status: MeterStatus = MeterStatus.UNREAD,
    val lastReading: String? = null,
    val lastReadingDate: Long? = null,
    val isSynced: Boolean = false,
    val createdAt: Long = 0L
) {
    val displaySerialNumber: String
        get() = serialNumber.ifBlank { "-" }

    val displayFlatNumber: String
        get() = flatNumber.ifBlank { "-" }

    val displayOwnerName: String
        get() = ownerName.ifBlank { "-" }

    val displayBuildingName: String
        get() = buildingName.ifBlank { "-" }

    val displayStatus: String
        get() = status.displayName
}