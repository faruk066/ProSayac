package com.prosayac.app.util.serial

data class SerialConfig(
    val baudRate: Int = 9600,
    val dataBits: Int = 8,
    val stopBits: Int = 1,
    val parity: Int = 0
)
