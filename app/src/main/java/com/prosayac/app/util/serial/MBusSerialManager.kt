package com.prosayac.app.util.serial

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import com.prosayac.app.util.log.LoggerService
import com.prosayac.app.util.log.LogTag
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class SerialConfig(
    val baudRate: Int = 9600,
    val dataBits: Int = 8,
    val stopBits: Int = 1,
    val parity: Int = 0
)

@Singleton
class MBusSerialManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val usbManager: UsbManager =
        context.getSystemService(Context.USB_SERVICE) as UsbManager

    private var serialPort: UsbSerialPort? = null
    private var ioManager: SerialInputOutputManager? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _receivedData = Channel<String>(Channel.BUFFERED)
    val receivedData = _receivedData

    private var permissionIntent: PendingIntent? = null

    /** Raw byte accumulator for hardware response polling */
    private val rawBuffer = mutableListOf<Byte>()

    /** Callback for suspend-based response waiting (meter polling) */
    private var rawResponseCallback: ((ByteArray) -> Unit)? = null

    init {
        setupPermissionIntent()
        registerUsbReceiver()
    }

    private fun setupPermissionIntent() {
        permissionIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(ACTION_USB_PERMISSION).apply {
                setPackage(context.packageName)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun registerUsbReceiver() {
        val filter = IntentFilter().apply {
            addAction(ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        }
        context.registerReceiver(usbReceiver, filter)
    }

    fun connect(config: SerialConfig = SerialConfig()) {
        try {
            _connectionState.value = ConnectionState.CONNECTING
            LoggerService.log(LogTag.HARDWARE, "M-Bus bağlantı başlatılıyor... (baud=${config.baudRate})")

            val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)

            if (availableDrivers.isEmpty()) {
                LoggerService.log(LogTag.WARN, "USB sürücü bulunamadı - cihaz takılı değil")
                _connectionState.value = ConnectionState.DISCONNECTED
                return
            }

            val driver = availableDrivers.first()
            val device = driver.device

            LoggerService.log(
                LogTag.HARDWARE,
                "USB cihaz algılandı: vid=${device.vendorId} pid=${device.productId} " +
                    "sürücü=${driver.javaClass.simpleName}"
            )

            if (!usbManager.hasPermission(device)) {
                LoggerService.log(LogTag.HARDWARE, "USB izni isteniyor...")
                usbManager.requestPermission(device, permissionIntent)
                return
            }

            val connection = usbManager.openDevice(device)
            if (connection == null) {
                LoggerService.log(LogTag.ERROR, "USB aygıt bağlantısı açılamadı")
                _connectionState.value = ConnectionState.ERROR
                return
            }

            val port = driver.ports.firstOrNull()
            if (port == null) {
                LoggerService.log(LogTag.ERROR, "Seri port bulunamadı")
                _connectionState.value = ConnectionState.ERROR
                return
            }

            port.open(connection)
            port.setParameters(
                config.baudRate,
                config.dataBits,
                config.stopBits,
                config.parity
            )

            serialPort = port
            _connectionState.value = ConnectionState.CONNECTED

            LoggerService.log(LogTag.INFO, "M-Bus bağlantısı kuruldu (${config.baudRate} bps)")

            startIoManager(port)
        } catch (e: IOException) {
            LoggerService.log(LogTag.ERROR, "Bağlantı IO hatası: ${e.message}")
            _connectionState.value = ConnectionState.ERROR
        } catch (e: SecurityException) {
            LoggerService.log(LogTag.ERROR, "Bağlantı güvenlik hatası: ${e.message}")
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    fun onPermissionGranted(device: UsbDevice, granted: Boolean) {
        if (granted) {
            LoggerService.log(LogTag.INFO, "USB izni verildi, bağlantı yeniden deneniyor")
            connect()
        } else {
            LoggerService.log(LogTag.WARN, "USB izni reddedildi")
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    fun disconnect() {
        try {
            LoggerService.log(LogTag.INFO, "M-Bus bağlantısı kesiliyor...")
            ioManager?.stop()
            ioManager = null
            serialPort?.close()
            serialPort = null
        } catch (e: Exception) {
            LoggerService.log(LogTag.WARN, "Bağlantı kesme hatası: ${e.message}")
        } finally {
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    fun send(bytes: ByteArray) {
        try {
            val hex = bytes.joinToString(" ") { "%02X".format(it) }
            LoggerService.log(LogTag.HARDWARE, "GÖNDER → $hex (${bytes.size} byte)")
            ioManager?.writeAsync(bytes)
        } catch (e: IOException) {
            LoggerService.log(LogTag.ERROR, "Veri gönderme hatası: ${e.message}")
            _connectionState.value = ConnectionState.ERROR
        }
    }

    /**
     * Suspends until raw bytes are received from the serial port.
     * Used by the meter polling pipeline to wait for Rsp_UD frames.
     *
     * @return The raw ByteArray received from M-Bus hardware
     */
    suspend fun waitForRawResponse(): ByteArray = suspendCancellableCoroutine { cont ->
        synchronized(rawBuffer) {
            // If there's already data in the buffer, return it immediately
            if (rawBuffer.isNotEmpty()) {
                val data = rawBuffer.toByteArray()
                rawBuffer.clear()
                rawResponseCallback = null
                cont.resume(data)
                return@suspendCancellableCoroutine
            }

            // Register callback - will be invoked from onNewData
            rawResponseCallback = { data ->
                cont.resume(data)
            }
        }
    }

    fun listDevices(): List<UsbDevice> {
        return try {
            usbManager.deviceList.values.toList()
        } catch (e: Exception) {
            LoggerService.log(LogTag.ERROR, "Cihaz listeleme hatası: ${e.message}")
            emptyList()
        }
    }

    fun isDevicePhysicallyConnected(): Boolean {
        return try {
            val drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
            drivers.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    fun refreshConnectionState() {
        try {
            if (serialPort != null && _connectionState.value == ConnectionState.CONNECTED) {
                try {
                    if (serialPort!!.isOpen) return
                } catch (e: Exception) {
                    LoggerService.log(LogTag.WARN, "Bağlantı kaybı algılandı")
                }
                _connectionState.value = ConnectionState.DISCONNECTED
            }

            if (!isDevicePhysicallyConnected()) {
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    private fun startIoManager(port: UsbSerialPort) {
        ioManager = SerialInputOutputManager(port, object : SerialInputOutputManager.Listener {
            override fun onNewData(data: ByteArray) {
                // Log raw hex data for diagnostics
                val hex = data.joinToString(" ") { "%02X".format(it) }
                LoggerService.log(LogTag.HARDWARE, "ALINAN ← $hex (${data.size} byte)")

                // Forward to Channel (legacy path)
                _receivedData.trySend(String(data, Charsets.UTF_8))

                // Forward to raw buffer for polling path
                synchronized(rawBuffer) {
                    val callback = rawResponseCallback
                    if (callback != null) {
                        rawResponseCallback = null
                        callback(data)
                    } else {
                        // Accumulate for next poll request
                        rawBuffer.addAll(data.toList())
                    }
                }
            }

            override fun onRunError(e: Exception) {
                LoggerService.log(LogTag.ERROR, "IO yönetici hatası: ${e.message}")
                _connectionState.value = ConnectionState.ERROR
            }
        })
        try {
            ioManager?.start()
        } catch (e: Exception) {
            LoggerService.log(LogTag.ERROR, "IO yönetici başlatma hatası: ${e.message}")
            _connectionState.value = ConnectionState.ERROR
        }
    }

    fun cleanup() {
        try {
            ioManager?.stop()
            ioManager = null
            serialPort?.close()
            serialPort = null
            _connectionState.value = ConnectionState.DISCONNECTED
            context.unregisterReceiver(usbReceiver)
            LoggerService.log(LogTag.INFO, "M-Bus yöneticisi temizlendi")
        } catch (e: Exception) {
            // Already unregistered or cleaned up
        }
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return

            when (intent.action) {
                ACTION_USB_PERMISSION -> {
                    synchronized(this) {
                        val device: UsbDevice? =
                            intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        val granted = intent.getBooleanExtra(
                            UsbManager.EXTRA_PERMISSION_GRANTED, false
                        )
                        if (device != null) {
                            onPermissionGranted(device, granted)
                        }
                    }
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device: UsbDevice? =
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    if (device != null && serialPort?.driver?.device?.deviceId == device.deviceId) {
                        LoggerService.log(LogTag.WARN, "USB cihaz çıkarıldı")
                        disconnect()
                    }
                    refreshConnectionState()
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    LoggerService.log(LogTag.HARDWARE, "USB cihaz takıldı")
                    refreshConnectionState()
                }
            }
        }
    }

    companion object {
        private const val ACTION_USB_PERMISSION = "com.prosayac.app.USB_PERMISSION"
    }
}