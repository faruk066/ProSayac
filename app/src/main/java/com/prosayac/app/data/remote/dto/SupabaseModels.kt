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
    val meter_type: String? = null
)
