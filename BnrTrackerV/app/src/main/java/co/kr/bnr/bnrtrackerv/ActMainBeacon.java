package co.kr.bnr.bnrtrackerv;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import org.altbeacon.beacon.BeaconManager;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import co.kr.bnr.bnrtrackerv.data.Common;
import co.kr.bnr.bnrtrackerv.util.Api;
import co.kr.bnr.bnrtrackerv.util.CommonUtil;
import co.kr.bnr.bnrtrackerv.util.Util;
import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.location.providers.LocationGooglePlayServicesProvider;
import issc.Bluebit;
import issc.data.DeviceInfo;
import issc.gatt.Gatt;
import issc.gatt.GattCharacteristic;
import issc.gatt.GattDescriptor;
import issc.gatt.GattService;
import issc.impl.GattTransaction;
import issc.impl.LeService;
import issc.util.Log;
import issc.util.UuidMatcher;

public class ActMainBeacon extends AppCompatActivity {
    Context context;
    private static final int REQUEST_ENABLE_BT = 1;

    private final static String INFO_CONTENT = "the_information_body";
    private final static String RCV_ENABLED = "could_receive_data_if_enabled";

    private final static int PAYLOAD_MAX = 20; // 90 bytes might be max

    private final static int LAUNCH_FUNCTION = 0x101;
    private final static int DISCOVERY_DIALOG = 1;
    private final static int CONNECT_DIALOG   = 2;

    private ProgressDialog mDiscoveringDialog;
    private ProgressDialog mConnectDialog;



    private int mSuccess = 0;
    private int mFail    = 0;

    private Calendar mStartTime;

    @Bind(R.id.recyclerView) RecyclerView recyclerView;
    @Bind(R.id.tvLatitude) TextView tvLatitude;
    @Bind(R.id.tvLongitude) TextView tvLongitude;
    @Bind(R.id.tvAddress) TextView tvAddress;

    ArrayList<DeviceInfo> deviceInfoList = new ArrayList<>();

    AdapterConnectedDevice adapter;
    LinearLayoutManager linearLayoutManager;

    HashMap<String, String> lastSendTimeMap = new HashMap<>();

    //GPSTracker gps;
    private LocationGooglePlayServicesProvider provider;
    public static final int RESULTCODE_SET_GPS = 1;
    String lat ="", lon ="";

    //비콘용
    private BluetoothAdapter mBluetoothAdapter;

    void initScanning() {
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        }
        else {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, final byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //있는거만 넣는다
                    DeviceInfo selectedDevice = getDevice(device.getName());

                    if(selectedDevice == null) {
                        //CommonUtil.myLog(deviceInfoList.size() + ": " + "없자나: " + device.getName() + "------" + CommonUtil.byteArrayToHexString(scanRecord));
                        return;
                    }

                    String currentTime = Util.makeStringFromCalendar(Calendar.getInstance(), "yyyy-MM-dd HH:mm");

                    if(!lastSendTimeMap.containsKey(device.getName()) || !lastSendTimeMap.get(device.getName()).equals(currentTime)) {
                        lastSendTimeMap.put(device.getName(), Util.makeStringFromCalendar(Calendar.getInstance(), "yyyy-MM-dd HH:mm"));
                        extractDataFromRawData(CommonUtil.byteArrayToHexString(scanRecord), device);
                    }
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_main_list);
        ButterKnife.bind(this);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        context = this;

        Intent launch = new Intent(this, LeService.class);
        launch.putExtra(Bluebit.USE_FAKE, false);
        startService(launch);

        initRecyclerView();

        initScanning();
        scanLeDevice(true);

