package com.prosayac.app.domain.model

sealed class PollOutcome {
    data class Success(val meterId: String, val value: Double, val unit: String) : PollOutcome()
    data class Timeout(val meterId: String?, val durationMs: Long) : PollOutcome()
    data class ProtocolError(val meterId: String?, val message: String) : PollOutcome()
    data class InvalidSerial(val serial: String, val reason: String) : PollOutcome()
    data class DeviceNotFound(val meterId: String?) : PollOutcome()
}
