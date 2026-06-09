package com.prosayac.app.domain.model

enum class MeterStatus(val displayName: String) {
    UNREAD("Okunamadı"),
    READ("Okundu"),
    SKIPPED("Atlandı");

    companion object {
        fun fromString(value: String): MeterStatus = when (value) {
            "Read" -> READ
            "Skipped" -> SKIPPED
            else -> UNREAD
        }
    }

    /** Value stored in the database (matches legacy string format). */
    val dbValue: String
        get() = when (this) {
            UNREAD -> "Unread"
            READ -> "Read"
            SKIPPED -> "Skipped"
        }
}
