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
    private var receiverRegistered = false

    private val managerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ── Echo Cancellation (1:1 port from MBusService.dart) ──
    private val lastSentBytes = mutableListOf<Byte>()
    private val dataBuffer = mutableListOf<Byte>()

    // Raw response callback used by waitForRawResponse()
    private val rawBuffer = mutableListOf<Byte>()
    @Volatile
    private var rawResponseCallback: ((ByteArray) -> Unit)? = null

    // ── Smart Frame Accumulator (persistent callback — NOT single-shot) ──
    // Used by waitForRspUdFrame() to scavenge for 0x68 and accumulate L+6 bytes.
    @Volatile
    private var accumulatorCallback: ((ByteArray) -> Unit)? = null

    // E5 detection callback
    @Volatile
    private var e5Callback: (() -> Unit)? = null

    // Pending config used for permission retry
    private var pendingConfig: SerialConfig? = null

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
        try {
            managerScope.cancel()
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

    fun reset() {
        try {
            // Re-register receiver if needed
            if (!receiverRegistered) {
                registerUsbReceiver()
            }
            LoggerService.log(LogTag.INFO, "M-Bus yöneticisi sıfırlandı")
        } catch (e: Exception) {
            LoggerService.log(LogTag.WARN, "Reset hatası: ${e.message}")
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
        if (_connectionState.value == ConnectionState.CONNECTED) {
            LoggerService.log(LogTag.WARN, "Zaten bağlı, tekrar bağlanma atlanıyor")
            return
        }
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
                pendingConfig = config  // Cache config for retry
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
            val config = pendingConfig ?: SerialConfig()
            pendingConfig = null
            // Offload blocking USB I/O to IO dispatcher to prevent ANR on main thread
            managerScope.launch {
                connect(config)
            }
        } else {
            LoggerService.log(LogTag.WARN, "USB izni reddedildi")
            pendingConfig = null
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
            pendingConfig = null  // Clear cached config
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

        // ── AGGRESSIVE BUFFER CLEARING ──
        // Clear ALL buffers before starting a new read sequence to prevent
        // stale data from previous meters bleeding into the current read.
        synchronized(dataBuffer) {
            dataBuffer.clear()
            lastSentBytes.clear()
        }
        synchronized(rawBuffer) {
            rawBuffer.clear()
            rawResponseCallback = null
        }
        e5Callback = null

        if (!targetSerial.isNullOrEmpty()) {
            // ── Normalize and validate serial number ──
            val normalized = targetSerial.trim().replace(Regex("[^0-9A-Fa-f]"), "")
            if (normalized.length < 8) {
                throw IllegalArgumentException("Geçersiz seri numarası: $targetSerial (en az 8 hex karakter gerekli)")
            }

            // Left-pad to 8 hex characters if needed
            val paddedSerial = normalized.padStart(8, '0').takeLast(8)

            // Validate hex format
            try {
                paddedSerial.toLong(16)
            } catch (e: NumberFormatException) {
                throw IllegalArgumentException("Geçersiz hex seri numarası: $targetSerial")
            }

            // ── 5-Adımlı Uyandırma ve Bekleme Zinciri (Calmet) ──

            // Parse serial number: b1,b2,b3,b4 = BCD reversed serial
            val b1 = paddedSerial.substring(6, 8).toInt(16)
            val b2 = paddedSerial.substring(4, 6).toInt(16)
            val b3 = paddedSerial.substring(2, 4).toInt(16)
            val b4 = paddedSerial.substring(0, 2).toInt(16)

            LoggerService.log(LogTag.HARDWARE, "Seri normalize edildi: $targetSerial → $paddedSerial")

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

            // ── AGGRESSIVE HARDWARE BUFFER PURGE ──
            // Clear USB hardware buffers AND software buffers before sending
            // the REQ_UD2 blind read command. This prevents leftover garbage
            // bytes (F9, E5, etc.) from poisoning the Rsp_UD frame.
            purgeAllBuffers()

            // Step E: Delay then forced REQ_UD2 (regardless of E5 reception)
            delay(150)
            write(byteArrayOf(0x10.toByte(), 0x7B.toByte(), 0xFD.toByte(), 0x78.toByte(), 0x16.toByte()))
            LoggerService.log(LogTag.HARDWARE, "Okuma Komutu (FD - 7B) hatta basıldı! (tamponlar temizlendi)")
        } else {
            // M-Bus Short Frame Broadcast Read Request
            write(byteArrayOf(0x10.toByte(), 0x5B.toByte(), 0xFE.toByte(), 0x59.toByte(), 0x16.toByte()))
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SEND DIRECT READ REQUEST (Blind 0x7B — no Calmet wake-up)
    //
    // Skips the entire 5-step Calmet wake-up sequence (ping, reset, selection)
    // and just sends the REQ_UD2 (0x7B) command directly. Some older or
    // battery-save meters (e.g. 280-series) ignore selection frames but
    // respond to a direct 0x7B request.
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun sendDirectReadRequest(targetSerial: String? = null) = withContext(Dispatchers.IO) {
        if (serialPort == null) {
            throw Exception("Port bağlı değil!")
        }

        // ── AGGRESSIVE BUFFER CLEARING ──
        synchronized(dataBuffer) {
            dataBuffer.clear()
            lastSentBytes.clear()
        }
        synchronized(rawBuffer) {
            rawBuffer.clear()
            rawResponseCallback = null
        }
        e5Callback = null
        accumulatorCallback = null

        if (!targetSerial.isNullOrEmpty()) {
            // Normalize and validate serial (same as sendReadRequest)
            val normalized = targetSerial.trim().replace(Regex("[^0-9A-Fa-f]"), "")
            if (normalized.length < 8) {
                throw IllegalArgumentException("Geçersiz seri numarası: $targetSerial (en az 8 hex karakter gerekli)")
            }
            val paddedSerial = normalized.padStart(8, '0').takeLast(8)
            try {
                paddedSerial.toLong(16)
            } catch (e: NumberFormatException) {
                throw IllegalArgumentException("Geçersiz hex seri numarası: $targetSerial")
            }

            LoggerService.log(
                LogTag.HARDWARE,
                "KÖR OKUMA (Calmet atlandı): $targetSerial → $paddedSerial, sadece 7B gönderiliyor"
            )

            // Aggressive buffer purge before sending
            purgeAllBuffers()

            // Sleep to let the bus settle (no wake-up sent)
            delay(150)

            // ── Step E only: Send REQ_UD2 (0x7B) directly ──
            write(byteArrayOf(0x10.toByte(), 0x7B.toByte(), 0xFD.toByte(), 0x78.toByte(), 0x16.toByte()))
            LoggerService.log(LogTag.HARDWARE, "KÖR OKUMA: 10 7B FD 78 16 hatta basıldı (uyandırma yapılmadı)")
        } else {
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
                synchronized(dataBuffer) {
                    e5Callback = {
                        if (cont.isActive) {
                            cont.resume(true)
                        }
                    }
                }
                cont.invokeOnCancellation {
                    synchronized(dataBuffer) {
                        e5Callback = null
                    }
                }
            }
        } ?: false
    }

    // ─────────────────────────────────────────────────────────────────────────
    // WAIT FOR RAW RESPONSE (kept for backward compatibility)
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun waitForRawResponse(): ByteArray? = withTimeoutOrNull(1500L) {
        suspendCancellableCoroutine { cont ->
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
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AGGRESSIVE PORT PURGE
    // Clears the USB hardware buffer AND all internal software buffers.
    // This MUST be called before sending the blind read command (10 7B FD 78 16)
    // to guarantee zero leftover bytes (F9, E5, etc.) are in the buffer.
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun purgeAllBuffers() {
        LoggerService.log(LogTag.HARDWARE, "TAMPON TEMİZLEME: Donanım ve yazılım tamponları temizleniyor...")

        // 1. Purge hardware buffers (USB serial driver level) — blocking I/O on IO dispatcher
        withContext(Dispatchers.IO) {
            try {
                serialPort?.purgeHwBuffers(true, true)
                LoggerService.log(LogTag.HARDWARE, "Donanım tamponları purged (RX+TX)")
            } catch (e: Exception) {
                LoggerService.log(LogTag.WARN, "Donanım tampon temizleme hatası: ${e.message}")
                // Fallback: drain manually if purgeHwBuffers is not available
                try {
                    val drainBuf = ByteArray(256)
                    var loopCount = 0
                    while (true) {
                        val read = serialPort?.read(drainBuf, 50) ?: -1
                        if (read <= 0) break
                        LoggerService.log(LogTag.HARDWARE, "Manuel tahliye: $read byte atıldı")
                        loopCount++
                        if (loopCount > 100) {
                            LoggerService.log(LogTag.WARN, "Manuel tahliye 100 döngü limitini aştı, durduruluyor")
                            break
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        // 2. Clear internal software buffers
        synchronized(dataBuffer) {
            dataBuffer.clear()
            lastSentBytes.clear()
        }
        synchronized(rawBuffer) {
            rawBuffer.clear()
            rawResponseCallback = null
        }
        accumulatorCallback = null
        e5Callback = null

        LoggerService.log(LogTag.HARDWARE, "Tüm tamponlar temizlendi ✓")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SMART FRAME ACCUMULATOR: waitForRspUdFrame
    //
    // Listens to the incoming stream for up to 'timeoutMs' milliseconds.
    // - Discards garbage bytes (0xF9, 0xE5, 0x00) until 0x68 is found.
    // - Once 0x68 is found, reads length byte (L) and waits for L+6 total bytes.
    // - Only returns the complete frame when the buffer holds exactly L+6 bytes.
    //
    // This prevents the "instant fail on garbage" bug where the parser reads
    // leftover noise bytes and aborts before the real Rsp_UD frame arrives.
    // ─────────────────────────────────────────────────────────────────────────
    suspend fun waitForRspUdFrame(timeoutMs: Long = 1500L): ByteArray? {
        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<ByteArray?> { cont ->
                // Accumulator buffer: raw bytes collected while hunting for 0x68
                val accumulator = mutableListOf<Byte>()
                var targetSize: Int? = null  // L + 6, set once we have the length byte

                synchronized(dataBuffer) {
                    accumulatorCallback = { cleanData ->
                    for (b in cleanData) {
                        val ub = b.toInt() and 0xFF

                        if (targetSize == null) {
                            if (accumulator.isEmpty()) {
                                // ── GARBAGE FILTER ──
                                // Discard known noise. 0x00 MUST NOT be filtered here.
                                if (ub == 0xF9 || ub == 0xE5) {
                                    LoggerService.log(LogTag.HARDWARE, "ÇÖP BYTE ATLANDI: %02X".format(ub))
                                    continue
                                }
                                
                                // ── HUNT FOR 0x68 START BYTE ──
                                if (ub == 0x68) {
                                    LoggerService.log(LogTag.HARDWARE, "0x68 BAŞLANGIÇ BULUNDU! Çerçeve başlığı biriktiriliyor...")
                                    accumulator.add(b)
                                } else {
                                    LoggerService.log(LogTag.HARDWARE, "Beklenmeyen byte (0x68 aranıyor): %02X".format(ub))
                                }
                            } else {
                                // ── ACCUMULATING HEADER (68 L L 68) ──
                                accumulator.add(b)
                                
                                // ── VALIDATE M-BUS LONG FRAME HEADER ──
                                if (accumulator.size == 4) {
                                    val b0 = accumulator[0].toInt() and 0xFF
                                    val b1 = accumulator[1].toInt() and 0xFF
                                    val b2 = accumulator[2].toInt() and 0xFF
                                    val b3 = accumulator[3].toInt() and 0xFF
                                    
                                    if (b0 == 0x68 && b3 == 0x68 && b1 == b2) {
                                        targetSize = b1 + 6
                                        LoggerService.log(LogTag.HARDWARE,
                                            "Geçerli başlık (68 %02X %02X 68) → toplam %d byte bekleniyor".format(b1, b2, targetSize!!))
                                    } else {
                                        LoggerService.log(LogTag.HARDWARE,
                                            "Geçersiz başlık (68 %02X %02X %02X) → tampon sıfırlanıyor".format(b1, b2, b3))
                                        accumulator.clear()
                                        // Recover if the mismatching 4th byte was actually a new frame start
                                        if (b3 == 0x68) {
                                            accumulator.add(b)
                                        }
                                    }
                                }
                            }
                        } else {
                            // ── ACCUMULATING PAYLOAD & FOOTER ──
                            // targetSize is known, unconditionally accept every byte including 0x00
                            accumulator.add(b)
                        }

                        // ── CHECK IF FRAME IS COMPLETE ──
                        if (targetSize != null && accumulator.size >= targetSize!!) {
                            val frame = accumulator.toByteArray()
                            LoggerService.log(LogTag.HARDWARE,
                                "ÇERÇEVE TAMAMLANDI: %d / %d byte".format(accumulator.size, targetSize!!))
                            // Detach the accumulator so no further callbacks arrive
                            accumulatorCallback = null
                            // Resume the suspending coroutine — this completes it
                            if (cont.isActive) {
                                cont.resume(frame)
                            }
                            // Coroutine is already resumed; just fall through naturally
                            // (break would be non-local in a non-inline lambda, so we
                            //  let the loop finish — it won't cause harm since the
                            //  accumulator is already detached)
                        }
                    }

                    }
                }

                cont.invokeOnCancellation {
                    synchronized(dataBuffer) {
                        accumulatorCallback = null
                        accumulator.clear()
                    }
                }
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
                            dataBuffer.subList(0, lastSentBytes.size).clear()
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

                        // ── SMART FRAME ACCUMULATOR (highest priority) ──
                        // Forward to the accumulator if one is active (waitForRspUdFrame).
                        // The accumulator is PERSISTENT — it stays registered until the
                        // frame is complete or the coroutine is cancelled.
                        // When active, the accumulator OWNS the data stream and rawBuffer
                        // is bypassed entirely.
                        val acc = accumulatorCallback
                        if (acc != null) {
                            acc(cleanData)
                            // Bypass rawBuffer/channel — the accumulator is hunting for 0x68
                        } else {
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