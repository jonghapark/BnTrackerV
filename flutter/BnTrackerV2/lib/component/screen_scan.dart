import 'dart:ffi';
import 'package:http/http.dart' as http;
import 'package:flutter/material.dart';
import 'dart:io';
import 'package:flutter_ble_lib/flutter_ble_lib.dart';
import 'package:permission_handler/permission_handler.dart';
import '../models/model_bleDevice.dart';
import '../utils/util.dart';
import 'dart:typed_data';
import 'dart:convert';
import 'dart:async';
import 'package:location/location.dart' as loc;
import 'package:qrscan/qrscan.dart' as scanner;
import 'package:geocoder/geocoder.dart';
// URL
// 10.3.141.1:4000

class Scanscreen extends StatefulWidget {
  @override
  ScanscreenState createState() => ScanscreenState();
}

class ScanscreenState extends State<Scanscreen> {
  BleManager _bleManager = BleManager();
  bool _isScanning = false;
  bool _connected = false;
  String currentMode = 'normal';
  String message = '';
  Peripheral _curPeripheral; // 연결된 장치 변수
  List<BleDeviceItem> deviceList = []; // BLE 장치 리스트 변수
  String _statusText = ''; // BLE 상태 변수
  loc.LocationData currentLocation;
  int dataSize = 0;
  loc.Location location = new loc.Location();
  int processState = 1;
  StreamSubscription<loc.LocationData> _locationSubscription;
  String _error;
  String geolocation;
  String currentDeviceName;

  String currentTemp;
  String currentHumi;

  @override
  void initState() {
    super.initState();
    currentDeviceName = '';
    currentTemp = '-';
    currentHumi = '-';
    _listenLocation();
    // getCurrentLocation();
    init();
    // location.onLocationChanged.listen((loc.LocationData currentLocation) {
    //   this.currentLocation = currentLocation;
    //   print('여긴오냐 ->' + currentLocation.latitude.toString());
    //   // Use current location
    // });
  }

  Future<void> _listenLocation() async {
    _locationSubscription =
        location.onLocationChanged.handleError((dynamic err) {
      setState(() {
        _error = err.code;
      });
      _locationSubscription.cancel();
    }).listen((loc.LocationData currentLocation) async {
      final coordinates =
          new Coordinates(currentLocation.latitude, currentLocation.longitude);
      var addresses =
          await Geocoder.local.findAddressesFromCoordinates(coordinates);
      var first = addresses.first;
      if (!_isScanning) {
        scan();
      }
      setState(() {
        _error = null;
        this.currentLocation = currentLocation;

        this.geolocation = first.addressLine;
      });
    });
  }

  Future<Post> sendData(Data data) async {
    var client = http.Client();
    try {
      var uriResponse =
          await client.post('http://175.126.232.236/_API/saveData.php', body: {
        "isRegularData": "true",
        "tra_datetime": data.time,
        "tra_temp": data.temper,
        "tra_humidity": data.humi,
        "tra_lat": data.lat,
        "tra_lon": data.lng,
        "de_number": data.deviceName,
        "tra_battery": data.battery,
        "tra_impact": data.lex
      });
      // print(await client.get(uriResponse.body.['uri']));
    } finally {
      client.close();
    }
  }

