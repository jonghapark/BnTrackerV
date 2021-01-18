package co.kr.bnr.opbt;

import android.support.v4.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.wang.avi.AVLoadingIndicatorView;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import co.kr.bnr.opbt.util.CommonUtil;
import issc.data.DeviceInfo;

/**
 * 주변 블투 기기 검색하고 스캔된 기기들을 보여주고 선택하는 다이얼로그
 */
public class DialBleScan extends AppCompatDialogFragment {
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private String resultName;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;// 한번에 스캔하는 시간. 밀리세컨드 단위

    @Bind(R.id.dialBleScanRv) RecyclerView rv;
    @Bind(R.id.dialBleScanBtnReScan) Button btnReScan;
    @Bind(R.id.dialBleScanLodingView) AVLoadingIndicatorView lodingView;

    private Listener listener;

    private ArrayList<BluetoothDevice> scanDeviceList = new ArrayList<>();
    private ArrayList<String> arrBleAddress = new ArrayList<>();
    AdapterBleScanDevice adapter;
    LinearLayoutManager linearLayoutManager;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dial_ble_scan, null);
        ButterKnife.bind(this, view);

        initView();
        initBle();

        return view;
    }

    void initView() {
        linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        rv.setHasFixedSize(true);
        rv.setLayoutManager(linearLayoutManager);
        rv.setNestedScrollingEnabled(false);//스크롤뷰안에 리싸이클러뷰가 버벅일때 해결법

        adapter = new AdapterBleScanDevice(getActivity(), scanDeviceList);
        adapter.setListener(new AdapterBleScanDevice.ListenerOnItemClick() {
            @Override
            public void onClickItem(BluetoothDevice item) {
                listener.onDeviceSelect(item);
                dismiss();
            }
        });
        rv.setAdapter(adapter);
    }

    void initBle() {
        mHandler = new Handler();
        final BluetoothManager bluetoothManager = (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        scanLeDevice(true);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    btnReScan.setEnabled(true);
                    lodingView.setVisibility(View.GONE);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            btnReScan.setEnabled(false);
            lodingView.setVisibility(View.VISIBLE);
        }
        else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            btnReScan.setEnabled(true);
            lodingView.setVisibility(View.GONE);
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, final byte[] scanRecord) {
                    if(isAdded()) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                //CommonUtil.myLog("찍어: " + CommonUtil.byteArrayToHexString(scanRecord));

                                //없는거만 넣는다
                                if(!addedDeviceNameList.contains(device.getName()) && !scanDeviceList.contains(device) && device.getName() != null && device.getName().startsWith("OPBT") && !arrBleAddress.contains(device.getAddress())) {

                                    CommonUtil.myLog("device: " + device.getName());
                                    CommonUtil.myLog("device: " + device.getAddress());
                                    scanDeviceList.add(device);
                                    arrBleAddress.add(device.getAddress());

                                    adapter.notifyDataSetChanged();
                                }
                            }
                        });
                    }
                }
            };

    public void showDialog(FragmentManager fragmentManager) {
        show(fragmentManager, "DialBleScan");
    }

    //취소
    @OnClick(R.id.dialBleScanBtnCancel)
    void onClickCancel() {
        dismiss();
    }

    //다시 스캔
    @OnClick(R.id.dialBleScanBtnReScan)
    void onClickReScan() {
        scanDeviceList.clear();
        arrBleAddress.clear();
        adapter.notifyDataSetChanged();

        scanLeDevice(true);
    }

    ArrayList addedDeviceNameList = new ArrayList();

    public DialBleScan setListener(ArrayList<DeviceInfo> deviceInfoList , Listener listener) {
        this.listener = listener;

        for(DeviceInfo deviceInfo : deviceInfoList) {
            if(deviceInfo.mDevice == null) {continue;}
            addedDeviceNameList.add(deviceInfo.mDevice.getName());
        }
        return this;
    }

    public static DialBleScan newInstance() {
        DialBleScan dialBleScan = new DialBleScan();
        dialBleScan.setStyle(AppCompatDialogFragment.STYLE_NO_TITLE, 0);
        return dialBleScan;
    }

    public interface Listener {
        void onDeviceSelect(BluetoothDevice device);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        scanLeDevice(false);
        mHandler = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }
}
