package co.kr.bnr.bnrtrackerv;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import co.kr.bnr.bnrtrackerv.util.CommonUtil;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * DialBleScan의 스캔된 기기들 담는 어댑터
 */
public class AdapterBleScanDevice extends RecyclerView.Adapter<AdapterBleScanDevice.ViewHolder> {
    Context context;
    ArrayList<BluetoothDevice> arrData;
    ListenerOnItemClick listener;

    public AdapterBleScanDevice(Context context, ArrayList<BluetoothDevice> arrData) {
        this.context = context;
        this.arrData = arrData;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ble_scan, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final BluetoothDevice item = arrData.get(position);

        holder.tvDeviceName.setText(CommonUtil.isEmpty(item.getName()) ? "Unknown" : item.getName());
        holder.tvDeviceAddress.setText(CommonUtil.isEmpty(item.getAddress()) ? "-" : item.getAddress());

        holder.layRoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onClickItem(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return this.arrData.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.itemBleScanLayRoot) LinearLayout layRoot;
        @Bind(R.id.itemBleScanTvDeviceName) TextView tvDeviceName;
        @Bind(R.id.itemBleScanTvDeviceAddress) TextView tvDeviceAddress;

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