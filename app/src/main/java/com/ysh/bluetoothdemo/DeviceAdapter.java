package com.ysh.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

/**
 * 作者：created by @author{ YSH } on 2018/12/12
 * 描述：
 * 修改备注：
 */
public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.MsgHolder>{
    private Context mContext;
    private List<BluetoothDevice> mDeviceList;
    private OnDeviceClickListener mListener;
    public DeviceAdapter(Context context, List<BluetoothDevice> list){
        mContext = context;
        mDeviceList = list;


    }

    public void setDeviceClickListener(OnDeviceClickListener listener) {
        this.mListener = listener;
    }

    public void addList(List<BluetoothDevice> list){
        mDeviceList.clear();
        mDeviceList.addAll(list);
    }

    @NonNull
    @Override
    public MsgHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.msg_item,viewGroup,false);
        return new MsgHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MsgHolder msgHolder, int i) {
        final BluetoothDevice device = mDeviceList.get(i);
        msgHolder.mTvMsg.setText(device.getName()+" "+device.getAddress());

        msgHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mListener != null){
                    mListener.onConnectClick(device);
                }
            }
        });

    }

    @Override
    public int getItemCount() {
        return mDeviceList.size();
    }

    class MsgHolder extends RecyclerView.ViewHolder{
        private TextView mTvMsg;

        public MsgHolder(@NonNull View itemView) {
            super(itemView);
            mTvMsg = (TextView) itemView.findViewById(R.id.tv_msg);
        }
    }

    public interface OnDeviceClickListener {
        void onConnectClick(BluetoothDevice device);
    }

}
