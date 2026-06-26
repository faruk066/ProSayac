package com.prosayac.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.prosayac.app.data.local.entity.MeterEntity

@Entity(
    tableName = "readings",
    foreignKeys = [
        ForeignKey(
            entity = MeterEntity::class,
            parentColumns = ["id"],
            childColumns = ["meter_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("meter_id"), Index("reading_date")]
)
data class ReadingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "meter_id")
    val meterId: Long,

    @ColumnInfo(name = "reading_value")
    val readingValue: String,

    @ColumnInfo(name = "reading_date")
    val readingDate: Long,

    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,

    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "PENDING", // "PENDING", "SYNCED", "FAILED"

    @ColumnInfo(name = "site_id")
    val siteId: String? = null,

    @ColumnInfo(name = "reading_type")
    val readingType: String = "manual", // "manual", "m-bus", "import"

    @ColumnInfo(name = "notes")
    val notes: String? = null
)