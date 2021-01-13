package issc.data;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import co.kr.bnr.bnrtrackerv.ActMainBeacon;
import co.kr.bnr.bnrtrackerv.util.CommonUtil;
import issc.gatt.GattCharacteristic;
import issc.impl.GattTransaction;
import issc.impl.LeService;
import issc.util.Log;
import issc.util.TransactionQueue;

public class DeviceInfo implements TransactionQueue.Consumer<GattTransaction> {
    Context context;
    ActMainBeacon actMainBeacon;

    public String lastSendTime = "";
    public boolean isHeaderType = false;
    public String deviceStatus = "";//0x01 : Sensing(1) / stop(0) ////// 0x10 : Beacon(0) / Storage&Beacon(1) mode /////// 0x80 : memory Full alarm
    public String deviceName = "";
    public String deviceId = "";
    public String temperature = "";
    public String humidity = "";
    public String rawData = "";
    public String time = "";
    public String battery = "";
    public String lat = "";
    public String lon = "";

    public GattTransaction transaction;
    public GattCharacteristic mTransTx;
    public GattCharacteristic mTransRx;
    //public ActMainBeacon.ViewHandler mViewHandler;
    public TransactionQueue mQueue;
    public BluetoothDevice mDevice;
    public ActMainBeacon.SrvConnection mConn;
    public LeService mService;

    public boolean isBeaconMode = false;
    public boolean isDeviceConnected = false;

    public void setConnectInfo(GattTransaction transaction) {
        if(transaction != null) {
            this.transaction = transaction;
        }
    }

    public void setConnectInfo(GattCharacteristic mTransTx, GattCharacteristic mTransRx) {
        if(mTransTx != null) {
            this.mTransTx = mTransTx;
        }

        if(mTransRx != null) {
            this.mTransRx = mTransRx;
        }
    }

//    public void setConnectInfo(ActMainBeacon.ViewHandler mViewHandler) {
//        if(mViewHandler != null) {
//            this.mViewHandler = mViewHandler;
//        }
//    }

    public void setConnectInfo(TransactionQueue mQueue) {
        if(mQueue != null) {
            this.mQueue = mQueue;
        }
    }

    public void setConnectInfo(BluetoothDevice mDevice) {
        if(mDevice != null) {
            this.mDevice = mDevice;
        }
    }

    public void setConnectInfo(ActMainBeacon.SrvConnection mConn) {
        if(mConn != null) {
            this.mConn = mConn;
        }
    }

    public void setConnectInfo(LeService mService) {
        if(mService != null) {
            this.mService = mService;
        }
    }

    public void setDeviceReadInfo(boolean isHeaderType, String deviceStatus, String deviceName, String deviceId, String temperature, String humidity, String time, String battery, String lat, String lon, String rawData) {
        this.isHeaderType = isHeaderType;
        this.deviceStatus = deviceStatus;
        this.deviceName = deviceName;
        this.deviceId = deviceId;
        this.temperature = temperature;
        this.humidity = humidity;
        this.time = time;
        this.battery = battery;
        this.lat = lat;
        this.lon = lon;
        this.rawData = rawData;
    }

    //7E 7E 01 B0 11 00 00 00 80 0B CC 0D 00 BC 67 BA 5D 00 00 00 00 00 00 00 00 7D 7D
    //02010619FF A604 4C 14 11 F608 310B 3804FA0C5E0000000000000000001F0B094F504254313033343230000000000000000000000000000000000000000000
    public DeviceInfo(Context context) {
        this.context = context;
        this.mQueue = new TransactionQueue(this);
        //this.mViewHandler = new ActMainBeacon.ViewHandler(mDevice);
    }

    public DeviceInfo(String lastSendTime, boolean isHeaderType, String deviceStatus, String deviceName, String deviceId, String temperature, String humidity, String time, String battery, String lat, String lon, String rawData) {
        this.lastSendTime = lastSendTime;
        this.isHeaderType = isHeaderType;
        this.deviceStatus = deviceStatus;
        this.deviceName = deviceName;
        this.deviceId = deviceId;
        this.temperature = temperature;
        this.humidity = humidity;
        this.time = time;
        this.battery = battery;
        this.lat = lat;
        this.lon = lon;
        this.rawData = rawData;

//        CommonUtil.myLog("temperature: " + temperature + "/" + getTemperature() + " "
//                +"deviceName: " + deviceName + " "
//                +"humidity: " + humidity + "/" + getHumidity() + " "
//                +"time: " + time + "/" + getTime() + " "
//                +"battery: " + battery + "/" + getBattery() + " "
//                +"lat: " + lat + " "
//                +"lon: " + lon + " "
//                +"rawData: " + rawData);
    }
    //7E 7E 01 B0 11 00 00 00 80 0B CC 0D 00 BC 67 BA 5D 00 00 00 00 00 00 00 00 7D 7D
    public DeviceInfo(String lastSendTime, boolean isHeaderType, String deviceName, String deviceId, String temperature, String humidity, String time, String battery, String lat, String lon, String rawData) {
        this.lastSendTime = lastSendTime;
        this.isHeaderType = isHeaderType;
        this.deviceName = deviceName;
        this.deviceId = deviceId;
        this.temperature = temperature;
        this.humidity = humidity;
        this.time = time;
        this.battery = battery;
        this.lat = lat;
        this.lon = lon;
        this.rawData = rawData;
    }

