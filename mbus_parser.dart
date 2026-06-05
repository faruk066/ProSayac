import 'dart:math' as math;
import 'package:flutter/foundation.dart';


class MBusRawData {
  final String meterId;
  final double energy;
  final double volume;

  MBusRawData({
    required this.meterId,
    required this.energy,
    required this.volume,
  });
}

class MBusParser {
  /// Hex verisini ayrıştırıp MBusRawData döner
  static MBusRawData? parseData(List<int> bytes, {bool isWaterMeter = false}) {
    if (bytes.isEmpty) return null;

    if (bytes[0] != 0x68) return null;
    if (bytes.length < 19) return null;

    final idBytes = bytes.sublist(7, 11);
    final meterId = _decodeBcd(idBytes);

    double energy = 0.0;
    double volume = 0.0;

    int i = 19;
    while (i < bytes.length) {
      final dif = bytes[i];
      if (dif == 0x0F || dif == 0x1F) break;

      final dataType = dif & 0x0F;
      i++;

      while (i < bytes.length && (bytes[i - 1] & 0x80) != 0) {
        if ((bytes[i] & 0x80) == 0) {
          i++;
          break;
        }
        i++;
      }
      if (i >= bytes.length) break;

      final vif = bytes[i];
      i++;

      while (i < bytes.length && (bytes[i - 1] & 0x80) != 0) {
        if ((bytes[i] & 0x80) == 0) {
          i++;
          break;
        }
        i++;
      }
      if (i >= bytes.length) break;

      int length = _dataLength(dataType);
      if (length < 0 || i + length > bytes.length) break;

      final valueBytes = bytes.sublist(i, i + length);
      i += length;

      double rawVal = 0.0;
      if (dataType == 0x04) {
        rawVal = _decodeInt32(valueBytes);
      } else if (dataType == 0x0C) {
        rawVal = _decodeBcdInt(valueBytes);
      } else {
        continue;
      }

      final vifCode = vif & 0x7F;

      if (vifCode >= 0x10 && vifCode <= 0x17) {
        int exponent = vifCode - 0x16;
        volume += rawVal * _pow10(exponent);
        if (isWaterMeter) break;
      } else if (!isWaterMeter && vifCode >= 0x00 && vifCode <= 0x07) {
        int exponent = vifCode - 0x03;
        energy += rawVal * _pow10(exponent);
        break;
      }
    }

    return MBusRawData(meterId: meterId, energy: energy, volume: volume);
  }

  static String _decodeBcd(List<int> bytes) {
    final buffer = StringBuffer();
    for (int i = bytes.length - 1; i >= 0; i--) {
      final b = bytes[i];
      buffer.write(((b >> 4) & 0x0F).toRadixString(16));
      buffer.write((b & 0x0F).toRadixString(16));
    }
    return buffer.toString();
  }

  @visibleForTesting
  static double decodeBcdIntForTesting(List<int> bytes) {
    return _decodeBcdInt(bytes);
  }

  static double _decodeBcdInt(List<int> bytes) {
    double res = 0;
    double multiplier = 1;
    for (int i = 0; i < bytes.length; i++) {
      final b = bytes[i];
      int low = b & 0x0F;
      int high = (b >> 4) & 0x0F;
      res += low * multiplier;
      multiplier *= 10;
      res += high * multiplier;
      multiplier *= 10;
    }
    return res;
  }

  static int _dataLength(int dataType) {
    switch (dataType) {
      case 0x00:
        return 0;
      case 0x01:
        return 1;
      case 0x02:
        return 2;
      case 0x03:
        return 3;
      case 0x04:
        return 4;
      case 0x05:
        return 4;
      case 0x06:
        return 6;
      case 0x07:
        return 8;
      case 0x0C:
        return 4;
      case 0x0D:
        return -1;
      case 0x0E:
        return 6;
      default:
        return -1;
    }
  }

  static double _decodeInt32(List<int> bytes) {
    if (bytes.length < 4) return 0.0;
    int val = bytes[0] | (bytes[1] << 8) | (bytes[2] << 16) | (bytes[3] << 24);
    if ((val & 0x80000000) != 0) {
      val = val - 0x100000000;
    }
    return val.toDouble();
  }

  static const List<double> _pow10Lookup = [
    1.0,
    10.0,
    100.0,
    1000.0,
    10000.0,
    100000.0,
    1000000.0,
    10000000.0,
    100000000.0,
    1000000000.0,
  ];

  static double _pow10(int exponent) {
    if (exponent >= 0 && exponent < _pow10Lookup.length) {
      return _pow10Lookup[exponent];
    }
    return math.pow(10.0, exponent).toDouble();
  }
}