  void startBtn() async {
    print('연결안됨? ' + _curPeripheral.name);
    if (_curPeripheral.name != '') {
      setState(() {
        message = '측정 시작을 위한 세팅중입니다.';
      });
      try {
        dataSize = 0;
        var values_tempreadDataResult = await _curPeripheral.readCharacteristic(
            'a2fac1f4-dd28-4e5a-ac2d-4d6e2cb410f9',
            'deee3567-5abb-4ae5-94f0-90c80b62dd46');

        double tmp = ByteData.sublistView(values_tempreadDataResult.value)
            .getFloat32(0, Endian.little);

        if (tmp != -100.0) {
          setState(() {
            currentTemp = tmp.toStringAsFixed(2) + '℃';
          });
        }

        print('내부온도:' +
            ByteData.sublistView(values_tempreadDataResult.value)
                .getFloat32(0, Endian.little)
                .toString());

        var values_tempreadDataResult2 =
            await _curPeripheral.readCharacteristic(
                'a2fac1f4-dd28-4e5a-ac2d-4d6e2cb410f9',
                '65ac0f6d-3a8c-4592-a20d-d1bd373d2f64');
        print('외부온도:' +
            ByteData.sublistView(values_tempreadDataResult2.value)
                .getFloat32(0, Endian.little)
                .toString());
        double tmp2 = ByteData.sublistView(values_tempreadDataResult2.value)
            .getFloat32(0, Endian.little);

        if (tmp2 != -100.0) {
          setState(() {
            currentTemp = tmp2.toStringAsFixed(2) + '℃';
          });
        }

        var values_tempreadDataResult3 =
            await _curPeripheral.readCharacteristic(
                'a2fac1f4-dd28-4e5a-ac2d-4d6e2cb410f9',
                'a57381ab-e640-46e9-89e3-47d1f34ed75f');
        print('내부습도:' +
            ByteData.sublistView(values_tempreadDataResult3.value)
                .getFloat32(0, Endian.little)
                .toString());
        double tmp3 = ByteData.sublistView(values_tempreadDataResult3.value)
            .getFloat32(0, Endian.little);

        if (tmp3 != -100.0) {
          setState(() {
            currentHumi = tmp3.toStringAsFixed(2) + '%';
          });
        }

        // 데이터 삭제 (검증 완료)
        //TODO: FIX Point
        var values_deleteData = await _curPeripheral.writeCharacteristic(
            '66ee2010-bc01-4e93-8a06-c3d7af72dec3',
            '12a13958-d4a2-4808-9297-c8b277833ed8',
            Uint8List.fromList([1]),
            true);
        // print('0 쓰기 결과: ' + values_deleteData.toString());
        // 데이터 개수
        // 삭제 확인용 !
        var values_readDatasize = await _curPeripheral.readCharacteristic(
            '66ee2010-bc01-4e93-8a06-c3d7af72dec3',
            '785b15a7-b67c-4e72-81a4-9904f73ef357');
        setState(() {
          message = '기존 데이터를 삭제중입니다.';
        });

        var blob = ByteData.sublistView(values_readDatasize.value);
        dataSize = blob.getUint16(0, Endian.little);
        print('저장된 데이터 : ' + dataSize.toString());

        print('what? ' +
            Util.convertInt2Bytes(dataSize, Endian.little, 4).toString());
        // print(Uint8List. blob.getInt16(0,Endian.little).)

        //  시간정보
        //  시간쓰기

        DateTime temp = DateTime.now();
        var values_settingTime = await _curPeripheral.writeCharacteristic(
            'cf08a8c2-485a-4e18-8fe9-018700449787',
            '74a75fa1-b998-454c-a3b2-a66f438c955a',
            Uint8List.fromList([
              temp.year - 2000,
              temp.month,
              temp.day,
              temp.hour,
              temp.minute,
              temp.second
            ]),
            true);

        //  시간읽기
        //  시간설정확인
        var values_readData = await _curPeripheral.readCharacteristic(
            'cf08a8c2-485a-4e18-8fe9-018700449787',
            '74a75fa1-b998-454c-a3b2-a66f438c955a');
        print('시간 : ' + values_readData.value.toString());

        // var values_testreadData = await _curPeripheral.readCharacteristic(
        //     '66ee2010-bc01-4e93-8a06-c3d7af72dec3',
        //     '9f69da88-1108-4f72-ab08-fa2dcbec72b4',
        //     Uint8List.fromList([1, 0, 0, 0]),
        //     false);
        //  print('test : ' + values_testreadData.value.toString());

        //  외부 온도센서 활성화 - 왜 안될까
        //TODO: FIX Point
        // var values_setTempData = await _curPeripheral.writeCharacteristic(
        //     'cf08a8c2-485a-4e18-8fe9-018700449787',
        //     '51186f4c-c8f9-4c67-ac1f-a33ba77eb76b',
        //     Uint8List.fromList([1]),
        //     false);

        //  데이터 읽기
        //  인덱스 설정

        //  print(Uint8List.fromList([2, 0]));
        // 측정시작 명령어
        //TODO: FIX Point
        // await _curPeripheral.writeCharacteristic(
        //     'cf08a8c2-485a-4e18-8fe9-018700449787',
        //     '705ec125-966d-4275-bd80-a1b2a8bc28d6',
        //     Uint8List.fromList([1]),
        //     false);

        // //설정 종료
        await _curPeripheral.writeCharacteristic(
            'cf08a8c2-485a-4e18-8fe9-018700449787',
            'e371f072-24aa-40a2-9af8-1b030c0f2acb',
            Uint8List.fromList([1]),
            false);
      } catch (e) {
        setState(() {
          message = '시작중 오류가 발생했습니다.';
        });
        return;
      }

      setState(() {
        message = '측정 시작';
      });
      await _curPeripheral.disconnectOrCancelConnection();
    }
  }

