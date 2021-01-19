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
  String message = '-';
  Peripheral _curPeripheral; // 연결된 장치 변수
  List<BleDeviceItem> deviceList = []; // BLE 장치 리스트 변수
  String _statusText = ''; // BLE 상태 변수
  loc.LocationData currentLocation;
  int dataSize = 0;
  loc.Location location = new loc.Location();

  @override
  void initState() {
    getCurrentLocation();
    init();
    location.onLocationChanged.listen((loc.LocationData currentLocation) {
      //print('여긴오냐' + '');
      this.currentLocation = currentLocation;
      // Use current location
    });
    super.initState();
  }

  Future<Post> sendTest(Data data) async {
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

  Future<Post> sendPost() async {
    final response = await http.post(
      'http://175.126.232.236/_API/saveData.php',
      body: jsonEncode(
        {
          "isRegularData": "true",
          "tra_datetime": new DateTime.now().toString(),
          "tra_temp": "22",
          "tra_humidity": "28",
          "tra_lat": "36.1523523",
          "tra_lon": "127.5339587",
          "de_number": "OPBT102814",
          "tra_battery": "3.52"
        },
      ),
      headers: {'content-type': "application/json"},
    );
    var responseResult;
    print(response.statusCode.toString());
    if (response.statusCode == 200) {
      // 만약 서버가 OK 응답을 반환하면, JSON을 파싱합니다.
      //print(utf8.decode(response.bodyBytes));

      responseResult = utf8.decode(response.bodyBytes);
      print(responseResult.toString());
      return Post.fromJson(jsonDecode(response.body));
    } else {
      responseResult = utf8.decode(response.bodyBytes);
      print("error code : " + response.statusCode.toString());

      //401 : 아이디 패스워드 일치 x
      //400 : 빈 공간.
      // 만약 응답이 OK가 아니면, 에러를 던집니다.
      //showAlertDialog(context);
    }
  }

  void startBtn() async {
    if (_curPeripheral.name != null) {
      setState(() {
        message = '측정 시작을 위한 세팅중입니다.';
      });
      dataSize = 0;
      var values_tempreadDataResult = await _curPeripheral.readCharacteristic(
          'a2fac1f4-dd28-4e5a-ac2d-4d6e2cb410f9',
          '65ac0f6d-3a8c-4592-a20d-d1bd373d2f64');
      print('온도:' + values_tempreadDataResult.value.toString());

      // 데이터 삭제 (검증 완료)
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
      var values_setTempData = await _curPeripheral.writeCharacteristic(
          'cf08a8c2-485a-4e18-8fe9-018700449787',
          '51186f4c-c8f9-4c67-ac1f-a33ba77eb76b',
          Uint8List.fromList([1]),
          false);

      //  데이터 읽기
      //  인덱스 설정

      //  print(Uint8List.fromList([2, 0]));
      // 측정시작 명령어
      await _curPeripheral.writeCharacteristic(
          'cf08a8c2-485a-4e18-8fe9-018700449787',
          '705ec125-966d-4275-bd80-a1b2a8bc28d6',
          Uint8List.fromList([1]),
          false);

      // //설정 종료
      await _curPeripheral.writeCharacteristic(
          'cf08a8c2-485a-4e18-8fe9-018700449787',
          'e371f072-24aa-40a2-9af8-1b030c0f2acb',
          Uint8List.fromList([1]),
          false);

      setState(() {
        message = '측정중';
      });
    }
  }

  void endBtn() async {
    // sendTest();
    // 측정종료 명령어
    setState(() {
      message = '데이터를 읽어오고 있습니다.';
    });
    await _curPeripheral.writeCharacteristic(
        'cf08a8c2-485a-4e18-8fe9-018700449787',
        '705ec125-966d-4275-bd80-a1b2a8bc28d6',
        Uint8List.fromList([0]),
        false);

    var values_readDatasize = await _curPeripheral.readCharacteristic(
        '66ee2010-bc01-4e93-8a06-c3d7af72dec3',
        '785b15a7-b67c-4e72-81a4-9904f73ef357');

    var blob = ByteData.sublistView(values_readDatasize.value);
    dataSize = blob.getUint16(0, Endian.little);
    setState(() {
      message = '데이터 전송중';
    });
    for (var i = 0; i < dataSize; i++) {
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
          battery: (ByteData.sublistView(values_readDataResult.value.sublist(2, 4)).getUint16(0, Endian.little) * 0.01)
              .toString(),
          humi: (ByteData.sublistView(values_readDataResult.value.sublist(14, 16))
                      .getUint16(0, Endian.little) *
                  0.01)
              .toStringAsFixed(2),
          temper: (ByteData.sublistView(values_readDataResult.value.sublist(12, 14))
                      .getUint16(0, Endian.little) *
                  0.01)
              .toStringAsFixed(2),
          lat: currentLocation.latitude.toString(),
          lng: currentLocation.longitude.toString(),
          lex: (ByteData.sublistView(values_readDataResult.value.sublist(22, 24))
                      .getUint16(0, Endian.little) *
                  0.01)
              .toString(),
          time: datatime.toString());

      sendTest(currentData);
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
              // 리스트중 한개를 탭(터치) 하면 해당 디바이스와 연결을 시도한다.
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
            deviceList.add(BleDeviceItem(name, scanResult.rssi,
                scanResult.peripheral, scanResult.advertisementData));
            // print(scanResult.peripheral.name +
            //     "의 advertiseData  \n" +

          }
        }
        //페이지 갱신용
        setState(() {});
      });
      setState(() {
        //BLE 상태가 변경되면 화면도 갱신
        _isScanning = true;
        setBLEState('Scanning');
      });
    } else {
      //스켄중이었으면 스캔 중지
      _bleManager.stopPeripheralScan();
      setState(() {
        //BLE 상태가 변경되면 페이지도 갱신
        _isScanning = false;
        setBLEState('Stop Scan');
      });
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
            //peripheral.
            setBLEState('connected');
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
            setBLEState('connecting');
          } //연결중
          break;
        case PeripheralConnectionState.disconnected:
          {
            //해제됨
            _connected = false;
            print("${peripheral.name} has DISCONNECTED");
            setBLEState('disconnected');
          }
          break;
        case PeripheralConnectionState.disconnecting:
          {
            setBLEState('disconnecting');
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
        return;
      }

      //연결 시작!
      await peripheral.connect().then((_) {
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
    });
  }

  TextStyle boldTextStyle = TextStyle(
    fontSize: 17,
    color: Color.fromRGBO(255, 255, 255, 1),
    fontWeight: FontWeight.w700,
  );

  TextStyle thinTextStyle = TextStyle(
    fontSize: 15,
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
                  flex: 6,
                  child: list(), //리스트 출력
                ),
                Expanded(
                  flex: 1,
                  child: Container(
                    child: Row(
                      children: <Widget>[
                        RaisedButton(
                          //scan 버튼
                          onPressed: scan,
                          child: Icon(_isScanning
                              ? Icons.stop
                              : Icons.bluetooth_searching),
                        ),
                        SizedBox(
                          width: 10,
                        ),
                        Text("State : "), Text(_statusText), //상태 정보 표시
                      ],
                    ),
                  ),
                ),
                Expanded(
                  flex: 1,
                  child: Container(
                      child: Column(
                    children: [
                      Text(message),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                        children: [
                          Container(
                              width: MediaQuery.of(context).size.width * 0.35,
                              decoration: BoxDecoration(
                                border: Border(),
                              ),
                              child: RaisedButton(
                                padding: EdgeInsets.all(1),
                                onPressed: () {
                                  startBtn();
                                },
                                shape: RoundedRectangleBorder(
                                    borderRadius:
                                        BorderRadius.all(Radius.circular(10))),
                                color: Color.fromRGBO(22, 33, 55, 1),
                                child: Container(
                                  child:
                                      Text('측정 시작', style: this.startTextStyle),
                                ),
                              )),
                          Container(
                              width: MediaQuery.of(context).size.width * 0.35,
                              decoration: BoxDecoration(
                                border: Border(),
                              ),
                              child: RaisedButton(
                                padding: EdgeInsets.all(1),
                                onPressed: () {
                                  getCurrentLocation();
                                  endBtn();
                                },
                                shape: RoundedRectangleBorder(
                                    borderRadius:
                                        BorderRadius.all(Radius.circular(10))),
                                color: Color.fromRGBO(22, 33, 55, 1),
                                child: Container(
                                  child:
                                      Text('측정 종료', style: this.startTextStyle),
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
    fontSize: 17,
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

    _permissionGranted = await location.hasPermission();
    if (_permissionGranted == loc.PermissionStatus.denied) {
      _permissionGranted = await location.requestPermission();
      if (_permissionGranted != loc.PermissionStatus.granted) {
        return;
      }
    }

    _locationData = await location.getLocation();
    setState(() {
      currentLocation = _locationData;
    });
  }
}
