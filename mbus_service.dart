import 'dart:async';
import 'package:usb_serial/usb_serial.dart';
import 'package:flutter/foundation.dart';

/// USB bağlantı durumları
enum ConnectionStatus {
  disconnected,
  connecting,
  connected,
  error,
}

/// MBus Service - USB OTG üzerinden M-Bus protokolü haberleşme katmanı.
///
/// Bağlantı parametreleri (M-Bus standardı):
///   Baud Rate : 2400
///   Data Bits : 8
///   Stop Bits : 1
///   Parity    : Even (Çift)
class MBusService {
  UsbPort? _port;
  StreamSubscription<Uint8List>? _dataSubscription;

  // Donanımsal Yankı (Hardware Echo) iptali için değişkenler
  List<int> _lastSentBytes = [];
  final List<int> _dataBuffer = [];

  // Gelen veriler için stream controller
  final _dataController = StreamController<Uint8List>.broadcast();

  /// Gelen ham veri stream'i
  Stream<Uint8List> get dataStream => _dataController.stream;

  bool get isConnected => _port != null;

  /// Mevcut USB cihazlarını listele
  Future<List<UsbDevice>> listDevices() async {
    return await UsbSerial.listDevices();
  }

  /// Seçilen USB cihazına M-Bus parametreleriyle bağlan
  Future<bool> connect(UsbDevice device, {int baudRate = 2400}) async {
    try {
      _port = await device.create();
      if (_port == null) return false;

      final opened = await _port!.open();
      if (!opened) {
        _port = null;
        return false;
      }

      // M-Bus standardı port ayarları
      await _port!.setDTR(true);
      await _port!.setRTS(true);
      await _port!.setPortParameters(
        baudRate, // Baud Rate
        UsbPort.DATABITS_8, // Data Bits
        UsbPort.STOPBITS_1, // Stop Bits
        UsbPort.PARITY_EVEN, // Parity: Even (Çift)
      );

      // Gelen veri dinleyicisini başlat (Yankı Filtresi ile)
      _dataSubscription = _port!.inputStream!.listen(
        (Uint8List data) {
          _dataBuffer.addAll(data);

          if (_lastSentBytes.isNotEmpty && _dataBuffer.isNotEmpty) {
            bool stillMatching = true;
            for (int i = 0; i < _dataBuffer.length && i < _lastSentBytes.length; i++) {
              if (_dataBuffer[i] != _lastSentBytes[i]) {
                stillMatching = false;
                break;
              }
            }

            if (!stillMatching) {
              // Eşleşme bozuldu, demek ki yankı değil (veya yankı olmayan donanım)
              _lastSentBytes.clear();
            } else if (_dataBuffer.length >= _lastSentBytes.length) {
              // Yankı birebir eşleşti ve tamamlandı!
              _dataBuffer.removeRange(0, _lastSentBytes.length);
              _lastSentBytes.clear();
            }
          }

          // Yankı temizlendikten sonra (veya zaten yankı beklenmiyorsa) kalan temiz veriyi aktar
          if (_lastSentBytes.isEmpty && _dataBuffer.isNotEmpty) {
            _dataController.add(Uint8List.fromList(_dataBuffer));
            _dataBuffer.clear();
          }
        },
        onError: (error) {
          _dataController.addError(error);
        },
      );

      return true;
    } catch (e) {
      _port = null;
      rethrow;
    }
  }

  /// M-Bus okuma isteği gönder (Hedefe Özel veya Broadcast)
  Future<void> sendReadRequest({String? targetSerial}) async {
    if (_port == null) {
      throw Exception('Port bağlı değil!');
    }

    if (targetSerial != null && targetSerial.length >= 8) {
      // 7 Adımlı Uyandırma ve Bekleme Zinciri
      final b1 = int.parse(targetSerial.substring(6, 8), radix: 16);
      final b2 = int.parse(targetSerial.substring(4, 6), radix: 16);
      final b3 = int.parse(targetSerial.substring(2, 4), radix: 16);
      final b4 = int.parse(targetSerial.substring(0, 2), radix: 16);

      // Adım A (Ping)
      await write(Uint8List.fromList([0x10, 0x40, 0xFE, 0x3E, 0x16]));
      debugPrint('Ping gönderildi, 1sn bekleniyor');
      await Future.delayed(const Duration(milliseconds: 1000));

      // Adım B (Sıfırlama)
      await write(Uint8List.fromList([0x10, 0x40, 0xFD, 0x3D, 0x16]));
      debugPrint('SIFIRLAMA: 10 40 FD 3D 16');
      await Future.delayed(const Duration(milliseconds: 600));

      // Seçim Çerçevesi Oluştur
      final baseFrame = [0x68, 0x0B, 0x0B, 0x68, 0x53, 0xFD, 0x52, b1, b2, b3, b4, 0xFF, 0xFF, 0xFF, 0xFF];
      int cs = 0;
      for (int i = 4; i < baseFrame.length; i++) {
        cs = (cs + baseFrame[i]) % 256;
      }
      final selectionFrame = Uint8List.fromList([...baseFrame, cs, 0x16]);

      // E5 (ACK) yakalamak için yardımcı metod
      Future<bool> waitForE5(Duration timeout) async {
        final completer = Completer<bool>();
        final sub = dataStream.listen((data) {
          if (data.contains(0xE5) && !completer.isCompleted) {
            completer.complete(true);
          }
        });
        try {
          return await completer.future.timeout(timeout);
        } catch (_) {
          return false;
        } finally {
          await sub.cancel();
        }
      }

      // 1. ADIM: Seçim Çerçevesini Gönder
      await write(selectionFrame);
      debugPrint("Seçim Çerçevesi gönderildi: $targetSerial");

      // 2. ADIM: E5 Bekleme (Sadece dinle, okumayı iptal etme!)
      bool e5Received = await waitForE5(const Duration(milliseconds: 400));

      if (e5Received) {
        debugPrint("E5 Alındı. 7B okuma komutu gönderiliyor...");
      } else {
        // KRİTİK DEĞİŞİKLİK: E5 gelmese bile işlemi iptal ETME!
        debugPrint("E5 ALINAMADI! (Kör Okuma Aktif - Zorla 7B gönderiliyor...)");
      }

      // 3. ADIM: E5 GELSE DE GELMESE DE 7B KOMUTUNU ZORLA GÖNDER
      await Future.delayed(const Duration(milliseconds: 150)); // Hattın rahatlaması için küçük es
      await write(Uint8List.fromList([0x10, 0x7B, 0xFD, 0x78, 0x16]));
      debugPrint("Okuma Komutu (FD - 7B) hatta basıldı!");
    } else {
      // M-Bus Short Frame Broadcast Read Request
      final mBusCommand = Uint8List.fromList([0x10, 0x5B, 0xFE, 0x59, 0x16]);
      await write(mBusCommand);
    }
  }

  /// Ham byte dizisi gönderir (Uyandırma komutu vb. için)
  Future<void> write(Uint8List data) async {
    if (_port == null) {
      throw Exception('Port bağlı değil!');
    }
    _dataBuffer.clear(); // Eski kalıntıları temizle
    _lastSentBytes = data.toList(); // Gönderilen komutu hafızaya al
    await _port!.write(data);
  }

  /// Bağlantıyı kes
  Future<void> disconnect() async {
    await _dataSubscription?.cancel();
    _dataSubscription = null;
    await _port?.close();
    _port = null;
  }

  /// Service'i temizle
  void dispose() {
    disconnect();
    _dataController.close();
  }
}
