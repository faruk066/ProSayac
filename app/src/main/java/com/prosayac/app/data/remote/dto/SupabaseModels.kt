package com.prosayac.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SiteDto(
    val id: String,
    val name: String,
    val address: String? = null
)

@Serializable
data class MeterDto(
    val id: String,
    val site_id: String,
    val meter_serial: String,
    val meter_type: String? = null,
    val apartment_number: String? = null
)

@Serializable
data class ReadingUploadDto(
    val id: String? = null, // İŞTE BU EKSİKTİ! Bunu mutlaka geri ekle.
    val meter_serial: String,
    val meter_type: String? = null,
    val reading_value: Double,
    val unit: String,
    val site_id: String,
    val read_by: String? = null,
    val read_at: String? = null
)