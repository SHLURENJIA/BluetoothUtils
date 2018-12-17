package com.ysh.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
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
public class MsgAdapter extends RecyclerView.Adapter<MsgAdapter.MsgHolder>{
    private Context mContext;
    private List<String> mMsgList;
    /**
     * 0-device
     * 1-string
     */
    private int type;
    public MsgAdapter(Context context, List<String> list){
        mContext = context;
        mMsgList = list;


    }

    public void addList(List<String> list){
        mMsgList.clear();
        mMsgList.addAll(list);
    }

    @NonNull
    @Override
    public MsgHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.msg_item,viewGroup,false);
        return new MsgHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MsgHolder msgHolder, int i) {
        msgHolder.mTvMsg.setText(mMsgList.get(i));

    }

    @Override
    public int getItemCount() {
        return mMsgList.size();
    }

    class MsgHolder extends RecyclerView.ViewHolder{
        private TextView mTvMsg;

        public MsgHolder(@NonNull View itemView) {
            super(itemView);
            mTvMsg = (TextView) itemView.findViewById(R.id.tv_msg);
        }
    }

}