        CommonUtil.myLog("시간: " + CommonUtil.getDateTimeToHexa());
        CommonUtil.myLog("새함수: " + CommonUtil.byteArrayToHexString(CommonUtil.hexStringToByteArray(CommonUtil.getDateTimeToHexa())));
    }

    public void onEnableClicked(View view) {
        BeaconReferenceApplication application = ((BeaconReferenceApplication) this.getApplicationContext());
        if (BeaconManager.getInstanceForApplication(this).getMonitoredRegions().size() > 0) {
            application.disableMonitoring();
            ((Button)findViewById(R.id.enableButton)).setText("Re-Enable Monitoring");
        }
        else {
            ((Button)findViewById(R.id.enableButton)).setText("Disable Monitoring");
            application.enableMonitoring();
        }
    }

    public void updateLog(final String log) {
        runOnUiThread(new Runnable() {
            public void run() {
                CommonUtil.myLog("로그:" + log);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    //들어오자마자 연결된거 없으면 바로 보여준다
    @Override
    protected void onPostResume() {
        super.onPostResume();

        initLocationTracking(this);
    }

    //출발지 세팅
    void initLocationTracking(Context context){
        if(!SmartLocation.with(context).location().state().locationServicesEnabled() || !SmartLocation.with(context).location().state().isAnyProviderAvailable()){
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);

            alertDialog.setTitle("GPS 사용유무셋팅");
            alertDialog.setMessage("GPS가 꺼져있으면 위치를 받아올 수 없습니다.\n 설정창으로 가시겠습니까?");

            // OK 를 누르게 되면 설정창으로 이동합니다.
            alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,int which) {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(intent, RESULTCODE_SET_GPS);
                }
            });
            // Cancel 하면 종료 합니다.
            alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    finish();
                }
            });

            alertDialog.show();
        }
        else {
            Util.myLog("모든 권한 통과");
            startLocation();

            if(deviceInfoList.size() <= 0) {
                showFindDevice();
            }
        }
    }

    //116 278
    private void startLocation() {
        //롤리팝에서 LocationGooglePlayServicesProvider를 쓰면 onLocationUpdated가 호출이 안되는 이슈가있다
        provider = new LocationGooglePlayServicesProvider();
        provider.setCheckLocationSettings(true);

        SmartLocation smartLocation = new SmartLocation.Builder(getApplicationContext()).logging(true).build();
        smartLocation.location(provider).start(new OnLocationUpdatedListener() {
            @Override
            public void onLocationUpdated(Location location) {
                lat = String.valueOf(location.getLatitude());
                lon = String.valueOf(location.getLongitude());

                tvLatitude.setText("위도: " + lat);
                tvLongitude.setText("경도: " + lon);

                getAddress();
            }
        });
    }

    void initRecyclerView() {
        linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setNestedScrollingEnabled(false);//스크롤뷰안에 리싸이클러뷰가 버벅일때 해결법
        adapter = new AdapterConnectedDevice(this, deviceInfoList);
        recyclerView.setAdapter(adapter);
//        makeDataObject("7E 7E 01 B0 11 00 00 00 80 0B CC 0D 00 BC 67 BA 5D 00 00 00 00 00 00 00 00 7D 7D", "7E 7E 01 B0 11 00 00 00 80 0B CC 0D 00 BC 67 BA 5D 00 00 00 00 00 00 00 00 7D 7D".split(" "));
//        makeDataObject("7E 7E 01 B0 11 00 00 00 80 0B CC 0D 00 BC 67 BA 5D 00 00 00 00 00 00 00 00 7D 7D", "7E 7E 01 B0 11 00 00 00 80 0B CC 0D 00 BC 67 BA 5D 00 00 00 00 00 00 00 00 7D 7D".split(" "));
//        makeDataObject("7E 7E 01 B0 11 00 00 00 80 0B CC 0D 00 BC 67 BA 5D 00 00 00 00 00 00 00 00 7D 7D", "7E 7E 01 B0 11 00 00 00 80 0B CC 0D 00 BC 67 BA 5D 00 00 00 00 00 00 00 00 7D 7D".split(" "));
//        makeDataObject("7E 7E 01 B0 11 00 00 00 80 0B CC 0D 00 BC 67 BA 5D 00 00 00 00 00 00 00 00 7D 7D", "7E 7E 01 B0 11 00 00 00 80 0B CC 0D 00 BC 67 BA 5D 00 00 00 00 00 00 00 00 7D 7D".split(" "));
//        makeDataObject("7E 7E 01 B0 11 00 00 00 80 0B CC 0D 00 BC 67 BA 5D 00 00 00 00 00 00 00 00 7D 7D", "7E 7E 01 B0 11 00 00 00 80 0B CC 0D 00 BC 67 BA 5D 00 00 00 00 00 00 00 00 7D 7D".split(" "));
    }
    //QRCode Scanner
    @OnClick(R.id.btnQRcode)
    void onClickAddDevice2(View view) {
        showFindDevice2();
    }

    //BLE스캔으로 찾은 디바이스를 다이얼로그로 띄워준다
    public void showFindDevice2() {
        DialBleQRscan.newInstance("hello","message", new DialBleQRscan.MessageDialogListener() {
            @Override
            public void onDialogPositiveClick(DialogFragment dialog) {
                System.out.println("hello");
            }
        });
//                .setListener(deviceInfoList, new DialBleQRscan.Listener() {
//            @Override
//            public void onDeviceSelect(BluetoothDevice device) {
//                DeviceInfo deviceInfo = new DeviceInfo(context);
//                CommonUtil.myLog("device함수내부: " + device.getName());
//                deviceInfo.mDevice = device;
//                deviceInfo.mConn = new SrvConnection(deviceInfo.mDevice);
//                bindService(new Intent(context, LeService.class), deviceInfo.mConn, 0);
//                deviceInfoList.add(deviceInfo);
//
//                adapter.notifyDataSetChanged();
//            }
//        }).showDialog(getSupportFragmentManager());
    }

    //기기추가
    @OnClick(R.id.btnAddDevice)
    void onClickAddDevice(View view) {
        showFindDevice();
    }

    //BLE스캔으로 찾은 디바이스를 다이얼로그로 띄워준다
    public void showFindDevice() {

        DialBleScan.newInstance().setListener(deviceInfoList, new DialBleScan.Listener() {
            @Override
            public void onDeviceSelect(BluetoothDevice device) {
                //device : F8:B4 ...
                DeviceInfo deviceInfo = new DeviceInfo(context);
                deviceInfo.mDevice = device;
                deviceInfo.mConn = new SrvConnection(deviceInfo.mDevice);
                bindService(new Intent(context, LeService.class), deviceInfo.mConn, 0);
                deviceInfoList.add(deviceInfo);
                CommonUtil.myLog("디바이스 골랐다1: " + device);


                adapter.notifyDataSetChanged();
            }
        }).showDialog(getSupportFragmentManager());
    }

    public void sendCommand(String command, BluetoothDevice mDevice) {
        if(mDevice == null) {
            CommonUtil.myLog("sendCommand : 디바이스가 널이네?");
            return;
        }
        CommonUtil.myLog("sendCommand : 디바이스가 널아니다!!" + mDevice.getName());
        //CharSequence cs = "0271100000008103";
        msgShow("send", command, mDevice);
        write(command, mDevice);//TODO제거해
    }

    private void onConnected(BluetoothDevice mDevice) {
        if(mDevice == null) {
            //showFindDevice();
            CommonUtil.myLog("onConnected : 디바이스가 널이네?");
            return;
        }

        List<GattService> list = null;

        DeviceInfo selectedDevice = getDevice(mDevice.getName());
        if(selectedDevice == null) {CommonUtil.myLog("onConnected : 디바이스가 널이네?"); return;}
        selectedDevice.mService.getServices(mDevice);
        selectedDevice.isBeaconMode = false;
        selectedDevice.isDeviceConnected = true;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });
        //setButtonStatus(false, true, tvConnectStatus1, tvStart1, mDevice1);

        if ((list == null) || (list.size() == 0)) {
            selectedDevice.mService.discoverServices(mDevice);
        } else {
            onDiscovered(mDevice);
        }
    }

    private void onDiscovered(BluetoothDevice mDevice) {
        if(mDevice == null) {
            CommonUtil.myLog("onDiscovered : 없어디바이스");
            //showFindDevice();
            return;
        }
        CommonUtil.myLog("onDiscovered : onDiscovered");
        //updateView(DISMISS_CONNECTION_DIALOG, null);
        CommonUtil.myLog("DISMISS_CONNECTION_DIALOG, null");
        //GattService proprietary = null;
//        for(GattService service : mService.getServices(mDevice) ) {
//            CommonUtil.myLog("확인: " + service.getUuid());
//        }

        DeviceInfo selectedDevice = getDevice(mDevice.getName());
        if(selectedDevice == null) {CommonUtil.myLog("onDiscovered : 디바이스가 널이네?"); return;}

        GattService proprietary = selectedDevice.mService.getService(mDevice, UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e"));
        selectedDevice.mService.getService(mDevice, UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e"));
        selectedDevice.mTransTx = proprietary.getCharacteristic(UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e"));
        selectedDevice.mTransRx = proprietary.getCharacteristic(UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e"));
        CommonUtil.myLog(String.format("found Tx:%b, Rx:%b", selectedDevice.mTransTx != null, selectedDevice.mTransRx != null));
        for(GattDescriptor descriptor : selectedDevice.mTransTx.getDescriptors()) {
            //mService.readDescriptor(descriptor);
            CommonUtil.myLog("디스크립터: " + descriptor.getUuid() + "/ " + descriptor.getValue());
        }

        if (selectedDevice.mService != null) {
            List<GattService> srvs = selectedDevice.mService.getServices(mDevice);
            CommonUtil.myLog("discovered result:" + srvs.size());
            Iterator<GattService> it = srvs.iterator();
            while (it.hasNext()) {
                GattService s = it.next();
                appendService(selectedDevice.mService, selectedDevice.mTransTx, s);
            }
        }
        enableNotification(selectedDevice.mService, selectedDevice.mTransTx);

    }

    class GattListener extends Gatt.ListenerHelper {
        BluetoothDevice mDevice;
        GattListener(BluetoothDevice mDevice) {
            super("ActMain");
            this.mDevice = mDevice;
        }

        @Override
        public void onServicesDiscovered(Gatt gatt, int status) {
            dismissDiscovery();
            onDiscovered(mDevice);
        }

        @Override
        public void onConnectionStateChange(Gatt gatt, int status, int newState) {
            CommonUtil.myLog(": mDevice: " + mDevice + ": newState: " + newState);
            //이거 콜백이라서 없으면 안되는 부분이다
            if(mDevice == null) {
                CommonUtil.myLog("There is no Gatt to be used, skip1");
                return;
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                dismissConnect();
                CommonUtil.myLog("connected to device, start discovery");
                displayDiscovering();

                onConnected(mDevice);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                CommonUtil.myLog("connection state changed to disconnected in function picker");
                onDisconnected(mDevice);
                mDevice = null;
            }
        }

        @Override
        public void onCharacteristicRead(Gatt gatt, GattCharacteristic charac, int status) {
            CommonUtil.myLog("리드드드드 : "+charac);//여기가 진짜다
            byte[] value = charac.getValue();
            for (int i = 0; i < value.length; i++) {
                CommonUtil.myLog("[" + i + "]" + Byte.toString(value[i]));
            }

            DeviceInfo selectedDevice = getDevice(mDevice.getName());
            if(selectedDevice == null) {CommonUtil.myLog("onDiscovered : 디바이스가 널이네?"); return;}

            selectedDevice.mQueue.onConsumed();
        }

        @Override
        public void onCharacteristicChanged(Gatt gatt, GattCharacteristic chrc) {
            CommonUtil.myLog("on chr changed : " + mDevice );
//            if (chrc.getUuid().equals(Bluebit.CHR_ISSC_TRANS_TX)) {
//                onReceived(chrc.getValue(), mDevice);
//            }
        }

        @Override
        public void onCharacteristicWrite(Gatt gatt, GattCharacteristic charac, int status) {
            if (status == Gatt.GATT_SUCCESS) {
                mSuccess +=charac.getValue().length;
            } else {
                mFail += charac.getValue().length;
            }

            String s = String.format("%d bytes, success= %d, fail= %d",
                    charac.getValue().length,
                    mSuccess,
                    mFail);

            if(mDevice == null) {Util.myLog("onCharacteristicWrite 끝");return;}
            Util.myLog("--------onCharacteristicWrite: " + s + "/" + mDevice);
            msgShow("wrote", s, mDevice);

            DeviceInfo selectedDevice = getDevice(mDevice.getName());
            if(selectedDevice == null) {CommonUtil.myLog("onCharacteristicWrite : 디바이스가 널이네?"); return;}

            selectedDevice.mQueue.onConsumed();

            if (selectedDevice.mQueue.size() == 0 && mStartTime != null) {
                long elapse =  Calendar.getInstance().getTimeInMillis() - mStartTime.getTimeInMillis();
                msgShow("time", "spent " + (elapse / 1000) + " seconds", mDevice);
                mStartTime = null;
            }
            selectedDevice.mService.readCharacteristic(selectedDevice.mTransTx);

            //updateView(CONSUME_TRANSACTION, null, mDevice1);
            CommonUtil.myLog("CONSUME_TRANSACTION");
        }

        @Override
        public void onDescriptorWrite(Gatt gatt, GattDescriptor dsc, int status) {
            if (status == Gatt.GATT_SUCCESS) {
                byte[] value = dsc.getValue();
                if (Arrays.equals(value, dsc.getConstantBytes(GattDescriptor.ENABLE_NOTIFICATION_VALUE))) {
                    Bundle state = new Bundle();
                    state.putBoolean(RCV_ENABLED, true);

                    //듣기 허용한 다음 시간 세팅해준다
                    sendCommand(Common.commandSetCurrentTime, mDevice);
                    sendCommand(Common.commandSetLoggingMode, mDevice);

                    //updateView(RCV_STATE, state);
                    CommonUtil.myLog("RCV_STATE1 : "+state + "/ " + CommonUtil.byteArrayToHexString(value));
                } else if (Arrays.equals(value, dsc.getConstantBytes(GattDescriptor.DISABLE_NOTIFICATION_VALUE))) {
                    Bundle state = new Bundle();
                    state.putBoolean(RCV_ENABLED, false);
                    //updateView(RCV_STATE, state);
                    CommonUtil.myLog("RCV_STATE2 : "+state + "/ " +  CommonUtil.byteArrayToHexString(value));
                }
//                //시간 세팅하고왔다
//                else if (CommonUtil.byteArrayToHexString(value).equals("0100")) {
//                    CommonUtil.myLog("시간 세팅하고왔다 RCV_STATE3 : " + CommonUtil.byteArrayToHexString(value));
//                    sendCommand(Common.commandSetLoggingMode, mDevice);
//                }
//                //로깅 세팅하고왔다
//                else if (CommonUtil.byteArrayToHexString(value).equals(Common.commandSetCurrentTime)) {
//                    CommonUtil.myLog("로깅 세팅하고왔다 RCV_STATE4 : " + CommonUtil.byteArrayToHexString(value));
//                    sendCommand(Common.commandSetLoggingMode, mDevice);
//                }
                else {
                    CommonUtil.myLog("RCV_STATE5 : ");
                }
            }
        }
    }

    /**
     * Received data from remote when enabling Echo.
     *
     * Display the data and transfer back to device.
     */
    private void onReceived(byte[] data, BluetoothDevice mDevice) {
        CommonUtil.myLog("리시브드 : "+CommonUtil.bytesToHex(data));//여기가 진짜다z
        Bundle msg = new Bundle();
        msg.putCharSequence(INFO_CONTENT, CommonUtil.bytesToHex(data));

        extractDataFromRawData(String.valueOf(CommonUtil.bytesToHex(data)), mDevice);

        //updateView(APPEND_MESSAGE, msg, mDevice);
        CommonUtil.myLog("APPEND_MESSAGE : "+msg);//여기가 진짜다
    }

    private void msgShow(CharSequence prefix, CharSequence cs, BluetoothDevice mDevice) {
        StringBuffer sb = new StringBuffer();
        sb.append(prefix);
        sb.append(": ");
        sb.append(cs);
        //CommonUtil.myLog(sb.toString());
        Bundle msg = new Bundle();
        msg.putCharSequence(INFO_CONTENT, sb.toString());

        //extractDataFromRawData(sb.toString(), mDevice);
        //updateView(APPEND_MESSAGE, msg, mDevice);
        //CommonUtil.myLog("APPEND_MESSAGE : "+msg);
    }

    /**
     * Write string to remote device.
     */
    private void write(CharSequence cs, BluetoothDevice mDevice) {
        byte[] bytes = new java.math.BigInteger(cs.toString(), 16).toByteArray();
        write(bytes, mDevice);
    }

    /**
     * Write data to remote device.
     */
    private void write(byte[] bytes, BluetoothDevice mDevice) {
        if(mDevice == null) {
            return;
        }
        ByteBuffer buf = ByteBuffer.allocate(bytes.length);
        buf.put(bytes);
        buf.position(0);
        while(buf.remaining() != 0) {
            int size = (buf.remaining() > PAYLOAD_MAX) ? PAYLOAD_MAX: buf.remaining();
            byte[] dst = new byte[size];
            buf.get(dst, 0, size);

            DeviceInfo selectedDevice = getDevice(mDevice.getName());
            if(selectedDevice == null) {CommonUtil.myLog("onCharacteristicWrite : 디바이스가 널이네?"); return;}

            selectedDevice.transaction = new GattTransaction(selectedDevice.mTransRx, dst);
            selectedDevice.mQueue.add(selectedDevice.transaction);
            CommonUtil.myLog("하하1 :"+selectedDevice.mTransRx+"/"+selectedDevice.transaction + " / " + CommonUtil.byteArrayToHexString(bytes));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // this activity is invisible, remove listener
//        if(mService != null) {
//            mService.rmListener(mListener);
//            mService = null;
//        }
//        unbindService(mConn);
        stopService(new Intent(this, LeService.class));
    }

//    @Override
//    protected void onPause() {
//        super.onPause();
//        // this activity is invisible, remove listener
//        if(mService != null) {
//            mService.rmListener(mListener);
//            mService = null;
//        }
//        unbindService(mConn);
//    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == LAUNCH_FUNCTION) {
            if (resultCode == Bluebit.RESULT_REMOTE_DISCONNECT) {
                CommonUtil.myLog("function picker found remote disconnect, closing");
                //onDisconnected();
            }
        }
        else if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            CommonUtil.myToast(context, getString(R.string.bleUnauthorized));
            return;
        }
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        if (id == DISCOVERY_DIALOG) {
            mDiscoveringDialog = new ProgressDialog(this);
            mDiscoveringDialog.setMessage(this.getString(R.string.discovering));
            mDiscoveringDialog.setOnCancelListener(new Dialog.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                }
            });
            return mDiscoveringDialog;
        } else if (id == CONNECT_DIALOG) {
            mConnectDialog = new ProgressDialog(this);
            mConnectDialog.setMessage(this.getString(R.string.connecting));
            mConnectDialog.setOnCancelListener(new Dialog.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    stopConnect();
                }
            });
            return mConnectDialog;
        }
        return null;
    }

    private void onDisconnected(BluetoothDevice mDevice) {
        CommonUtil.myLog("transparent activity disconnected, closing");
        mStartTime = null;
        LeService mService = null;

        if(mDevice == null) {
            return;
        }

        DeviceInfo selectedDevice = getDevice(mDevice.getName());
        if(selectedDevice == null || selectedDevice.mService == null) {CommonUtil.myLog("onCharacteristicWrite : 디바이스가 널이네?"); return;}

        selectedDevice.mQueue.clear();
        mService = selectedDevice.mService;

        mService.disconnect(mDevice);
        mService.closeGatt(mDevice);

        this.setResult(Bluebit.RESULT_REMOTE_DISCONNECT);

        //isDeviceConnected = false;


        if(mConnectDialog != null && mConnectDialog.isShowing()) {
            mConnectDialog.dismiss();
        }

//        if(fragRemote != null && fragRemote.isAdded())
//            fragRemote.changeBtState();
//        if(fragInfo != null && fragInfo.isAdded())
//            fragInfo.changeBtState();
    }

    private void connectToDevice(BluetoothDevice mDevice) {
        LeService mService = null;
        if(mDevice == null) {
            return;
        }

        DeviceInfo selectedDevice = getDevice(mDevice.getName());
        if(selectedDevice == null) {CommonUtil.myLog("connectToDevice : 디바이스가 널이네?"); return;}

        mService = selectedDevice.mService;
        CommonUtil.myLog("connectToDevice 1");

        //if(mDevice1 != null && mDevice1 != null) {
        selectedDevice.mService.connectGatt(this, false, mDevice);
            if (selectedDevice.mService.getConnectionState(mDevice) == BluetoothProfile.STATE_CONNECTED) {
                CommonUtil.myLog("already connected to device");
                List<GattService> list = selectedDevice.mService.getServices(selectedDevice.mDevice);
                if ((list == null) || (list.size() == 0)) {
                    displayDiscovering();
                    CommonUtil.myLog("start discovering services");
                    selectedDevice.mService.discoverServices(selectedDevice.mDevice);
                } else {
                    onDiscovered(selectedDevice.mDevice);
                }
            } else {
                displayConnecting();
                boolean init = selectedDevice.mService.connect(selectedDevice.mDevice, false);
                CommonUtil.myLog("Try to connec to device, successfully? " + init + "/" + (mService == selectedDevice.mService));
            }
    }

    /**
     * Add found GattService to Adapter to decide what functions
     * does this bluetooth device support.
     */
    private void appendService(LeService mService, GattCharacteristic mTransTx, GattService srv) {
        UuidMatcher matcher = new UuidMatcher();
        matcher.setTarget("co.kr.bnr.bnrtrackerv", "co.kr.bnr.bnrtrackerv.ActMain");
        for (int i = 0; i < Bluebit.UUIDS_OF_TRANSPARENT.length; i++) {
            matcher.addRule(Bluebit.UUIDS_OF_TRANSPARENT[i]);
        }
        matcher.setInfo("Transparent", "Transfer data to device");

        ArrayList<UUID> arrUUID = new ArrayList<>();
        CommonUtil.myLog("append Service:" + srv.getUuid().toString());
        //appendUuid(srv.getUuid());
        arrUUID.add(srv.getUuid());
        if (matcher.contains(srv.getUuid())) {
//            Intent intent = new Intent(this, ActivityTransparent.class);
//            intent.putExtra(Bluebit.CHOSEN_DEVICE, mDevice);
//            startActivityForResult(intent, LAUNCH_FUNCTION);
            //btnCommand.setEnabled(true);

//            fragRemote.changeBtState();
//
//            if(fragInfo != null && fragInfo.isAdded())
//                fragInfo.changeBtState();
            //isDeviceConnected = true;
            enableNotification(mService, mTransTx);
        }
    }

    private void stopConnect() {
        for(DeviceInfo deviceInfo : deviceInfoList) {
            if(deviceInfo != null && deviceInfo.mService != null) {
                deviceInfo.mService.disconnect(deviceInfo.mDevice);
                deviceInfo.mService.closeGatt(deviceInfo.mDevice);
            }
        }
    }

    private void displayConnecting() {
        runOnUiThread(new Runnable() {
            public void run() {
                showDialog(CONNECT_DIALOG);
            }
        });
    }

    private void displayDiscovering() {
        runOnUiThread(new Runnable() {
            public void run() {
                showDialog(DISCOVERY_DIALOG);
            }
        });
    }

    private void dismissConnect() {
        runOnUiThread(new Runnable() {
            public void run() {
                if (mConnectDialog != null && mConnectDialog.isShowing()) {
                    dismissDialog(CONNECT_DIALOG);
                }
            }
        });
    }

    private void dismissDiscovery() {
        runOnUiThread(new Runnable() {
            public void run() {
                if (mDiscoveringDialog != null && mDiscoveringDialog .isShowing()) {
                    dismissDialog(DISCOVERY_DIALOG);
                }
            }
        });
    }

    public class SrvConnection implements ServiceConnection {
        BluetoothDevice mDevice;
        SrvConnection(BluetoothDevice mDevice) {
            this.mDevice = mDevice;
        }
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            CommonUtil.myLog("onServiceConnected----------" + mDevice);

            int conn = 0;

            if(mDevice == null) {
                CommonUtil.myLog("머야힝" + componentName);
                return;
            }

            DeviceInfo selectedDevice = getDevice(mDevice.getName());
            if(selectedDevice == null) {CommonUtil.myLog("onServiceConnected : 디바이스가 널이네?"); return;}

            selectedDevice.mService = ((LeService.LocalBinder)service).getService();
            selectedDevice.mService.addListener(new GattListener(selectedDevice.mDevice));
            conn = selectedDevice.mService.getConnectionState(selectedDevice.mDevice);//((LeService.LocalBinder)service).getService().getConnectionState(mDevice);

            CommonUtil.myLog("onServiceConnected 1" + selectedDevice.mService + "/" + conn + "/");
            connectToDevice(selectedDevice.mDevice);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            CommonUtil.myLog("Gatt Service disconnected------");
        }
    }

    public void enableNotification(LeService service, GattCharacteristic mTransTx) {
        // XXX: we did not app this request to queue, so the system call might not work correctly
        // if we send request to fast
        LeService mService = getService(service);
        if(service == null) {CommonUtil.myLog("enableNotification 서비스가 널이네");}

        if(mService != null) {
            boolean set = mService.setCharacteristicNotification(mTransTx, true);
            Log.d("set notification:" + set);

            GattDescriptor dsc = mTransTx.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
            //dsc.setValue(dsc.getConstantBytes(GattDescriptor.ENABLE_NOTIFICATION_VALUE));
            dsc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            boolean success = mService.writeDescriptor(dsc);
            Log.d("writing enable descriptor:" + success);
        }
    }

    public void extractDataFromRawData(String rawData, BluetoothDevice mDevice) {
        if(mDevice == null) {
            return;
        }

        //CommonUtil.myLog("결과로우데이터: " + rawData);
        String[] arrSplitedRawdata = new String[rawData.length()/2];

        for(int i=0; i<rawData.length()/2; i++) {
            arrSplitedRawdata[i] = rawData.substring(i*2, i*2+2);
        }
        makeDataObject(rawData, arrSplitedRawdata, mDevice);
    }

    //바이트 반위로 자른 배열로 데이터 객체 만들자
    void makeDataObject(String rawData, String[] arrSplitedRawdata, BluetoothDevice mDevice) {
        CommonUtil.myLog("---------결과배열: " + arrSplitedRawdata);
//        for(int i=0; i<arrSplitedRawdata.length; i++) {
//            CommonUtil.myLog("결과: " + arrSplitedRawdata[i]);
//        }
//        CommonUtil.myLog("-----------------");

        //enableNotification();
        if(mDevice == null) {
            CommonUtil.myLog("디바이스 널이야1");
            return;
        }

        //필터코드
        if(rawData.startsWith("02010619FFA6044C14")) {
            //DataReceive dataReceive = new DataReceive(arrSplitedRawdata);
                //public DeviceInfo(boolean isHeaderType, String deviceStatus, String deviceName, String deviceId, String temperature, String humidity, String time, String battery, String lat, String lon, String rawData) {

                final DeviceInfo dataReceive = new DeviceInfo(Util.makeStringFromCalendar(Calendar.getInstance(), "yyyy-MM-dd HH:mm"), false,
                        arrSplitedRawdata[9],//deviceStatus
                        mDevice == null ? "-" : mDevice.getName().replaceAll("OPBT", ""),//deviceName
                        mDevice == null ? "-" : mDevice.getAddress(),//deviceId
                    (arrSplitedRawdata[11] + arrSplitedRawdata[10]),//temperature
                    (arrSplitedRawdata[13] + arrSplitedRawdata[12]),//humidity
                        (arrSplitedRawdata[18] + arrSplitedRawdata[17] + arrSplitedRawdata[16] + arrSplitedRawdata[15]),//time
                        arrSplitedRawdata[14],//battery
                        "" +lat,
                        "" +lon,
                    rawData);
                if(mDevice == null) {
                    CommonUtil.myLog("디바이스 널이야2");
                    return;
                }
            //CommonUtil.myLog("여기들어오나요1");

            DeviceInfo selectedDevice = getDevice(mDevice.getName());
            if(selectedDevice == null) {CommonUtil.myLog("onServiceConnected : 디바이스가 널이네?"); return;}

            selectedDevice.setDeviceReadInfo(false,
                    arrSplitedRawdata[9],//deviceStatus
                    mDevice == null ? "-" : mDevice.getName().replaceAll("OPBT", ""),//deviceName
                    mDevice == null ? "-" : mDevice.getAddress(),//deviceId
                    (arrSplitedRawdata[11] + arrSplitedRawdata[10]),//temperature
                    (arrSplitedRawdata[13] + arrSplitedRawdata[12]),//humidity
                    (arrSplitedRawdata[18] + arrSplitedRawdata[17] + arrSplitedRawdata[16] + arrSplitedRawdata[15]),//time
                    arrSplitedRawdata[14],//battery
                    "" +lat,
                    "" +lon,
                    rawData);
            //deviceInfoList1.add(dataReceive);
            //CommonUtil.myLog("여기들어오나요2");

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    CommonUtil.myLog("일분이 지나서 보냄" + dataReceive.toString());
                    sendToServer(dataReceive);
                    adapter.notifyDataSetChanged();
                    //recyclerView1.scrollToPosition(deviceInfoList.size() - 1);
                }
            });
        }
    }

    void sendToServer(DeviceInfo dataReceive) {
        Api.sendData(context, dataReceive, new Api.CallbackSendData() {
            @Override
            public void callback(boolean isSuccess, String message) {
                CommonUtil.myLog("-----결과: " + message);
            }
        });
    }

    DeviceInfo getDevice(String deviceName) {
        DeviceInfo selectedDevice = null;

        for(DeviceInfo deviceInfo : deviceInfoList) {
            if(deviceInfo.mDevice == null) {continue;}
            if(deviceInfo.mDevice.getName().equals(deviceName)) {
                selectedDevice = deviceInfo;
                break;
            }
        }
        return selectedDevice;
    }

    LeService getService(LeService service) {
        LeService selectedService = null;

        for(DeviceInfo deviceInfo : deviceInfoList) {
            if(deviceInfo.mService == null) {continue;}
            if(deviceInfo.mService == service) {
                selectedService = deviceInfo.mService;
                break;
            }
        }
        return selectedService;
    }

    public void removeDevice(String deviceName) {
        for(DeviceInfo deviceInfo : deviceInfoList) {
            if(deviceInfo.mDevice == null) {continue;}
            if(deviceInfo.mDevice.getName().equals(deviceName)) {
                deviceInfoList.remove(deviceInfo);
                break;
            }
        }
        adapter.notifyDataSetChanged();
    }

    public void getAddress() {
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(context, Locale.getDefault());

        try {

            addresses = geocoder. getFromLocation(Double.parseDouble(lat), Double.parseDouble(lon), 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
            String address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
            String city = addresses.get(0).getLocality();
            String state = addresses.get(0).getAdminArea();
            String country = addresses.get(0).getCountryName();
            String postalCode = addresses.get(0).getPostalCode();
            String knownName = addresses.get(0).getFeatureName(); // Only if available else return NULL

            tvAddress.setText(address);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
