package com.prosayac.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sites")
data class SiteEntity(
    @PrimaryKey
    val id: String,

    val name: String,

    @ColumnInfo(name = "address")
    val address: String? = null
)