  endBtn() async {
    // sendTest();
    // 측정종료 명령어
    setState(() {
      message = '데이터를 읽어오고 있습니다.';
    });
    //TODO: FIX Point
    // await _curPeripheral.writeCharacteristic(
    //     'cf08a8c2-485a-4e18-8fe9-018700449787',
    //     '705ec125-966d-4275-bd80-a1b2a8bc28d6',
    //     Uint8List.fromList([0]),
    //     false);

    var values_tempreadDataResult = await _curPeripheral.readCharacteristic(
        'a2fac1f4-dd28-4e5a-ac2d-4d6e2cb410f9',
        'deee3567-5abb-4ae5-94f0-90c80b62dd46');

    double tmp = ByteData.sublistView(values_tempreadDataResult.value)
        .getFloat32(0, Endian.little);

    if (tmp != -100.0) {
      setState(() {
        currentTemp = tmp.toStringAsFixed(2) + '℃';
      });
    }

    print('내부온도:' +
        ByteData.sublistView(values_tempreadDataResult.value)
            .getFloat32(0, Endian.little)
            .toString());

    var values_tempreadDataResult2 = await _curPeripheral.readCharacteristic(
        'a2fac1f4-dd28-4e5a-ac2d-4d6e2cb410f9',
        '65ac0f6d-3a8c-4592-a20d-d1bd373d2f64');
    print('외부온도:' +
        ByteData.sublistView(values_tempreadDataResult2.value)
            .getFloat32(0, Endian.little)
            .toString());
    double tmp2 = ByteData.sublistView(values_tempreadDataResult2.value)
        .getFloat32(0, Endian.little);

    if (tmp2 != -100.0) {
      setState(() {
        currentTemp = tmp2.toStringAsFixed(2) + '℃';
      });
    }

    var values_tempreadDataResult3 = await _curPeripheral.readCharacteristic(
        'a2fac1f4-dd28-4e5a-ac2d-4d6e2cb410f9',
        'a57381ab-e640-46e9-89e3-47d1f34ed75f');
    print('내부습도:' +
        ByteData.sublistView(values_tempreadDataResult3.value)
            .getFloat32(0, Endian.little)
            .toString());
    double tmp3 = ByteData.sublistView(values_tempreadDataResult3.value)
        .getFloat32(0, Endian.little);

    if (tmp3 != -100.0) {
      setState(() {
        currentHumi = tmp3.toStringAsFixed(2) + '%';
      });
    }

    var values_readDatasize = await _curPeripheral.readCharacteristic(
        '66ee2010-bc01-4e93-8a06-c3d7af72dec3',
        '785b15a7-b67c-4e72-81a4-9904f73ef357');

    var blob = ByteData.sublistView(values_readDatasize.value);
    dataSize = blob.getUint16(0, Endian.little);
    print('dataSize : ' + dataSize.toString());
    setState(() {
      message = '총 ' + dataSize.toString() + '개의 데이터 전송중';
    });
    for (var i = 0; i < dataSize; i++) {
      if ((((i / dataSize) * 100) % 10) == 0) {
        setState(() {
          message = '총 ' +
              dataSize.toString() +
              '개의 데이터 전송중 ' +
              (i / dataSize * 100).toStringAsFixed(0) +
              '%';
        });
      }
      Data data = new Data();
      DateTime datatime;
      var values_setIndex = await _curPeripheral.writeCharacteristic(
          '66ee2010-bc01-4e93-8a06-c3d7af72dec3',
          'c816ccd4-14d6-457c-8872-e0c1b1bfb1e0',
          Uint8List.fromList(Util.convertInt2Bytes(i, Endian.little, 4)),
          true);
      //sleep(const Duration(milliseconds: 80));
      //인덱스 데이터 읽기
      var values_readDataResult = await _curPeripheral.readCharacteristic(
          '66ee2010-bc01-4e93-8a06-c3d7af72dec3',
          '764dcf6f-0d44-4144-945e-4922d47cc254');
      //print('data:' + values_readDataResult.value.toString());
      datatime = DateTime(
          ByteData.sublistView(values_readDataResult.value.sublist(4, 5))
                  .getUint8(0) +
              2000,
          ByteData.sublistView(values_readDataResult.value.sublist(5, 6))
              .getUint8(0),
          ByteData.sublistView(values_readDataResult.value.sublist(6, 7))
              .getUint8(0),
          ByteData.sublistView(values_readDataResult.value.sublist(7, 8))
              .getUint8(0),
          ByteData.sublistView(values_readDataResult.value.sublist(8, 9))
              .getUint8(0),
          ByteData.sublistView(values_readDataResult.value.sublist(9, 10))
              .getUint8(0));

      Data currentData = new Data(
          deviceName: 'IOT_Tracker',
          battery:
              (ByteData.sublistView(values_readDataResult.value.sublist(2, 4))
                          .getUint16(0, Endian.little) *
                      0.01)
                  .toString(),
          humi: (ByteData.sublistView(values_readDataResult.value.sublist(14, 16))
                      .getUint16(0, Endian.little) *
                  0.01)
              .toStringAsFixed(2),
          temper:
              (ByteData.sublistView(values_readDataResult.value.sublist(12, 14))
                          .getUint16(0, Endian.little) *
                      0.01)
                  .toStringAsFixed(2),
          lat: currentLocation.latitude.toString(),
          lng: currentLocation.longitude.toString(),
          lex: (ByteData.sublistView(values_readDataResult.value.sublist(22, 24)).getUint16(0, Endian.little) * 0.01).toStringAsFixed(2),
          time: datatime.toString());

      sendData(currentData);

      // print('배터리 :' +
      //     (ByteData.sublistView(values_readDataResult.value.sublist(2, 4))
      //                 .getUint16(0, Endian.little) *
      //             0.01)
      //         .toString());
      // print('내부온도 :' +
      //     (ByteData.sublistView(values_readDataResult.value.sublist(10, 12))
      //                 .getInt16(0, Endian.little) *
      //             0.01)
      //         .toString() +
      //     "c");
      // print('외부온도 :' +
      //     (ByteData.sublistView(values_readDataResult.value.sublist(12, 14))
      //                 .getInt16(0, Endian.little) *
      //             0.01)
      //         .toStringAsFixed(2) +
      //     "c");
      // print('습도 :' +
      //     (ByteData.sublistView(values_readDataResult.value.sublist(14, 16))
      //                 .getUint16(0, Endian.little) *
      //             0.01)
      //         .toStringAsFixed(2) +
      //     "%");

      //print('is? ' + Uint8List.fromList([1, 0, 0, 0]).toString());

    }
    await _curPeripheral.disconnectOrCancelConnection();
    setState(() {
      message = '측정이 완료되었습니다.';
    });
    // setState(() {
    //   _statusText += 'end';
    // });
  }

