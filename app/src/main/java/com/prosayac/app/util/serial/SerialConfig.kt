package com.prosayac.app.util.serial

import com.hoho.android.usbserial.driver.UsbSerialPort

/**
 * M-Bus standard serial configuration.
 *
 * Defaults match the M-Bus specification (EN 13757-2):
 *   Baud Rate : 2400
 *   Data Bits : 8
 *   Stop Bits : 1
 *   Parity    : EVEN
 */
data class SerialConfig(
    val baudRate: Int = 2400,
    val dataBits: Int = UsbSerialPort.DATABITS_8,
    val stopBits: Int = UsbSerialPort.STOPBITS_1,
    val parity: Int = UsbSerialPort.PARITY_EVEN
)