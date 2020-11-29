package co.kr.bnr.bnrtrackerv;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import co.kr.bnr.bnrtrackerv.util.Common;
import co.kr.bnr.bnrtrackerv.util.CommonUtil;
import issc.data.DeviceInfo;

public class AdapterDeviceData extends RecyclerView.Adapter<AdapterDeviceData.ViewHolder> {
    Context context;
    ArrayList<DeviceInfo> arrData;
    ListenerOnItemClick listener;

    public AdapterDeviceData(Context context, ArrayList<DeviceInfo> arrData) {
        this.context = context;
        this.arrData = arrData;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_device_data, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final DeviceInfo item = arrData.get(position);

        holder.tvDeviceId.setText(item.deviceName);
        holder.tvTemperature.setText(!item.isHeaderType ? item.getTemperature() : item.temperature);
        holder.tvHumidity.setText(!item.isHeaderType ? item.getHumidity() : item.humidity);
        holder.tvRawData.setText(item.rawData);
        holder.tvRawData.setVisibility(item.rawData.isEmpty() ? View.GONE : View.VISIBLE);
        holder.tvRawData.setVisibility(Common.isShowRawData ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {
        return this.arrData.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.tvDeviceId) TextView tvDeviceId;
        @Bind(R.id.tvTemperature) TextView tvTemperature;
        @Bind(R.id.tvHumidity) TextView tvHumidity;
        @Bind(R.id.tvRawData) TextView tvRawData;

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
}