  static hex(int c) {
    if (c >= '0'.codeUnitAt(0) && c <= '9'.codeUnitAt(0)) {
      return c - '0'.codeUnitAt(0);
    }
    if (c >= 'A'.codeUnitAt(0) && c <= 'F'.codeUnitAt(0)) {
      return (c - 'A'.codeUnitAt(0)) + 10;
    }
  }

  startQRroutine(String result) async {
    int findIndex = -1;
    for (var i = 0; i < deviceList.length; i++) {
      if (deviceList[i].deviceName == result) {
        findIndex = i;
        break;
      }
    }
    if (findIndex != -1) {
      setState(() {
        processState = 2;
        message = '연결중 입니다.';
      });
      bool result = await connect(findIndex);
      return result;
    } else {
      setState(() {
        message = '디바이스를 찾을 수 없습니다.';
      });
      for (var i = 0; i < deviceList.length; i++) {
        if (deviceList[i].deviceName == result) {
          findIndex = i;
          break;
        }
      }
      if (findIndex != -1) {
        deviceList[findIndex].peripheral.disconnectOrCancelConnection();
      }
    }
  }

  static toUnitList(String str) {
    int length = str.length;
    if (length % 2 != 0) {
      str = "0" + str;
      length++;
    }
    List<int> s = str.toUpperCase().codeUnits;
    Uint8List bArr = Uint8List(length >> 1);
    for (int i = 0; i < length; i += 2) {
      bArr[i >> 1] = ((hex(s[i]) << 4) | hex(s[i + 1]));
    }
    return bArr;
  }

  // BLE 초기화 함수
  void init() async {
    //ble 매니저 생성
    await _bleManager
        .createClient(
            restoreStateIdentifier: "example-restore-state-identifier",
            restoreStateAction: (peripherals) {
              peripherals?.forEach((peripheral) {
                print("Restored peripheral: ${peripheral.name}");
              });
            })
        .catchError((e) => print("Couldn't create BLE client  $e"))
        .then((_) => _checkPermissions()) //매니저 생성되면 권한 확인
        .catchError((e) => print("Permission check error $e"));
  }

  // 권한 확인 함수 권한 없으면 권한 요청 화면 표시, 안드로이드만 상관 있음
  _checkPermissions() async {
    if (Platform.isAndroid) {
      if (await Permission.contacts.request().isGranted) {
        print('입장하냐?');
        //getCurrentLocation();
        scan();
        return;
      }
      Map<Permission, PermissionStatus> statuses =
          await [Permission.location].request();
      print("여기는요?" + statuses[Permission.location].toString());
      if (statuses[Permission.location].toString() ==
          "PermissionStatus.granted") {
        //getCurrentLocation();
        scan();
      }
    }
  }

