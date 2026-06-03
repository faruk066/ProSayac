package com.prosayac.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meters")
data class MeterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "serial_number")
    val serialNumber: String,

    @ColumnInfo(name = "flat_number")
    val flatNumber: String = "",

    @ColumnInfo(name = "meter_type")
    val meterType: String, // "Isı Sayacı", "Sıcak Su Sayacı"

    @ColumnInfo(name = "owner_name")
    val ownerName: String = "",

    @ColumnInfo(name = "address")
    val address: String = "",

    @ColumnInfo(name = "building_name")
    val buildingName: String = "",

    @ColumnInfo(name = "status")
    val status: String = "Unread", // "Unread", "Read", "Skipped"

    @ColumnInfo(name = "last_reading")
    val lastReading: String? = null,

    @ColumnInfo(name = "last_reading_date")
    val lastReadingDate: Long? = null,

    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)