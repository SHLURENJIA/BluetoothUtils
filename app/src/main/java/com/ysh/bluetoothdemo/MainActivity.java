package com.ysh.bluetoothdemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.ysh.bluetoothdemo.utils.BluetoothManager;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        DeviceAdapter.OnDeviceClickListener, BluetoothManager.OnConnectListener,
        BluetoothManager.OnReceiveMessageListener, BluetoothManager.OnSearchDevicesListener,
        BluetoothManager.OnSendMessageListener {

    private static final int REQUEST_CODE_PERMISSION = 101;

    private Button mBtnOpen;
    private Button mBtnClose;
    private Button mBtnSearch;
    private RecyclerView mRvMatch;
    private RecyclerView mRvMsg;
    private RecyclerView mRvUnmatch;
    private EditText mEtMsg;
    private Button mBtnSend;
    private Button mBtnDiscovery;

    private List<BluetoothDevice> mPaireList = new ArrayList<>();
    private DeviceAdapter mPaireAdapter;
    private List<BluetoothDevice> mUnpaireList = new ArrayList<>();
    private DeviceAdapter mUnpaireAdapter;
    private List<String> mChatList = new ArrayList<>();
    private MsgAdapter mMsgAdapter;

    private BluetoothManager mBluetoothManager;

    private AcceptThread mAcceptThread;
    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1001:
                    String s = (String) msg.obj;
                    mChatList.add(s);
                    mMsgAdapter.notifyDataSetChanged();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initData();
        initListener();

        //TODO 动态权限
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            //大于23
            int i = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
            if(i != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        REQUEST_CODE_PERMISSION);
            }
        }

        //开启服务器线程
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        mAcceptThread = new AcceptThread(adapter, BluetoothManager.SPP_UUID, mHandler);
        mAcceptThread.start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_CODE_PERMISSION:
                //权限处理
                break;
            default:
                break;
        }
    }

    private void initView() {
        mBtnOpen = (Button) findViewById(R.id.btn_open);
        mBtnClose = (Button) findViewById(R.id.btn_close);
        mBtnSearch = (Button) findViewById(R.id.btn_search);
        mRvMatch = (RecyclerView) findViewById(R.id.rv_match);
        mRvUnmatch = (RecyclerView) findViewById(R.id.rv_unmatch);
        mRvMsg = (RecyclerView) findViewById(R.id.rv_msg);
        mEtMsg = (EditText) findViewById(R.id.et_msg);
        mBtnSend = (Button) findViewById(R.id.btn_send);
        mBtnDiscovery = (Button) findViewById(R.id.btn_discovery);


        mRvMatch.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mRvUnmatch.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mRvMsg.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
    }

    private void initData() {
        mPaireAdapter = new DeviceAdapter(this, mPaireList);
        mRvMatch.setAdapter(mPaireAdapter);
        mUnpaireAdapter = new DeviceAdapter(this, mUnpaireList);
        mRvUnmatch.setAdapter(mUnpaireAdapter);
        mMsgAdapter = new MsgAdapter(this, mChatList);
        mRvMsg.setAdapter(mMsgAdapter);

        mBluetoothManager = BluetoothManager.getInstance(getApplicationContext());
    }

    private void initListener() {
        mBtnOpen.setOnClickListener(this);
        mBtnClose.setOnClickListener(this);
        mBtnSearch.setOnClickListener(this);
        mBtnDiscovery.setOnClickListener(this);
        mBtnSend.setOnClickListener(this);
        mPaireAdapter.setDeviceClickListener(this);
        mUnpaireAdapter.setDeviceClickListener(this);

        mBluetoothManager.setOnConnectListener(this);
        mBluetoothManager.setOnReceiveMessageListener(this);
        mBluetoothManager.setOnSearchDevicesListener(this);
        mBluetoothManager.setOnSendMessageListener(this);
    }

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_open:
                if (mBluetoothManager.openBt()) {
                    Toast.makeText(this, "蓝牙开启成功", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_close:
                mBluetoothManager.closeDevice();
                break;
            case R.id.btn_search:
                mPaireList.clear();
                mPaireList.addAll(mBluetoothManager.searchLocalDevice());
                mPaireAdapter.notifyDataSetChanged();
                mUnpaireList.clear();
                mBluetoothManager.searchUnkonwnDevices();
                break;
            case R.id.btn_discovery:
                mBluetoothManager.setDiscoverable(120);
                break;
            case R.id.btn_send:
                if (mBluetoothManager.isConnect()) {
                    String s = mEtMsg.getText().toString();
                    mBluetoothManager.sendMessage(s);
                    mEtMsg.setText("");
                    mChatList.add("我：" + s);
                    mMsgAdapter.notifyDataSetChanged();
                } else {
                    //没连接，应该是服务器线程
                    String s = mEtMsg.getText().toString();
                    mAcceptThread.write(s);
                    mEtMsg.setText("");
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onConnectClick(BluetoothDevice device) {
        //点击连接设备
        mBluetoothManager.connectDevice(device.getAddress());
    }

    @Override
    public void onNewDeviceFound(BluetoothDevice device) {
        //发现新设备
        if (!mUnpaireList.contains(device)) {
            mUnpaireList.add(device);
            mUnpaireAdapter.notifyDataSetChanged();
        }
    }

    //连接状态
    @Override
    public void onConnectStart() {

    }

    @Override
    public void onConnectFailed(String errorMessage) {
        Toast.makeText(this, "连接失败！", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectSuccess(String mac) {
        Toast.makeText(this, "连接成功", Toast.LENGTH_SHORT).show();
    }

    //接收消息
    @Override
    public void onReceive(String s) {
        mChatList.add("receive:" + s);
        mMsgAdapter.notifyDataSetChanged();
    }

    //发送消息
    @Override
    public void onSuccess() {

    }

    //发送消息失败
    @Override
    public void onFail(String error) {

    }
}