  //장치 화면에 출력하는 위젯 함수
  list() {
    return ListView.builder(
      itemCount: deviceList.length,
      itemBuilder: (context, index) {
        return ListTile(
            title: Text(deviceList[index].deviceName),
            subtitle: Text(deviceList[index].peripheral.identifier),
            trailing: Text("${deviceList[index].rssi}"),
            onTap: () {
              // itemCount: deviceList.length,
              // itemBuilder: (context, index) () ListView.builder()
              // 처음에 1.. 시작하면 2, connected 3 disconnected 4
              // 리스트중 한개를 탭(터치) 하면 해당 디바이스와 연결을 시도한다.
              bool currentState = false;
              setState(() {
                processState = 2;
              });
              connect(index);
            });
      },
    );
  }

  //scan 함수
  void scan() async {
    if (!_isScanning) {
      deviceList.clear(); //기존 장치 리스트 초기화
      //SCAN 시작
      _bleManager.startPeripheralScan().listen((scanResult) {
        //listen 이벤트 형식으로 장치가 발견되면 해당 루틴을 계속 탐.
        //periphernal.name이 없으면 advertisementData.localName확인 이것도 없다면 unknown으로 표시
        //print(scanResult.peripheral.name);
        var name = scanResult.peripheral.name ??
            scanResult.advertisementData.localName ??
            "Unknown";
        // 기존에 존재하는 장치면 업데이트
        var findDevice = deviceList.any((element) {
          if (element.peripheral.identifier ==
              scanResult.peripheral.identifier) {
            element.peripheral = scanResult.peripheral;
            element.advertisementData = scanResult.advertisementData;
            element.rssi = scanResult.rssi;
            return true;
          }
          return false;
        });
        // 새로 발견된 장치면 추가
        if (!findDevice) {
          if (name != "Unknown") {
            // if (name.substring(0, 3) == 'IOT') {
            deviceList.add(BleDeviceItem(name, scanResult.rssi,
                scanResult.peripheral, scanResult.advertisementData));
            // print(scanResult.peripheral.name +
            //     "의 advertiseData  \n" +
            // }
          }
        }
        //페이지 갱신용
        setState(() {});
      });
      setState(() {
        //BLE 상태가 변경되면 화면도 갱신
        _isScanning = true;
        setBLEState('스캔중');
      });
    } else {
      //스캔중이었으면 스캔 중지
      // TODO: 일단 주석!
      // _bleManager.stopPeripheralScan();
      // setState(() {
      //   //BLE 상태가 변경되면 페이지도 갱신
      //   _isScanning = false;
      //   setBLEState('Stop Scan');
      // });
    }
  }

  //BLE 연결시 예외 처리를 위한 래핑 함수
  _runWithErrorHandling(runFunction) async {
    try {
      await runFunction();
    } on BleError catch (e) {
      print("BleError caught: ${e.errorCode.value} ${e.reason}");
    } catch (e) {
      if (e is Error) {
        debugPrintStack(stackTrace: e.stackTrace);
      }
      print("${e.runtimeType}: $e");
    }
  }

  // 상태 변경하면서 페이지도 갱신하는 함수
  void setBLEState(txt) {
    setState(() => _statusText = txt);
  }

