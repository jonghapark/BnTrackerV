package co.kr.bnr.opbt;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import co.kr.bnr.opbt.util.CommonUtil;
import co.kr.bnr.opbt.util.Util;
import issc.data.DeviceInfo;

public class AdapterConnectedDevice extends RecyclerView.Adapter<AdapterConnectedDevice.ViewHolder> {
    Context context;
    ArrayList<DeviceInfo> arrData;
    ListenerOnItemClick listener;

    public AdapterConnectedDevice(Context context, ArrayList<DeviceInfo> arrData) {
        this.context = context;
        this.arrData = arrData;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_connected_device, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final DeviceInfo item = arrData.get(position);
        //CommonUtil.myLog("onBindViewHolder까진왔어");

        setButtonStatus(item.isBeaconMode, item.isDeviceConnected, holder.tvDeviceName, holder.tvStart, item.mDevice);

        //holder.tvDeviceName.setText(item.mDevice.getName());
        holder.tvTemperature.setText(!item.isHeaderType ? item.getTemperature() : item.temperature);
        holder.tvHumidity.setText(!item.isHeaderType ? item.getHumidity() : item.humidity);
        holder.tvStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                holder.tvStart.setSelected(!holder.tvStart.isSelected());

                if(item.mDevice == null) {
                    Util.myToast(context, "연결이 되어있지 않습니다" + item.mDevice + "/" + item.mDevice.getBondState());return;}
                if(holder.tvStart.isSelected()) {
                    CommonUtil.myLog("시작해1");
                    item.isBeaconMode = true;
                    item.isDeviceConnected = false;
                    //TODO 여기서 이제 로깅 스타트! 하고 스타트 명령 날리고 나서 연결 끊는다
                    //sendCommand(Common.command, mDevice1);
                    item.mService.disconnect(item.mDevice);

                    setButtonStatus(item.isBeaconMode, item.isDeviceConnected, holder.tvDeviceName, holder.tvStart, item.mDevice);
                }
                else {
                    CommonUtil.myLog("그만해1");
                    ((ActMainBeacon)context).removeDevice(item.mDevice.getName());

                    setButtonStatus(item.isBeaconMode, item.isDeviceConnected, holder.tvDeviceName, holder.tvStart, item.mDevice);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return this.arrData.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.tvDeviceName) TextView tvDeviceName;
        @Bind(R.id.tvTemperature) TextView tvTemperature;
        @Bind(R.id.tvHumidity) TextView tvHumidity;
        @Bind(R.id.tvStart) TextView tvStart;

        public ViewHolder(View v) {
            super(v);
            ButterKnife.bind(this, v);
        }
    }

    public interface ListenerOnItemClick {
        void onClickItem(BluetoothDevice item);
    }

    public void setListener(ListenerOnItemClick listener) {
        this.listener = listener;
    }

    void setButtonStatus(final boolean isBeaconMode, final boolean isDeviceConnected, final TextView tvDeviceName, final TextView tvStart, final BluetoothDevice mDevice) {
        ((Activity)context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //tvStart.setEnabled(isDeviceConnected);

                if(isDeviceConnected) {
                    tvDeviceName.setText(mDevice.getName().replaceAll("OPBT", "") + " 기기와 연결되었습니다.");
                    tvStart.setText("측정시작");
                    tvStart.setBackgroundColor(Color.parseColor("#298A08"));
                    tvStart.setEnabled(true);
                    tvStart.setVisibility(View.VISIBLE);
                }
                else {
                    if(isBeaconMode) {
                        tvDeviceName.setText(mDevice.getName().replaceAll("OPBT", "") + " 기기로 측정중입니다.");
                        tvStart.setText("측정종료");
                        tvStart.setBackgroundColor(Color.RED);
                        tvStart.setEnabled(true);
                        tvStart.setVisibility(View.VISIBLE);
                    }
                    else {
                        tvDeviceName.setText("연결되지않았습니다");
                        tvStart.setText("Not Connected");
                        tvStart.setVisibility(View.INVISIBLE);
                    }
                }
            }
        });
    }
}