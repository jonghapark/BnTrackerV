import 'package:flutter_ble_lib/flutter_ble_lib.dart';

//BLE 장치 정보 저장 클래스
class BleDeviceItem {
  String deviceName;
  Peripheral peripheral;
  int rssi;
  AdvertisementData advertisementData;
  BleDeviceItem(
      this.deviceName, this.rssi, this.peripheral, this.advertisementData);
}

class Data {
  String lat;
  String lng;
  String deviceName;
  String temper;
  String humi;
  String time;
  String battery;
  String lex;

  Data(
      {this.deviceName,
      this.humi,
      this.lat,
      this.lng,
      this.temper,
      this.time,
      this.lex,
      this.battery});
}