  //연결 함수
  connect(index) async {
    if (_connected) {
      //이미 연결상태면 연결 해제후 종료
      await _curPeripheral?.disconnectOrCancelConnection();
      return;
    }

    //선택한 장치의 peripheral 값을 가져온다.
    Peripheral peripheral = deviceList[index].peripheral;

    //해당 장치와의 연결상태를 관촬하는 리스너 실행
    peripheral
        .observeConnectionState(emitCurrentValue: false)
        .listen((connectionState) {
      // 연결상태가 변경되면 해당 루틴을 탐.
      switch (connectionState) {
        case PeripheralConnectionState.connected:
          {
            //연결됨
            _curPeripheral = peripheral;
            getCurrentLocation();
            //peripheral.
            setBLEState('연결 완료');
            setState(() {
              processState = 3;
            });
            Stream<CharacteristicWithValue> characteristicUpdates;

            // BigInt data = BigInt.parse('0x7E7E0100000000007D7D');

            //final writeData = Util.convertInt2Bytes(data, Endian.little, 10);

            // characteristicUpdates = peripheral.monitorCharacteristic(
            //     '6e400001-b5a3-f393-e0a9-e50e24dcca9e',
            //     '6e400003-b5a3-f393-e0a9-e50e24dcca9e');

            print('결과 ' + characteristicUpdates.toString());

            //데이터 받는 리스너 핸들 변수
            StreamSubscription monitoringStreamSubscription;

            //이미 리스너가 있다면 취소
            //  await monitoringStreamSubscription?.cancel();
            // ?. = 해당객체가 null이면 무시하고 넘어감.

            monitoringStreamSubscription = characteristicUpdates.listen(
              (value) {
                print("read data : ${value.value}"); //데이터 출력
              },
              onError: (error) {
                print("Error while monitoring characteristic \n$error"); //실패시
              },
              cancelOnError: true, //에러 발생시 자동으로 listen 취소
            );
            // peripheral.writeCharacteristic(BLE_SERVICE_UUID, characteristicUuid, value, withResponse)
          }
          break;
        case PeripheralConnectionState.connecting:
          {
            print('연결중입니당!');
            setBLEState('연결 중');
          } //연결중
          break;
        case PeripheralConnectionState.disconnected:
          {
            //해제됨
            _connected = false;
            print("${peripheral.name} has DISCONNECTED");
            setBLEState('연결 종료');
            if (processState == 2) {
              setState(() {
                processState = 4;
              });
            }
            //if (failFlag) {}
          }
          break;
        case PeripheralConnectionState.disconnecting:
          {
            setBLEState('연결 종료중');
          } //해제중
          break;
        default:
          {
            //알수없음...
            print("unkown connection state is: \n $connectionState");
          }
          break;
      }
    });

    _runWithErrorHandling(() async {
      //해당 장치와 이미 연결되어 있는지 확인
      bool isConnected = await peripheral.isConnected();
      if (isConnected) {
        print('device is already connected');
        //이미 연결되어 있기때문에 무시하고 종료..
        return this._connected;
      }

      //연결 시작!
      await peripheral.connect().then((_) {
        this._curPeripheral = peripheral;
        //연결이 되면 장치의 모든 서비스와 캐릭터리스틱을 검색한다.
        peripheral
            .discoverAllServicesAndCharacteristics()
            .then((_) => peripheral.services())
            .then((services) async {
          print("PRINTING SERVICES for ${peripheral.name}");
          //각각의 서비스의 하위 캐릭터리스틱 정보를 디버깅창에 표시한다.
          for (var service in services) {
            print("Found service ${service.uuid}");
            List<Characteristic> characteristics =
                await service.characteristics();
            for (var characteristic in characteristics) {
              print("charUUId: " + "${characteristic.uuid}");
            }
          }
          //모든 과정이 마무리되면 연결되었다고 표시
          _connected = true;
          _isScanning = true;
          if (currentMode == 'Start')
            startBtn();
          else {
            endBtn();
          }
          setState(() {});

          // print(values[''];
          // var values = await peripheral.writeCharacteristic(
          //     '6e400001-b5a3-f393-e0a9-e50e24dcca9e',
          //     '6e400002-b5a3-f393-e0a9-e50e24dcca9e',
          //     toUnitList('7E7E0100000000007D7D'),
          //     false);

          // print("결과222: " +
          //     values.service
          //         .readCharacteristic('6e400003-b5a3-f393-e0a9-e50e24dcca9e')
          //         .toString());
        });
      });
      return _connected;
    });
  }

  TextStyle boldTextStyle = TextStyle(
    fontSize: 30,
    color: Color.fromRGBO(255, 255, 255, 1),
    fontWeight: FontWeight.w700,
  );

