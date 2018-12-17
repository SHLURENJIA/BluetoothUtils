package com.ysh.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Keep;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;


/**
 * 作者：created by @author{ YSH } on 2018/12/12
 * 描述：蓝牙服务器端线程，监听设备连接
 * 修改备注：
 */
public class AcceptThread extends Thread {

    private final BluetoothServerSocket mBtServerSocket;
    private BluetoothSocket mBtSocket;
    private InputStream mInputStream;
    private OutputStream mOutputStream;
    private boolean isCanAccept;
    private boolean isCanRecv;
    private Handler mHandler;

    public AcceptThread(BluetoothAdapter adapter, String uuid, Handler handler) {
        BluetoothServerSocket tmp = null;
        mHandler = handler;

        try {
            tmp = adapter.listenUsingInsecureRfcommWithServiceRecord("Test Server", UUID.fromString(uuid));
//            tmp = (BluetoothServerSocket) adapter.getClass().getMethod("listenUsingInsecureRfcommWithServiceRecord", new Class[]{String.class, UUID.class})
//                    .invoke(adapter, "test", UUID.fromString(uuid));
            //这个可以
//            tmp = (BluetoothServerSocket) adapter.getClass().getMethod("listenUsingRfcommWithServiceRecord", new Class[]{String.class, UUID.class})
//                    .invoke(adapter,"test",UUID.fromString(uuid));
            //强行绑定端口
//            tmp = (BluetoothServerSocket) adapter.getClass().getMethod("listenUsingRfcommOn", new Class[]{int.class})
//                    .invoke(adapter, new Object[]{1});
        }
        /*catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } */
        catch (IOException e) {
            e.printStackTrace();
        }

        mBtServerSocket = tmp;
    }

    @Override
    public void run() {
        try {
            if (mBtServerSocket != null) {
                //阻塞等待
                mBtSocket = mBtServerSocket.accept();
                sendHandlerMsg("服务器收到连接");
                mBtServerSocket.close();
                mInputStream = mBtSocket.getInputStream();
                mOutputStream = mBtSocket.getOutputStream();
                mOutputStream.write("服务器收到连接".getBytes());
                byte[] buffer = new byte[1024];  // buffer store for the stream
                int bytes; // bytes returned from read()
//             Keep listening to the InputStream until an exception occurs
                while (true) {
                    try {
                        // Read from the InputStream
                        bytes = mInputStream.read(buffer);
                        // Send the obtained bytes to the UI activity
                        String s = new String(buffer, 0, bytes);
                        sendHandlerMsg(s);

                    } catch (IOException e) {
                        break;
                    }
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void sendHandlerMsg(String content) {

        Message msg = mHandler.obtainMessage();
        msg.what = 1001;
        msg.obj = content;
        mHandler.sendMessage(msg);

    }

    public void cancel() {

        try {
            mBtServerSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void write(String msg) {
        try {
            if (mOutputStream != null) {
                mOutputStream.write(msg.getBytes());
                sendHandlerMsg("服务器："+msg);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
