package com.prosayac.app.util.serial

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.core.content.ContextCompat
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import com.prosayac.app.util.log.LoggerService
import com.prosayac.app.util.log.LogTag
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

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

    private val _receivedData = Channel<ByteArray>(Channel.BUFFERED)
    val receivedData = _receivedData

    private var permissionIntent: PendingIntent? = null
    internal var cleanupCalled = false
    private var receiverRegistered = false

    // ── Echo Cancellation (1:1 port from MBusService.dart) ──
    private val lastSentBytes = mutableListOf<Byte>()
    private val dataBuffer = mutableListOf<Byte>()

    // Raw response callback used by waitForRawResponse()
    private val rawBuffer = mutableListOf<Byte>()
    private var rawResponseCallback: ((ByteArray) -> Unit)? = null

    // E5 detection callback
    private var e5Callback: (() -> Unit)? = null

    private var usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            when (intent.action) {
                ACTION_USB_PERMISSION -> {
                    synchronized(this) {
                        val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                        val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                        if (device != null) { onPermissionGranted(device, granted) }
                    }
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    if (device != null && serialPort?.driver?.device?.deviceId == device.deviceId) { disconnect() }
                    refreshConnectionState()
                }
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    refreshConnectionState()
                }
            }
        }
    }

    init {
        setupPermissionIntent()
        registerUsbReceiver()
    }

    fun cleanup() {
        if (cleanupCalled) return
        cleanupCalled = true
        try {
            ioManager?.stop()
            ioManager = null
            serialPort?.close()
            serialPort = null
            _connectionState.value = ConnectionState.DISCONNECTED
            try { context.unregisterReceiver(usbReceiver) } catch (_: Exception) {}
            receiverRegistered = false
            LoggerService.log(LogTag.INFO, "M-Bus yöneticisi temizlendi")
        } catch (_: Exception) {
        }
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
        if (receiverRegistered) {
            LoggerService.log(LogTag.WARN, "USB alıcısı zaten kayıtlı, tekrar kayıt engellendi")
            return
        }
        val filter = IntentFilter().apply {
            addAction(ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        }
        ContextCompat.registerReceiver(
            context,
            usbReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        receiverRegistered = true
    }

    /**
     * Connect to USB device with M-Bus standard parameters (1:1 port from MBusService.connect).
     *   Baud Rate : 2400
     *   Data Bits : 8
     *   Stop Bits : 1
     *   Parity    : Even
     *   DTR       : true
     *   RTS       : true
     */
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

            // M-Bus standardı port ayarları (1:1 Dart port)
            port.setDTR(true)
            port.setRTS(true)
            port.setParameters(
                config.baudRate,
                config.dataBits,
                config.stopBits,
                config.parity
            )

            serialPort = port
            _connectionState.value = ConnectionState.CONNECTED

            LoggerService.log(LogTag.INFO, "M-Bus bağlantısı kuruldu (${config.baudRate} bps, DTR/RTS=true)")

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

    // ─────────────────────────────────────────────────────────────────────────
    // WRITE (1:1 port of MBusService.write)
    // Clears data buffer, stores sent bytes for echo cancellation, then sends.
    // ─────────────────────────────────────────────────────────────────────────
    fun write(data: ByteArray) {
        try {
            val hex = data.joinToString(" ") { "%02X".format(it) }
            LoggerService.log(LogTag.HARDWARE, "GÖNDER → $hex (${data.size} byte)")

            synchronized(dataBuffer) {
                dataBuffer.clear()           // Eski kalıntıları temizle
                lastSentBytes.clear()
                lastSentBytes.addAll(data.toList()) // Gönderilen komutu hafızaya al
            }

            serialPort?.write(data, 500)
        } catch (e: IOException) {
            LoggerService.log(LogTag.ERROR, "Veri gönderme hatası: ${e.message}")
            _connectionState.value = ConnectionState.ERROR
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SEND READ REQUEST (1:1 port of MBusService.sendReadRequest)
    // Implements the 5-step Calmet wake-up sequence for targeted reads.
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun sendReadRequest(targetSerial: String? = null) = withContext(Dispatchers.IO) {
        if (serialPort == null) {
            throw Exception("Port bağlı değil!")
        }

        if (!targetSerial.isNullOrEmpty() && targetSerial.length >= 8) {
            // ── 5-Adımlı Uyandırma ve Bekleme Zinciri (Calmet) ──

            // Parse serial number: b1,b2,b3,b4 = BCD reversed serial
            val b1 = targetSerial.substring(6, 8).toInt(16)
            val b2 = targetSerial.substring(4, 6).toInt(16)
            val b3 = targetSerial.substring(2, 4).toInt(16)
            val b4 = targetSerial.substring(0, 2).toInt(16)

            // Step A (Ping)
            write(byteArrayOf(0x10.toByte(), 0x40.toByte(), 0xFE.toByte(), 0x3E.toByte(), 0x16.toByte()))
            LoggerService.log(LogTag.HARDWARE, "Ping gönderildi, 1sn bekleniyor")
            delay(1000)

            // Step B (Reset / NKE)
            write(byteArrayOf(0x10.toByte(), 0x40.toByte(), 0xFD.toByte(), 0x3D.toByte(), 0x16.toByte()))
            LoggerService.log(LogTag.HARDWARE, "SIFIRLAMA: 10 40 FD 3D 16")
            delay(600)

            // Step C (Select frame)
            val baseFrame = byteArrayOf(
                0x68.toByte(), 0x0B.toByte(), 0x0B.toByte(), 0x68.toByte(),
                0x53.toByte(), 0xFD.toByte(), 0x52.toByte(),
                b1.toByte(), b2.toByte(), b3.toByte(), b4.toByte(),
                0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()
            )
            var cs = 0
            for (i in 4 until baseFrame.size) {
                cs = (cs + (baseFrame[i].toInt() and 0xFF)) and 0xFF
            }
            val selectionFrame = baseFrame + byteArrayOf(cs.toByte(), 0x16.toByte())

            // Step D: Send selection frame, then wait for E5
            write(selectionFrame)
            LoggerService.log(LogTag.HARDWARE, "Seçim Çerçevesi gönderildi: $targetSerial")

            val e5Received = waitForE5(400)

            if (e5Received) {
                LoggerService.log(LogTag.HARDWARE, "E5 Alındı. 7B okuma komutu gönderiliyor...")
            } else {
                LoggerService.log(LogTag.HARDWARE, "E5 ALINAMADI! (Kör Okuma Aktif - Zorla 7B gönderiliyor...)")
            }

            // Step E: Delay then forced REQ_UD2 (regardless of E5 reception)
            delay(150)
            write(byteArrayOf(0x10.toByte(), 0x7B.toByte(), 0xFD.toByte(), 0x78.toByte(), 0x16.toByte()))
            LoggerService.log(LogTag.HARDWARE, "Okuma Komutu (FD - 7B) hatta basıldı!")
        } else {
            // M-Bus Short Frame Broadcast Read Request
            write(byteArrayOf(0x10.toByte(), 0x5B.toByte(), 0xFE.toByte(), 0x59.toByte(), 0x16.toByte()))
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // WAIT FOR E5 (1:1 port of waitForE5 from MBusService.dart)
    // Listens for an 0xE5 byte on the filtered data stream with a timeout.
    // ─────────────────────────────────────────────────────────────────────────
    private suspend fun waitForE5(timeoutMs: Long): Boolean {
        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<Boolean> { cont ->
                e5Callback = {
                    if (cont.isActive) {
                        cont.resume(true)
                    }
                }
                cont.invokeOnCancellation { e5Callback = null }
            }
        } ?: false
    }

    // ─────────────────────────────────────────────────────────────────────────
    // WAIT FOR RAW RESPONSE (kept for backward compatibility)
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun waitForRawResponse(): ByteArray = suspendCancellableCoroutine { cont ->
        synchronized(rawBuffer) {
            if (rawBuffer.isNotEmpty()) {
                val data = rawBuffer.toByteArray()
                rawBuffer.clear()
                rawResponseCallback = null
                cont.resume(data)
                return@suspendCancellableCoroutine
            }

            rawResponseCallback = { data ->
                cont.resume(data)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DEVICE LISTING / STATE
    // ─────────────────────────────────────────────────────────────────────────

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
                } catch (_: Exception) {
                    LoggerService.log(LogTag.WARN, "Bağlantı kaybı algılandı")
                }
                _connectionState.value = ConnectionState.DISCONNECTED
            }

            if (!isDevicePhysicallyConnected()) {
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        } catch (_: Exception) {
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // IO MANAGER (with Echo Cancellation — 1:1 port of MBusService._dataSubscription)
    // ─────────────────────────────────────────────────────────────────────────
    private fun startIoManager(port: UsbSerialPort) {
        ioManager = SerialInputOutputManager(port, object : SerialInputOutputManager.Listener {
            override fun onNewData(data: ByteArray) {
                val hex = data.joinToString(" ") { "%02X".format(it) }
                LoggerService.log(LogTag.HARDWARE, "ALINAN ← $hex (${data.size} byte)")

                // ── Echo Cancellation (1:1 from Dart) ──
                synchronized(dataBuffer) {
                    dataBuffer.addAll(data.toList())

                    if (lastSentBytes.isNotEmpty() && dataBuffer.isNotEmpty()) {
                        var stillMatching = true
                        for (i in dataBuffer.indices) {
                            if (i >= lastSentBytes.size) break
                            if (dataBuffer[i] != lastSentBytes[i]) {
                                stillMatching = false
                                break
                            }
                        }

                        if (!stillMatching) {
                            // Eşleşme bozuldu → yankı değil (veya yankı olmayan donanım)
                            lastSentBytes.clear()
                        } else if (dataBuffer.size >= lastSentBytes.size) {
                            // Yankı birebir eşleşti ve tamamlandı!
                            repeat(lastSentBytes.size) { dataBuffer.removeAt(0) }
                            lastSentBytes.clear()
                        }
                    }

                    // Yankı temizlendikten sonra temiz veriyi aktar
                    if (lastSentBytes.isEmpty() && dataBuffer.isNotEmpty()) {
                        val cleanData = dataBuffer.toByteArray()
                        dataBuffer.clear()

                        // Check for E5 in clean data
                        if (cleanData.contains(0xE5.toByte())) {
                            e5Callback?.invoke()
                            e5Callback = null
                        }

                        // Forward to raw response callback or buffer
                        synchronized(rawBuffer) {
                            val callback = rawResponseCallback
                            if (callback != null) {
                                rawResponseCallback = null
                                callback(cleanData)
                            } else {
                                rawBuffer.addAll(cleanData.toList())
                            }
                        }

                        // Also send to received data channel
                        _receivedData.trySend(cleanData)
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

    companion object {
        private const val ACTION_USB_PERMISSION = "com.prosayac.app.USB_PERMISSION"
    }
}