  TextStyle thinTextStyle = TextStyle(
    fontSize: 22,
    color: Color.fromRGBO(244, 244, 244, 1),
    fontWeight: FontWeight.w500,
  );
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
        title: 'Orior',
        theme: ThemeData(
          // primarySwatch: Colors.grey,
          primaryColor: Color.fromRGBO(22, 33, 55, 1),
          //canvasColor: Colors.transparent,
        ),
        home: Scaffold(
          appBar: AppBar(
              // backgroundColor: Color.fromARGB(22, 27, 32, 1),
              title: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              Text(
                'Orior',
                textAlign: TextAlign.center,
              ),
            ],
          )),
          body: Center(
            child: Column(
              children: <Widget>[
                Expanded(
                    flex: 1,
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                      children: [
                        Icon(
                          Icons.location_on,
                          color: Colors.black,
                          size: MediaQuery.of(context).size.width * 0.15,
                        ),
                        currentLocation != null ? Text(geolocation) : Text(''),
                      ],
                    )),
                Expanded(
                    flex: 2,
                    child: Container(
                      margin: EdgeInsets.all(
                          MediaQuery.of(context).size.width * 0.035),
                      width: MediaQuery.of(context).size.width * 0.915,
                      // height:
                      //     MediaQuery.of(context).size.width * 0.45,
                      decoration: BoxDecoration(
                          //boxShadow: [customeBoxShadow()],
                          //color: Color.fromRGBO(81, 97, 130, 1),
                          border: Border(
                        top: BorderSide(
                            width: 2, color: Color.fromRGBO(22, 33, 55, 1)),
                        bottom: BorderSide(
                            width: 2, color: Color.fromRGBO(22, 33, 55, 1)),
                      )),
                      child: list(),
                    ) //리스트 출력
                    ),
                Expanded(
                    flex: 2,
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                          children: [
                            Container(
                                width: MediaQuery.of(context).size.width * 0.45,
                                height:
                                    MediaQuery.of(context).size.width * 0.45,
                                decoration: BoxDecoration(
                                  boxShadow: [customeBoxShadow()],
                                  border: Border(),
                                ),
                                child: RaisedButton(
                                  padding: EdgeInsets.all(1),
                                  onPressed: () {},
                                  shape: RoundedRectangleBorder(
                                      borderRadius: BorderRadius.all(
                                          Radius.circular(10))),
                                  color: Color.fromRGBO(22, 33, 55, 1),
                                  child: Container(
                                      child: Column(
                                    mainAxisAlignment:
                                        MainAxisAlignment.spaceEvenly,
                                    children: [
                                      Image(
                                        image: AssetImage(
                                            'images/ic_thermometer.png'),
                                        fit: BoxFit.cover,
                                        width:
                                            MediaQuery.of(context).size.width *
                                                0.20,
                                        height:
                                            MediaQuery.of(context).size.width *
                                                0.20,
                                      ),
                                      currentHumi == ''
                                          ? Text('온도',
                                              style: this.boldTextStyle)
                                          : Text(currentTemp,
                                              style: this.boldTextStyle),
                                    ],
                                  )),
                                )),
                            Container(
                                width: MediaQuery.of(context).size.width * 0.45,
                                height:
                                    MediaQuery.of(context).size.width * 0.45,
                                decoration: BoxDecoration(
                                  boxShadow: [customeBoxShadow()],
                                  border: Border(),
                                ),
                                child: RaisedButton(
                                  padding: EdgeInsets.all(1),
                                  onPressed: () {},
                                  shape: RoundedRectangleBorder(
                                      borderRadius: BorderRadius.all(
                                          Radius.circular(10))),
                                  color: Color.fromRGBO(22, 33, 55, 1),
                                  child: Container(
                                      child: Column(
                                    mainAxisAlignment:
                                        MainAxisAlignment.spaceEvenly,
                                    children: [
                                      Image(
                                        image: AssetImage(
                                            'images/ic_humidity.png'),
                                        fit: BoxFit.cover,
                                        width:
                                            MediaQuery.of(context).size.width *
                                                0.20,
                                        height:
                                            MediaQuery.of(context).size.width *
                                                0.20,
                                      ),
                                      currentHumi == ''
                                          ? Text('습도',
                                              style: this.boldTextStyle)
                                          : Text(currentHumi,
                                              style: this.boldTextStyle),
                                    ],
                                  )),
                                ))
                          ],
                        ),
                      ],
                    )),
                Expanded(
                  flex: 1,
                  child: Container(
                      child: Column(
                    mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                    children: [
                      Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Text(
                            currentDeviceName + ' ' + _statusText,
                            style: startTextStyle,
                          ),
                          processState == 4
                              ? Text(
                                  '연결에 실패했습니다.',
                                  style: startTextStyle,
                                )
                              : Text(message, style: startTextStyle)
                        ],
                      ),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                        children: [
                          Container(
                              width: MediaQuery.of(context).size.width * 0.5,
                              height: MediaQuery.of(context).size.width * 0.15,
                              decoration: BoxDecoration(
                                border: Border(),
                              ),
                              child: RaisedButton(
                                //padding: EdgeInsets.all(1),
                                onPressed: () async {
                                  scan();
                                  currentMode = 'Start';
                                  String checkPermission =
                                      await _checkPermissionCamera();
                                  if (checkPermission == 'Pass') {
                                    String result = await scanner.scan();

                                    if (result != '') {
                                      setState(() {
                                        currentDeviceName = result;
                                      });
                                      bool connectResult =
                                          await startQRroutine(result);
                                    }
                                  }
                                },
                                // shape: RoundedRectangleBorder(
                                //     borderRadius:
                                //         BorderRadius.all(Radius.circular(10))),
                                color: Color.fromRGBO(22, 33, 55, 1),
                                child: Container(
                                  child:
                                      Text('측정 시작', style: this.btnTextStyle),
                                ),
                              )),
                          Container(
                              width: MediaQuery.of(context).size.width * 0.5,
                              height: MediaQuery.of(context).size.width * 0.15,
                              decoration: BoxDecoration(
                                border: Border(),
                              ),
                              child: RaisedButton(
                                //padding: EdgeInsets.all(1),
                                onPressed: () async {
                                  scan();
                                  currentMode = 'End';
                                  String checkPermission =
                                      await _checkPermissionCamera();
                                  if (checkPermission == 'Pass') {
                                    String result = await scanner.scan();
                                    if (result != '') {
                                      setState(() {
                                        currentDeviceName = result;
                                      });
                                      await startQRroutine(result);
                                    }
                                  }
                                },
                                // shape: RoundedRectangleBorder(
                                //     borderRadius:
                                //         BorderRadius.all(Radius.circular(10))),
                                color: Color.fromRGBO(22, 33, 55, 1),
                                child: Container(
                                  child:
                                      Text('측정 종료', style: this.btnTextStyle),
                                ),
                              ))
                        ],
                      ),
                    ],
                  )),
                )
              ],
            ),
          ),
        ));
  }

  BoxShadow customeBoxShadow() {
    return BoxShadow(
        color: Colors.black.withOpacity(0.5),
        offset: Offset(0, 5),
        blurRadius: 6);
  }

  TextStyle startTextStyle = TextStyle(
    fontSize: 19,
    color: Color.fromRGBO(22, 33, 55, 1),
    fontWeight: FontWeight.w600,
  );
  TextStyle btnTextStyle = TextStyle(
    fontSize: 20,
    color: Color.fromRGBO(255, 255, 255, 1),
    fontWeight: FontWeight.w700,
  );

  Uint8List stringToBytes(String source) {
    var list = new List<int>();
    source.runes.forEach((rune) {
      if (rune >= 0x10000) {
        rune -= 0x10000;
        int firstWord = (rune >> 10) + 0xD800;
        list.add(firstWord >> 8);
        list.add(firstWord & 0xFF);
        int secondWord = (rune & 0x3FF) + 0xDC00;
        list.add(secondWord >> 8);
        list.add(secondWord & 0xFF);
      } else {
        list.add(rune >> 8);
        list.add(rune & 0xFF);
      }
    });
    return Uint8List.fromList(list);
  }

  String bytesToString(Uint8List bytes) {
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < bytes.length;) {
      int firstWord = (bytes[i] << 8) + bytes[i + 1];
      if (0xD800 <= firstWord && firstWord <= 0xDBFF) {
        int secondWord = (bytes[i + 2] << 8) + bytes[i + 3];
        buffer.writeCharCode(
            ((firstWord - 0xD800) << 10) + (secondWord - 0xDC00) + 0x10000);
        i += 4;
      } else {
        buffer.writeCharCode(firstWord);
        i += 2;
      }
    }
    return buffer.toString();
  }

  _checkPermissionCamera() async {
    if (Platform.isAndroid) {
      if (await Permission.contacts.request().isGranted) {
        print('입장하냐?');
        //scan();
        return '';
      }
      Map<Permission, PermissionStatus> statuses =
          await [Permission.camera, Permission.storage].request();
      //print("여기는요?" + statuses[Permission.location].toString());
      if (statuses[Permission.camera].toString() ==
              "PermissionStatus.granted" &&
          statuses[Permission.storage].toString() ==
              'PermissionStatus.granted') {
        return 'Pass';
      }
    }
  }

  getCurrentLocation() async {
    bool _serviceEnabled;
    loc.PermissionStatus _permissionGranted;
    loc.LocationData _locationData;

    _serviceEnabled = await location.serviceEnabled();
    if (!_serviceEnabled) {
      _serviceEnabled = await location.requestService();
      if (!_serviceEnabled) {
        return;
      }
    }
    print('서비스는사용가능? ');
    _permissionGranted = await location.hasPermission();
    if (_permissionGranted == loc.PermissionStatus.denied) {
      _permissionGranted = await location.requestPermission();
      if (_permissionGranted != loc.PermissionStatus.granted) {
        return;
      }
    }
    print('위치받는중? ');
    _locationData = await location.getLocation();
    print('lat: ' + _locationData.latitude.toString());
    setState(() {
      currentLocation = _locationData;
    });
  }
}
