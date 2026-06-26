package com.prosayac.app.domain.model

data class Reading(
    val id: Long = 0,
    val meterId: Long,
    val readingValue: String,
    val readingDate: Long,
    val isSynced: Boolean = false,
    val syncStatus: String = "PENDING",
    val siteId: String? = null,
    val readingType: String = "manual",
    val notes: String? = null
)