    @NonNull
    @Override
    public String toString() {
        return "temperature: " + temperature + "/" + getTemperature() + " "
                +"deviceName: " + deviceName + " "
                +"humidity: " + humidity + "/" + getHumidity() + " "
                +"time: " + time + "/" + getTime() + " "
                +"battery: " + battery + "/" + getBattery() + " "
                +"lat: " + lat + " "
                +"lon: " + lon + " "
                +"rawData: " + rawData;
    }

    public String getTemperature() {
        if(temperature.isEmpty()) return "";

        Long i = Long.parseLong(temperature, 16);
        Float f = Float.intBitsToFloat(i.intValue());
        return String.format("%.2f", f);
        //"" + CommonUtil.hexToDecimal(temperature) * 0.01;
    }

    public String getHumidity() {
        if(humidity.isEmpty()) return "";
        Long i = Long.parseLong(humidity, 16);
        Float f = Float.intBitsToFloat(i.intValue());
        return String.format("%.2f", f);
        //return String.format("%.2f", Long.parseLong(humidity, 16));
    }

    public String getTime() {
        String hexString = "" + CommonUtil.hexToDecimal(time);
        //CommonUtil.myLog("변환하기전 스트링: " + hexString);
        hexString = hexString.replaceAll("\\.", "");
        if(hexString.contains("E")) {
            hexString = hexString.split("E")[0];
        }
        long hexToLong = Long.parseLong(hexString);
        //CommonUtil.myLog("변환할 스트링: " + hexToLong);

        long date1 =  hexToLong * 1000;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date dt = new Date(date1);
        return dateFormat.format(dt);
    }

    public String getBattery() {
        return "3." + (int)CommonUtil.hexToDecimal(battery);
    }

    public String getTemperatureString() {
        return temperature + "℃";
    }

    public String getHumidityString() {
        return humidity + "%";
    }

    @Override
    public void onTransact(GattTransaction t) {
        if(t.chr != null) {
            t.chr.setValue(t.value);
            if (t.isWrite) {
                int type = GattCharacteristic.WRITE_TYPE_DEFAULT;
                t.chr.setWriteType(type);
                mService.writeCharacteristic(t.chr);
                CommonUtil.myLog("--------onTransact/"+t.chr);
            } else {
                mService.discoverServices(mDevice);
                mService.readCharacteristic(t.chr);
                CommonUtil.myLog("--------"+t.chr);
            }
        }
    }

    /*
    public class ViewHandler extends Handler {
        BluetoothDevice mDevice;
        ViewHandler(BluetoothDevice mDevice) {
            this.mDevice = mDevice;
        }

        public void handleMessage(Message msg) {
            if(mDevice == null) {
                return;
            }
            Bundle bundle = msg.getData();
            if (bundle == null) {
                Log.d("ViewHandler handled a message without information");
                return;
            }

            int tag = msg.what;
            if (tag == SHOW_CONNECTION_DIALOG) {
                showDialog(CONNECTION_DIALOG);
            }
            else if (tag == CONSUME_TRANSACTION) {
            } else if (tag == APPEND_MESSAGE) {
                CharSequence content = bundle.getCharSequence(INFO_CONTENT);
                if (content != null) {
                    //extractDataFromRawData(String.valueOf(content), mDevice);
                }
            }
        }
    }

    public void updateView(int tag, Bundle info, BluetoothDevice mDevice) {
        if (info == null) {
            info = new Bundle();
        }
        if(mDevice == null) {
            return;
        }

        if(mDevice != null && mDevice.getName().equals(mDevice.getName())) {
            mViewHandler.removeMessages(tag);

            Message msg = mViewHandler.obtainMessage(tag);
            msg.what = tag;
            msg.setData(info);
            mViewHandler.sendMessage(msg);
        }
        // remove previous log since the latest log
        // already contains needed information.;
    }
     */
}
