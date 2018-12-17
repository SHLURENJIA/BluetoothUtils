package com.ysh.bluetooth.bluetoothtools;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 作者：created by @author{ YSH } on 2018/12/14
 * 描述：
 * 修改备注：
 */
public class BluetoothManager {
    private static final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
    private static final int DEFAULT_BUFFER_SIZE = 1024;
    private static volatile BluetoothManager mBtManager;
    private Context mContext;
    private BluetoothAdapter mBtAdapter;
    private BluetoothSocket mSocket;

    private volatile boolean mWritable = true;
    private volatile boolean mReadable = true;
    private InputStream mInputStream;
    private OutputStream mOutputStream;
    private ReadRunnable mReadRunnable;
    //TODO 手动创建线程池
    private ExecutorService mExecutorService = Executors.newFixedThreadPool(3);
    private Handler mHandler = new Handler(Looper.getMainLooper());

    private OnSearchDevicesListener mSearchListener;
    private OnConnectListener mConnectListener;
    private OnReceiveMessageListener mReceiveMessageListener;
    private OnSendMessageListener mSendMessageListener;

    //注意请在act中注册注销
    private final BroadcastReceiver mBtDeviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //拿到device可进行操作
                if (mSearchListener != null) {
                    mSearchListener.onNewDeviceFound(device);
                }
            }
        }
    };

    /**
     * @param context applicationcontext
     * @return
     */
    public static BluetoothManager getInstance(Context context) {
        if (mBtManager == null) {
            synchronized (BluetoothManager.class) {
                if (mBtManager == null) {
                    mBtManager = new BluetoothManager(context);
                }
            }
        }
        return mBtManager;
    }

    private BluetoothManager(Context context) {
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        mContext = context;
        //注册发现蓝牙设备结果监听广播
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        mContext.registerReceiver(mBtDeviceReceiver, filter);
    }

    public void setOnSearchDevicesListener(OnSearchDevicesListener listener) {
        this.mSearchListener = listener;
    }

    public void setOnConnectListener(OnConnectListener listener) {
        this.mConnectListener = listener;
    }

    public void setOnReceiveMessageListener(OnReceiveMessageListener listener) {
        this.mReceiveMessageListener = listener;
    }

    public void setOnSendMessageListener(OnSendMessageListener listener) {
        this.mSendMessageListener = listener;
    }

    /**
     * 检测设备支持蓝牙
     */
    public boolean checkBtEnable() {
        if (mBtAdapter == null) {
            return false;
        }
        return false;
    }

    /**
     * 开启蓝牙
     * test pass
     */
    public boolean openBt() {
        if (!mBtAdapter.isEnabled()) {
            return mBtAdapter.enable();
        }
        return true;
    }

    /**
     * 设置蓝牙设备可被检测
     * 默认120秒可见
     * 0-表示始终可见
     *
     * @param seconds [0-3600]
     */
    public void setDiscoverable(int seconds) {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, seconds);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    /**
     * 搜索本地已经配对的设备
     * test pass
     */
    public List<BluetoothDevice> searchLocalDevice() {
        Set<BluetoothDevice> devices = mBtAdapter.getBondedDevices();
        List<BluetoothDevice> list = new ArrayList<BluetoothDevice>();
        for (BluetoothDevice deivce : devices) {
            list.add(deivce);
        }
        return list;
    }

    /**
     * 搜索未知设备
     * test pass
     */
    public boolean searchUnkonwnDevices() {
        //TODO 是否会出现多次register导致的bug？
        return mBtAdapter.startDiscovery();
    }

    public void unregisterReveiver() {
        mContext.unregisterReceiver(mBtDeviceReceiver);
    }

    public void connectDevice(String mac) {
        if (mac == null || TextUtils.isEmpty(mac)) {
            throw new IllegalArgumentException("mac address is null or empty!");
        }
        if (!BluetoothAdapter.checkBluetoothAddress(mac)) {
            throw new IllegalArgumentException("mac address is not correct! make sure it's upper case!");
        }
        if (mReadable == false) {
            mReadable = true;
        }
        if (mWritable == false) {
            mWritable = true;
        }
        if (mConnectListener != null) {
            mConnectListener.onConnectStart();
            ConnectDeviceRunnable connectDeviceRunnable = new ConnectDeviceRunnable(mac);
            mExecutorService.submit(connectDeviceRunnable);

        }

    }

    public void sendMessage(String s) {
        if(mSendMessageListener == null){
            throw new NullPointerException("OnSendMessageListener can not be null");
        }
        if(mSocket.isConnected()){
            try {
                mOutputStream.write(s.getBytes());
                mSendMessageListener.onSuccess();
            } catch (IOException e) {
                mSendMessageListener.onFail(e.getMessage());
                e.printStackTrace();
            }
        } else {
            mSendMessageListener.onFail("连接已断开");
        }
    }

    /**
     * 连接蓝牙，会存在断开连接后另外一个设备不知道已经断开，端口绑定，导致连接不上的情况
     */
    private class ConnectDeviceRunnable implements Runnable {
        private String mac;

        public ConnectDeviceRunnable(String mac) {
            this.mac = mac;
        }

        @Override
        public void run() {
            if (mConnectListener == null) {
                throw new NullPointerException("OnConnectListener can't be null");
            }
            try {
                BluetoothDevice device = mBtAdapter.getRemoteDevice(mac);
                mBtAdapter.cancelDiscovery();
                mSocket = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString(SPP_UUID));
                mConnectListener.onConnectting();
                mSocket.connect();
                mInputStream = mSocket.getInputStream();
                mOutputStream = mSocket.getOutputStream();
                if(mReadRunnable == null){
                    mReadRunnable = new ReadRunnable();
                    mExecutorService.submit(mReadRunnable);
                }
                mConnectListener.onConnectSuccess(mac);
            } catch (IOException e) {
                e.printStackTrace();
                mConnectListener.onConnectFailed(e.getMessage());
                try {
                    mInputStream.close();
                    mOutputStream.close();
                } catch (Exception closeException) {
                    closeException.printStackTrace();
                }
            }
        }
    }

    /**
     * 蓝牙读取流线程
     * test pass
     */
    private class ReadRunnable implements Runnable {

        @Override
        public void run() {
            if (mReceiveMessageListener == null) {
                throw new RuntimeException("OnReceiveMessageListener can't be null");
            }
            InputStream input = mInputStream;
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int bytes = 0;
            while (mReadable) {
                try {
                    bytes = input.read(buffer);
                    //TODO 数据更为复杂的处理
                    final String s = new String(buffer, 0, bytes);
                    //接收消息的回调如果不在主线程中处理似乎会造成阻塞导致接收不到下一次信息
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mReceiveMessageListener.onReceive(s);
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    /**
     * 搜索蓝牙设备监听
     */
    public interface OnSearchDevicesListener {
        void onNewDeviceFound(BluetoothDevice device);
    }

    /**
     * 蓝牙连接监听
     */
    public interface OnConnectListener {
        void onConnectStart();

        void onConnectting();

        void onConnectFailed(String errorMessage);

        void onConnectSuccess(String mac);
    }

    /**
     * 接收消息监听
     */
    public interface OnReceiveMessageListener {
        void onReceive(String s);
    }

    /**
     * 消息发送监听
     */
    public interface OnSendMessageListener {
        void onSuccess();

        void onFail(String error);
    }
}
